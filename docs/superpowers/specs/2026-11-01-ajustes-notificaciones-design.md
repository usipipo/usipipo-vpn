# Spec de diseño — Ajustes: notificaciones

**Fecha:** 2026-11-01
**Estado:** aprobado por el usuario (2026-11-01)
**Implementación:** fase 4 · completada · rama `feat/ajustes-notificaciones` · PR #17 · merge `7c363db`
**Proyecto:** uSipipo VPN
**Alcance:** Sección «Notificaciones» del hub de Ajustes (`ui/settings/notifications/`): interruptor general de avisos, notificación de estado con tráfico, aviso de desconexión/fallo y aviso de actualización disponible. Canales de notificación propios (`estado`, `avisos`) además del canal `tunel` existente del servicio en primer plano. Permiso `POST_NOTIFICATIONS` en tiempo de ejecución (API 33+).

---

## 1. Objetivo y tabla antes / después

| Comportamiento | Antes | Después |
|---|---|---|
| Avisos del túnel | Solo la notificación del servicio en primer plano (canal `tunel`) | Además: notificación de estado con tráfico, aviso de desconexión/fallo, aviso de actualización |
| Control de avisos | No hay | Interruptor general `notifications_enabled` + por tipo en Ajustes |
| Canales | Solo `tunel` (FGS del túnel) | `tunel` (conservado), `estado` (IMPORTANCE_LOW), `avisos` (DEFAULT) |
| Permiso de notificaciones | No se gestiona | `POST_NOTIFICATIONS` en tiempo de ejecución (API 33+) al activar el primer aviso |
| Actualización disponible | Diálogo global en `UsipipoRoot` | También notificación que abre el diálogo de actualización |

---

## 2. Decisiones de la sección

| Tema | Decisión | Por qué |
|---|---|---|
| Canal «estado» | Nuevo canal `estado`, `IMPORTANCE_LOW` | Notificación de estado con tráfico: informativa, sin interrupciones |
| Canal «avisos» | Nuevo canal `avisos`, `IMPORTANCE_DEFAULT` | Desconexión/fallo y actualización: avisos que merecen atención del usuario |
| Canal «tunel» del FGS | Se conserva tal como está (`TunnelForegroundService`) | Android exige la notificación del servicio en primer plano; no se puede desactivar |
| Interruptor general | `notifications_enabled` silencia avisos no obligatorios | El usuario decide qué avisos recibe; el FGS del túnel nunca se desactiva (documentar en UI) |
| Notificación de estado con tráfico | Velocidad subida/bajada + tiempo de conexión, actualizada periódicamente con caducidad | Da visibilidad al túnel sin entrar en la app |
| Aviso de desconexión/fallo | Al pasar a `NoResponse`/`Failed` se publica una notificación | El usuario se entera aunque la app esté en segundo plano |
| Publicación de avisos | `TunnelForegroundService` (estado y desconexión) y `UsipipoRoot`/flujo de actualización (aviso de versión) | El servicio ya sigue `TunnelManager.state`; el aviso de versión surge de `UpdateChecker` |
| Caducidad de notificaciones | `setTimeoutAfter` en la notificación de estado (y refresco periódico) | Evitar notificaciones huérfanas cuando el túnel ya no está activo |
| Permiso | `POST_NOTIFICATIONS` en tiempo de ejecución (API 33+) | Obligatorio desde Android 13 para publicar notificaciones propias |

---

## 3. Requisitos funcionales

