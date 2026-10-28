# Spec de diseño — Fase 7: Accesos rápidos que funcionan en segundo plano

**Fecha:** 2026-10-28
**Estado:** pendiente de revisión del usuario
**Proyecto:** uSipipo VPN
**Alcance:** Corrección de los tres fallos reportados por el beta tester (app 0.5.0, Xiaomi 24117RN76L, Android 16 / API 36) más los defectos colaterales que la investigación destapó: el widget y el mosaico solo conectan con la app abierta, el widget se añade en 3×3 en vez de 2×2, y el mosaico de ajustes rápidos no aparece en la lista de mosaicos del sistema.

---

## 1. Objetivo

Que el usuario pueda **conectar y desconectar desde el widget de la pantalla de inicio y desde el mosaico de Ajustes rápidos sin abrir la app**, que el **widget se añada en 2×2** como dice el diseño original, y que el **mosaico se pueda añadir con un toque** desde la propia app (en vez de tener que buscarlo en el panel de ajustes del fabricante).

En concreto, tras esta fase:

| Comportamiento | Antes | Después |
|---|---|---|
| Toque en el widget con la app cerrada | «Fallo» (`BackgroundServiceStartNotAllowedException`) | Conecta y levanta la notificación de VPN |
| Toque en el mosaico con la app cerrada | Sin mosaico; y si existiera, fallaría igual | Conecta (el mosaico queda «encendido») |
| Tamaño por defecto del widget | 3×3 | 2×2 |
| Añadir el mosaico | Buscar el lápiz de «Editar» a mano | Botón «Añadir mosaico» en Diagnóstico (API 33+) o pista clara (API < 33) |
| El mosaico se repinta al cambiar el estado | No (falta `ACTIVE_TILE`) | Sí |
| El túnel sobrevive con la app en segundo plano | Lo tumba el sistema («El sistema cerró el túnel» ×5) | Servicio en primer plano mientras el túnel está arriba |

---

## 2. Evidencia y causa raíz

La investigación siguió `systematic-debugging`: reproducir → evidencia → causa raíz, sin tocar código. Fuentes: el informe del tester (`docs/diagnostics/diagnostico-20261027-2015.txt`), el código fuente de la librería WireGuard y el código de AOSP (rama `android14-release`, equivalente funcional a API 36 en estos puntos).

### 2.1 Fallo 1 — «solo conecta si abro la app»

**Síntoma en el informe:** diez apariciones idénticas de

```
E/Tunel: Fallo al subir el túnel
android.app.BackgroundServiceStartNotAllowedException: Not allowed to start service Intent {
  xflg=0x4 cmp=com.usipipo.vpn/com.wireguard.android.backend.GoBackend$VpnService}:
  app is in background uid UidRecord{... LAST bg:+3m3s240ms idle change:cached|procstate|procadj procs:0 ...}
```

en las líneas 529, 547, 565, 583, 601, 619, 706, 724, 742 y 760.

**Causa raíz.** La librería oficial `com.wireguard.android:tunnel:1.0.20251231` arranca su propio servicio de VPN con un `startService()` normal (`tunnel/src/main/java/com/wireguard/android/backend/GoBackend.java`, método `setStateInternal`):

```java
if (!vpnService.isDone()) {
    Log.d(TAG, "Requesting to start VpnService");
    context.startService(new Intent(context, VpnService.class));
}
```

Con `targetSdk = 36`, Android 12+ impide arrancar un servicio en segundo plano desde un proceso en estado `cached`/`idle`:

- `ActiveServices.startServiceLocked` (líneas 940-971) consulta `ActivityManagerService.getAppStartModeLOSP`.
- `getAppStartModeLOSP` (AMS 6067-6117) devuelve `APP_START_MODE_DELAYED_RIGID` cuando `UidRecord.isIdle()` es cierto.
- `ActiveServices` devuelve entonces `ComponentName("?", "app is in background uid …")` y `ContextImpl.startService` lanza `BackgroundServiceStartNotAllowedException`.

El proceso de nuestra app **no tiene ningún componente propio que lo mantenga fuera del estado `idle`**: no hay servicio en primer plano, y el `GoBackend$VpnService` de la librería es un servicio normal (no un FGS) que además se destruye en cada desconexión (`stopSelf()` en `setStateInternal`).

