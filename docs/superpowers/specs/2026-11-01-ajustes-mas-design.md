# Spec de diseño — Ajustes: Más

**Fecha:** 2026-11-01
**Estado:** pendiente de revisión del usuario
**Proyecto:** uSipipo VPN
**Alcance:** Pantalla "Más" dentro del hub de Ajustes con 4 filas: Reportar un problema, Invitar amigos, Donar al proyecto, Acerca de.

---

## 1. Objetivo

Que el usuario acceda desde un solo lugar a acciones secundarias: reportar un problema (diagnóstico completo), invitar amigos (compartir enlace), donar al proyecto (dirección cripto con copia al portapapeles) y ver información de la app (versión, atribución, soporte, licencia, enlaces).

| Comportamiento | Antes | Después |
|---|---|---|
| Reportar un problema | `DiagnosticsScreen` accesible desde la pestaña Diagnóstico | Fila en Más que abre `DiagnosticsScreen` intacta con título "Reportar un problema" |
| Invitar amigos | No existe | Compartir texto + enlace mediante `Intent.ACTION_SEND` con chooser del sistema |
| Donar al proyecto | No existe | Mostrar BTC address + Lightning address con botón de copiar al portapapeles; desactivado si la dirección está vacía |
| Acerca de | `AboutSection` dentro de DiagnosticsScreen | Fila en Más con logo, versión, atribución IPinfo, correo de soporte, licencia y enlaces |

---

## 2. Decisiones

| Tema | Decisión | Por qué |
|---|---|---|
| Ubicación del código | `ui/settings/more/MoreScreen.kt` + `MoreViewModel.kt` | Co-locación según `AGENTS.md` |
| Reportar un problema | Reutiliza `DiagnosticsScreen` tal cual; solo cambia el título visible a "Reportar un problema" y añade cabecera con flecha de vuelta | El usuario pidió que el diagnóstico se mueva intacto; no se rehace |
| Invitar amigos | `Intent.ACTION_SEND` con texto y enlace `https://github.com/usipipo/usipipo-vpn/releases/latest` | Estándar de Android para compartir; el chooser deja elegir la app |
| Donar al proyecto | Strings `donate_btc_address` y `donate_lightning_address` como placeholders marcados PENDIENTE; botón copiar al portapapeles; fila desactivada si ambas direcciones están vacías | No se inventan direcciones cripto; el mantenedor debe completar los valores reales |
| Acerca de | Logo (`AppLogo`), versión (`getPackageInfo`), atribución IPinfo, correo de soporte (`mailto:`), licencia y enlaces (repositorio, issues) | Reutiliza `AboutSection` existente; añade licencia y enlaces |
| LICENSE | No existe archivo `LICENSE` en la raíz del repositorio; se documenta como pendiente de verificación por el mantenedor | No se inventa un nombre de licencia; se señala explícitamente |
| Portapapeles | `ClipboardManager.setPrimaryClip()` para copiar la dirección de donación | API estándar de Android; no requiere permisos |

---

## 3. Requisitos funcionales

- **RF-1** — La pantalla muestra 4 filas: Reportar un problema, Invitar amigos, Donar al proyecto, Acerca de.
- **RF-2** — "Reportar un problema" abre `DiagnosticsScreen` completa e intacta (información del dispositivo, modo detallado, registro, enviar informe, borrar registro) con título "Reportar un problema" y cabecera con flecha de vuelta.
- **RF-3** — "Invitar amigos" abre el chooser del sistema con `Intent.ACTION_SEND` que incluye texto y el enlace `https://github.com/usipipo/usipipo-vpn/releases/latest`.
- **RF-4** — "Donar al proyecto" muestra BTC address y Lightning address (strings `donate_btc_address`, `donate_lightning_address`); si una dirección está vacía, su fila se desactiva; botón de copiar al portapapeles para cada dirección.
- **RF-5** — "Acerca de" muestra logo, versión actual, atribución a IPinfo, correo de soporte, nombre de la licencia y enlaces a repositorio e issues.
- **RF-6** — Las direcciones de donación son placeholders marcados explícitamente como "PENDIENTE: valor real, debe completar el mantenedor".