- **RF-1** — La sección «Notificaciones» del hub de Ajustes tiene un interruptor general «Notificaciones» (clave `notifications_enabled`) que silencia los avisos no obligatorios.
- **RF-2** — La notificación del servicio en primer plano del túnel (canal `tunel`) **no se puede desactivar**: Android la exige mientras el servicio esté en primer plano; la pantalla de Ajustes lo informa explícitamente.
- **RF-3** — Mientras el túnel esté activo, se publica una notificación de estado (canal `estado`) con velocidad de subida/bajada y tiempo de conexión, actualizada periódicamente y con caducidad (`setTimeoutAfter`).
- **RF-4** — Al pasar el túnel a `NoResponse` o `Failed`, se publica una notificación de aviso (canal `avisos`) de desconexión o fallo.
- **RF-5** — Cuando `UpdateChecker` encuentra una versión nueva, se publica una notificación (canal `avisos`) que abre el diálogo de actualización (`UpdateDialog`).
- **RF-6** — Cada tipo de aviso (tráfico, desconexión/fallo, actualizaciones) tiene su propio interruptor, deshabilitado si el interruptor general está apagado.
- **RF-7** — Se solicita `POST_NOTIFICATIONS` en tiempo de ejecución (API 33+) al activar el primer aviso que lo requiera.
- **RF-8** — Si el usuario deniega `POST_NOTIFICATIONS`, los avisos no se publican (sin fallo de la app) y la pantalla muestra un aviso con ruta a los Ajustes de notificaciones de la app.
- **RF-9** — Los canales se crean una vez (`tunel` ya existe; `estado` y `avisos` son nuevos) con sus IDs fijos.

---

## 4. Interfaz

**Pantalla de Ajustes «Notificaciones» (`ui/settings/notifications/NotificationsScreen.kt`):**
- Cabecera con flecha de vuelta y título «Notificaciones».
- Fila `SettingsSwitchRow`: «Notificaciones» (interruptor general `notifications_enabled`).
- Si el interruptor general está encendido, tres filas de interruptor (por defecto encendidas):
  - «Estado con tráfico»: notificación de velocidad y tiempo de conexión (canal `estado`).
  - «Desconexión o fallo»: aviso al pasar a `NoResponse`/`Failed` (canal `avisos`).
  - «Actualización disponible»: aviso cuando hay versión nueva (canal `avisos`).
- Cada fila muestra su resumen de estado (activo/inactivo).
- Banner informativo (siempre visible): «La notificación del servicio en primer plano del túnel no se puede desactivar: Android la exige mientras el túnel está activo».
- Si `POST_NOTIFICATIONS` está denegado: aviso destacado con botón «Abrir ajustes de notificaciones» que lanza `Settings.ACTION_APP_NOTIFICATION_SETTINGS` (con `EXTRA_PACKAGE_NAME`, API 27+; en API 26 se abre la pantalla general de notificaciones).

**Notificaciones públicas:**
- Estado con tráfico: título del perfil, texto «↓ rx · ↑ tx», subtexto con tiempo de conexión; icono pequeño del túnel; `setTimeoutAfter` para caducar.
- Desconexión/fallo: título «Túnel desconectado» / «Error de conexión», texto con motivo breve (sin claves ni contenido del .conf).
- Actualización disponible: título «Actualización disponible», texto con versión nueva; acción de apertura del diálogo de actualización.

**Estados:**
- Vacío: no hay túnel activo ni actualización pendiente → no se publica notificación de estado ni aviso.
- Error: `UpdateChecker` falla → no se publica aviso de actualización (falla en silencio, solo registro, como hoy).
- Permiso denegado: `POST_NOTIFICATIONS` denegado → los avisos no se publican; la pantalla lo indica; no hay bloqueo.

---

## 5. Datos y ajustes

**Claves en `AppSettings` (`settings/AppSettings.kt`):**

| Clave | Tipo | Valor por defecto | Quién escribe |
|---|---|---|---|
| `notifications_enabled` | Boolean | `true` | `NotificationsViewModel` (interruptor general) |
| `notification_traffic` | Boolean | `true` | `NotificationsViewModel` |
| `notification_disconnect` | Boolean | `true` | `NotificationsViewModel` |
| `notification_updates` | Boolean | `true` | `NotificationsViewModel` |

**Modelo de UI:** `NotificationsUiState(notificationsEnabled: Boolean, trafficEnabled: Boolean, disconnectEnabled: Boolean, updatesEnabled: Boolean, notificationsGranted: Boolean)`.

