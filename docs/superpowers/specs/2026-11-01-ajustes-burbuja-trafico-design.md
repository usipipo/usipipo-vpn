# Spec de diseño — Ajustes: burbuja de tráfico (overlay)

**Fecha:** 2026-11-01
**Estado:** pendiente de revisión del usuario
**Proyecto:** uSipipo VPN
**Alcance:** Sección «Burbuja de tráfico» del hub de Ajustes (`ui/settings/bubble/`): pantalla de configuración de la burbuja flotante y la ventana overlay en sí, dibujada sobre otras apps con el permiso especial `SYSTEM_ALERT_WINDOW`. La burbuja muestra velocidad de subida/bajada, tiempo de conexión y país del servidor únicamente mientras el túnel está activo, y se aloja en `TunnelForegroundService` para no crear un segundo servicio en primer plano ni una notificación adicional.

---

## 1. Objetivo y tabla antes / después

Que el usuario vea información viva del túnel activo sin volver a la app, mediante una burbuja arrastrable sobre otras apps.

| Comportamiento | Antes | Después |
|---|---|---|
| Información del túnel activa | Solo dentro de la app (Inicio / Detalles) | También como burbuja flotante sobre cualquier app mientras el túnel esté activo |
| Burbuja | No existe | Arrastrable, posición persistida (`bubble_x`, `bubble_y`) |
| Contenido de la burbuja | — | Velocidad subida/bajada (vivo), tiempo de conexión, país del servidor |
| Servicio que la dibuja | — | `TunnelForegroundService` (mismo que mantiene el túnel) |
| Botón conectar/desconectar | — | No tiene |
| Apertura al tocar | — | Abre `MainActivity` con extra para mostrar Detalles del perfil activo (`showDetails = true`) |

---

## 2. Decisiones de la sección

| Tema | Decisión | Por qué |
|---|---|---|
| Hospedaje de la burbuja | `TunnelForegroundService` crea y remueve la ventana overlay | Decisión del usuario: no crear un segundo servicio en primer plano ni una notificación adicional |
| Fuente de velocidad | `TunnelManager.counters()` (poll de 1 s) | Ya existe; el servicio lo recoge mientras está en primer plano |
| País del servidor | `CountryRepository` (país cachéado por configuración) | Ya existe; no hay que consultar red desde el overlay |
| Tiempo de conexión | `TunnelState.Connected(sinceMs)` (`SystemClock.elapsedRealtime()`) | Ya existe en el modelo de dominio |
| Permiso overlay | `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (URI `package:` del paquete), API 23+ | API estándar para ventanas sobre otras apps |
| Flags de ventana | `FLAG_NOT_FOCUSABLE \| FLAG_NOT_TOUCH_MODAL` | No bloquean toques del usuario en otras apps |
| Accesibilidad | `contentDescription` en el overlay | Un icono sin nombre es inaccesible para TalkBack |
| Posición | Guardada en `AppSettings` (`bubble_x`, `bubble_y`) | Persistir entre sesiones |
| Play Store | Documentar política de `SYSTEM_ALERT_WINDOW` como riesgo | Es un permiso especial sujeto a revisión de Google Play |

---

## 3. Requisitos funcionales

- **RF-1** — La sección «Burbuja de tráfico» aparece en el hub de Ajustes con un interruptor «Mostrar burbuja de tráfico» (clave `bubble_enabled`).
- **RF-2** — Al activar el interruptor, si no hay permiso de overlay, la app solicita `SYSTEM_ALERT_WINDOW` mediante `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
- **RF-3** — La burbuja se dibuja solo cuando el túnel está activo (`TunnelState.isActive`: `Connecting`, `Connected`, `NoResponse`) y desaparece al pasar a `Disconnected` o `Failed`.
- **RF-4** — La burbuja muestra velocidad de subida y bajada en vivo (muestra cada 1 s de `TunnelManager.counters()`), tiempo de conexión desde `TunnelState.Connected.sinceMs` y país del servidor desde `CountryRepository`.
- **RF-5** — La burbuja es arrastrable; su posición se guarda (`bubble_x`, `bubble_y`) y se restaura al volver a mostrarla.
- **RF-6** — Tocar la burbuja abre `MainActivity` con un extra que hace que `UsipipoRoot` muestre Detalles del perfil activo (`showDetails = true`).
- **RF-7** — La burbuja no tiene botón de conectar/desconectar.
- **RF-8** — La ventana overlay usa `WindowManager.LayoutParams` con `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL` y `contentDescription`.
- **RF-9** — Si el usuario deniega el permiso de overlay o desactiva el interruptor, la burbuja se oculta y no vuelve a solicitar el permiso hasta que el usuario lo intente de nuevo.
- **RF-10** — La configuración de la burbuja (activado, posición) se guarda en `AppSettings` y se aplica en vivo sin reiniciar la app.

