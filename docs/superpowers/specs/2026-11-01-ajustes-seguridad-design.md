# Spec de diseño — Ajustes: Seguridad (biométrico, kill switch, apps excluidas)

**Fecha:** 2026-11-01
**Estado:** pendiente de revisión del usuario
**Proyecto:** uSipipo VPN
**Alcance:** Subpantalla **Seguridad** del hub de Ajustes: desbloqueo biométrico de la app, kill switch (solo lectura de siempre-on/lockdown + apertura de Ajustes de VPN) y lista global de apps excluidas del túnel (split tunneling inverso).

---

## 1. Objetivo

Proteger el acceso a la app (volver del segundo plano y/o entrar a Ajustes) con biometría, informar del estado always-on/lockdown del sistema sin implementarlo, y excluir paquetes del túnel WireGuard mediante `ExcludedApplications` inyectado al leer el `.conf`.

| Comportamiento | Antes | Después |
|---|---|---|
| Acceso a la app | Sin protección al volver de segundo plano ni para entrar a Ajustes | Opción de desbloqueo biométrico (con respaldo de credencial de dispositivo) |
| Always-on / lockdown | No se lee ni se informa | La app lee `isAlwaysOn()` / `isLockdownEnabled()` y abre `Settings.ACTION_VPN_SETTINGS` para guiar |
| Apps excluidas del túnel | No existe | Lista GLOBAL de paquetes (todos los perfiles), inyectada como `ExcludedApplications` en el [Interface] del `.conf` al leerlo (sin modificar el `.conf` guardado) |

---

## 2. Decisiones de la sección

| Tema | Decisión | Por qué |
|---|---|---|
| Biométrico | `androidx.biometric:biometric` + `BiometricPrompt`, permite credencial de dispositivo como respaldo | Estándar Android; soporta `minSdk 26`; fallback a PIN/patrón/contraseña cuando no hay hardware biométrico |
| Qué protege | Volver del segundo plano **y** entrar a Ajustes (dos switches independientes) | El usuario pidió ambas posibilidades |
| Kill switch | **Solo LECTURA** de siempre-on y lockdown (`Backend.isAlwaysOn()` / `isLockdownEnabled()`) + abrir `Settings.ACTION_VPN_SETTINGS`; la app **no implementa** always-on | `CODING_STANDARDS` §7: no tocar lógica de conexión del túnel; el sistema gestiona always-on |
| Apps excluidas | Lista GLOBAL (aplica a todos los perfiles); permiso `QUERY_ALL_PACKAGES`; inyectada en `data/` antes de `Config.parse`; **no** modificar el `.conf` guardado | Decisión del usuario; `Interface.parse` soporta `excludedapplications` (verificado en upstream WireGuard Android) |
| Dónde inyectar | Utilidad de `data/` aplicada en `HomeViewModel`, `TunnelForegroundService` y `QuickConnectController` (los sitios que leen `readText` antes de pasarlo al túnel) | Respeta CODING_STANDARDS §7: no se toca `tunnel/` ni la lógica `connect`/`disconnect`; solo se prepara el texto del `.conf` |
| Paquetes inexistentes | Al conectar, si un paquete de la lista ya no está instalado, se elimina de la lista | Evita inyectar paquetes inválidos |
| Lista vacía | Si está vacía, no se inyecta nada | Sin comportamiento extra cuando no hay exclusiones |

> **Nota sobre `isAlwaysOn()` / `isLockdownEnabled()`:** el contrato base las da por existentes en WireGuard (`Backend.isAlwaysOn()` / `isLockdownEnabled()`). En el código actual (`tunnel/WireGuardTunnelManager.kt`) **no existen**. Dependencia: deben añadirse como lecturas de solo-estado en `tunnel/` (autorizado por esta fase) o leerse a través de una nueva API de `TunnelManager`. Si no se añaden, el bloque de kill switch muestra "no disponible". Véase §12 Dependencias y §11 Riesgos.

---

## 3. Requisitos funcionales

- **RF-1** — Pantalla Seguridad con tres bloques: Desbloqueo biométrico (switch), Kill switch / estado siempre-on (informativo + botón abrir Ajustes de VPN), Apps excluidas del túnel (lista + selector).
- **RF-2** — Activar el desbloqueo biométrico requiere `USE_BIOMETRIC` (concedido en instalación) y flujo `BiometricPrompt`; se guarda en `biometric_enabled`.
- **RF-3** — Al volver de segundo plano y/o al abrir Ajustes, si `biometric_enabled` está activo, se solicita autenticación biométrica (o credencial de dispositivo) antes de mostrar contenido.
- **RF-4** — Fallo del dispositivo: si no hay hardware biométrico, se usa credencial de dispositivo (PIN/patrón/contraseña) vía `BiometricPrompt`.
- **RF-5** — Kill switch muestra si always-on y lockdown están activos (lectura de `isAlwaysOn()` / `isLockdownEnabled()`); botón "Abrir ajustes de VPN" lanza `Settings.ACTION_VPN_SETTINGS` ("android.settings.VPN_SETTINGS").
- **RF-6** — Pantalla de apps excluidas muestra paquetes instalados (requiere `QUERY_ALL_PACKAGES`); agregar/quitar paquetes; guarda en `excluded_apps`.
- **RF-7** — Al conectar, la lista de exclusiones se inyecta como `ExcludedApplications = pkg1,pkg2` en el bloque [Interface] del `.conf` **antes** de `Config.parse`; si la lista está vacía no se inyecta.
- **RF-8** — Al conectar, paquetes de la lista que ya no existen se eliminan automáticamente.
- **RF-9** — El `.conf` guardado en disco **no se modifica**; la inyección es en tiempo de lectura.

