# Fase 7 — Accesos rápidos que funcionan en segundo plano: Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que el widget de la pantalla de inicio y el mosaico de Ajustes rápidos puedan conectar y desconectar el túnel WireGuard con la aplicación en segundo plano, que el widget se añada por defecto en 2×2 y que el mosaico se pueda añadir desde la propia app con un solo toque.

**Architecture:** Un único camino de conexión (Inicio, widget, mosaico) pasa por `TunnelConnector`, que arranca un servicio en primer plano propio (`specialUse`) antes de leer el perfil cifrado y llamar a `TunnelManager.connect`. Si el sistema deniega el servicio, el widget/mosaico recurren a la Activity transparente y la app conecta directamente. El widget se compacta a 2 celdas y el mosaico gana los metadatos `ACTIVE_TILE`/`TOGGLEABLE_TILE` y un botón de añadir en Diagnóstico.

**Tech Stack:** Kotlin, Jetpack Compose, kotlinx.coroutines (StateFlow, runTest), Android SDK 36 (minSdk 26, compileSdk 37), Java 17, JUnit 4 para tests JVM (sin Robolectric: no se instancian clases de Android en tests), documentación en Markdown.

**Spec:** `docs/superpowers/specs/2026-10-28-accesos-rapidos-segundo-plano-design.md`

## Global Constraints

- `minSdk 26`, `targetSdk 36`, `compileSdk 37`, Java 17.
- Sin dependencias nuevas (todo resuelto con el SDK de la plataforma y `libs.versions.toml` inalterado).
- `tunnel/WireGuardTunnelManager.kt` no se modifica: se lee y se llama, nunca se edita.
- El texto del `.conf` nunca viaja por un Intent: el servicio lo lee del almacén cifrado por `configId`.
- `startForeground()` se llama antes de cualquier llamada al túnel y dentro de los 5 s que da la plataforma.
- El servicio en primer plano vive mientras el túnel está activo y se apaga al llegar a `Disconnected` o `Failed`.
- El registro solo contiene metadatos (id corto del perfil, estados, nombres de excepción).
- Las tres superficies (Inicio, widget, mosaico) usan el mismo camino de conexión.
- La notificación del servicio no pide `POST_NOTIFICATIONS`: si el usuario no lo ha concedido, la notificación no es visible, pero el servicio sigue en primer plano y el túnel protegido (limitación conocida; el estado de VPN del sistema sí es visible).
- Cada tarea termina con los tests pasando (`./gradlew testDebugUnitTest`) y un commit independiente (convencional commits en español, un cambio lógico por commit).

## Review Focus

1. El sistema deniega el arranque del servicio en primer plano desde segundo plano (API 31+, HyperOS, OEM agresivos): `connect` devuelve `false`, el registro lo anota y se abre la Activity transparente; nunca un fallo silencioso.
2. El perfil desaparece entre la validación del controlador y la lectura del servicio: el servicio conecta con texto vacío y el estado final es «Fallo», nunca «Desconectado» silencioso (verificado en la checklist manual, Task 8, fila «eliminar el perfil justo antes de tocar el widget»).
3. El estado «Sin respuesta» (túnel arriba sin handshake): la notificación sigue viva y el widget/mosaico muestran «Sin respuesta», no «Fallo».
4. Conexión iniciada desde la app (primer plano) con el servicio denegado: la app conecta directamente con `tunnelManager.connect` y lo anota.
5. Android 13+ sin permiso `POST_NOTIFICATIONS` concedido: la notificación del servicio no es visible, pero el servicio sigue protegiendo el túnel.

---