---

## 4. Interfaz

**Pantalla de Ajustes «Burbuja de tráfico» (`ui/settings/bubble/TrafficBubbleScreen.kt`):**
- Cabecera con flecha de vuelta y título «Burbuja de tráfico».
- Fila `SettingsSwitchRow`: título «Mostrar burbuja de tráfico», interruptor ligado a `bubble_enabled`.
- Resumen de estado a la derecha: «Activa» / «Inactiva» / «Permiso denegado».
- Si el permiso de overlay no está concedido: fila destacada con botón «Conceder permiso de superposición» que lanza `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` con URI `package:<paquete>`.
- Texto explicativo: «La burbuja se dibuja sobre otras apps solo mientras el túnel está activo».

**Ventana overlay (dibujada por `TunnelForegroundService`):**
- Pequeña tarjeta flotante con `contentDescription`, esquinas redondeadas, fondo del tema.
- Contenido: país (código ISO o nombre, o «—» si no disponible), tiempo de conexión (HH:mm:ss), velocidad subida/bajada (KB/s o MB/s, 0 s mientras no hay muestras).
- Sin botones de conectar/desconectar.
- Tocar → abre `MainActivity` con extra de Detalles.

**Estados:**
- Vacío: túnel inactivo → la burbuja no se dibuja; la pantalla de ajustes muestra el interruptor desactivado.
- Error: `counters()` devuelve null o la geolocalización falla → se muestra velocidad «—» y país «—» sin romper la burbuja; el error se registra en `AppLog`.
- Permiso denegado: `Settings.canDrawOverlays()` devuelve false → la pantalla de ajustes muestra el aviso y el botón para abrir Ajustes del sistema; la burbuja permanece oculta.

---

## 5. Datos y ajustes

**Claves en `AppSettings` (`settings/AppSettings.kt`, fachada sobre `SharedPreferences` archivo `usipipo_settings`):**

| Clave | Tipo | Valor por defecto | Quién escribe |
|---|---|---|---|
| `bubble_enabled` | Boolean | `false` | `TrafficBubbleViewModel` (interruptor) |
| `bubble_x` | Float | centro horizontal aproximado | `TrafficBubbleViewModel` (al soltar el arrastre) |
| `bubble_y` | Float | cerca de la parte superior | `TrafficBubbleViewModel` (al soltar el arrastre) |

**Modelo de UI:** `TrafficBubbleUiState(bubbleEnabled: Boolean, overlayGranted: Boolean, speedRx: String, speedTx: String, connectionTime: String, country: String)`.

**Quién escribe qué:**
- `TrafficBubbleViewModel` escribe `bubble_enabled`, `bubble_x`, `bubble_y` en `AppSettings`.
- `TunnelForegroundService` lee `bubble_enabled` de `AppSettings` para decidir mostrar/ocultar la ventana; lee `TunnelManager.state`, `TunnelManager.counters()` y `CountryRepository.countryFor()` para llenar el contenido de la burbuja; no escribe prefs.
- La posición del arrastre se captura en la UI (offset del `WindowManager`) y se persiste en `bubble_x` / `bubble_y`.