---

## 4. Interfaz

### Qué se ve

- Cabecera con flecha de vuelta y título "Más".
- 4 filas con icono, título y descri
pcion opcional:
  1. Reportar un problema — icono de reporte/bandera
  2. Invitar amigos — icono de compartir
  3. Donar al proyecto — icono de moneda/corazón
  4. Acerca de — icono de información

### Acciones

- Tocar "Reportar un problema" → navegar a `DiagnosticsScreen` con título "Reportar un problema".
- Tocar "Invitar amigos" → `Intent.createChooser(Intent.ACTION_SEND, ...)` con texto + enlace.
- Tocar "Copiar" (en fila de donación) → `ClipboardManager.setPrimaryClip()` con la dirección; snackbar de confirmación.
- Tocar "Acerca de" → navegar a pantalla de Acerca de (o mostrar en línea si es simple).

### Estados

- **Dirección vacía:** la fila de donación correspondiente se muestra desactivada (sin botón de copiar).
- **Portapapeles:** tras copiar, snackbar breve "Dirección copiada".
- **Error:** si no hay app para compartir, snackbar con el error (manejado con `ActivityNotFoundException` como en `ReportSender.emailIntent`).

---

## 5. Datos y ajustes

### Claves en AppSettings

No se añaden claves nuevas a `AppSettings`. Todo es de solo lectura (strings, versión, enlaces).

### Modelo

`MoreUiState` (en `MoreViewModel`):
- `btcAddress: String` (de `R.string.donate_btc_address`)
- `lightningAddress: String` (de `R.string.donate_lightning_address`)
- `appVersion: String`

### Quién escribe

- `MoreViewModel` lee los strings de recursos y la versión de `packageManager`. No escribe nada.

---

## 6. Permisos

| Permiso | Tipo | Cuándo se pide | Si se deniega |
|---|---|---|---|
| Ninguno | — | N/A | N/A |

No se requieren permisos de runtime. `ACTION_SEND` usa el chooser del sistema; el portapapeles no requiere permiso.

---

## 7. Integración con el código existente

### Ficheros tocados

| Fichero | Cambio |
|---|---|
| `app/src/main/java/com/usipipo/vpn/ui/settings/more/MoreScreen.kt` | **Nuevo**: pantalla Más con 4 filas |
| `app/src/main/java/com/usipipo/vpn/ui/settings/more/MoreViewModel.kt` | **Nuevo**: ViewModel con datos de lectura |
| `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt` | Añadir ruta `SettingsRoute.More` y pantalla `MoreScreen` |
| `app/src/main/java/com/usipipo/vpn/ui/settings/SettingsRoute.kt` | Añadir `More` al enum |
| `app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt` | Se reutiliza sin cambios de lógica; solo se le pasa un título diferente o se envuelve con cabecera "Reportar un problema" |
| `app/src/main/res/values/strings.xml` | Añadir `donate_btc_address`, `donate_lightning_address`, `more_*`, `about_*` strings nuevos |

### APIs del SDK

- `android.content.Intent(Intent.ACTION_SEND)` — compartir texto + enlace
- `android.content.Intent.createChooser()` — chooser del sistema
- `android.content.ClipboardManager` — copiar al portapapeles (`getSystemService(CLIPBOARD_SERVICE)`)
- `android.content.Context.packageManager.getPackageInfo(...)` — versión actual (misma que `AboutSection.appVersion()`)
- `android.net.Uri.parse("https://github.com/usipipo/usipipo-vpn/releases/latest")` — enlace de GitHub (proporcionado por el usuario)
- `android.content.Intent(Intent.ACTION_VIEW, uri)` — abrir enlace en navegador
- `android.content.Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:..."))` — correo de soporte (reutiliza `AboutSection.openSupportEmail()`)