### Task 1: Contrato del conector y traducción de la denegación del sistema

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/quickconnect/TunnelConnector.kt`
- Create: `app/src/main/java/com/usipipo/vpn/quickconnect/AndroidTunnelConnector.kt`
- Create: `app/src/main/java/com/usipipo/vpn/tunnel/BackgroundStartDeniedException.kt`
- Create: `app/src/test/java/com/usipipo/vpn/quickconnect/FakeTunnelConnector.kt`
- Test: `app/src/test/java/com/usipipo/vpn/quickconnect/AndroidTunnelConnectorTest.kt`

**Interfaces:**
- Consumes: nada (tarea base).
- Produces: `interface TunnelConnector { suspend fun connect(configId: String): Boolean }`; `class BackgroundStartDeniedException(message: String, cause: Throwable? = null) : Exception(message, cause)` en paquete `tunnel`; `class AndroidTunnelConnector(private val startService: (String) -> Unit) : TunnelConnector` (el `startService` del mundo real apunta a `TunnelForegroundService.start(appContext, configId)` y se inyecta en los tests); `class FakeTunnelConnector(var result: Boolean = true) : TunnelConnector` con `val requestedIds: MutableList<String>`.

- [ ] **Step 1: Write failing test — `AndroidTunnelConnector` devuelve `true` cuando el arranque tiene éxito**

```kotlin
@Test
fun `arranca el servicio con el configId y confirma la conexion`() = runTest {
    var arrancado: String? = null
    val connector = AndroidTunnelConnector { configId -> arrancado = configId }

    assertTrue(connector.connect("abc-123"))
    assertEquals("abc-123", arrancado)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.quickconnect.AndroidTunnelConnectorTest"`
Expected: FAIL (`AndroidTunnelConnector` y `BackgroundStartDeniedException` no existen).

- [ ] **Step 2: Write failing test — `AndroidTunnelConnector` devuelve `false` cuando el sistema deniega el arranque**

```kotlin
@Test
fun `el sistema deniega el arranque en segundo plano y el conector lo cuenta`() = runTest {
    val connector = AndroidTunnelConnector { throw BackgroundStartDeniedException("denegado") }

    assertFalse(connector.connect("abc-123"))
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.quickconnect.AndroidTunnelConnectorTest"`
Expected: FAIL (`BackgroundStartDeniedException` no existe).

- [ ] **Step 3: Implement `BackgroundStartDeniedException` en `tunnel/BackgroundStartDeniedException.kt`**

- [ ] **Step 4: Implement `TunnelConnector` e `AndroidTunnelConnector`**

En `AndroidTunnelConnector.connect(configId)`: llamar a `startService(configId)` dentro de un `try/catch`; capturar `Exception`, devolver `false` y anotar en `AppLog.w`; devolver `true` si no hay excepción.

Nota de implementación: `TunnelForegroundService.start` aún no existe; se declara en Task 3. Por ahora basta que `AndroidTunnelConnector` acepte el `startService` y lo llame; la referencia a `TunnelForegroundService.start` se resuelve en el cableado de `AppContainer` (Task 3).

- [ ] **Step 5: Run test — `AndroidTunnelConnectorTest` pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.quickconnect.AndroidTunnelConnectorTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/quickconnect/TunnelConnector.kt
git add app/src/main/java/com/usipipo/vpn/quickconnect/AndroidTunnelConnector.kt
git add app/src/main/java/com/usipipo/vpn/tunnel/BackgroundStartDeniedException.kt
git add app/src/test/java/com/usipipo/vpn/quickconnect/FakeTunnelConnector.kt
git add app/src/test/java/com/usipipo/vpn/quickconnect/AndroidTunnelConnectorTest.kt
git commit -m "feat(quickconnect): contrato del conector para conectar en segundo plano"
```

---

### Task 2: El controlador de acceso rápido delega la conexión en el conector

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectController.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectAction.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectState.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/widget/WidgetStateMapper.kt`
- Test: `app/src/test/java/com/usipipo/vpn/quickconnect/QuickConnectControllerTest.kt`
- Test: `app/src/test/java/com/usipipo/vpn/widget/WidgetStateMapperTest.kt`

**Interfaces:**
- Consumes: `TunnelConnector.connect(String): Boolean` (Task 1), `FakeTunnelConnector` (Task 1).
- Produces: `QuickConnectAction.BackgroundStartDenied` (nueva acción sealed); `QuickConnectFailure.BackgroundStartDenied(val configId: String)` (nuevo fallo, mapeado a `WidgetStatus.Failed` mientras el túnel no esté activo); `QuickConnectController.toggle()` llama a `connector.connect(id)` en lugar de llamar directamente a `tunnelManager.connect(id, text)`.

- [ ] **Step 1: Write failing test — `toggle` pide la conexión al conector y devuelve `ConnectRequested` cuando tiene éxito**

```kotlin
@Test
fun `conectar usa el conector y devuelve ConnectRequested cuando arranca`() = runTest {
    val store = FakeConfigStore(listOf(casa, trabajo))
    val tunnel = FakeTunnelManager()
    val connector = FakeTunnelConnector(result = true)
    val controller = controller(store, tunnel, permissionChecker = { true }, connector = connector)

    val action = controller.toggle()

    assertEquals(QuickConnectAction.ConnectRequested("a"), action)
    assertEquals(listOf("a"), connector.requestedIds)
    assertTrue(tunnel.connectCalls.isEmpty())
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.quickconnect.QuickConnectControllerTest"`
Expected: FAIL (`FakeTunnelConnector` y el parámetro `connector` del helper `controller` no existen).

- [ ] **Step 2: Write failing test — `toggle` devuelve `BackgroundStartDenied` si el conector devuelve `false`**

```kotlin
@Test
fun `toggle devuelve BackgroundStartDenied si el conector deniega el arranque`() = runTest {
    val store = FakeConfigStore(listOf(casa, trabajo))
    val tunnel = FakeTunnelManager()
    val connector = FakeTunnelConnector(result = false)
    val controller = controller(store, tunnel, permissionChecker = { true }, connector = connector)

    assertEquals(QuickConnectAction.BackgroundStartDenied, controller.toggle())
    assertTrue(tunnel.connectCalls.isEmpty())
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.quickconnect.QuickConnectControllerTest"`
Expected: FAIL (`QuickConnectAction.BackgroundStartDenied` no existe).

- [ ] **Step 3: Write failing test — no se llama al conector si falta el permiso, no hay perfil o el perfil es ilegible**

```kotlin
@Test
fun `conectar se ignora si falta el permiso vpn sin tocar el conector`() = runTest {
    val connector = FakeTunnelConnector()
    val controller = controller(FakeConfigStore(listOf(casa)), FakeTunnelManager(), permissionChecker = { false }, connector = connector)

    assertEquals(QuickConnectAction.VpnPermissionRequired, controller.toggle())
    assertTrue(connector.requestedIds.isEmpty())
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.quickconnect.QuickConnectControllerTest"`
Expected: FAIL.

- [ ] **Step 4: Implement `QuickConnectFailure.BackgroundStartDenied` en `quickconnect/QuickConnectState.kt`**

Añadir a `QuickConnectState.kt`:

```kotlin
data class BackgroundStartDenied(val configId: String) : QuickConnectFailure
```

- [ ] **Step 5: Implement `QuickConnectAction.BackgroundStartDenied` en `quickconnect/QuickConnectAction.kt`**

Añadir a la sealed interface:

```kotlin
data object BackgroundStartDenied : QuickConnectAction
```

- [ ] **Step 6: Implement el delegado al conector en `QuickConnectController.toggle()`**

Añadir `private val connector: TunnelConnector` al constructor. Sustituir la línea `tunnelManager.connect(id, text)` por:

```kotlin
val started = connector.connect(id)
if (!started) {
    AppLog.w("AccesoRapido", "El sistema denegó el arranque en segundo plano (cfg ${id.take(8)})")
    _failure.value = QuickConnectFailure.BackgroundStartDenied(id)
    _state.value = _state.value.copy(failure = _failure.value)
    return QuickConnectAction.BackgroundStartDenied
}
_state.value = _state.value.copy(
    tunnel = tunnelManager.state.value,
    selectedConfig = configs.firstOrNull { it.id == id },
    failure = null,
)
return QuickConnectAction.ConnectRequested(id)
```

Nota: el controlador todavía lee el texto antes de delegar (`ConfigUnreadable` si no se puede leer); el texto viaja al servicio solo dentro del túnel (Task 3), nunca por un Intent.

- [ ] **Step 7: Write failing test — la denegación del sistema se presenta como fallo en el widget**

```kotlin
@Test
fun `la denegacion del sistema se presenta como fallo si no hay tunel`() {
    val p = WidgetStateMapper.map(
        QuickConnectState(
            selectedConfig = casa,
            failure = QuickConnectFailure.BackgroundStartDenied("a"),
        ),
    )

    assertEquals(WidgetStatus.Failed, p.status)
    assertEquals(R.string.widget_status_failed, p.statusText)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.WidgetStateMapperTest"`
Expected: FAIL (el fallo nuevo no está mapeado y el estado vivo «Desconectado» gana).

- [ ] **Step 8: Implement el mapeo del fallo nuevo en `WidgetStateMapper.status()`**

En `widget/WidgetStateMapper.kt`, dentro de `status(state)`, añadir antes del `when` sobre el túnel:

```kotlin
val deniedMatches = state.failure is QuickConnectFailure.BackgroundStartDenied &&
    state.failure.configId == selectedConfigId
if (deniedMatches && !state.tunnel.isActive) return WidgetStatus.Failed
```

El estado vivo manda cuando el túnel está activo (p. ej. la Activity de reserva ya conectó): por eso la condición incluye `!state.tunnel.isActive`.

- [ ] **Step 9: Update helper `controller` y el helper de `FakeConfigStore` en el test**

Añadir el parámetro `connector: TunnelConnector` al helper `TestScope.controller(...)` y pasarlo al constructor de `QuickConnectController`.

- [ ] **Step 10: Run tests — `QuickConnectControllerTest` y `WidgetStateMapperTest` pasan**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.quickconnect.QuickConnectControllerTest" --tests "com.usipipo.vpn.widget.WidgetStateMapperTest"`
Expected: PASS.

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectController.kt
git add app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectAction.kt
git add app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectState.kt
git add app/src/main/java/com/usipipo/vpn/widget/WidgetStateMapper.kt
git add app/src/test/java/com/usipipo/vpn/quickconnect/QuickConnectControllerTest.kt
git add app/src/test/java/com/usipipo/vpn/widget/WidgetStateMapperTest.kt
git commit -m "feat(quickconnect): delegar la conexión en el conector del servicio"
```

---

### Task 3: Servicio en primer plano que hospeda el túnel, mapper de notificación y manifiesto

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/tunnel/TunnelForegroundService.kt`
- Create: `app/src/main/java/com/usipipo/vpn/tunnel/TunnelNotificationMapper.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/usipipo/vpn/tunnel/TunnelNotificationMapperTest.kt`
- Test: `app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt`

**Interfaces:**
- Consumes: `TunnelConnector` y `BackgroundStartDeniedException` (Task 1); `WidgetStateMapper.map` (reutilización del mapeo de estado a texto/recursos).
- Produces: `class TunnelForegroundService : Service()` con acción `ACTION_CONNECT` + extra `EXTRA_CONFIG_ID` y `ACTION_DISCONNECT`; `object TunnelNotificationMapper { fun map(state: TunnelState): TunnelNotification }` con `data class TunnelNotification(val titleRes: Int, val textRes: Int, val ongoing: Boolean)`; manifest con los permisos `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_SPECIAL_USE`, el servicio `.tunnel.TunnelForegroundService` (`foregroundServiceType="specialUse"`), la propiedad `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, y los metadatos `ACTIVE_TILE`/`TOGGLEABLE_TILE` del mosaico.

- [ ] **Step 1: Write failing tests — `TunnelNotificationMapper` mapea cada estado del túnel**

```kotlin
@Test
fun `conectando mapea a texto de conectando y notificacion viva`() {
    val n = TunnelNotificationMapper.map(TunnelState.Connecting("a"))
    assertEquals(R.string.widget_status_connecting, n.textRes)
    assertTrue(n.ongoing)
}

@Test
fun `conectado mapea a texto de activo y notificacion viva`() {
    val n = TunnelNotificationMapper.map(TunnelState.Connected("a"))
    assertEquals(R.string.widget_status_active, n.textRes)
    assertTrue(n.ongoing)
}

@Test
fun `sin respuesta mantiene la notificacion viva`() {
    val n = TunnelNotificationMapper.map(TunnelState.NoResponse("a"))
    assertEquals(R.string.widget_status_no_response, n.textRes)
    assertTrue(n.ongoing)
}

@Test
fun `desconectado y fallo retiran la notificacion`() {
    assertFalse(TunnelNotificationMapper.map(TunnelState.Disconnected).ongoing)
    assertFalse(TunnelNotificationMapper.map(TunnelState.Failed(null)).ongoing)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.tunnel.TunnelNotificationMapperTest"`
Expected: FAIL (`TunnelNotificationMapper` no existe).

- [ ] **Step 2: Implement `TunnelNotificationMapper.kt` (paquete `tunnel`)**

`data class TunnelNotification(val titleRes: Int, val textRes: Int, val ongoing: Boolean)` y un `object TunnelNotificationMapper` con `fun map(state: TunnelState): TunnelNotification`. Reutilizar los recursos `R.string.widget_status_*` para el texto (`Disconnected`, `Connecting`, `Connected`, `NoResponse`, `Failed` → `ongoing` verdadero solo para `Connecting`/`Connected`/`NoResponse`). El título es siempre `R.string.app_name`.

- [ ] **Step 3: Run test — `TunnelNotificationMapperTest` pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.tunnel.TunnelNotificationMapperTest"`
Expected: PASS.

- [ ] **Step 4: Implement `TunnelForegroundService`**

Clave de diseño: `startForeground()` es lo primero y se hace de forma síncrona (dentro de los 5 s). Todo lo que toca al túnel va después, dentro de una coroutine del servicio.

- `onCreate`: crear el canal de notificación (`IMPORTANCE_LOW`, sin sonido).
- `onStartCommand(intent, flags, startId)`:
  - `ACTION_CONNECT`: `configId` del extra; `startForeground(NOTIFICATION_ID, buildNotification(TunnelState.Connecting(configId), profileName))` de forma síncrona; luego lanzar en `serviceScope` la lectura del perfil (`container.configStore.readText(configId)` — si es `null`, conectar con texto vacío y anotar en `AppLog.w`) y `container.tunnelManager.connect(configId, texto)`, y finalmente observar `container.tunnelManager.state`: cuando pase a `Disconnected` o `Failed`, `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`.
  - `ACTION_DISCONNECT`: `container.tunnelManager.disconnect()`.
  - otro: `stopSelf()`.
- `serviceScope`: `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`, cancelado en `onDestroy`.
- `buildNotification(state, profileName)`: `NotificationCompat.Builder` con `setSmallIcon(R.drawable.ic_shield_check)`, `setContentTitle(app_name)`, `setContentText(texto del estado)`, `setSubText(nombre del perfil)`, `setOngoing(ongoing)`, `setOnlyAlertOnce(true)`, `setContentIntent` hacia `MainActivity`, y una acción «Desconectar» (`ACTION_DISCONNECT`) con `PendingIntent.getService` inmutable.
- `companion object`: `fun start(context: Context, configId: String)` que construye el `Intent` con `ACTION_CONNECT` y llama a `ContextCompat.startForegroundService`; captura `IllegalStateException` y lanza `BackgroundStartDeniedException` (Task 1).

Nota de versión: `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` requiere API 34; en API 29-33 se llama a `startForeground(id, notificación)` sin tipo y en API 26-28 la sobrecarga de dos argumentos; el tipo `specialUse` se declara en el manifiesto (ignorada en plataformas antiguas).

- [ ] **Step 5: Run test — `TunnelNotificationMapperTest` sigue pasando; `assembleDebug` compila el servicio**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: PASS y APK de debug sin errores.

- [ ] **Step 6: Escribir las sentencias del manifiesto**

Añadir a `<manifest>`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

y dentro de `<application>`:

```xml
<service
    android:name=".tunnel.TunnelForegroundService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Mantiene el tunel WireGuard conectado mientras la app esta en segundo plano" />
</service>
```

- [ ] **Step 7: Write failing tests de manifiesto en `SystemIntegrationResourceTest`**

```kotlin
@Test
fun `el manifiesto declara los permisos del servicio en primer plano`() {
    val manifest = parse(File(MANIFEST_PATH))
    listOf(
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
    ).forEach { permiso ->
        assertTrue("falta el permiso $permiso en el manifiesto",
            manifest.getElementsByTagName("uses-permission").elements()
                .any { it.androidAttribute("name") == permiso })
    }
}

@Test
fun `el manifiesto declara el servicio en primer plano con tipo especial`() {
    val service = manifestService(TUNNEL_SERVICE_NAME)
    assertEquals("false", service.androidAttribute("exported"))
    assertEquals("specialUse", service.androidAttribute("foregroundServiceType"))
    val property = service.childElements().singleOrNull {
        it.tagName == "property" && it.androidAttribute("name") == "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
    } ?: throw AssertionError("falta la propiedad PROPERTY_SPECIAL_USE_FGS_SUBTYPE")
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.SystemIntegrationResourceTest"`
Expected: FAIL (faltan los permisos y el servicio).

Antes de escribir los tests, adaptar el helper del test al nuevo manifiesto: ahora hay dos `<service>`, así que `manifestService()` pasa a recibir el nombre.

```kotlin
private fun manifestService(name: String): Element =
    parse(File(MANIFEST_PATH))
        .getElementsByTagName("service")
        .elements()
        .singleOrNull { it.androidAttribute("name") == name }
        ?: throw AssertionError("no existe el service $name en el manifiesto")
```

Añadir las constantes `TUNNEL_SERVICE_NAME = ".tunnel.TunnelForegroundService"` y `TILE_SERVICE_NAME = ".widget.UsipipoTileService"`, y actualizar la llamada existente del test `el manifiesto declara el mosaico de ajustes rapidos` a `manifestService(TILE_SERVICE_NAME)`.

- [ ] **Step 8: Run test — manifiesto pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.SystemIntegrationResourceTest"`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/tunnel/TunnelForegroundService.kt
git add app/src/main/java/com/usipipo/vpn/tunnel/TunnelNotificationMapper.kt
git add app/src/main/AndroidManifest.xml
git add app/src/test/java/com/usipipo/vpn/tunnel/TunnelNotificationMapperTest.kt
git add app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt
git commit -m "feat(tunnel): servicio en primer plano que hospeda el tunel"
```

---

### Task 4: Inicio usa el mismo camino (HomeViewModel, cableado en AppContainer y UsipipoRoot)

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/AppContainer.kt`
- Test: `app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelTest.kt`
- Test: `app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelFavoritesTest.kt`
- Test: `app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelRenameTest.kt`

**Interfaces:**
- Consumes: `TunnelConnector.connect(String): Boolean` (Task 1).
- Produces: `HomeViewModel(store, tunnelManager, connector)` donde `connectSelected()` valida el perfil, llama a `connector.connect(id)` y, si devuelve `false` (la app está en primer plano), conecta directamente con `tunnelManager.connect(id, text)` y lo anota en `AppLog`; `AppContainer.tunnelConnector: TunnelConnector`; `HomeViewModel(container.configStore, container.tunnelManager, container.tunnelConnector)` en `UsipipoRoot.kt`.

- [ ] **Step 1: Write failing test — `connectSelected` pide la conexión al conector**

```kotlin
@Test
fun `conectar pide la conexion al conector`() = runTest {
    val tunnel = FakeTunnelManager()
    val connector = FakeTunnelConnector(result = true)
    val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), tunnel, connector)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    vm.select("b")
    vm.connectSelected()

    assertEquals(listOf("b"), connector.requestedIds)
    assertTrue(tunnel.connectCalls.isEmpty())
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.ui.home.HomeViewModelTest"`
Expected: FAIL (`HomeViewModel` con tres parámetros y `connector.requestedIds` no existen).

- [ ] **Step 2: Write failing test — si el conector deniega, `connectSelected` conecta directamente porque la app está en primer plano**

```kotlin
@Test
fun `conectar con conector denegado conecta directamente`() = runTest {
    val tunnel = FakeTunnelManager()
    val connector = FakeTunnelConnector(result = false)
    val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), tunnel, connector)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    vm.select("b")
    vm.connectSelected()

    assertEquals(listOf("b" to "conf-de-b"), tunnel.connectCalls)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.ui.home.HomeViewModelTest"`
Expected: FAIL.

- [ ] **Step 3: Write failing test — perfil ilegible no conecta ni por el conector ni directamente**

```kotlin
@Test
fun `conectar con perfil ilegible no conecta y muestra ConfigUnreadable`() = runTest {
    val tunnel = FakeTunnelManager()
    val connector = FakeTunnelConnector(result = true)
    val store = FakeConfigStore(listOf(casa), unreadableIds = setOf("a"))
    val vm = HomeViewModel(store, tunnel, connector)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    vm.select("a")
    vm.connectSelected()

    assertEquals(HomeMessage.ConfigUnreadable, vm.uiState.value.message)
    assertTrue(tunnel.connectCalls.isEmpty())
    assertTrue(connector.requestedIds.isEmpty())
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.ui.home.HomeViewModelTest"`
Expected: FAIL.

- [ ] **Step 4: Implement `HomeViewModel(store, tunnelManager, connector)`**

Añadir `private val connector: TunnelConnector` al constructor. `connectSelected()`:

```kotlin
fun connectSelected() {
    val id = selectedConfigId(store.configs.value, store.lastUsedId.value) ?: return
    viewModelScope.launch {
        val text = store.readText(id)
        if (text == null) {
            AppLog.e("Inicio", "No se pudo leer la configuración seleccionada (cfg ${id.take(8)})")
            message.value = HomeMessage.ConfigUnreadable
        } else {
            val started = connector.connect(id)
            if (!started) {
                AppLog.w("Inicio", "El sistema denegó el servicio en primer plano; se conecta directamente (cfg ${id.take(8)})")
                tunnelManager.connect(id, text)
            }
        }
    }
}
```

- [ ] **Step 5: Cablear en `AppContainer` y `UsipipoRoot`**

En `AppContainer.kt` añadir:

```kotlin
val tunnelConnector: TunnelConnector by lazy {
    AndroidTunnelConnector { configId -> TunnelForegroundService.start(appContext, configId) }
}
```

y pasar `connector = tunnelConnector` al `QuickConnectController`.

En `ui/UsipipoRoot.kt` cambiar `initializer { HomeViewModel(container.configStore, container.tunnelManager) }` por:

```kotlin
initializer { HomeViewModel(container.configStore, container.tunnelManager, container.tunnelConnector) }
```

- [ ] **Step 6: Update las 14 instancias de `HomeViewModel` en los tres ficheros de test**

`HomeViewModel` gana un tercer parámetro obligatorio: hay que actualizar las 14 construcciones repartidas en `HomeViewModelTest.kt` (6), `HomeViewModelFavoritesTest.kt` (3) y `HomeViewModelRenameTest.kt` (3), añadiendo `FakeTunnelConnector()` como tercer argumento.

- [ ] **Step 7: Run test — `HomeViewModelTest` pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.ui.home.HomeViewModelTest"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/home/HomeViewModel.kt
git add app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt
git add app/src/main/java/com/usipipo/vpn/AppContainer.kt
git add app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelTest.kt
git add app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelFavoritesTest.kt
git add app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelRenameTest.kt
git commit -m "feat(ui): conectar desde Inicio por el servicio en primer plano"
```

---

### Task 5: Reserva por Activity transparente (widget y mosaico)

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/widget/QuickActionRouter.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/widget/VpnPermissionActivity.kt`
- Test: `app/src/test/java/com/usipipo/vpn/widget/QuickActionRouterTest.kt`

**Interfaces:**
- Consumes: `QuickConnectAction.BackgroundStartDenied` (Task 2).
- Produces: `internal enum class QuickActionTarget { None, PermissionActivity, Importer }` y `internal fun targetFor(action: QuickConnectAction): QuickActionTarget` (mapeo puro, sin dependencias de Android); el `when` de `QuickActionRouter.toggle()` usa `targetFor`; `VpnPermissionActivity.handleAction` trata `BackgroundStartDenied` anotándolo en el registro (el `when` queda exhaustivo).

- [ ] **Step 1: Write failing tests — `targetFor` mapea cada acción**

```kotlin
@Test
fun `denegacion del sistema abre la activity de permiso`() {
    assertEquals(QuickActionTarget.PermissionActivity, targetFor(QuickConnectAction.BackgroundStartDenied))
    assertEquals(QuickActionTarget.PermissionActivity, targetFor(QuickConnectAction.VpnPermissionRequired))
    assertEquals(QuickActionTarget.Importer, targetFor(QuickConnectAction.OpenImporter))
    assertEquals(QuickActionTarget.None, targetFor(QuickConnectAction.DisconnectRequested))
    assertEquals(QuickActionTarget.None, targetFor(QuickConnectAction.ConnectRequested("a")))
    assertEquals(QuickActionTarget.None, targetFor(QuickConnectAction.ConfigUnreadable("a")))
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.QuickActionRouterTest"`
Expected: FAIL (`QuickActionRouterTest`, `QuickActionTarget` y `targetFor` no existen).

- [ ] **Step 2: Implement `QuickActionTarget` y `targetFor` en `widget/QuickActionRouter.kt`**

Colocar ambos en `widget/QuickActionRouter.kt` con visibilidad `internal` (los tests del mismo módulo acceden a `internal`):

```kotlin
internal enum class QuickActionTarget { None, PermissionActivity, Importer }

internal fun targetFor(action: QuickConnectAction): QuickActionTarget = when (action) {
    QuickConnectAction.DisconnectRequested,
    is QuickConnectAction.ConnectRequested,
    is QuickConnectAction.ConfigUnreadable,
    -> QuickActionTarget.None

    QuickConnectAction.VpnPermissionRequired,
    QuickConnectAction.BackgroundStartDenied,
    -> QuickActionTarget.PermissionActivity

    QuickConnectAction.OpenImporter -> QuickActionTarget.Importer
}
```

- [ ] **Step 3: Implement el `when` del router basado en `targetFor`**

```kotlin
when (targetFor(controller.toggle())) {
    QuickActionTarget.None -> Unit
    QuickActionTarget.PermissionActivity -> launchActivity(context, Intent(context, VpnPermissionActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    QuickActionTarget.Importer -> launchActivity(context, WidgetIntents.importerIntent(context))
}
```

- [ ] **Step 4: Implement `VpnPermissionActivity.handleAction` para `BackgroundStartDenied`**

Añadir al `when` de `handleAction`:

```kotlin
QuickConnectAction.BackgroundStartDenied -> {
    AppLog.w("Permiso", "El sistema denegó el arranque en segundo plano")
}
```

(el `when` queda exhaustivo).

- [ ] **Step 5: Run test — `QuickActionRouterTest` pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.QuickActionRouterTest"`
Expected: PASS.

- [ ] **Step 6: Compile check del resto del módulo**

Run: `./gradlew compileDebugKotlin`
Expected: PASS (el `when` exhaustivo de `handleAction` obliga a tratar la nueva acción).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/widget/QuickActionRouter.kt
git add app/src/main/java/com/usipipo/vpn/widget/VpnPermissionActivity.kt
git add app/src/test/java/com/usipipo/vpn/widget/QuickActionRouterTest.kt
git commit -m "fix(widget): abrir la Activity de permiso si el sistema deniega el servicio"
```

---

### Task 6: Widget 2×2 por defecto

**Files:**
- Modify: `app/src/main/res/xml/usipipo_widget_info.xml`
- Modify: `app/src/main/res/layout/widget_usipipo.xml`
- Test: `app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt`

**Interfaces:**
- Consumes: nada (tarea de recursos).
- Produces: `usipipo_widget_info.xml` con `targetCellWidth="2"`, `targetCellHeight="2"`, `minWidth="110dp"`, `minHeight="110dp"`; `widget_usipipo.xml` compacto (cabecera logo 20 dp + pastilla 20 dp, botón 36 dp, perfil 12 sp, país 10 sp, relleno 8 dp; altura total ≈ 106 dp que cabe en 2 celdas).

Nota: `minResizeWidth`/`minResizeHeight` se mantienen sin cambios (110 dp / 70 dp): el diseño ancho (`widget_usipipo_wide.xml`) no se modifica.

- [ ] **Step 1: Write failing tests — el widget declara 2×2 y mínimo de 2 celdas**

```kotlin
@Test
fun `el widget declara tamano por defecto de 2 celdas`() {
    val provider = widgetProviderInfo()
    assertEquals("2", provider.androidAttribute("targetCellWidth"))
    assertEquals("2", provider.androidAttribute("targetCellHeight"))
    assertEquals("110dp", provider.androidAttribute("minWidth"))
    assertEquals("110dp", provider.androidAttribute("minHeight"))
}

@Test
fun `el diseno compacto cabe en 2 celdas (110 dp)`() {
    val provider = widgetProviderInfo()
    val minHeight = provider.androidAttribute("minHeight").toDp()
    assertTrue("minHeight ($minHeight dp) debe ser al menos ${COMPACT_LAYOUT_MIN_HEIGHT_DP} dp",
        minHeight >= COMPACT_LAYOUT_MIN_HEIGHT_DP)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.SystemIntegrationResourceTest"`
Expected: FAIL (faltan `targetCell*` y `minWidth`/`minHeight` = 110dp).

- [ ] **Step 2: Implement `usipipo_widget_info.xml`**

Cambiar `minWidth="180dp"` → `110dp`, `minHeight="190dp"` → `110dp`, añadir `android:targetCellWidth="2"` y `android:targetCellHeight="2"`.

- [ ] **Step 3: Implement `widget_usipipo.xml` compacto**

Reducir el layout para que quepa en ≈106 dp: `padding="8dp"` en la raíz, logo `20dp`×`20dp` con padding 2dp, pastilla de estado `textSize="12sp"` con padding 6/2dp, botón de acción `40dp`×`40dp` con padding 8dp, progreso `32dp`×`32dp`, nombre de perfil `textSize="13sp"` con `marginTop="4dp"`, país `textSize="11sp"` con `marginTop="2dp"`. Conservar todos los ids (`widget_root`, `widget_logo`, `widget_status`, `widget_action`, `widget_progress`, `widget_profile_name`, `widget_country`).

- [ ] **Step 4: Update `COMPACT_LAYOUT_MIN_HEIGHT_DP` y añadir el comentario del desglose**

En `SystemIntegrationResourceTest.kt` cambiar `COMPACT_LAYOUT_MIN_HEIGHT_DP` de 186 a 106 y actualizar el KDoc:

```kotlin
/**
 * Alto que necesita el layout compacto vertical: padding 8+8, cabecera 20,
 * margen 4, botón 40, margen 2, perfil 15 (13 sp) y país 13 (11 sp).
 */
const val COMPACT_LAYOUT_MIN_HEIGHT_DP: Int = 106
```

- [ ] **Step 5: Run test — `SystemIntegrationResourceTest` pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.SystemIntegrationResourceTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/xml/usipipo_widget_info.xml
git add app/src/main/res/layout/widget_usipipo.xml
git add app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt
git commit -m "fix(widget): añadir el widget en 2x2 por defecto"
```

---

### Task 7: Mosaico añadible desde la app, repintado al cambiar de estado y robusto

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/usipipo/vpn/widget/UsipipoTileService.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/widget/WidgetUpdater.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/diagnostics/QuickAccessHint.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt`
- Test: `app/src/test/java/com/usipipo/vpn/widget/WidgetUpdaterTest.kt`

**Interfaces:**
- Consumes: `QuickConnectAction.BackgroundStartDenied` (Task 2).
- Produces: `UsipipoTileService.requestAdd(context: Context, onResult: (Boolean) -> Unit)` (API 33+: `StatusBarManager.requestAddTileService`; por debajo invoca `onResult(false)`); metadatos `android.service.quicksettings.ACTIVE_TILE` y `android.service.quicksettings.TOGGLEABLE_TILE` en el `<service>` del mosaico; `WidgetUpdater.apply()` captura el fallo de `refreshTile()` sin impedir el guardado del estado; `QuickAccessHint(onAddTile: () -> Unit, resultText: Int? = null)` con botón «Añadir mosaico a Ajustes rápidos»; strings nuevos `notification_disconnect`, `quick_access_add_tile`, `diag_tile_added`, `diag_tile_denied` y `quick_access_tile_hint` reescrito.

- [ ] **Step 1: Write failing test — un fallo al refrescar el mosaico no impide guardar el estado**

```kotlin
@Test
fun `un fallo al refrescar el mosaico no impide guardar el estado`() = runTest {
    val store = FakeWidgetStateStore()
    val states = MutableStateFlow(desconectado)
    val updater = updater(states, store = store, refreshTile = { error("el mosaico no responde") })

    updater.start()

    assertEquals(WidgetStatus.Disconnected, store.written?.status)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.WidgetUpdaterTest"`
Expected: FAIL (la excepción sale de `apply`).

- [ ] **Step 2: Implement el `try/catch` en `WidgetUpdater.apply()`**

```kotlin
private fun apply(state: QuickConnectState) {
    val presentation = WidgetStateMapper.map(state)
    invalidateStalePersistedState(presentation.status)
    renderWidget(state)
    runCatching { refreshTile() }
        .onFailure { AppLog.w(TAG, "No se pudo refrescar el mosaico", it) }
    store.write(PersistedWidgetState(presentation.status, clock()))
}
```

- [ ] **Step 3: Run test — `WidgetUpdaterTest` pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.WidgetUpdaterTest"`
Expected: PASS.

- [ ] **Step 4: Write failing test — el mosaico declara los metadatos de escucha activa**

```kotlin
@Test
fun `el mosaico declara los metadatos de escucha activa y conmutable`() {
    val service = manifestService(TILE_SERVICE_NAME)
    listOf(
        "android.service.quicksettings.ACTIVE_TILE",
        "android.service.quicksettings.TOGGLEABLE_TILE",
    ).forEach { nombre ->
        val meta = service.childElements().singleOrNull {
            it.tagName == "meta-data" && it.androidAttribute("name") == nombre
        } ?: throw AssertionError("falta el metadato $nombre")
        assertEquals("true", meta.androidAttribute("value"))
    }
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.SystemIntegrationResourceTest"`
Expected: FAIL (faltan los metadatos).

- [ ] **Step 5: Implement los metadatos del mosaico en el manifiesto**

Dentro del `<service android:name=".widget.UsipipoTileService" ...>`:

```xml
<meta-data android:name="android.service.quicksettings.ACTIVE_TILE" android:value="true" />
<meta-data android:name="android.service.quicksettings.TOGGLEABLE_TILE" android:value="true" />
```

- [ ] **Step 6: Run test — `SystemIntegrationResourceTest` pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.SystemIntegrationResourceTest"`
Expected: PASS.

- [ ] **Step 7: Implement `UsipipoTileService.requestAdd(context, onResult)`**

```kotlin
/** Pide al sistema que añada el mosaico al panel (API 33+). En versiones anteriores no hay API. */
fun requestAdd(context: Context, onResult: (Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onResult(false)
        return
    }
    val statusBar = context.getSystemService(StatusBarManager::class.java) ?: run {
        onResult(false)
        return
    }
    statusBar.requestAddTileService(
        ComponentName(context, UsipipoTileService::class.java),
        context.getString(R.string.app_name),
        Icon.createWithResource(context, R.drawable.ic_shield_check),
        context.mainExecutor,
        Consumer { added -> onResult(added) },
    )
}
```

Imports necesarios: `android.app.StatusBarManager`, `java.util.function.Consumer` (el `Consumer` de Java, no el de Kotlin).

- [ ] **Step 8: Añadir los strings nuevos y reescribir la pista del mosaico**

En `res/values/strings.xml`:

```xml
<string name="notification_disconnect">Desconectar</string>
<string name="quick_access_add_tile">Añadir mosaico a Ajustes rápidos</string>
<string name="diag_tile_added">Mosaico añadido a Ajustes rápidos</string>
<string name="diag_tile_denied">No se pudo añadir el mosaico</string>
```

Reescribir `quick_access_tile_hint` para nombrar la ruta de HyperOS: «Desliza el panel de control, toca el lápiz de editar y añade uSipipo VPN.» (el texto anterior («Abre el panel de ajustes rápidos, toca Editar…») no describe la ruta del fabricante).

- [ ] **Step 9: Implement el botón en `QuickAccessHint`**

Cambiar la firma a `fun QuickAccessHint(onAddTile: () -> Unit, resultText: Int? = null)` y añadir, debajo de los dos pasos, un `Button` con `stringResource(R.string.quick_access_add_tile)` que llama a `onAddTile()`, y un `Text` con `resultText?.let { stringResource(it) }` cuando no sea `null`. Actualizar el `@Preview` (`PreviewQuickAccessHint`) para pasar `onAddTile = {}`.

- [ ] **Step 10: Conectar el botón en `DiagnosticsScreen`**

Donde hoy se llama `QuickAccessHint()` (línea ~125), obtener el `Context` con `LocalContext.current`, mantener un estado local `var tileResult by remember { mutableStateOf<Int?>(null) }` y pasar:

```kotlin
QuickAccessHint(
    onAddTile = {
        UsipipoTileService.requestAdd(context) { added ->
            tileResult = if (added) R.string.diag_tile_added else R.string.diag_tile_denied
        }
    },
    resultText = tileResult,
)
```

La pista solo se muestra desde Android 13 (`shouldShowQuickAccessHint`), que es justo donde existe `requestAddTileService`.

- [ ] **Step 11: Compile check y suite**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: PASS.

- [ ] **Step 12: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git add app/src/main/java/com/usipipo/vpn/widget/UsipipoTileService.kt
git add app/src/main/java/com/usipipo/vpn/widget/WidgetUpdater.kt
git add app/src/main/java/com/usipipo/vpn/ui/diagnostics/QuickAccessHint.kt
git add app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt
git add app/src/main/res/values/strings.xml
git add app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt
git add app/src/test/java/com/usipipo/vpn/widget/WidgetUpdaterTest.kt
git commit -m "feat(widget): mosaico añadible desde la app y repintado al cambiar de estado"
```

---

### Task 8: Verificación integral y checklist manual

**Files:**
- Modify: `docs/plans/2026-10-26-widget-beta-checklist.md`

**Interfaces:**
- Consumes: todo lo anterior.
- Produces: la matriz manual ampliada para el beta tester (widget 2×2, conexión en segundo plano, notificación, mosaico, permiso revocado).

- [ ] **Step 1: Run suite completa**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (todos los tests JVM verdes).

- [ ] **Step 2: Build del APK de debug**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Ampliar el checklist manual**

En `docs/plans/2026-10-26-widget-beta-checklist.md` añadir una sección «Accesos rápidos en segundo plano (Android 16, Xiaomi)» con esta matriz:

| # | Paso | Resultado esperado |
|---|---|---|
| 1 | Añadir el widget desde Widgets | Se coloca en 2×2 y el contenido es legible |
| 2 | Cerrar la app por completo y tocar el botón del widget | Conecta; aparece la notificación «uSipipo VPN · Conectando…» y luego «Activo» |
| 3 | Con la app cerrada, tocar «Desconectar» en la notificación | El túnel baja y la notificación desaparece |
| 4 | Con el widget en 2×2, redimensionar a 1×2 | El launcher recorta el diseño sin romper el botón |
| 5 | Añadir el mosaico con el botón de Diagnóstico | Aparece el diálogo del sistema y, al aceptar, el mosaico está en el panel |
| 6 | Con la app cerrada, tocar el mosaico | Conecta y el mosaico queda encendido |
| 7 | Cambiar el estado del túnel con el panel de ajustes abierto | El mosaico se repinta sin cerrar el panel |
| 8 | Revocar el permiso de VPN en Ajustes y tocar el widget | La app vuelve a pedir el permiso |
| 9 | Dejar el túnel activo 10 min con la app en segundo plano | El túnel sigue vivo; no aparece «El sistema cerró el túnel» en el registro |
| 10 | Borrar el perfil seleccionado en la app y tocar el widget en los 2 s siguientes | El registro anota que el perfil desapareció y el widget muestra «Fallo» (no «Desconectado») |

- [ ] **Step 4: Commit**

```bash
git add docs/plans/2026-10-26-widget-beta-checklist.md
git commit -m "docs(diagnostics): checklist manual de accesos rápidos en segundo plano"
```

---

## Notas de ejecución

- **Orden de ejecución: 1 → 3 → 2 → 4 → 5 → 6 → 7 → 8.** La Task 3 va antes que la Task 2 porque la Task 2 da a `QuickConnectController` un parámetro `connector` obligatorio y `AppContainer` (único punto de construcción) necesita `TunnelForegroundService.start` para cablearlo: sin la Task 3 antes, el módulo no compilaría al terminar la Task 2. La Task 1 va la primera porque las demás usan `BackgroundStartDeniedException`; la 4 y la 5 necesitan la 2 (la acción y el fallo nuevos).
- **Ramas**: todo el trabajo va en `feat/fase7-accesos-rapidos` (ya creada desde `main`). No se hace push a `main`.
- **Verificación por tarea**: cada tarea termina con su comando de test en verde y su commit; la suite completa se ejecuta en la Task 8 y también antes de abrir la PR.
- **Lo que no se toca**: `tunnel/WireGuardTunnelManager.kt`, `tunnel/TunnelManager.kt` y `gradle/libs.versions.toml`.