**Por qué el patrón del registro encaja exactamente.** Los intentos fallidos ocurren cuando el proceso lleva tiempo en `cached` (`bg:+1m32s`, `bg:+3m33s`); los intentos que sí conectan ocurren cuando el proceso aún no está `idle` (app recién abierta, o el `VpnService` de la librería todavía vivo tras un `stopSelf()` que aún no ha destruido el servicio). Es decir: **funciona por casualidad del estado del proceso, no por diseño**.

**Alcance real.** El widget y el mosaico comparten `QuickConnectController.toggle()`, así que **ambos** sufferían el mismo fallo aunque el mosaico existiera. La causa no es del widget ni del mosaico: es del camino de conexión en segundo plano.

### 2.2 Fallo 2 — el widget aparece 3×3

`app/src/main/res/xml/usipipo_widget_info.xml` declara `android:minWidth="180dp"` y `android:minHeight="190dp"`, y **no** declara `android:targetCellWidth`/`android:targetCellHeight`.

Según `AppWidgetProviderInfo`, `minWidth`/`minHeight` son el tamaño **mínimo** que el host concede, y `targetCellWidth`/`targetCellHeight` (API 31+) son el tamaño **por defecto** en celdas del launcher. Con la fórmula de celdas (~70 dp por celda + 30 dp de margen), 180 dp corresponden a 3 columnas y 190 dp a 3 filas: el widget se añade en 3×3. Sin `targetCell*`, el host deduce el tamaño inicial de `min*`.

Esto contradice el diseño aprobado (`docs/plans/2026-10-23-usipipo-vpn-widget-design.md`, §2: «Widget (2×2 por defecto, redimensionable a 4×2)»).

### 2.3 Fallo 3 — el mosaico no aparece para añadirlo

La declaración del `TileService` en el manifest es correcta (permiso `BIND_QUICK_SETTINGS_TILE`, `exported="true"`, icono y etiqueta). **Los mosaicos nunca se colocan solos**: el usuario tiene que añadirlos desde el editor del panel de ajustes rápidos, una ruta que en HyperOS (Xiaomi) no es donde el tester buscaba. La app solo ofrecía una pista de texto en Diagnóstico (`QuickAccessHint`).

**Defecto adicional descubierto:** el manifest **no** declara el metadato `android.service.quicksettings.ACTIVE_TILE`. Según `TileService.requestListeningState` (líneas 525-526 de AOSP), sin ese metadato la llamada «no hace nada»: hoy `WidgetUpdater.refreshTile()` es un **no-op** y el mosaico no se repinta al cambiar el estado (solo al abrir el panel).

### 2.4 Hallazgos colaterales (no bloquean esta fase)

- `E/Geo: ipapi.co respondió HTTP 404 para 165.140.x.x` en casi todas las conexiones: la geolocalización por IP no reconoce la IP de salida del ISP → **el país/bandera del widget nunca aparece**. Bug independiente, se documenta y se trata aparte.
- Cinco eventos «El sistema cerró el túnel» (18/10, 21/10, 24/10, 25/10, 26/10): el sistema (Xiaomi) tumba el túnel. Esta fase lo mitiga con el servicio en primer plano.
- Fallos de red al comprobar actualizaciones (`SocketTimeoutException`, `unexpected end of stream`) contra GitHub: benignos, sin acción.
- El informe es de la app **0.5.0 (5000)** mientras el repo está en `main` con `versionName 0.1.0` / `versionCode 1` (los valores reales los inyecta CI con `-PVERSION_NAME` / `-PVERSION_CODE`).

---

## 3. Decisiones

