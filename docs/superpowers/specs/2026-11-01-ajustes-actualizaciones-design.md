# Spec de diseño — Ajustes: Actualizaciones

**Fecha:** 2026-11-01
**Estado:** aprobado por el usuario (2026-11-01)
**Implementación:** fase 9 · no iniciada · rama `feat/ajustes-actualizaciones` · -
**Proyecto:** uSipipo VPN
**Alcance:** Pantalla de Actualizaciones dentro del hub de Ajustes: versión actual, comprobación forzada con spinner y snackbar, interruptor de comprobación automática al arranque, estado de posponer y enlace al historial de versiones.

---

## 1. Objetivo

Que el usuario pueda comprobar si hay una nueva versión, activar/desactivar la comprobación automática al arranque, ver qué versión ha pospuesto y quitar el posponer, además de abrir el historial de releases en GitHub.

| Comportamiento | Antes | Después |
|---|---|---|
| Versión actual | Solo en `AboutSection` (logo + versión) | Fila propia en Actualizaciones con versión leída de `packageManager` |
| Buscar actualizaciones | Botón en `AboutSection.kt:104` con spinner + snackbar | Fila propia con spinner durante la comprobación y snackbar con el resultado |
| Comprobación automática | `AppContainer.checkForUpdatesOnStartup()` siempre se ejecuta | Interruptor `update_auto_check` en Ajustes que habilita/deshabilita la comprobación de arranque |
| Posponer versión | `UpdateStateStore.skippedVersion` solo desde `UpdateDialog` | Fila que muestra la versión pospuesta y permite quitar el posponer |
| Historial de versiones | Solo desde el diálogo global `UpdateDialog` (enlace a GitHub) | Fila "Ver historial de versiones" que abre `https://github.com/usipipo/usipipo-vpn/releases/latest` |
| Diálogo global | `UpdateDialog` en `UsipipoRoot.kt` | Se conserva sin cambios; esta sección solo añade controles |

---

## 2. Decisiones

| Tema | Decisión | Por qué |
|---|---|---|
| Ubicación del código | `ui/settings/updates/UpdatesScreen.kt` + `UpdatesViewModel.kt` | Co-locación según `AGENTS.md` |
| Versión actual | `context.packageManager.getPackageInfo(context.packageName, 0).versionName` | Misma lectura que `AboutSection.appVersion()`; no se inventa nueva API |
| Comprobación forzada | `container.checkForUpdates(force = true)` | `UpdateChecker.check()` ignora el throttle de 24 h cuando `force = true` |
| Auto-check al arranque | `AppContainer.checkForUpdatesOnStartup()` solo se llama si `AppSettings.update_auto_check == true` | El toggle lo habilita/deshabilita; valor por defecto `true` para no cambiar el comportamiento actual |
| Estado de posponer | `UpdateStateStore.skippedVersion` leído y borrado desde la pantalla | `UpdateChecker.isSkipped()` ya filtra versiones pospuestas |
| Enlace a releases | `https://github.com/usipipo/usipipo-vpn/releases/latest` | URL proporcionada por el usuario; no se inventa otra |
| Diálogo global | `UpdateDialog` en `UsipipoRoot.kt` se conserva | No se duplica; la pantalla de Actualizaciones muestra estado, el diálogo global sigue apareciendo cuando hay release |

---

## 3. Requisitos funcionales

- **RF-1** — La pantalla muestra la versión actual de la app (leída de `packageManager.getPackageInfo`).
- **RF-2** — "Buscar actualizaciones" llama a `container.checkForUpdates(force = true)`, muestra spinner durante la comprobación y snackbar con el resultado (actual disponible / ya actualizada / pospuesta / error).
- **RF-3** — Un interruptor "Comprobar actualizaciones al arranque" lee/escribe `AppSettings.update_auto_check`; `AppContainer.checkForUpdatesOnStartup()` solo se ejecuta si el interruptor está activado.
- **RF-4** — Si hay una versión pospuesta (`UpdateStateStore.skippedVersion`), se muestra en pantalla con opción de "Quitar posponer" (borra `skippedVersion`).
- **RF-5** — "Ver historial de versiones" abre `https://github.com/usipipo/usipipo-vpn/releases/latest` en el navegador mediante `Intent.ACTION_VIEW`.
- **RF-6** — El diálogo global `UpdateDialog` en `UsipipoRoot.kt` continúa funcionando igual; esta pantalla no lo duplica ni lo reemplaza.

---

## 4. Interfaz

### Qué se ve

- Cabecera con flecha de vuelta y título "Actualizaciones".
- Fila "Versión actual": muestra `vX.Y.Z` (o "—" si no se puede leer).
- Fila "Buscar actualizaciones": botón con spinner circular mientras se comprueba y snackbar con el resultado.
- Fila "Comprobar actualizaciones al arranque": interruptor (Switch) vinculado a `update_auto_check`.
- Fila "Versión pospuesta": solo visible si `UpdateStateStore.skippedVersion` no es nula; muestra la versión y un botón "Quitar posponer".
- Fila "Ver historial de versiones": abre el enlace de GitHub.

### Acciones

- Tocar "Buscar actualizaciones" → `checkForUpdates(force = true)` → spinner → snackbar con resultado.
- Cambiar interruptor auto-check → escribir `AppSettings.update_auto_check`.
- Tocar "Quitar posponer" → borrar `UpdateStateStore.skippedVersion` → ocultar fila.
- Tocar "Ver historial de versiones" → `Intent.ACTION_VIEW` a GitHub releases.

### Estados

- **Vacío:** si no hay versión pospuesta, la fila no se muestra.
- **Error:** si la comprobación falla, snackbar con "No se pudo comprobar si hay actualizaciones".
- **Cargando:** spinner durante `checkForUpdates(force = true)`.