### Strings nuevos (en `strings.xml`)

| Clave | Valor (ejemplo) | Nota |
|---|---|---|
| `more_title` | "Más" | Título de la pantalla |
| `more_report_issue` | "Reportar un problema" | Fila 1 |
| `more_invite_friends` | "Invitar amigos" | Fila 2 |
| `more_donate` | "Donar al proyecto" | Fila 3 |
| `more_about` | "Acerca de" | Fila 4 |
| `donate_btc_address` | "PENDIENTE: dirección BTC" | Placeholder, debe completar el mantenedor |
| `donate_lightning_address` | "PENDIENTE: dirección Lightning" | Placeholder, debe completar el mantenedor |
| `donate_copy` | "Copiar dirección" | Botón de portapapeles |
| `donate_copied` | "Dirección copiada" | Snackbar de confirmación |

---

## 8. Criterios de aceptación

1. La pantalla Más muestra exactamente 4 filas en el orden indicado.
2. "Reportar un problema" abre `DiagnosticsScreen` completa con título "Reportar un problema" y flecha de vuelta.
3. "Invitar amigos" abre el chooser del sistema con el enlace `https://github.com/usipipo/usipipo-vpn/releases/latest`.
4. "Donar al proyecto" muestra BTC address y Lightning address; si una está vacía, su fila está desactivada.
5. El botón de copiar coloca la dirección en el portapapeles y muestra snackbar de confirmación.
6. "Acerca de" muestra logo, versión, atribución IPinfo, correo de soporte, licencia y enlaces.
7. Las direcciones de donación llevan la nota "PENDIENTE: valor real, debe completar el mantenedor".
8. `./gradlew testDebugUnitTest` y `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

| Ámbito | Prueba | Tipo |
|---|---|---|
| `MoreViewModel` | `btcAddress` y `lightningAddress` leídos de recursos | JVM |
| `MoreViewModel` | `appVersion` leída de `packageManager` | JVM con `Context` de test |
| Más | Fila de donación desactivada si la dirección está vacía | JVM (ComposeTest) o dispositivo |
| Más | Copiar al portapapeles llama a `ClipboardManager.setPrimaryClip` | JVM con `ClipboardManager` mockeado |
| Más | `ACTION_SEND` incluye el enlace de GitHub | Dispositivo (verificar intent) |
| Diagnóstico | `DiagnosticsScreen` se abre con título "Reportar un problema" | Dispositivo |

---

## 10. Riesgos y fuera de alcance

### Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El usuario copia una dirección placeholder | Medio | Las strings llevan nota "PENDIENTE"; la UI no valida que sea una dirección real |
| No hay app de correo instalada | Bajo | `ActivityNotFoundException` capturado (como en `AboutSection.openSupportEmail`) |
| No hay app para compartir | Bajo | `ActivityNotFoundException` capturado; snackbar de error |

### Fuera de alcance

- Procesamiento de pagos o pasarelas de pago (la donación es solo mostrar una dirección).
- Generación de QR para la dirección de donación.
- Historial de donaciones o seguimiento de donantes.
- Nombre de licencia real si no existe `LICENSE` en el repo.

---

## 11. Dependencias de otras secciones

- **Base (navegación):** requiere `SettingsRoute` y el patrón de subpantalla con flecha de vuelta (`ui/UsipipoRoot.kt`).
- **Diagnóstico:** "Reportar un problema" reutiliza `DiagnosticsScreen` sin modificar su lógica interna.
- **Actualizaciones:** "Acerca de" puede mostrar un enlace a las releases, pero la lógica de comprobación vive en la sección de Actualizaciones.
- **LICENSE:** no existe archivo `LICENSE` en la raíz del repositorio; el nombre de la licencia es pendiente de verificación por el mantenedor.