**Canales (IDs fijos):**
- `tunel` — existente, `TunnelForegroundService.createNotificationChannel()`, `IMPORTANCE_LOW`, ongoing, sin sonido ni vibración. Conservado sin cambios.
- `estado` — nuevo, `IMPORTANCE_LOW`, nombre «Estado del túnel», descripción para la notificación de tráfico.
- `avisos` — nuevo, `IMPORTANCE_DEFAULT`, nombre «Avisos», descripción para desconexión/fallo y actualización.

**Quién escribe qué:**
- `NotificationsViewModel` escribe las cuatro claves de `AppSettings` y expone el estado de los interruptores.
- `NotificationsManager` (nuevo, paquete `com.usipipo.vpn.notifications`) crea los canales y publica las notificaciones; lo invocan `TunnelForegroundService` (estado y desconexión) y el flujo de actualización en `UsipipoRoot`/`UsipipoApp` (aviso de versión).
- El estado `notificationsGranted` se deriva de `ContextCompat.checkSelfPermission(..., POST_NOTIFICATIONS)`.

---

## 6. Permisos

| Permiso | Tipo | Cuándo se pide | Qué pasa si se deniega |
|---|---|---|---|
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Al activar el primer aviso que requiera notificaciones en la sección «Notificaciones» | Los avisos no se publican; la app no falla; la pantalla muestra aviso con botón a Ajustes de notificaciones de la app |

`tunel` (FGS del túnel) no requiere `POST_NOTIFICATIONS` porque es una notificación de servicio en primer plano obligatoria de Android.

---

## 7. Integración con el código existente

**Ficheros tocados:**
- `ui/settings/notifications/NotificationsScreen.kt` (nuevo) — pantalla de la sección.
- `ui/settings/notifications/NotificationsViewModel.kt` (nuevo) — lógica de interruptores y lectura de `AppSettings`.
- `com.usipipo.vpn.notifications.NotificationsManager.kt` (nuevo, paquete de nivel top `notifications/` junto a `update/` y `tunnel/`) — crea canales (`tunel`, `estado`, `avisos`) y publica notificaciones.
- `tunnel/TunnelForegroundService.kt` (modificado) — publica notificación de estado con tráfico cada 1 s y aviso de desconexión/fallo al pasar a `NoResponse`/`Failed`, respetando las claves de `AppSettings`.
- `ui/UsipipoRoot.kt` (modificado) — al recibir `UpdateStatus.UpdateAvailable` de `UpdateChecker` y si el interruptor `notification_updates` está encendido, publica la notificación de actualización que abre `UpdateDialog`.
- `UsipipoApp.kt` (modificado, opcional) — inicialización de canales de notificación en `onCreate` (o en el primer acceso a la sección).
- `settings/AppSettings.kt` (nuevo, introducido por Apariencia) — claves de notificaciones.
- `AndroidManifest.xml` (modificado) — declara `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`.

**APIs del SDK:**
- `android.app.NotificationManager`, `createNotificationChannel(NotificationChannel)` (API 26+).
- `android.app.NotificationChannel` con `IMPORTANCE_LOW` / `IMPORTANCE_DEFAULT`, `setSound`, `enableVibration`.
- `androidx.core.app.NotificationCompat.Builder` con `setChannelId`, `setTimeoutAfter` (caducidad, API 24+), `setOngoing`, `setOnlyAlertOnce`.
- `android.permission.POST_NOTIFICATIONS`, `ContextCompat.checkSelfPermission`, `requestPermissions` (API 33+).
- `android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS` con `EXTRA_PACKAGE_NAME` (API 27+).
- `com.usipipo.vpn.update.UpdateChecker.check()` / `UpdateStatus.UpdateAvailable(release)`.
- `com.usipipo.vpn.ui.update.UpdateDialog` (ya existe).
- `com.usipipo.vpn.tunnel.TunnelManager.state` y `TunnelState` (`Connected`, `NoResponse`, `Failed`).
- `settings.AppSettings`.

---

