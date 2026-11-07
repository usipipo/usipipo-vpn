# Auditoría de seguridad — uSipipo VPN

**Fecha:** 2026-11-07
**Alcance:** rama `feat/seguridad-plataforma` (revisión de código estático, más los hallazgos de la fase 6 encontrados por revisión de código con subagentes)
**Método:** revisión de fuente con dos agentes de caza en paralelo, disciplina de frontera de confianza + evidencia + fix mínimo (skill `security-audit` en modo *guidance*)
**Verificado en dispositivo:** no. Esta máquina no tiene JDK ni Android SDK: **la compilación y los tests los verifica la CI de GitHub**. Todo lo que depende del comportamiento real en dispositivo está marcado como `needs-validation`.

## Resumen

| # | Hallazgo | Gravedad | Estado |
|---|---|---|---|
| 1 | Acción privada del widget en receptor exportado | medium | **corregido** (PR #24, merge `ccac5ac`) |
| 2 | Lectura sin tope del zip importado | medium | **corregido** (PR #25, merge `f7b7faa`) |
| 3 | Controles de seguridad restaurados desde la copia | low | **corregido** (PR #26, merge `be10997`) |
| 4 | Sin mínimo de fortaleza de contraseña | low | **corregido** (PR #25) |
| 5 | `EXTRA_SHOW_DETAILS` sin validar en actividad exportada | low | abierto |
| 6 | `allowBackup` y extracción de datos | — | cerrado por diseño (`allowBackup="false"`) |
| 7 | Contenido de la notificación en pantalla de bloqueo | — | `needs-validation` |

## 1 · Acción privada en receptor exportado (medium, corregido)

**Frontera.** `UsipipoWidgetProvider` estaba `exported="true"` (el sistema necesita entregarle `APPWIDGET_UPDATE`) **y** atendía `ACTION_TOGGLE`, una acción privada de la app. Un receptor exportado que atiende una acción privada se alcanza con una emisión implícita: otra app podía ejecutar `toggle()` y conectar o desconectar el túnel sin consentimiento.

**Arreglo.** La acción se movió a `UsipipoWidgetActionReceiver`, sin `intent-filter` y con `exported="false"`, alcanzable solo por el `PendingIntent` de la app, que nombra el componente. El receptor del widget se queda como estaba: hay una prueba (`SystemIntegrationResourceTest`) que fija `exported="true"` a propósito.

**Regresión.** Dos pruebas de contrato: la acción privada no puede volver al receptor exportado, y el receptor nuevo está cerrado y sin filtro.

## 2 · Lectura sin tope del zip importado (medium, corregido)

**Frontera.** El fichero de la copia lo elige el usuario en el selector del sistema, así que su contenido no es de fianza. `BackupReader` leía cada entrada con `readBytes()` sin tope, y la ruta metía el fichero entero en memoria antes de detectar el formato.

**Consecuencia.** Una entrada muy comprimida podía agotar la memoria del proceso. Además, N entradas diminutas implicaban N derivaciones de clave de 100 000 iteraciones *antes* de comprobar la contraseña.

**Arreglo.** Tope por entrada (1 MiB, holgado para un `.conf` cifrado) y tope de entradas (512). Los dos inyectables, con tests.

## 3 · Controles de seguridad restaurados desde la copia (low, corregido)

**Frontera.** `BackupSettings.restore` escribía todas las claves reconocidas. Quien conoce la contraseña de una copia podía apagar en silencio el biométrico, repoblar las apps excluidas del túnel o silenciar los avisos.

**Arreglo.** Seis claves quedan fuera de la restauración: `biometric_enabled`, `excluded_apps`, `notifications_enabled`, `notification_traffic`, `notification_disconnect`, `notification_updates`.

**Alternativa descartada.** Enseñar la diferencia de ajustes antes de aplicarla: es cambio de UI y el hueco se cierra igual sin él.

## 4 · Sin mínimo de fortaleza de contraseña (low, corregido)

La exportación solo rechazaba la cadena vacía. Se piden 8 caracteres: la copia vive donde el usuario la guarde y se puede romper sin conexión.

## 5 · `EXTRA_SHOW_DETAILS` sin validar (low, abierto)

`MainActivity` está exportado y lee el booleano `EXTRA_SHOW_DETAILS` sin validar. Otra app puede forzar el panel de Detalles del perfil activo. No hay escalada de privilegios ni fuga de datos.

**Arreglo propuesto:** actividad dedicada no exportada para ese extra, como ya se hizo con la acción del widget. Pendiente de decidir.

## Needs-validation (sin gravedad asignada)

| Hecho que falta | Cómo validarlo |
|---|---|
| Resultado exacto del fallo de memoria (¿`OutOfMemoryError` o solo lentitud?) | Copia con una entrada de 200 MB en un dispositivo de gama baja |
| Tapjacking sobre la burbuja de tráfico | Servicio de accesibilidad del sistema sobre la burbuja; la ventana consume solo toques dentro de sus límites y falsificar `MotionEvent` exige `INJECT_EVENTS` (permiso `signature\|privileged`) |
| Visibilidad del contenido en pantalla de bloqueo | No hay `setVisibility` ni `lockscreenVisibility` en ningún sitio; la notificación muestra nombre de perfil, velocidades y tiempo, sin claves ni IP |
| Que ningún launcher de OEM rompa si el receptor del widget pasara a `exported="false"` | Prueba en dispositivos reales (hoy no se cambió) |

## Cumplimiento (no verificado por herramienta)

 Este proyecto persigue el estándar de facto para apps móviles, **OWASP MASVS/MASVS-RESILIENCE** (Mobile Application Security Verification Standard), cuya estructura se verificó con Context7 contra la librería OWASP. **No se verificó contra el texto literal de ninguna norma ISO**: Context7 indexa documentación de librerías, no catálogos de normas, y las normas ISO son de pago en iso.org. El mapeo que se persigue, a título indicativo y **sin verificar**, es:

| Objetivo de esta app | Norma indicativa |
|---|---|
| Cifrar los `.conf` en reposo con el Keystore | ISO/IEC 27001 · 27002 (A.10 controles criptográficos) |
| Cifrar las copias exportables | ISO/IEC 27034 (seguridad de aplicaciones) |
| Minimizar datos personales (IP del servidor fuera del dispositivo) | ISO/IEC 27701 · 29100 (privacidad) |
| Dispositivo personal | ETSI EN 303 645 |

**MASVS-RESILIENCE queda fuera de alcance a propósito:** detección de root, emulador, Play Integrity y ofuscación. Son controles caros que no aportan a una app que no guarda credenciales de servidor, y complican el release.

## Lo que ya cumple (verificado en código)

| Control | Dónde |
|---|---|
| Datos cifrados en reposo con clave no exportable | `data/Crypto.kt`, `EncryptedFileConfigStore.kt` |
| `allowBackup="false"` | `AndroidManifest.xml` |
| Registro sin claves ni datos sensibles | `diagnostics/LogRedactor.kt` |
| Copia con AES-GCM + PBKDF2-HMAC-SHA256, sal e IV por archivo | `backup/BackupCrypto.kt` |
| `PendingIntent` inmutables y con componente explícito | widget, servicio del túnel, notificaciones |
| Mosaico protegido por `BIND_QUICK_SETTINGS_TILE` | `AndroidManifest.xml` |
| Servicios y actividades auxiliares `exported="false"` | `AndroidManifest.xml` |
| Zip-slip inalcanzable (las entradas nunca tocan el disco) | `backup/BackupReader.kt` |
| Exportación `.conf` en claro tras aviso de claves privadas | `ui/settings/backup/BackupScreen.kt` |
| App sin WebView | — |
| Contraseña nunca registrada ni persistida | `backup/` |

## Regla que salió de aquí

Los tres fallos que más ciclos de CI quemaron en este proyecto (un import de `kotlinx.coroutines` en vez de `kotlin.coroutines`, una anotación que no existía y una caché que nunca existió) motivaron el paso 7 de `AGENTS.md`: **no escribir un símbolo que no se haya visto en una fuente real**.