---

## 4. Interfaz

**Qué se ve:** `SecurityScreen` (subpantalla de `SettingsScreen`) con cabecera y flecha de vuelta.

1. **Bloque "Desbloqueo biométrico":** switch + descripción. Si se activa por primera vez, se lanza `BiometricPrompt` (biometría o credencial de dispositivo). Al fallar, mensaje "No se pudo autenticar".
2. **Bloque "Siempre-on VPN / Kill switch":** filas informativas "Siempre-on: Sí/No" y "Bloqueo sin VPN: Sí/No"; botón "Abrir ajustes de VPN".
3. **Bloque "Apps excluidas del túnel":** lista de paquetes con checkbox, botón "Añadir app" (abre selector de paquetes instalados), advertencia sobre `QUERY_ALL_PACKAGES`.

**Acciones:** toggle biométrico → solicitar auth → escribir `biometric_enabled`; abrir Ajustes VPN → intent `ACTION_VPN_SETTINGS`; añadir/quitar app → escribir `excluded_apps`.

**Estados:**
- Vacío: lista de apps excluidas vacía → mensaje "Ninguna aplicación excluida".
- Error: `BiometricPrompt` fallido → snackbar "No se pudo autenticar"; permiso `QUERY_ALL_PACKAGES` denegado → no se pueden listar apps (mensaje informativo).
- Permiso denegado: si se deniega `QUERY_ALL_PACKAGES`, la lista queda vacía y el túnel se conecta sin exclusiones.

---

## 5. Datos y ajustes

**Fichero:** `app/src/main/java/com/usipipo/vpn/settings/AppSettings.kt` (ya creado en la sección Apariencia).

| Clave | Tipo | Valor por defecto | Quién escribe |
|---|---|---|---|
| `biometric_enabled` | `Boolean` | `false` | `SecurityViewModel` |
| `excluded_apps` | `String` (JSON array o lista separada por comas) | `""` | `SecurityViewModel` |

**Modelo:** `ui/settings/security/SecurityViewModel.kt` + posible data class `ExcludedApp(packageName: String, label: String)`.

**Quién lee:** la utilidad de `data/` (inyección de `ExcludedApplications`) lee `excluded_apps`; `biometric_enabled` lo lee la capa de autenticación (pantalla de bloqueo al volver de segundo plano — esbozada aquí, detalle de implementación pendiente).

---

## 6. Permisos

| Permiso | Tipo | Cuándo se pide | Si se deniega |
|---|---|---|---|
| `USE_BIOMETRIC` | Normal (concedido en instalación) | Al activar el switch de desbloqueo biométrico | El switch no se activa; se informa que no hay soporte biométrico |
| `QUERY_ALL_PACKAGES` | Normal (concedido en instalación) | Al abrir el selector de apps excluidas | No se pueden listar apps; túnel sin exclusiones; mensaje informativo |
| `VpnService` (implícito) | — | Siempre-on / lockdown se **leen**, no se piden; la app abre `Settings.ACTION_VPN_SETTINGS` | N/A |

Declarar en `AndroidManifest.xml`: `android.permission.USE_BIOMETRIC` y `android.permission.QUERY_ALL_PACKAGES`. `QUERY_ALL_PACKAGES` requiere justificación en Play Store; la app se distribuye por releases de GitHub.

---

## 7. Integración con el código existente

**Ficheros a crear:**
- `app/src/main/java/com/usipipo/vpn/ui/settings/security/SecurityScreen.kt`
- `app/src/main/java/com/usipipo/vpn/ui/settings/security/SecurityViewModel.kt`
- `app/src/main/java/com/usipipo/vpn/data/ExcludedAppsConfig.kt` — utilidad de `data/` con función `injectExcludedApplications(configText: String, excludedPackages: List<String>): String` que inserta `ExcludedApplications = pkg1,pkg2` en el bloque [Interface] del `.conf` antes de `Config.parse`.