## 8. Criterios de aceptación

1. La sección «Notificaciones» muestra el interruptor general y los tres interruptores por tipo, deshabilitados si el general está apagado.
2. Activar cualquier aviso por primera vez solicita `POST_NOTIFICATIONS` (API 33+).
3. Con el túnel activo, aparece la notificación de estado con tráfico (velocidad + tiempo de conexión) y se actualiza periódicamente.
4. Al pasar el túnel a `NoResponse` o `Failed`, aparece la notificación de aviso correspondiente.
5. Cuando `UpdateChecker` detecta versión nueva, aparece la notificación de actualización que abre `UpdateDialog`.
6. La notificación del servicio en primer plano (canal `tunel`) no se puede desactivar; la pantalla lo indica claramente.
7. Denegar `POST_NOTIFICATIONS` no rompe la app: los avisos no se publican y aparece la ruta a los Ajustes de notificaciones.
8. Los canales `estado` y `avisos` se crean con los IDs y niveles de importancia especificados; `tunel` se conserva.
9. `./gradlew testDebugUnitTest` y `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

| Ámbito | Prueba | Tipo |
|---|---|---|
| `NotificationsViewModel` | Interruptores leen/escriben las cuatro claves de `AppSettings`; el general desactiva los tres por tipo | JVM (JUnit 4) con `AppSettings` falso |
| `AppSettings` | Valor por defecto de `notifications_enabled`, `notification_traffic`, `notification_disconnect`, `notification_updates`; `Flow` de cambios | JVM |
| `NotificationsManager` | Creación de canales con IDs/importancia correctos (mock de `NotificationManager`) | JVM |
| Publicación de estado | `TunnelForegroundService` publica notificación de tráfico solo con túnel activo y caducidad (`setTimeoutAfter`) | Dispositivo |
| Publicación de desconexión/fallo | Transición a `NoResponse`/`Failed` → notificación de aviso | Dispositivo |
| Permiso `POST_NOTIFICATIONS` | Denegado → avisos no publicados, sin crash, aviso en pantalla | Dispositivo |
| Aviso de actualización | `UpdateStatus.UpdateAvailable` → notificación que abre `UpdateDialog` | Dispositivo |

Los tests JVM son los únicos ejecutables localmente (`AGENTS.md`: JUnit 4 en JVM). Canales, permisos y publicación se validan en dispositivo / CI. Previews de `NotificationsScreen` en tema claro y oscuro.

---

## 10. Riesgos y fuera de alcance

**Riesgos:**
- Usuario deniega `POST_NOTIFICATIONS`: los avisos de desconexión y actualización se pierden sin ruido; mitigación: pantalla clara con ruta a Ajustes de notificaciones.
- Notificaciones excesivas agotan batería; mitigación: caducidad (`setTimeoutAfter`) y refresco limitado al estado de tráfico.
- Los canales creados no se pueden eliminar fácilmente una vez publicados; mitigación: IDs fijos y actualizar en lugar de recrear.
- La notificación del FGS es obligatoria por Android; no ofrecer interruptor que la elimine (documentado en UI).

**Fuera de alcance:**
- Acciones de conectar/desconectar dentro de la notificación (el FGS ya tiene «Desconectar»).
- Sonidos, vibración, luces o agrupación de notificaciones.
- Notificaciones programadas o silenciosas.
- Sincronización de preferencias de notificación entre dispositivos.

---

## 11. Dependencias de otras secciones

- **Ajustes base** (`2026-11-01-ajustes-navegacion-design.md`): hub de Ajustes, `AppSettings`, `SettingsRoute`, `UsipipoRoot`.
- **Actualizaciones** (`2026-11-01-ajustes-actualizaciones-design.md`): `UpdateChecker`, `UpdateStatus`, `UpdateDialog` para el aviso de versión.
- **`tunnel/`**: `TunnelManager`, `TunnelForegroundService`, `TunnelState` para estado y desconexión.
- No depende de Copia de seguridad ni de otras secciones de ajustes.