---

## 5. Datos y ajustes

### Claves en AppSettings

| Clave | Tipo | Valor por defecto | Quién escribe |
|---|---|---|---|
| `update_auto_check` | Boolean | `true` | `UpdatesViewModel` al cambiar el interruptor |

### Modelo

`UpdatesUiState` (en `UpdatesViewModel`):
- `currentVersion: String`
- `isChecking: Boolean`
- `lastResult: UpdateStatus?`
- `postponedVersion: String?` (de `UpdateStateStore.skippedVersion`)

### Quién escribe

- `UpdatesViewModel` escribe `AppSettings.update_auto_check` y `UpdateStateStore.skippedVersion` (para quitar el posponer).
- `AppContainer.checkForUpdates(force = true)` actualiza `_updateStatus` (StateFlow) que la UI observa.

---

## 6. Permisos

| Permiso | Tipo | Cuándo se pide | Si se deniega |
|---|---|---|---|
| Ninguno | — | N/A | N/A |

No se requieren permisos de runtime. La comprobación de actualizaciones usa HTTPS a `api.github.com` (permiso de internet implícito en Android).

---

## 7. Integración con el código existente

### Ficheros tocados

| Fichero | Cambio |
|---|---|
| `app/src/main/java/com/usipipo/vpn/ui/settings/updates/UpdatesScreen.kt` | **Nuevo**: pantalla de Actualizaciones |
| `app/src/main/java/com/usipipo/vpn/ui/settings/updates/UpdatesViewModel.kt` | **Nuevo**: ViewModel con lógica de comprobación, auto-check y posponer |
| `app/src/main/java/com/usipipo/vpn/AppContainer.kt` | `checkForUpdatesOnStartup()` condicionado a `appSettings.update_auto_check`; añadir `appSettings: AppSettings` |
| `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt` | Añadir ruta `SettingsRoute.Updates` y pantalla `UpdatesScreen` |
| `app/src/main/java/com/usipipo/vpn/ui/settings/SettingsRoute.kt` | Añadir `Updates` al enum |
| `app/src/main/java/com/usipipo/vpn/settings/AppSettings.kt` | Añadir clave `update_auto_check` con getter/setter (si no existe aún) |

### APIs del SDK

- `android.content.Context.packageManager.getPackageInfo(packageName, 0).versionName` — versión actual (mismas que `AboutSection.appVersion()`)
- `android.content.Intent(Intent.ACTION_VIEW, Uri.parse(url))` — abrir navegador a GitHub
- `com.usipipo.vpn.update.UpdateChecker.check(currentVersion, force = true)` — existente
- `com.usipipo.vpn.update.UpdateStateStore.skippedVersion` — existente
- `com.usipipo.vpn.update.UpdateStatus` — sealed interface existente
- `android.content.SharedPreferences` — para `update_auto_check`

---

## 8. Criterios de aceptación

1. La pantalla muestra la versión actual de la app.
2. Tocar "Buscar actualizaciones" muestra spinner y, al terminar, snackbar con el resultado.
3. El interruptor "Comprobar actualizaciones al arranque" lee y escribe `update_auto_check`; al desactivarlo, `checkForUpdatesOnStartup()` no ejecuta la comprobación.
4. Si hay una versión pospuesta, se muestra en pantalla con botón para quitar el posponer.
5. Tocar "Ver historial de versiones" abre `https://github.com/usipipo/usipipo-vpn/releases/latest` en el navegador.
6. El diálogo global `UpdateDialog` sigue funcionando cuando hay una release disponible.
7. `./gradlew testDebugUnitTest` y `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

| Ámbito | Prueba | Tipo |
|---|---|---|
| `UpdatesViewModel` | `currentVersion` leída de `packageManager` | JVM con `Context` de test |
| `UpdatesViewModel` | `checkForUpdates(force = true)` ignora el throttle | JVM con `UpdateChecker` mockeado |
| `UpdatesViewModel` | Cambiar interruptor escribe `update_auto_check` | JVM con `SharedPreferences` falso |
| `UpdatesViewModel` | Quitar posponer borra `skippedVersion` | JVM con `UpdateStateStore` mockeado |
| `AppContainer` | `checkForUpdatesOnStartup()` respeta `update_auto_check` | JVM |
| Interfaz | Spinner visible durante la comprobación | Dispositivo (o ComposeTest) |
| Snackbar | Resultado mostrado tras comprobación | Dispositivo |

---

## 10. Riesgos y fuera de alcance

### Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El throttle de 24 h impide reintentar rápido | Bajo | `force = true` lo ignora; documentado en la UI |
| `packageManager.getPackageInfo` es deprecada en API 28+ | Bajo | Ya se usa en `AboutSection.appVersion()` con `@Suppress("DEPRECATION")`; alternativa `PackageInfoCompat` si fuera necesario |
| Red no disponible | Medio | `UpdateStatus.Failed` → snackbar de error; silencioso en el registro |

### Fuera de alcance

- Descargar e instalar actualizaciones automáticamente (la app solo abre el navegador).
- Notificaciones push de actualizaciones (requeriría `POST_NOTIFICATIONS` y FCM, fuera de alcance).
- Canales de actualización (beta/estable) o versiones múltiples.

---

## 11. Dependencias de otras secciones

- **Base (navegación):** requiere `SettingsRoute` y el patrón de subpantalla con flecha de vuelta.
- **AppSettings:** requiere la clave `update_auto_check` (ya listada en §3.6 del contrato base).
- **Actualizaciones (diálogo global):** `UpdateDialog` en `UsipipoRoot.kt` se conserva; esta pantalla solo añade controles adicionales.