---

## 6. Permisos

| Permiso | Tipo | Cuándo se pide | Qué pasa si se deniega |
|---|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Especial (no es runtime normal) | Al activar el interruptor de la burbuja, mediante `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` | La burbuja no se dibuja; la pantalla de ajustes muestra «Permiso denegado» con botón para reintentar; no se bloquea el resto de la app |

Comprobación previa: `Settings.canDrawOverlays(context)` (API 23+). La actividad de Ajustes del sistema concede/deniega; al volver, la app reevalúa `canDrawOverlays`.

---

## 7. Integración con el código existente

**Ficheros tocados:**
- `ui/settings/bubble/TrafficBubbleScreen.kt` (nuevo) — pantalla de la sección en Ajustes.
- `ui/settings/bubble/TrafficBubbleViewModel.kt` (nuevo) — lógica de la sección, lecturas/escrituras de `AppSettings`.
- `tunnel/TunnelForegroundService.kt` (modificado) — crea/remueve la ventana overlay mediante `WindowManager.addView`/`removeView` vinculada a `TunnelState` (`Connected`/`NoResponse` muestra, `Disconnected`/`Failed` oculta); recoge `TunnelManager.counters()` cada 1 s; lee `bubble_enabled`, `bubble_x`, `bubble_y` de `AppSettings`. No se modifica la lógica de conexión del túnel (`CODING_STANDARDS.md` §7).
- `ui/UsipipoRoot.kt` (modificado) — añade `SettingsRoute.TrafficBubble` y pinta `TrafficBubbleScreen` como subpantalla de Ajustes; extiende `BackHandler` para la ruta de Ajustes.
- `ui/nav/MainTab.kt` — sin cambios (la sección se accede desde Ajustes, no como pestaña).
- `MainActivity.kt` (modificado) — lee un nuevo extra booleano (p. ej. `EXTRA_SHOW_DETAILS = "com.usipipo.vpn.extra.SHOW_DETAILS"`) y se lo pasa a `UsipipoRoot` como `initialShowDetails`, que inicializa `showDetails = true` para abrir Detalles del perfil activo.
- `settings/AppSettings.kt` (nuevo, introducido por la sección Apariencia) — getter/setter tipado para las tres claves de la burbuja.
- `AndroidManifest.xml` (modificado) — declara `WindowManager` no necesita permiso, pero se documenta que la app usa ventanas de overlay; no requiere `<uses-permission>` adicional para `SYSTEM_ALERT_WINDOW` (el sistema lo gestiona mediante `Settings`).

**APIs del SDK:**
- `android.view.WindowManager`, `WindowManager.LayoutParams` (`TYPE_APPLICATION_OVERLAY`, API 26+; minSdk 26), `FLAG_NOT_FOCUSABLE`, `FLAG_NOT_TOUCH_MODAL`, `PixelFormat.TRANSLUCENT`, `Gravity.START | Gravity.TOP`.
- `android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION`, `Settings.canDrawOverlays(context)`.
- `com.usipipo.vpn.tunnel.TunnelManager.state` (`StateFlow<TunnelState>`), `TunnelManager.counters()` (`Flow<TunnelCounters>`).
- `com.usipipo.vpn.domain.TunnelState.Connected(sinceMs)`, `TunnelState.isActive`.
- `com.usipipo.vpn.data.geo.CountryRepository.countryFor(configId, ip)` (devuelve `CountryInfo` sin bloquear).
- `com.usipipo.vpn.domain.TunnelCounters(rxBytes, txBytes, atMs)`.
- `settings.AppSettings` (lectura/escritura por clave con valor por defecto).

---

## 8. Criterios de aceptación