| Tema | Decisión | Por qué |
|---|---|---|
| Camino de conexión en segundo plano | **Servicio en primer plano propio** (`specialUse`) que hospeda el túnel mientras está arriba | Es la vía que la plataforma reserva para las apps VPN: con consentimiento de VPN concedido, `OP_ACTIVATE_VPN` está en `MODE_ALLOWED` y AOSP permite arrancar un FGS desde segundo plano (`ActiveServices` 7972-7980 + `AppOpsManager` 1069-1079). Además, el FGS saca el proceso del estado `idle`, con lo que el `startService()` interno de WireGuard vuelve a ser legal. |
| Vida del FGS | Vive **mientras el túnel está activo** (conectando, conectado o sin respuesta); se apaga al desconectar o fallar | Protege el túnel frente a los cierres del sistema y da al usuario una señal visible de que la VPN está activa. |
| Reserva si el sistema deniega el FGS | **Activity transparente** (la que ya existe) que conecta desde primer plano | El inicio de Activity por interacción del launcher está exento («the launch is initiated by the device's launcher app, such as … interacts with a widget»). Es la opción B, usada solo como reserva. |
| Tipo de FGS | `specialUse` + `FOREGROUND_SERVICE_SPECIAL_USE` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` | No existe un tipo «VPN»; `specialUse` es el tipo previsto para casos no cubiertos, y es el que admite el arranque desde segundo plano con `OP_ACTIVATE_VPN`. |
| Quién arranca el túnel | **El propio servicio** lee el perfil del almacén cifrado y llama a `TunnelManager.connect` **después** de `startForeground()` | Evita la condición de carrera: si el túnel subiera antes de que el proceso salga del estado `idle`, volveríamos a tener el mismo fallo. El texto del `.conf` (contiene claves privadas) **nunca viaja por un Intent**. |
| Orden de las acciones de conexión | Todas las superficies (Inicio, widget, mosaico) pasan por el mismo camino | Un único camino = una única causa raíz que arreglar. La app en primer plano también se beneficia del FGS (notificación + protección). |
| Tamaño del widget | `targetCellWidth/Height = 2` + `minWidth/minHeight = 110dp` (2 celdas) | `targetCell*` fija el tamaño por defecto en API 31+; `min*` a 110 dp hace que el launcher antiguo también lo añada en 2×2 y que el contenido quepa. |
| Mosaico | Metadatos `ACTIVE_TILE` y `TOGGLEABLE_TILE` + botón «Añadir mosaico» con `StatusBarManager.requestAddTileService` (API 33+) | `requestAddTileService` muestra el diálogo del sistema con nombre, etiqueta e icono: un toque y el mosaico está en el panel. En API < 33 se mantiene la pista manual. |
| Dependencias | **Ninguna nueva** | Todo se resuelve con el SDK de la plataforma. |
| Túnel | **No se modifica `WireGuardTunnelManager`** | `CODING_STANDARDS.md` §7 prohíbe tocar la lógica de conexión del túnel. Esta fase solo **añade** un archivo nuevo en `tunnel/` (el servicio que hospeda el túnel) y una interfaz nueva en `quickconnect/`. |

---

## 4. Requisitos funcionales

### RF-1 — Conectar desde el widget con la app en segundo plano
Al tocar el botón del widget con la app cerrada o en segundo plano, el túnel se levanta y el estado visible pasa a «Conectando…» → «Activo».

### RF-2 — Conectar desde el mosaico con la app en segundo plano
Al tocar el mosaico con la app cerrada, el túnel se levanta y el mosaico queda en estado nativo «encendido».

### RF-3 — Servicio en primer plano mientras el túnel está activo
Mientras el túnel esté en «Conectando», «Activo» o «Sin respuesta», hay una notificación permanente de uSipipo VPN con: título, estado, nombre del perfil y acción «Desconectar». Al pasar a «Desconectado» o «Fallo», la notificación desaparece y el servicio se detiene.

### RF-4 — Reserva por Activity transparente
Si el sistema deniega el arranque del servicio en primer plano (`ForegroundServiceStartNotAllowedException`), la app abre automáticamente su Activity transparente y conecta desde ahí, sin dejar al usuario con un error mudo. Si tampoco es posible, el widget/mosaico muestran «Fallo» y el registro lo anota.

### RF-5 — El widget se añade en 2×2
Al añadir el widget desde el selector de widgets, el launcher lo coloca en 2×2 con el diseño compacto (logo, píldora de estado, botón, nombre del perfil). El diseño horizontal (≥ 250 dp) se mantiene.

### RF-6 — El mosaico se puede añadir desde la app
En Diagnóstico, un botón «Añadir mosaico a Ajustes rápidos» (API 33+) pide al sistema que muestre el diálogo de confirmación; en API 26-32 se muestra la ruta manual.

### RF-7 — El mosaico se repinta al cambiar el estado
Al cambiar el estado del túnel, el mosaico actualiza icono y etiqueta sin necesidad de abrir el panel (gracias al metadato `ACTIVE_TILE`).

### RF-8 — Estado persistente del widget coherente
El estado guardado del widget se invalida cuando el túnel cae, como hasta ahora.

### RF-9 — Errores visibles y accionables
Un fallo de conexión muestra «Fallo» en el widget/mosaico y una línea en Diagnóstico que explica el motivo (permiso revocado, configuración ilegible, arranque denegado por el sistema), sin datos sensibles.

### RF-10 — Un único camino de conexión
Las tres superficies (Inicio, widget, mosaico) suben el túnel **siempre** a través del servicio en primer plano, de modo que ninguna conexión queda sin la protección del servicio y todas comparten el mismo manejo de errores.

---

## 5. Requisitos no funcionales

- **RNF-1:** sin dependencias nuevas; todo con el SDK de la plataforma.
- **RNF-2:** `WireGuardTunnelManager` no se modifica. `TunnelManager` (interfaz) tampoco: el servicio usa `connect`/`disconnect` y observa `state`.
- **RNF-3:** el registro solo contiene metadatos (id corto del perfil, estados, nombres de excepción). Ni claves privadas ni texto del `.conf`.
- **RNF-4:** `minSdk 26` / `compileSdk 37`: las APIs nuevas (`targetCell*`, `requestAddTileService`, FGS con tipo) se invocan con guarda de versión.
- **RNF-5:** en el camino normal no hay parpadeo de interfaz: el usuario no ve ninguna Activity.
- **RNF-6:** lógica testeable en JVM (JVM unit tests, JUnit 4) para: resolución de la acción de conexión, mapeo de estado a notificación y reserva cuando el sistema deniega el arranque. El comportamiento de plataforma se verifica en dispositivo real.
- **RNF-7:** el servicio en primer plano se promueve a primer plano **antes** de cualquier llamada al túnel, dentro de los 5 s que da la plataforma: así el proceso sale del estado `idle` y el `startService()` interno de la librería es legal.

---

## 6. Arquitectura y componentes

### 6.1 Diagrama del camino de conexión

```
Inicio (Compose)          Widget (BroadcastReceiver)         Mosaico (TileService)
      │                            │                                │
      ▼                            ▼                                ▼
HomeViewModel            QuickConnectController.toggle()   (mismo servicio)
.connectSelected()                │
      └──────────────┬─────────────┘
                     ▼
        decisión única: ¿hay perfil? ¿permiso VPN? ¿túnel activo?
             │                        │                        │
             │ no                     │ sí, túnel arriba       │ sí, conectar
             ▼                        ▼                        ▼
   OpenImporter /              disconnect                 TunnelConnector.connect(configId)
   VpnPermissionRequired   (el FGS se apaga solo)                   │
                                                                   ▼
                                                    AndroidTunnelConnector
                                                                   │
                              ┌────────────────────────────────────┴──────────────────────┐
                              ▼                                                           ▼
              ContextCompat.startForegroundService(TunnelForegroundService)    excepción del sistema
                              │                                                           │
                              ▼                                                           ▼
              onStartCommand:                                              devuelve false
                1) startForeground(notificación)                                      │
                2) leer el perfil cifrado por id                                      │
                3) tunnelManager.connect(id, texto)   ← aquí ya es legal              │
                                                                                       ▼
                                                              widget/mosaico ► QuickActionRouter lanza
                                                                                  la Activity transparente
                                                              Inicio          ► HomeViewModel conecta
                                                                                  directamente (app en primer plano)