**Ficheros a modificar:**
- `app/src/main/java/com/usipipo/vpn/ui/home/HomeViewModel.kt`: `connectSelected()` aplica la inyección al texto de `store.readText(id)` antes de `tunnelManager.connect(id, text)`.
- `app/src/main/java/com/usipipo/vpn/tunnel/TunnelForegroundService.kt`: `connect()` aplica la inyección al texto de `container.configStore.readText(configId)` antes de `tunnelManager.connect(configId, text)`. Solo preparación del texto; **no** se toca la lógica `connect`/`disconnect` del túnel.
- `app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectController.kt`: `toggle()` aplica la inyección al texto de `store.readText(id)` antes de `connector.connect(id)`.
- `app/src/main/java/com/usipipo/vpn/AndroidManifest.xml`: añadir `USE_BIOMETRIC` y `QUERY_ALL_PACKAGES`.
- `gradle/libs.versions.toml` + `app/build.gradle.kts`: añadir `androidx.biometric:biometric:1.1.0`.

**APIs del SDK:**
- `androidx.biometric.BiometricPrompt` (y `androidx.biometric:biometric-ktx` si se usa corrutinas).
- `android.provider.Settings.ACTION_VPN_SETTINGS` ("android.settings.VPN_SETTINGS").
- `android.content.pm.PackageManager.getInstalledApplications` (para enumerar paquetes).
- `android.permission.QUERY_ALL_PACKAGES`.
- `com.wireguard.config.Config.parse` + `com.wireguard.config.Interface.getExcludedApplications()` / `toWgQuickString()` (verificado en upstream WireGuard Android: `Interface.parse` reconoce `excludedapplications`).

**No se toca:** `tunnel/WireGuardTunnelManager.kt` lógica de conexión (solo se lee el texto del `.conf` antes de pasarlo).

---

## 8. Criterios de aceptación

1. Switch de desbloqueo biométrico: activar → `BiometricPrompt` → guarda `biometric_enabled`.
2. Kill switch muestra estado siempre-on/lockdown (lectura) y el botón abre Ajustes de VPN.
3. Lista de apps excluidas: añadir/quitar persiste en `excluded_apps`.
4. Al conectar, el `.conf` leído incluye `ExcludedApplications = pkg1,pkg2` en [Interface] **antes** de `Config.parse`; el `.conf` en disco no cambia.
5. Lista vacía → no se inyecta `ExcludedApplications`.
6. Paquete desinstalado → se quita de la lista al conectar.
7. `USE_BIOMETRIC` y `QUERY_ALL_PACKAGES` declarados en el manifiesto.
8. `./gradlew testDebugUnitTest` + `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

**JVM (JUnit 4):**
- `SecurityViewModelTest`: toggle `biometric_enabled`; agregar/quitar `excluded_apps`; valores por defecto.
- `ExcludedAppsConfigTest`: inyección de `ExcludedApplications` en [Interface] antes de `Config.parse`; lista vacía → sin inyección; `.conf` de salida parseable por `Config.parse`.
- `AppSettingsTest` (claves `biometric_enabled`, `excluded_apps`).

**Dispositivo:**
- `BiometricPrompt` real en dispositivo con sensor biométrico (y fallback a credencial de dispositivo en uno sin sensor).
- `QUERY_ALL_PACKAGES` solo observable en dispositivo/emulador con apps instaladas.
- `Settings.ACTION_VPN_SETTINGS` resuelve en el dispositivo.

---

## 10. Riesgos y fuera de alcance

| Riesgo | Impacto | Mitigación |
|---|---|---|
| `QUERY_ALL_PACKAGES` es sensible para las tiendas | Medio | Distribución por releases de GitHub; exclusión de apps es opcional |
| `isAlwaysOn()` / `isLockdownEnabled()` no existen hoy en `tunnel/` | Alto | Añadirlas como lecturas de solo-estado en `tunnel/` o marcar kill switch como "no disponible" hasta que existan |
| Inyectar `ExcludedApplications` en un `.conf` que ya lo tenga → duplicado | Bajo | La utilidad reemplaza si existe o falla si hay conflicto; documentar |

**Fuera de alcance:** implementar always-on por cuenta de la app; bloqueo de pantalla completo de la app (se esboza, no se detalla); exclusión diferente por perfil (la lista es global).

---

## 11. Dependencias de otras secciones

- **Apariencia** (`2026-11-01-ajustes-apariencia-design.md`): `AppSettings` debe existir primero; aquí se escriben las claves `biometric_enabled` y `excluded_apps`.
- **Base** (`2026-11-01-ajustes-navegacion-design.md`): `AppContainer.appSettings` expuesto; hub `SettingsScreen` y `SettingsRoute` creados.
- No depende de Pantalla inicial y panel, Burbuja, Notificaciones, etc.
- **Gradle:** añadir `androidx.biometric:biometric:1.1.0` a `gradle/libs.versions.toml` (justificación: estándar Android para biometría con `minSdk 26`).