1. El hub de Ajustes muestra la sección «Burbuja de tráfico» con interruptor y resumen de estado.
2. Al activar el interruptor sin permiso de overlay, la app abre `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`; si se concede, aparece la burbuja al conectar un túnel.
3. La burbuja se dibuja solo con el túnel activo y desaparece al desconectar o fallar.
4. La burbuja muestra velocidad de subida/bajada, tiempo de conexión y país; si faltan datos muestra «—» sin cerrarse.
5. Arrastrar la burbuja y volver a abrir la app restaura su posición (`bubble_x`, `bubble_y`).
6. Tocar la burbuja abre `MainActivity` y muestra Detalles del perfil activo (`showDetails = true`).
7. La burbuja no tiene botón de conectar/desconectar.
8. No hay segundo servicio en primer plano ni notificación extra por la burbuja.
9. Denegar el permiso de overlay deja la burbuja oculta y muestra el aviso en la pantalla de ajustes sin romper la app.
10. `./gradlew testDebugUnitTest` y `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

| Ámbito | Prueba | Tipo |
|---|---|---|
| `TrafficBubbleViewModel` | Interruptor lee/escribe `bubble_enabled`; posición guarda `bubble_x`/`bubble_y`; valor por defecto | JVM (JUnit 4) con `AppSettings` falso |
| `AppSettings` | Claves `bubble_enabled`, `bubble_x`, `bubble_y` con valor por defecto explícito; `Flow` de cambios emite la clave | JVM con `SharedPreferences` falso |
| Formato de velocidad | Conversión `rxBytes`/`txBytes` a KB/s/MB/s y `sinceMs` a HH:mm:ss | JVM |
| Overlay aparece/desaparece | `TunnelForegroundService` muestra/oculta la ventana según `TunnelState` (doble del servicio) | Dispositivo |
| Permiso denegado | `canDrawOverlays() == false` → burbuja oculta + aviso en pantalla | Dispositivo |
| Toque abre Detalles | Extra `SHOW_DETAILS` → `UsipipoRoot` pinta `DetailsScreen` | Dispositivo |
| Persistencia de posición | Tras reiniciar la app, la burbuja aparece en la última coordenada | Dispositivo |

Los tests JVM son los únicos ejecutables localmente (`AGENTS.md`: JUnit 4 en JVM). El comportamiento del overlay y el permiso solo se validan en dispositivo / CI. Previews de `TrafficBubbleScreen` en tema claro y oscuro.

---

## 10. Riesgos y fuera de alcance

**Riesgos:**
- Política de Google Play sobre `SYSTEM_ALERT_WINDOW`: requiere justificación y puede dar lugar a rechazo de la publicación; documentar caso de uso legítimo.
- Batería: overlay siempre visible + poll de contadores cada 1 s mientras el túnel está activo; mitigación: solo mientras `isActive` y detener el poll al ocultar.
- Android 14+: restricciones de inicio de servicio en segundo plano (ya gestionadas por `TunnelForegroundService` con `startForegroundCompat`).

**Fuera de alcance:**
- Botón de conectar/desconectar dentro de la burbuja.
- Segundo servicio en primer plano o notificación propia de la burbuja.
- Personalización de apariencia de la burbuja (color, tamaño) más allá de la posición.
- Mostrar la burbuja con el túnel inactivo.

---

## 11. Dependencias de otras secciones

- **Ajustes base** (`2026-11-01-ajustes-navegacion-design.md`): hub de Ajustes, `AppSettings`, `SettingsRoute`, `UsipipoRoot` enrutado por estado local.
- **`tunnel/`**: `TunnelManager`, `TunnelForegroundService`, `TunnelState`, `TunnelCounters` (ya existen; solo se extiende el servicio para el overlay).
- **`data/geo/`**: `CountryRepository` para el país del servidor.
- No depende de Notificaciones, Copia de seguridad ni de ninguna otra sección de ajustes.