```

### 6.2 Componente nuevo — `tunnel/TunnelForegroundService.kt`

Responsabilidad única: **alojar el túnel en primer plano mientras está activo**.

- Declarado en el manifest con `android:foregroundServiceType="specialUse"`, `android:exported="false"` y la propiedad `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` con la justificación.
- `onStartCommand` con acción propia `ACTION_CONNECT` y extra `configId` (solo el id, nunca el texto del `.conf`):
  1. `startForeground(NOTIFICATION_ID, notificación, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` en API 29+; en API 26-28, la sobrecarga de dos argumentos.
  2. Lee el perfil del `ConfigStore` cifrado mediante `AppContainer`.
  3. Llama a `tunnelManager.connect(configId, texto)`.
- Observa `tunnelManager.state` y actualiza la notificación («Conectando…», «Activo», «Sin respuesta», «Fallo»).
- Cuando el estado pasa a `Disconnected` o `Failed`: quita la notificación y se detiene (`stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`).
- `START_NOT_STICKY`: si el proceso muere, el túnel también; no tiene sentido arrastrar el servicio.
- Se apoya en `AppContainer` (ya existe y es el contenedor de dependencias manual del proyecto) para obtener `tunnelManager` y `configStore`.

### 6.3 Componente nuevo — `quickconnect/TunnelConnector.kt`

Interfaz estrecha que permite probar el flujo en JVM y sustituir la estrategia:

```kotlin
interface TunnelConnector {
    /** Arranca el servicio en primer plano y pide la conexión del perfil indicado.
     *  Devuelve false si el sistema deniega el arranque en segundo plano. */
    suspend fun connect(configId: String): Boolean
}
```

Implementaciones:

- `AndroidTunnelConnector` (producción): envuelve `TunnelForegroundService.start(context, configId)` y captura `ForegroundServiceStartNotAllowedException` / `IllegalStateException` de «Not allowed to start service» para devolver `false` (y anotarlo en el registro).
  Para que sea testeable sin Android, el arranque del servicio se inyecta como dependencia: `class AndroidTunnelConnector(private val starter: (Context, String) -> Unit = { ctx, id -> TunnelForegroundService.start(ctx, id) })`. `connect()` llama al `starter`, devuelve `true` si no hay excepción y `false` si el sistema la lanza. El test inyecta una lambda que lanza la excepción.
- `FakeTunnelConnector` en `app/src/test/java/com/usipipo/vpn/quickconnect/`: devuelve `true`/`false` según lo que el test necesite, al estilo del `FakeTunnelManager`/`FakeConfigStore` que ya usa `QuickConnectControllerTest`. **El fake vive en las fuentes de test, no en `main`.**

### 6.4 Cambios en `quickconnect/QuickConnectController.kt`

- Nueva dependencia: `TunnelConnector`.
- `toggle()` resuelve la decisión (perfil activo, permiso, túnel activo) y, en el caso de conectar, llama a `connector.connect(configId)`:
  - `true` → `QuickConnectAction.ConnectRequested(configId)`.
  - `false` → **nueva** `QuickConnectAction.BackgroundStartDenied`.
- `QuickConnectAction` (sealed interface) suma `data object BackgroundStartDenied`.
- El caso «configuración ilegible» se conserva: se sigue leyendo el perfil **antes** de delegar, para poder informar con precisión (`QuickConnectFailure.ConfigUnreadable`) sin tocar el servicio.
- La lectura del perfil ocurre dos veces (controlador para validar, servicio para ejecutar) a propósito: **el texto del `.conf` no viaja por un Intent**. Si la segunda lectura fallara (caso excepcional: el archivo desaparece entre ambas), el servicio lo anota con precisión y arranca con texto vacío, de modo que el estado final sea «Fallo» (el parser rechaza la configuración) y no un «Desconectado» silencioso.

### 6.5 Cambios en `ui/home/HomeViewModel.kt` (ruta de Inicio)

Hoy `HomeViewModel.connectSelected()` llama directamente a `tunnelManager.connect(id, text)`. Eso **no** rompe la conexión (la app está en primer plano), pero deja el túnel **sin el servicio en primer plano que lo proteja** frente a los cierres del sistema («El sistema cerró el túnel» ×5 en el informe). Para que haya **un único camino de conexión**:

- `HomeViewModel` recibe el `TunnelConnector` como dependencia adicional.
- `connectSelected()` valida el perfil (igual que hoy: `ConfigUnreadable` si `store.readText(id)` devuelve `null`) y después llama a `connector.connect(id)`.
- Si el conector devuelve `false` (el sistema denegó el FGS), la app, **que está en primer plano**, conecta directamente con `tunnelManager.connect(id, text)`: allí `startService()` es legal. Es la única situación en la que el túnel sube sin FGS, y se anota en el registro.
- La notificación aparece también en las conexiones iniciadas desde la app: es información útil («uSipipo VPN · Activo») y coherente con el estado que ya muestra Inicio.

### 6.6 Cambios en `widget/QuickActionRouter.kt`

- Al recibir `BackgroundStartDenied`, lanza `VpnPermissionActivity` (la Activity transparente existente) con `FLAG_ACTIVITY_NEW_TASK`, igual que hoy hace con `VpnPermissionRequired`.
- `VpnPermissionActivity` ya conecta cuando el permiso está concedido (`VpnService.prepare() == null` → `controller.toggle()`), así que **no hay ruta alternativa que mantener**: la reserva reutiliza el flujo probado. No hay posibilidad de bucle, porque la Activity llama al controlador directamente y no al router.

### 6.7 Widget 2×2

`res/xml/usipipo_widget_info.xml`:

| Atributo | Valor actual | Valor nuevo | Motivo |
|---|---|---|---|
| `minWidth` | 180dp | **110dp** | 2 celdas (70×2 − 30) |
| `minHeight` | 190dp | **110dp** | 2 celdas |
| `minResizeWidth` | 110dp | 110dp | sin cambios |
| `minResizeHeight` | 70dp | 70dp | sin cambios |
| `targetCellWidth` | — | **2** | Tamaño por defecto en API 31+ |
| `targetCellHeight` | — | **2** | Tamaño por defecto en API 31+ |
| `resizeMode` | horizontal\|vertical | sin cambios | |

`res/layout/widget_usipipo.xml` se compacta para que quepa en 2×2 (~150 dp de alto en un launcher de 5 columnas): relleno 12 dp, logo 24 dp, píldora de estado 12 sp, botón 52 dp, nombre de perfil 13 sp y país 11 sp. El diseño `widget_usipipo_wide.xml` (≥ 250 dp) no cambia.

### 6.8 Mosaico de Ajustes rápidos

- **Manifest:** añadir al `<service>` del mosaico:
  ```xml
  <meta-data android:name="android.service.quicksettings.ACTIVE_TILE" android:value="true" />
  <meta-data android:name="android.service.quicksettings.TOGGLEABLE_TILE" android:value="true" />
  ```
  `ACTIVE_TILE` hace que `TileService.requestListeningState` funcione (hoy es un no-op) → el mosaico se repinta al cambiar el estado. `TOGGLEABLE_TILE` da semántica de interruptor a TalkBack.
- **Añadir el mosaico:** `UsipipoTileService.requestAdd(context)` encapsula `StatusBarManager.requestAddTileService(component, etiqueta, icono, executor, callback)` (API 33+). La pantalla de Diagnóstico lo invoca desde un botón; el callback informa del resultado («Mosaico añadido» / «Solicitud denegada»).
- **Pista manual:** se reescribe el texto de `quick_access_tile_hint` para nombrar la ruta de HyperOS («desliza el panel de control, toca el lápiz de editar…») sin depender de una captura.

### 6.9 Robustez del actualizador del widget

`WidgetUpdater.apply()` invoca `refreshTile()` antes de persistir el estado. Se envuelve esa llamada en `try/catch` y se anota el fallo: un error al pedir el repintado del mosaico **no** debe impedir que se guarde el estado (ni tirar la app).

---

## 7. Archivos afectados

**Nuevos**

| Archivo | Contenido |
|---|---|
| `app/src/main/java/com/usipipo/vpn/tunnel/TunnelForegroundService.kt` | Servicio en primer plano que hospeda el túnel |
| `app/src/main/java/com/usipipo/vpn/tunnel/TunnelNotificationMapper.kt` | Función pura: estado del túnel → título/texto/acción de la notificación (testeable en JVM) |
| `app/src/main/java/com/usipipo/vpn/quickconnect/TunnelConnector.kt` | Interfaz `TunnelConnector` + implementación Android (`AndroidTunnelConnector`) |

**Modificados**

| Archivo | Cambio |
|---|---|
| `AndroidManifest.xml` | Permisos `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_SPECIAL_USE`; servicio nuevo con `specialUse` y `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; metadatos `ACTIVE_TILE`/`TOGGLEABLE_TILE` del mosaico |
| `quickconnect/QuickConnectController.kt` | Dependencia de `TunnelConnector`; nueva acción `BackgroundStartDenied` |
| `quickconnect/QuickConnectAction.kt` | Nueva acción |
| `ui/home/HomeViewModel.kt` | Dependencia de `TunnelConnector`; conecta por el FGS y cae a `tunnelManager.connect` si se deniega |
| `widget/QuickActionRouter.kt` | Reserva: `BackgroundStartDenied` → Activity transparente |
| `widget/UsipipoTileService.kt` | `requestAdd(context)` con `StatusBarManager.requestAddTileService` |
| `widget/WidgetUpdater.kt` | `try/catch` alrededor de `refreshTile()` |
| `ui/diagnostics/DiagnosticsScreen.kt`, `ui/diagnostics/QuickAccessHint.kt` | Botón «Añadir mosaico» y textos de la pista |
| `res/xml/usipipo_widget_info.xml` | `targetCell*` y `min*` de 2 celdas |
| `res/layout/widget_usipipo.xml` | Layout compacto 2×2 |
| `res/values/strings.xml` | Textos de la notificación y de la pista |
| `AppContainer.kt` | Cableado del `TunnelConnector` y del servicio |

**No se tocan:** `tunnel/WireGuardTunnelManager.kt`, `tunnel/TunnelManager.kt`, `gradle/libs.versions.toml` (sin dependencias nuevas).

**Tests (nuevos o modificados)**

| Archivo | Cambio |
|---|---|
| `app/src/test/java/com/usipipo/vpn/quickconnect/QuickConnectControllerTest.kt` | Fake del `TunnelConnector`; caso de arranque denegado |
| `app/src/test/java/com/usipipo/vpn/quickconnect/FakeTunnelConnector.kt` (nuevo) | Fake del conector (en fuentes de test) |
| `app/src/test/java/com/usipipo/vpn/tunnel/TunnelNotificationMapperTest.kt` (nuevo) | Mapeo de estado → notificación |
| `app/src/test/java/com/usipipo/vpn/quickconnect/AndroidTunnelConnectorTest.kt` (nuevo) | Denegación del sistema → `false` |
| `app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelTest.kt` (modificado) | Conecta por el conector; cae a `tunnelManager` si se deniega |
| `app/src/test/java/com/usipipo/vpn/ui/home/Fakes.kt` (modificado) | Añade el fake del conector a los fakes ya existentes |

---

## 8. Errores y casos límite

| Caso | Comportamiento esperado |
|---|---|
| Permiso de VPN revocado en Ajustes | El toque pide el permiso otra vez (`VpnPermissionActivity`); no se intenta el FGS |
| El sistema deniega el FGS (`ForegroundServiceStartNotAllowedException`) | Widget/mosaico: se abre la Activity transparente y se conecta desde primer plano; si tampoco se puede, estado «Fallo» + registro. Inicio: la app está en primer plano, así que `HomeViewModel` conecta directamente con `tunnelManager.connect` (sin FGS) y lo anota |
| `startForeground()` tarda más de 5 s | No puede ocurrir: se llama en la primera línea de `onStartCommand` |
| Doble toque rápido en el widget | El segundo toque cae en «Conectando» → desconecta (comportamiento actual, RFC-3 de la matriz manual) |
| Proceso muerto con el túnel «Activo» | Al tocar, el widget invalida el estado guardado y conecta de nuevo (comportamiento actual) |
| El sistema mata la app con el FGS activo | La notificación persistente hace visible el estado; Xiaomi podría aun así matar el proceso: el widget invalida el estado guardado al siguiente toque |
| Perfil eliminado mientras el túnel está arriba | El túnel baja con el perfil (comportamiento actual de Inicio) |
| Sin perfiles guardados | El toque abre la app en Importar (comportamiento actual, `QuickConnectAction.OpenImporter`) |
| API 26-32 (sin `requestAddTileService`) | El botón indica la ruta manual con el lápiz de «Editar» |
| OEM que no expone mosaicos de terceros | El botón informa «No disponible en este dispositivo» y se mantiene la pista manual |

---

## 9. Estrategia de pruebas

### 9.1 Unitarias (JVM, JUnit 4)

| Test | Qué fija |
|---|---|
| `QuickConnectControllerTest` (actualizado) | Conecta vía el `TunnelConnector`; devuelve `BackgroundStartDenied` si el sistema deniega el arranque; **no** conecta si falta el permiso, no hay perfil o la configuración es ilegible; desconecta sin pedir conexión |
| `HomeViewModelTest` (actualizado) | `connectSelected()` pide la conexión al `TunnelConnector`; si el conector deniega, cae a `tunnelManager.connect` con el texto leído; si el perfil es ilegible, no conecta y muestra `ConfigUnreadable` |
| `TunnelNotificationMapperTest` (nuevo) | Estado → título, texto, ongoing y acción; estado desconectado → sin notificación |
| `WidgetInfoSizeTest` (nuevo, opcional) | Lee `src/main/res/xml/usipipo_widget_info.xml` y verifica `targetCellWidth/Height = 2` y `minWidth/minHeight = 110dp` (guarda de regresión; el directorio de trabajo del test es el módulo `app/`) |
| `AndroidTunnelConnectorTest` (nuevo) | Traduce `ForegroundServiceStartNotAllowedException` a `false` y `true` en el camino feliz |

### 9.2 Manuales (dispositivo real)

Se amplía `docs/plans/2026-10-26-widget-beta-checklist.md` con:

1. Widget en 2×2 al añadirlo (y el layout compacto legible).
2. Toque en el widget **con la app cerrada** → conecta y aparece la notificación.
3. Toque en el mosaico **con la app cerrada** → conecta.
4. La notificación cambia de estado («Conectando…» → «Activo») y su acción «Desconectar» funciona.
5. El túnel sigue vivo 10 min con la app en segundo plano (y ya no aparece «El sistema cerró el túnel»).
6. Mosaico añadido desde el botón de Diagnóstico y repintado al cambiar el estado.
7. Revocar el permiso en Ajustes → el toque vuelve a pedirlo.

### 9.3 Verificación en CI

`./gradlew testDebugUnitTest` + `assembleDebug` (flujo de `.github/workflows/android-ci.yml`). Sin instrumentación: el comportamiento de plataforma no es comprobable en JVM.

---

## 10. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| `specialUse` requiere justificación en Google Play | La app se distribuye fuera de Play (APK firmada); la justificación queda documentada en el `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` |
| Dos notificaciones visibles (la del sistema VPN y la nuestra) | La nuestra es compacta y muestra el estado; el usuario puede ocultarla desde los ajustes de notificaciones |
| El sistema puede matar el proceso pese al FGS | La notificación lo hace visible y el widget invalida el estado guardado (comportamiento ya implementado) |
| La Activity de reserva parpadea en pantalla | Solo se usa cuando el sistema deniega el FGS; el camino normal no la muestra |
| La reserva podría entrar en bucle | La Activity llama al controlador directamente, nunca al router |
| El texto del `.conf` con claves privadas en un Intent | No viaja: el servicio lo lee del almacén cifrado por id |

---

## 11. Fuera de alcance

- La geolocalización por IP que responde 404 (`ipapi.co` no conoce la IP de salida del ISP): bug real, pero independiente; se documenta aquí y se trata en su propia fase.
- VPN «always-on» (modo administrador del dispositivo).
- Selector de perfil dentro del widget.
- Split tunneling por aplicación.
- Sincronización del estado del mosaico con el widget más allá del estado del túnel.

---

## 12. Trazabilidad con el informe del tester

| Queja del tester | Causa | Sección | Resultado esperado |
|---|---|---|---|
| «El widget sale por defecto 3×3» | `minWidth/minHeight` de 3 celdas sin `targetCell*` | §2.2, §6.7 | 2×2 al añadirlo |
| «Solo conecta cuando abro la aplicación; si no, fallo de conexión» | `startService()` de la librería con el proceso en `idle` | §2.1, §6.2 | Conecta desde el widget con la app cerrada |
| «Desde los ajustes rápidos no sale para añadir uSipipo VPN» | Los mosaicos no se colocan solos; faltaba el botón que lo pide | §2.3, §6.8 | Botón «Añadir mosaico» en Diagnóstico |
