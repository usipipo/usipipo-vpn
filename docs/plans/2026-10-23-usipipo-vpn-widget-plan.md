# uSipipo VPN: widget y mosaico de conexión rápida — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Lee antes `AGENTS.md`, `CODING_STANDARDS.md` y el spec aprobado.

**Goal:** permitir conectar y desconectar el perfil activo desde un widget de pantalla de inicio y desde un mosaico de ajustes rápidos, sin abrir la UI de la app y sin modificar la lógica del túnel.

**Architecture:** un `QuickConnectController` puro coordina `ConfigStore` y `TunnelManager`, decide entre conectar, desconectar, pedir permiso o abrir Importar, y expone un `StateFlow` con el perfil seleccionado y el estado real. Un `WidgetUpdater` observa ese flujo y publica una `RemoteViews` completa y el estado nativo del mosaico; un mapper puro traduce el estado a textos, iconos, colores y geometría visual. La Activity de permiso transparente solo aparece cuando `VpnService.prepare()` indica que falta el permiso.

**Tech Stack:** Kotlin 2.3.21, Android SDK 37, minSdk 26, targetSdk 36, Java 17, `AppWidgetProvider`, `RemoteViews`, `TileService`, `VpnService.prepare`, StateFlow/SharedFlow, JUnit 4 sobre JVM. Sin dependencias nuevas y sin Jetpack Glance.

**Spec:** `docs/plans/2026-10-23-usipipo-vpn-widget-design.md` (aprobado). Este plan ejecuta esa spec; donde la spec y el código actual discrepan, manda la spec, con las decisiones de cierre explícitas de este documento.

## Global Constraints

- Rama: `feat/mockup-fase6`, creada desde `main`. No se hace push a `main` ni se modifican versiones del toolchain.
- No se modifica `app/src/main/java/com/usipipo/vpn/tunnel/` ni el contrato de `TunnelManager`.
- No se añaden dependencias. El widget usa exclusivamente `RemoteViews` y `AppWidgetProvider`; el mosaico usa `TileService`.
- No hay selector de perfil, estadísticas en vivo, pantalla de configuración del widget ni animaciones.
- El widget no usa sombras, desenfoques ni imágenes rasterizadas pesadas.
- No se consulta la red para obtener país: solo se usa `SavedConfig.countryCode` y `SavedConfig.countryName`.
- `AppWidgetProvider.updatePeriodMillis` vale `0`; no existe refresco periódico ni servicio en segundo plano nuevo.
- Cada `RemoteViews` enviada a `AppWidgetManager` es una representación completa del widget, no un parche parcial.
- Todo texto visible sale de `res/values/strings.xml`; los nombres de perfil se recortan a una línea con elipsis.
- El registro contiene solo metadatos: acción, estado e ID corto del perfil. Nunca claves, texto `.conf`, endpoint ni dirección IP.
- El estado vivo de `TunnelManager.state` siempre prevalece sobre un estado anterior dibujado o persistido.
- Imports ordenados y sin uso, tipos explícitos en firmas públicas, sin `!!`, sin imports comodín.
- TDD obligatorio en lógica: test rojo antes de implementación y refactor posterior.
- Commits en español, convencionales, minúsculos y con un cambio lógico por commit.
- En este entorno no hay JDK local. La verificación autoritativa es `./gradlew testDebugUnitTest` y `assembleDebug` en GitHub Actions.

### Decisiones de cierre de la spec

1. **Selección del perfil:** se extrae `selectedConfigId(configs, lastUsedId)` y se reutiliza tanto en `HomeViewModel` como en el controlador. Si la última usada ya no existe, se usa la primera disponible.
2. **Toques durante `Connecting`:** no se duplica la conexión; el toque durante `Connecting`, `Connected` o `NoResponse` solicita desconexión. `Failed` sí permite reintentar.
3. **Orden sin perfiles y sin permiso:** primero se comprueba que exista un perfil. Sin perfiles se abre Importar, aunque falte el permiso VPN.
4. **Perfil ilegible:** el controlador publica un fallo local asociado al perfil. El mapper lo muestra como «Fallo» y el siguiente toque vuelve a leer el archivo y reintenta.
5. **Proceso muerto:** al arrancar de nuevo, `TunnelManager.state` empieza en `Disconnected`. Ese estado vivo corrige el widget aunque el último estado persistido fuese activo. Se persiste el estado semántico y su hora solo para diagnóstico y reconciliación.
6. **Mosaico en transición:** `Tile` no ofrece un cuarto estado nativo. «Conectando…» se representa como `Tile.STATE_INACTIVE` con descripción/subtítulo «Conectando…».
7. **Abrir Importar:** requiere un contrato de intent porque la navegación actual es estado local de Compose. Se añaden `MainActivity.EXTRA_INITIAL_TAB`, `MainTab.fromKey(...)` y un `initialTab` en `UsipipoRoot`.

## Review Focus

1. **Estado visual obsoleto tras morir el proceso:** un toque no debe desconectar un túnel inexistente; el estado vivo `Disconnected` debe mandar y el toque debe conectar. → Tarea 1 y Tarea 6.
2. **Toques repetidos durante `Connecting`:** no puede haber dos llamadas a `connect`; el toque permitido debe ser el que desconecta. → Tarea 1.
3. **Sin perfiles y sin permiso:** debe abrirse Importar, no el diálogo VPN, y no debe quedar un estado «Conectando» falso. → Tarea 1 y Tarea 3.
4. **Perfil ilegible o borrado:** el widget debe mostrar «Fallo» o pasar al siguiente perfil, respectivamente, sin filtrar el `.conf` al registro. → Tarea 1 y Tarea 6.
5. **Permiso revocado entre render y toque:** la comprobación se hace de nuevo en el controlador; nunca se intenta conectar sin permiso. → Tarea 1 y Tarea 3.

---

## Trazabilidad del spec

| Requisito del diseño aprobado | Tarea que lo implementa |
|---|---|
| Widget clásico con `RemoteViews` y `AppWidgetProvider`, sin Glance | Tarea 4 |
| Mosaico de ajustes rápidos | Tarea 5 |
| `QuickConnectController` reutilizando `ConfigStore` y `TunnelManager` | Tarea 1 |
| `WidgetStateMapper` puro, texto/icono/color por estado | Tarea 2 |
| `VpnPermissionActivity` transparente solo cuando falta permiso | Tarea 3 |
| Botón de toggle: desconectar en activo, conectar con permiso, abrir Importar sin perfiles | Tarea 1 y Tarea 3 |
| Nombre de perfil y país cacheado, sin selector de perfil | Tarea 2 y Tarea 4 |
| Layout 2×2 y 4×2, tema claro y oscuro, sin animaciones ni sombras | Tarea 2 y Tarea 4 |
| `WidgetUpdater` reactivo, sin refresco periódico | Tarea 6 |
| Estado desactualizado tras muerte de proceso, arranque o actualización | Tarea 1, Tarea 6 y Tarea 8 |
| Cambios de manifest para receiver, servicio y Activity | Tareas 3, 4, 5 y 6 |
| Registro solo con metadatos | Tareas 1, 6 y 8 |
| Perfil ilegible, eliminado, toques repetidos y permiso denegado | Tarea 1 |
| Cierre por otra VPN y detección de estado real | Tarea 8, con `TunnelManager` existente |
| Pista de widget/mosaico en Android 13+ | Tarea 7 |
| Prueba manual completa con APK y CI | Tarea 8 |

---

## Estructura de archivos

### Nuevos módulos Kotlin

| Archivo | Responsabilidad única |
|---|---|
| `domain/ConfigSelection.kt` | Criterio compartido de perfil seleccionado. |
| `quickconnect/QuickConnectState.kt` | Estado combinado de túnel, perfil y fallo local. |
| `quickconnect/QuickConnectAction.kt` | Resultado accionable de una pulsación. |
| `quickconnect/QuickConnectController.kt` | Decisión conectar/desconectar y estado observable, sin Android. |
| `quickconnect/VpnPermissionChecker.kt` | Contrato mínimo para comprobar permiso VPN. |
| `quickconnect/AndroidVpnPermissionChecker.kt` | Adaptador de `VpnService.prepare()`. |
| `widget/WidgetStatus.kt` | Estados semánticos del widget. |
| `widget/WidgetPresentation.kt` | Presentación completa y testeable de un estado. |
| `widget/WidgetStateMapper.kt` | Mapper puro de `QuickConnectState` a `WidgetPresentation`. |
| `widget/WidgetIntents.kt` | Acción de toggle e intent que abre Importar. |
| `widget/QuickActionRouter.kt` | Routing común de widget y mosaico. |
| `widget/VpnPermissionActivity.kt` | Activity transparente para el permiso VPN. |
| `widget/UsipipoWidgetProvider.kt` | Receiver del widget y dispatch de pulsaciones. |
| `widget/UsipipoWidgetRenderer.kt` | Construcción de `RemoteViews` y actualización de instancias. |
| `widget/UsipipoTileService.kt` | Mosaico nativo y dispatch de pulsaciones. |
| `widget/PersistedWidgetState.kt` | Estado semántico y hora persistidos. |
| `widget/WidgetStateStore.kt` | Contrato de persistencia. |
| `widget/SharedPreferencesWidgetStateStore.kt` | Persistencia en `SharedPreferences`. |
| `widget/WidgetUpdater.kt` | Publicación reactiva y deduplicada del estado. |
| `ui/diagnostics/QuickAccessHint.kt` | Pista de instalación para Android 13 o superior. |

### Recursos nuevos

| Archivo | Uso |
|---|---|
| `res/layout/widget_usipipo.xml` | Layout RemoteViews 2×2. |
| `res/layout/widget_usipipo_wide.xml` | Layout RemoteViews 4×2. |
| `res/xml/usipipo_widget_info.xml` | Metadatos y tamaño redimensionable del widget. |
| `res/values/widget_colors.xml` | Colores del widget en tema claro. |
| `res/values-night/widget_colors.xml` | Colores del widget en tema oscuro. |
| `res/drawable/widget_bg_disconnected.xml` | Fondo pastel claro y azul noche oscuro. |
| `res/drawable/widget_bg_connected.xml` | Degradado azul a turquesa/menta. |
| `res/drawable/widget_bg_failed.xml` | Fallo o ausencia de respuesta. |
| `res/drawable/widget_button_*.xml` | Anillo azul, relleno turquesa y anillo ámbar. |
| `res/drawable/widget_status_pill.xml` | Fondo de la píldora de estado. |
| `res/drawable/widget_logo_background.xml` | Cuadro azul redondeado de la «u». |
| `res/drawable/ic_widget_power.xml` | Power con acento del tema. |
| `res/drawable/ic_widget_power_failed.xml` | Power ámbar. |
| `res/drawable/ic_widget_add.xml` | Plus para «Importar perfil». |
| `res/drawable/ic_widget_shield_check.xml` | Escudo con check blanco. |

### Archivos existentes modificados

- `AppContainer.kt`: expone el controlador y arranca el actualizador.
- `UsipipoApp.kt`: inicia `WidgetUpdater` después de inicializar el registro.
- `MainActivity.kt`, `ui/UsipipoRoot.kt`, `ui/nav/MainTab.kt`: intent y pestaña inicial Importar.
- `ui/home/HomeViewModel.kt`: reutiliza la selección compartida.
- `AndroidManifest.xml`: receiver, service y Activity del subsistema.
- `values/themes.xml`: tema transparente de la Activity de permiso.
- `values/strings.xml`: textos del widget, mosaico y Diagnóstico.
- `PaletteContrastTest.kt`: contraste de los colores nuevos.
- `ui/home/Fakes.kt`: perfil que no se puede leer.

---

### Task 1: controlador JVM de conexión rápida

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/domain/ConfigSelection.kt`
- Create: `app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectState.kt`
- Create: `app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectAction.kt`
- Create: `app/src/main/java/com/usipipo/vpn/quickconnect/QuickConnectController.kt`
- Create: `app/src/main/java/com/usipipo/vpn/quickconnect/VpnPermissionChecker.kt`
- Create: `app/src/main/java/com/usipipo/vpn/quickconnect/AndroidVpnPermissionChecker.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/AppContainer.kt`
- Modify: `app/src/test/java/com/usipipo/vpn/ui/home/Fakes.kt`
- Create: `app/src/test/java/com/usipipo/vpn/quickconnect/QuickConnectControllerTest.kt`

**Interfaces:**
- Produces: `selectedConfigId(configs: List<SavedConfig>, lastUsedId: String?): String?`.
- Produces: `QuickConnectState(tunnel: TunnelState, selectedConfig: SavedConfig?, failure: QuickConnectFailure?)`.
- Produces: `QuickConnectController(store, tunnelManager, permissionChecker, scope)` con `state: StateFlow<QuickConnectState>` y `suspend fun toggle(): QuickConnectAction`.
- Produces: `QuickConnectAction.ConnectRequested(configId)`, `DisconnectRequested`, `VpnPermissionRequired`, `OpenImporter`, `ConfigUnreadable(configId)`.

- [ ] **Step 1: escribe los tests que fallan**

En `QuickConnectControllerTest.kt`, con `runTest` y `backgroundScope`, usa `FakeConfigStore`, `FakeTunnelManager` y un permiso lambda. Los casos y aserciones exactos son:

```kotlin
@Test
fun `conectar usa el perfil seleccionado y su texto`() = runTest {
    val store = FakeConfigStore(listOf(casa, trabajo))
    val tunnel = FakeTunnelManager()
    val controller = controller(store, tunnel, permissionChecker = { true })

    val action = controller.toggle()

    assertEquals(QuickConnectAction.ConnectRequested("a"), action)
    assertEquals(listOf("a" to "conf-de-a"), tunnel.connectCalls)
}

@Test
fun `conectar se ignora si falta el permiso vpn`() = runTest {
    val tunnel = FakeTunnelManager()
    val controller = controller(FakeConfigStore(listOf(casa)), tunnel, permissionChecker = { false })

    val action = controller.toggle()

    assertEquals(QuickConnectAction.VpnPermissionRequired, action)
    assertTrue(tunnel.connectCalls.isEmpty())
}

@Test
fun `sin perfiles abre importar aunque falte el permiso`() = runTest {
    val controller = controller(FakeConfigStore(), FakeTunnelManager(), permissionChecker = { false })

    assertEquals(QuickConnectAction.OpenImporter, controller.toggle())
}

@Test
fun `conectando un toque desconecta y no vuelve a conectar`() = runTest {
    val tunnel = FakeTunnelManager().apply { emit(TunnelState.Connecting("a")) }
    val controller = controller(FakeConfigStore(listOf(casa)), tunnel, permissionChecker = { true })

    assertEquals(QuickConnectAction.DisconnectRequested, controller.toggle())
    assertEquals(1, tunnel.disconnectCalls)
    assertTrue(tunnel.connectCalls.isEmpty())
}

@Test
fun `conectado y sin respuesta desconectan`() = runTest {
    listOf(TunnelState.Connected("a"), TunnelState.NoResponse("a")).forEach { estado ->
        val tunnel = FakeTunnelManager().apply { emit(estado) }
        val controller = controller(FakeConfigStore(listOf(casa)), tunnel, permissionChecker = { true })

        assertEquals(QuickConnectAction.DisconnectRequested, controller.toggle())
    }
}

@Test
fun `fallo permite reintentar la conexion`() = runTest {
    val tunnel = FakeTunnelManager().apply { emit(TunnelState.Failed("a")) }
    val controller = controller(FakeConfigStore(listOf(casa)), tunnel, permissionChecker = { true })

    assertEquals(QuickConnectAction.ConnectRequested("a"), controller.toggle())
    assertEquals(listOf("a" to "conf-de-a"), tunnel.connectCalls)
}

@Test
fun `un perfil ilegible publica fallo y no pasa el conf al tunel`() = runTest {
    val store = FakeConfigStore(listOf(casa), unreadableIds = setOf("a"))
    val tunnel = FakeTunnelManager()
    var permission = true
    val controller = controller(store, tunnel, permissionChecker = { permission })

    assertEquals(QuickConnectAction.ConfigUnreadable("a"), controller.toggle())
    assertTrue(tunnel.connectCalls.isEmpty())
    assertEquals(
        QuickConnectFailure.ConfigUnreadable("a"),
        controller.state.value.failure,
    )

    permission = false
    assertEquals(QuickConnectAction.VpnPermissionRequired, controller.toggle())
    assertNull(controller.state.value.failure)
}

@Test
fun `si se borra la ultima usada se conecta el siguiente perfil`() = runTest {
    val store = FakeConfigStore(listOf(casa, trabajo))
    store.setLastUsed("b")
    store.remove("b")
    val controller = controller(store, FakeTunnelManager(), permissionChecker = { true })

    assertEquals(QuickConnectAction.ConnectRequested("a"), controller.toggle())
    assertEquals("a", controller.state.value.selectedConfig?.id)
}
```

El helper `controller(...)` acepta `permissionChecker: () -> Boolean` y crea `QuickConnectController(store, tunnel, permissionChecker, backgroundScope)`. Añade además una aserción que confirme que el `QuickConnectState.selectedConfig` refleja el perfil actual.

- [ ] **Step 2: ejecuta el test y comprueba que falla**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.quickconnect.QuickConnectControllerTest'
```

Si no hay JDK local, envía la rama a CI y confirma que el fallo es `unresolved reference` para los tipos nuevos. El test no debe compilar todavía.

- [ ] **Step 3: extrae la selección compartida**

En `domain/ConfigSelection.kt`:

```kotlin
package com.usipipo.vpn.domain

/** La última usada si sigue existiendo; si no, la primera de la lista. */
fun selectedConfigId(configs: List<SavedConfig>, lastUsedId: String?): String? =
    configs.firstOrNull { it.id == lastUsedId }?.id ?: configs.firstOrNull()?.id
```

En `HomeViewModel.kt`, elimina el método privado `selectedId(...)` y usa `selectedConfigId(...)` tanto en `buildState(...)` como en `connectSelected()`. Los tests existentes de `HomeViewModel` siguen siendo la prueba de regresión.

- [ ] **Step 4: define el estado y la acción**

`QuickConnectState.kt`:

```kotlin
data class QuickConnectState(
    val tunnel: TunnelState = TunnelState.Disconnected,
    val selectedConfig: SavedConfig? = null,
    val failure: QuickConnectFailure? = null,
)

sealed interface QuickConnectFailure {
    data class ConfigUnreadable(val configId: String) : QuickConnectFailure
}
```

`QuickConnectAction.kt`:

```kotlin
sealed interface QuickConnectAction {
    data object DisconnectRequested : QuickConnectAction
    data class ConnectRequested(val configId: String) : QuickConnectAction
    data object VpnPermissionRequired : QuickConnectAction
    data object OpenImporter : QuickConnectAction
    data class ConfigUnreadable(val configId: String) : QuickConnectAction
}
```

- [ ] **Step 5: implementa el permiso como contrato inyectable**

`VpnPermissionChecker.kt`:

```kotlin
fun interface VpnPermissionChecker {
    fun isGranted(): Boolean
}
```

`AndroidVpnPermissionChecker.kt` usa `VpnService.prepare(appContext) == null`. Conserva solo `applicationContext`; no el contexto de Activity.

- [ ] **Step 6: implementa el controlador mínimo**

`QuickConnectController` combina `store.configs`, `store.lastUsedId`, `tunnelManager.state` y un `MutableStateFlow<QuickConnectFailure?>` local. El estado usa `SharingStarted.Eagerly` para que un receiver recién creado vea el estado actual sin esperar una recollect.

`toggle()` sigue exactamente este orden:

1. Si `tunnelManager.state.value.isActive` —esto incluye `Connecting`, `Connected` y `NoResponse`—, llama `tunnelManager.disconnect()` y devuelve `DisconnectRequested`.
2. Selecciona `selectedConfigId(...)`. Si no hay perfil, limpia el fallo local y devuelve `OpenImporter`.
3. Limpia el fallo local antes de comprobar el permiso: un permiso denegado no debe dejar visible un «Fallo» anterior.
4. Si `permissionChecker.isGranted()` es false, devuelve `VpnPermissionRequired`.
5. Llama `store.readText(id)`; si devuelve null, publica `QuickConnectFailure.ConfigUnreadable(id)` y devuelve `ConfigUnreadable(id)`.
6. Si el texto existe, llama `tunnelManager.connect(id, text)` y devuelve `ConnectRequested(id)`.

`onPermissionDenied()` registra `AppLog.w("Permiso", "Permiso de VPN denegado desde acceso rápido")`. Los demás mensajes usan tag `AccesoRapido` y `id.take(8)`; nunca incluyen el texto leído.

- [ ] **Step 7: amplía el fake sin romper los tests actuales**

`FakeConfigStore` añade `private val unreadableIds: Set<String> = emptySet()` al constructor. `readText(id)` devuelve null cuando `id in unreadableIds`; en cualquier otro caso conserva el comportamiento actual.

- [ ] **Step 8: conecta el controlador con `AppContainer`**

Cambia el scope privado a `internal val applicationScope: CoroutineScope`. Añade:

```kotlin
val quickConnectController: QuickConnectController by lazy {
    QuickConnectController(
        store = configStore,
        tunnelManager = tunnelManager,
        permissionChecker = AndroidVpnPermissionChecker(appContext),
        scope = applicationScope,
    )
}
```

No añadas todavía el actualizador; lo produce la Tarea 6.

- [ ] **Step 9: verifica y commit**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.quickconnect.QuickConnectControllerTest'
git add app/src/main/java/com/usipipo/vpn/domain/ConfigSelection.kt \
        app/src/main/java/com/usipipo/vpn/quickconnect \
        app/src/main/java/com/usipipo/vpn/ui/home/HomeViewModel.kt \
        app/src/main/java/com/usipipo/vpn/AppContainer.kt \
        app/src/test/java/com/usipipo/vpn/quickconnect/QuickConnectControllerTest.kt \
        app/src/test/java/com/usipipo/vpn/ui/home/Fakes.kt
git commit -m "feat(quickconnect): controlador de conexion rapida reutilizable"
```

El cuerpo del commit explica que el widget y el mosaico comparten una única decisión de conexión y que no se toca el túnel.

---

### Task 2: mapper de estados y paleta accesible

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/widget/WidgetStatus.kt`
- Create: `app/src/main/java/com/usipipo/vpn/widget/WidgetPresentation.kt`
- Create: `app/src/main/java/com/usipipo/vpn/widget/WidgetStateMapper.kt`
- Create: `app/src/test/java/com/usipipo/vpn/widget/WidgetStateMapperTest.kt`
- Create: `app/src/main/res/values/widget_colors.xml`
- Create: `app/src/main/res/values-night/widget_colors.xml`
- Create: `app/src/main/res/drawable/widget_bg_disconnected.xml`
- Create: `app/src/main/res/drawable/widget_bg_connected.xml`
- Create: `app/src/main/res/drawable/widget_bg_failed.xml`
- Create: `app/src/main/res/drawable/widget_button_disconnected.xml`
- Create: `app/src/main/res/drawable/widget_button_connected.xml`
- Create: `app/src/main/res/drawable/widget_button_failed.xml`
- Create: `app/src/main/res/drawable/widget_status_pill.xml`
- Create: `app/src/main/res/drawable/widget_logo_background.xml`
- Create: `app/src/main/res/drawable/ic_widget_power.xml`
- Create: `app/src/main/res/drawable/ic_widget_power_failed.xml`
- Create: `app/src/main/res/drawable/ic_widget_add.xml`
- Create: `app/src/main/res/drawable/ic_widget_shield_check.xml`
- Modify: `app/src/test/java/com/usipipo/vpn/ui/theme/PaletteContrastTest.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `QuickConnectState`, `QuickConnectFailure` y `selectedConfigId` de la Tarea 1.
- Produces: `WidgetStatus` y `WidgetStateMapper.map(state: QuickConnectState): WidgetPresentation`.

- [ ] **Step 1: escribe el test del mapper que falla**

`WidgetStateMapperTest.kt` debe comprobar las seis presentaciones:

```kotlin
@Test
fun `desconectado muestra power y el fondo pastel`() {
    val p = WidgetStateMapper.map(QuickConnectState(selectedConfig = casa))

    assertEquals(WidgetStatus.Disconnected, p.status)
    assertEquals(R.string.widget_status_disconnected, p.statusText)
    assertEquals(R.drawable.widget_bg_disconnected, p.background)
    assertEquals(R.drawable.ic_widget_power, p.actionIcon)
    assertEquals(R.drawable.widget_button_disconnected, p.buttonBackground)
    assertFalse(p.showProgress)
}

@Test
fun `conectando muestra el progreso y no el escudo`() {
    val p = WidgetStateMapper.map(
        QuickConnectState(tunnel = TunnelState.Connecting("a"), selectedConfig = casa),
    )

    assertEquals(WidgetStatus.Connecting, p.status)
    assertTrue(p.showProgress)
    assertEquals(R.drawable.ic_widget_power, p.actionIcon)
}

@Test
fun `conectado muestra el escudo y el fondo degradado`() {
    val p = WidgetStateMapper.map(
        QuickConnectState(tunnel = TunnelState.Connected("a"), selectedConfig = casa),
    )

    assertEquals(WidgetStatus.Connected, p.status)
    assertEquals(R.string.widget_status_active, p.statusText)
    assertEquals(R.drawable.widget_bg_connected, p.background)
    assertEquals(R.drawable.ic_widget_shield_check, p.actionIcon)
    assertEquals(R.drawable.widget_button_connected, p.buttonBackground)
}

@Test
fun `sin respuesta y fallo usan el fondo rosado y el anillo ambar`() {
    listOf(TunnelState.NoResponse("a"), TunnelState.Failed("a")).forEach { tunnel ->
        val p = WidgetStateMapper.map(QuickConnectState(tunnel = tunnel, selectedConfig = casa))
        assertEquals(R.drawable.widget_bg_failed, p.background)
        assertEquals(R.drawable.ic_widget_power_failed, p.actionIcon)
    }
}

@Test
fun `un fallo local se presenta como fallo`() {
    val p = WidgetStateMapper.map(
        QuickConnectState(
            selectedConfig = casa,
            failure = QuickConnectFailure.ConfigUnreadable("a"),
        ),
    )

    assertEquals(WidgetStatus.Failed, p.status)
    assertEquals(R.string.widget_status_failed, p.statusText)
}

@Test
fun `sin perfiles presenta importar perfil`() {
    val p = WidgetStateMapper.map(QuickConnectState())

    assertEquals(WidgetStatus.NoProfile, p.status)
    assertEquals(R.string.widget_import_profile, p.statusText)
    assertEquals(R.drawable.ic_widget_add, p.actionIcon)
    assertEquals("", p.profileName)
    assertEquals("", p.countryLabel)
}
```

Añade un caso con `SavedConfig(countryCode = "DE", countryName = "Germany")` que compruebe `countryLabel == "🇩🇪 Germany"`, y otro sin caché que compruebe `countryLabel == ""`.

- [ ] **Step 2: ejecuta el test y comprueba que falla**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.widget.WidgetStateMapperTest'
```

Esperado: fallo de compilación por `WidgetStateMapper` y `WidgetPresentation` inexistentes.

- [ ] **Step 3: define los tipos de presentación**

`WidgetStatus.kt`:

```kotlin
enum class WidgetStatus {
    Disconnected,
    Connecting,
    Connected,
    NoResponse,
    Failed,
    NoProfile,
}
```

`WidgetPresentation.kt`:

```kotlin
data class WidgetPresentation(
    val status: WidgetStatus,
    @StringRes val statusText: Int,
    @DrawableRes val background: Int,
    @DrawableRes val actionIcon: Int,
    @DrawableRes val buttonBackground: Int,
    val showProgress: Boolean,
    val profileName: String,
    val countryLabel: String,
)
```

- [ ] **Step 4: crea los recursos visuales que usa la presentación**

- `widget_bg_disconnected.xml`: shape con radio de 28 dp y relleno `@color/widget_background_disconnected`.
- `widget_bg_connected.xml`: gradient de `@color/widget_background_connected_start` a `@color/widget_background_connected_end`, radio de 28 dp.
- `widget_bg_failed.xml`: shape con radio de 28 dp y relleno `@color/widget_background_failed`.
- `widget_button_disconnected.xml`: óvalo transparente con trazo `@color/widget_accent_disconnected` de 3 dp.
- `widget_button_connected.xml`: óvalo relleno `@color/widget_accent_connected`.
- `widget_button_failed.xml`: óvalo transparente con trazo `@color/widget_accent_failed` de 3 dp.
- `widget_status_pill.xml`: shape redondeado que referencia `@color/widget_status_pill_background`.
- `widget_logo_background.xml`: cuadro `@color/ic_launcher_background` con radio de 12 dp.
- `ic_widget_power.xml` e `ic_widget_add.xml`: trazo `@color/widget_accent_disconnected`.
- `ic_widget_power_failed.xml`: trazo `@color/widget_accent_failed`.
- `ic_widget_shield_check.xml`: trazo `@color/widget_text_on_connected`.

No crees variantes `drawable-night`: los colores de `values-night/widget_colors.xml` cambian automáticamente el aspecto de los mismos drawables.

- [ ] **Step 5: declara la paleta exacta**

`values/widget_colors.xml`:

```xml
<color name="widget_background_disconnected">#E8F1FD</color>
<color name="widget_background_connected_start">#1565C0</color>
<color name="widget_background_connected_end">#00796B</color>
<color name="widget_background_failed">#FCE8EC</color>
<color name="widget_text_primary">#102A43</color>
<color name="widget_text_secondary">#365A78</color>
<color name="widget_text_on_connected">#FFFFFF</color>
<color name="widget_text_failed">#6B3A00</color>
<color name="widget_accent_disconnected">#0B57C9</color>
<color name="widget_accent_connected">#00796B</color>
<color name="widget_accent_failed">#B76E00</color>
<color name="widget_status_pill_background">#FFFFFF</color>
<color name="widget_status_pill_text">#102A43</color>
```

`values-night/widget_colors.xml`:

```xml
<color name="widget_background_disconnected">#061631</color>
<color name="widget_background_connected_start">#123B78</color>
<color name="widget_background_connected_end">#006B5F</color>
<color name="widget_background_failed">#2A1724</color>
<color name="widget_text_primary">#E6F0FF</color>
<color name="widget_text_secondary">#B8D2EF</color>
<color name="widget_text_on_connected">#FFFFFF</color>
<color name="widget_text_failed">#FFD9E2</color>
<color name="widget_accent_disconnected">#64B5F6</color>
<color name="widget_accent_connected">#006B5F</color>
<color name="widget_accent_failed">#FFB547</color>
<color name="widget_status_pill_background">#102A43</color>
<color name="widget_status_pill_text">#E6F0FF</color>
```

- [ ] **Step 6: implementa el mapper puro**

`WidgetStateMapper.map(...)` aplica este orden: fallo local cuyo `configId` coincide con el perfil actual → `Failed`; `Connecting`; `Connected`; `NoResponse`; `Failed`; túnel `Disconnected` sin perfil → `NoProfile`; resto → `Disconnected`.

`countryLabel` se construye con el helper existente `com.usipipo.vpn.ui.components.flagEmoji(countryCode)` y `countryName`. No copies la lógica de bandera a otro paquete.

- [ ] **Step 7: amplía `PaletteContrastTest`**

El test lee los dos XML con expresiones regulares, igual que hace `IconSetStyleTest`, y calcula contraste desde ARGB. Normaliza cada valor `#RRGGBB` como `0xFFRRGGBB` antes de comparar. Debe comprobar:

- texto primario y secundario contra el fondo desconectado: `>= 4.5` en ambos temas;
- texto `on_connected` contra los dos extremos del degradado: `>= 4.5`;
- texto de fallo contra el fondo de fallo: `>= 4.5`;
- texto de la píldora contra su fondo: `>= 4.5`;
- acento desconectado contra su fondo: `>= 3.0`;
- acento de fallo contra el fondo de fallo: `>= 3.0`;
- icono blanco contra el relleno conectado: `>= 4.5`.

No dupliques valores hardcodeados en el test: se leen de los XML.

- [ ] **Step 8: añade los strings**

En `values/strings.xml`:

```xml
<string name="widget_status_disconnected">Desconectado</string>
<string name="widget_status_connecting">Conectando…</string>
<string name="widget_status_active">Activo</string>
<string name="widget_status_no_response">Sin respuesta</string>
<string name="widget_status_failed">Fallo</string>
<string name="widget_import_profile">Importar perfil</string>
<string name="widget_description">Conectar o desconectar uSipipo VPN desde la pantalla de inicio.</string>
<string name="widget_cd_logo">uSipipo VPN</string>
<string name="widget_cd_action">Conectar o desconectar</string>
```

- [ ] **Step 9: verifica y commit**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.widget.WidgetStateMapperTest' \
    --tests 'com.usipipo.vpn.ui.theme.PaletteContrastTest'
git add app/src/main/java/com/usipipo/vpn/widget \
        app/src/test/java/com/usipipo/vpn/widget/WidgetStateMapperTest.kt \
        app/src/test/java/com/usipipo/vpn/ui/theme/PaletteContrastTest.kt \
        app/src/main/res/values/widget_colors.xml \
        app/src/main/res/values-night/widget_colors.xml \
        app/src/main/res/drawable/widget_* \
        app/src/main/res/drawable/ic_widget_* \
        app/src/main/res/values/strings.xml
git commit -m "feat(widget): mapa de estados y paleta accesible"
```

---

### Task 3: permiso VPN y apertura directa de Importar

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/widget/WidgetIntents.kt`
- Create: `app/src/main/java/com/usipipo/vpn/widget/QuickActionRouter.kt`
- Create: `app/src/main/java/com/usipipo/vpn/widget/VpnPermissionActivity.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/MainActivity.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/nav/MainTab.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/AndroidManifest.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/test/java/com/usipipo/vpn/ui/nav/MainTabTest.kt`
- Create: `app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt`

**Interfaces:**
- Consumes: `QuickConnectController.toggle()` y `QuickConnectAction` de la Tarea 1.
- Produces: `WidgetIntents.ACTION_TOGGLE`, `WidgetIntents.openImporter(context)`, `QuickActionRouter.toggle()`.
- Produces: `MainActivity.EXTRA_INITIAL_TAB` y `MainTab.fromKey(key: String?): MainTab`.

- [ ] **Step 1: escribe los tests que fallan**

En `MainTabTest.kt`:

```kotlin
@Test
fun `una clave de pestana valida se convierte en pestana`() {
    assertEquals(MainTab.Import, MainTab.fromKey("Import"))
}

@Test
fun `una clave desconocida abre inicio`() {
    assertEquals(MainTab.Home, MainTab.fromKey("desconocida"))
    assertEquals(MainTab.Home, MainTab.fromKey(null))
}
```

En `SystemIntegrationResourceTest.kt`, lee `src/main/AndroidManifest.xml` y `src/main/res/values/themes.xml` y comprueba:

- existe `.widget.VpnPermissionActivity` con `android:exported="false"` y `@style/Theme.Usipipo.Transparent`;
- existe un style `Theme.Usipipo.Transparent` con `android:windowIsTranslucent="true"` y `android:windowBackground="@android:color/transparent"`;
- el receiver del widget y el service del mosaico se comprueban en sus propias tareas.

- [ ] **Step 2: ejecuta los tests y comprueba que fallan**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.ui.nav.MainTabTest' \
    --tests 'com.usipipo.vpn.widget.SystemIntegrationResourceTest'
```

Esperado: `unresolved reference: fromKey` y ausencia de la Activity/tema.

- [ ] **Step 3: añade la conversión de pestaña e intent inicial**

En `MainTab.kt`:

```kotlin
fun fromKey(key: String?): MainTab = entries.firstOrNull { it.name == key } ?: MainTab.Home
```

En `MainActivity`, define `const val EXTRA_INITIAL_TAB = "com.usipipo.vpn.extra.INITIAL_TAB"`. Lee `intent.getStringExtra(EXTRA_INITIAL_TAB)` y pasa `MainTab.fromKey(...)` a `UsipipoRoot`.

En `UsipipoRoot`, cambia la firma a `UsipipoRoot(container: AppContainer, initialTab: MainTab = MainTab.Home)` y el estado a:

```kotlin
var tab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
```

Así una rotación conserva la pestaña elegida y un nuevo intent empieza en la pestaña solicitada.

- [ ] **Step 4: define intents compartidos**

`WidgetIntents.kt`:

```kotlin
object WidgetIntents {
    const val ACTION_TOGGLE = "com.usipipo.vpn.widget.action.TOGGLE"

    fun openImporter(context: Context) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_INITIAL_TAB, MainTab.Import.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
    }
}
```

`CLEAR_TOP` sin `SINGLE_TOP` fuerza una instancia nueva de `MainActivity` con el extra, evitando depender de `onNewIntent` y del estado Compose de una Activity existente.

- [ ] **Step 5: implementa la Activity transparente**

`VpnPermissionActivity` extiende `ComponentActivity` y usa `ActivityResultContracts.StartActivityForResult()`.

- En `onCreate`, llama `VpnService.prepare(this)`.
- Si devuelve null, el permiso ya existe: ejecuta `controller.toggle()` y termina.
- Si devuelve un Intent, lánzalo con el launcher.
- En el resultado `RESULT_OK`, ejecuta `controller.toggle()`. Si vuelve a devolver `VpnPermissionRequired`, registra `AppLog.w("Permiso", "El permiso VPN sigue sin estar concedido")` y termina; no abras otra Activity.
- En resultado distinto de `OK`, llama `controller.onPermissionDenied()` y termina.
- Si la acción es `OpenImporter`, abre Importar antes de terminar.

La Activity no contiene UI, no llama `setContent` y no permanece en el historial.

- [ ] **Step 6: añade el tema transparente**

En `values/themes.xml`:

```xml
<style name="Theme.Usipipo.Transparent" parent="android:style/Theme.Translucent.NoTitleBar">
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowAnimationStyle">@null</item>
</style>
```

No añadas `windowNoHistory=true`: la Activity necesita recibir el resultado del diálogo VPN.

- [ ] **Step 7: implementa el router común**

`QuickActionRouter(context, controller)` expone `suspend fun toggle()`:

- `DisconnectRequested` y `ConnectRequested`: no hacen nada más; el estado se propaga por el flujo.
- `VpnPermissionRequired`: inicia `VpnPermissionActivity` con `FLAG_ACTIVITY_NEW_TASK`.
- `OpenImporter`: llama `WidgetIntents.openImporter(context)`.
- `ConfigUnreadable`: no abre UI; el mapper ya publica «Fallo».

- [ ] **Step 8: declara la Activity y verifica**

En `AndroidManifest.xml`:

```xml
<activity
    android:name=".widget.VpnPermissionActivity"
    android:excludeFromRecents="true"
    android:exported="false"
    android:theme="@style/Theme.Usipipo.Transparent" />
```

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.ui.nav.MainTabTest' \
    --tests 'com.usipipo.vpn.widget.SystemIntegrationResourceTest'
```

- [ ] **Step 9: commit**

```bash
git add app/src/main/java/com/usipipo/vpn/MainActivity.kt \
        app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt \
        app/src/main/java/com/usipipo/vpn/ui/nav/MainTab.kt \
        app/src/main/java/com/usipipo/vpn/widget \
        app/src/main/AndroidManifest.xml \
        app/src/main/res/values/themes.xml \
        app/src/test/java/com/usipipo/vpn/ui/nav/MainTabTest.kt \
        app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt
git commit -m "feat(widget): permiso VPN y acceso directo a Importar"
```

---

### Task 4: widget de pantalla de inicio

**Files:**
- Create: `app/src/main/res/layout/widget_usipipo.xml`
- Create: `app/src/main/res/layout/widget_usipipo_wide.xml`
- Create: `app/src/main/res/xml/usipipo_widget_info.xml`
- Create: `app/src/main/java/com/usipipo/vpn/widget/UsipipoWidgetProvider.kt`
- Create: `app/src/main/java/com/usipipo/vpn/widget/UsipipoWidgetRenderer.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt`

**Interfaces:**
- Consumes: `WidgetStateMapper`, `WidgetPresentation`, `QuickActionRouter` y `AppContainer.quickConnectController`.
- Produces: `UsipipoWidgetProvider` y dos sobrecargas de renderer: `UsipipoWidgetRenderer.render(state: QuickConnectState, appWidgetIds: IntArray)` y `UsipipoWidgetRenderer.render(state: QuickConnectState)`.

- [ ] **Step 1: amplía el test de contrato de recursos**

`SystemIntegrationResourceTest.kt` debe comprobar que:

- ambos layouts existen y contienen los IDs `widget_root`, `widget_logo`, `widget_status`, `widget_action`, `widget_progress`, `widget_profile_name` y `widget_country`;
- `usipipo_widget_info.xml` declara `android:updatePeriodMillis="0"`, `android:resizeMode="horizontal|vertical"`, `android:widgetCategory="home_screen"` y `android:initialLayout="@layout/widget_usipipo"`;
- el manifest declara `.widget.UsipipoWidgetProvider` con `android:exported="true"`, la acción `android.appwidget.action.APPWIDGET_UPDATE` y metadata `android.appwidget.provider`;
- existen los tres fondos, tres botones y cuatro iconos específicos del widget.

- [ ] **Step 2: ejecuta el test y comprueba que falla**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.widget.SystemIntegrationResourceTest'
```

Esperado: fallo por recursos y receiver inexistentes.

- [ ] **Step 3: crea los layouts RemoteViews**

Los layouts solo usan `FrameLayout`, `LinearLayout`, `TextView`, `ImageView` y `ProgressBar`, todos soportados por `RemoteViews`.

El layout 2×2:

- raíz vertical con `android:padding="16dp"`;
- cabecera horizontal con el logo a la izquierda y `widget_status` a la derecha;
- `widget_action` de 64 dp centrado, dentro de un frame que también contiene `widget_progress`;
- `widget_profile_name` debajo, una línea y elipsis;
- `widget_country` debajo, texto pequeño.

El layout 4×2:

- raíz horizontal;
- acción de 64 dp a la izquierda;
- columna vertical con estado, perfil y país a la derecha.

Ambos layouts tienen exactamente los mismos IDs para que un solo renderer pueda usarlos.

- [ ] **Step 4: declara el proveedor y tamaño del widget**

`usipipo_widget_info.xml`:

```xml
<appwidget-provider
    android:description="@string/widget_description"
    android:initialLayout="@layout/widget_usipipo"
    android:minHeight="110dp"
    android:minResizeHeight="70dp"
    android:minResizeWidth="110dp"
    android:minWidth="180dp"
    android:previewLayout="@layout/widget_usipipo"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen" />
```

- [ ] **Step 5: implementa el renderer completo**

`UsipipoWidgetRenderer(context)` expone dos sobrecargas:

```kotlin
fun render(state: QuickConnectState, appWidgetIds: IntArray)
fun render(state: QuickConnectState)
```

La segunda resuelve los IDs activos con `AppWidgetManager.getAppWidgetIds(...)`, delega en la primera y omite la actualización cuando no hay ninguna instancia. El `WidgetUpdater` usa esta segunda sobrecarga.

Para cada ID:

1. Obtiene `OPTION_APPWIDGET_MIN_WIDTH`; usa `widget_usipipo_wide` cuando sea `>= 250` dp y `widget_usipipo` en otro caso.
2. Crea una `RemoteViews` completa.
3. Aplica `WidgetStateMapper.map(state)`.
4. Cambia fondos con `setInt(..., "setBackgroundResource", ...)`.
5. Cambia textos, colores, iconos y visibilidad.
6. Oculta `widget_profile_name` y `widget_country` si no hay perfil.
7. Muestra `widget_progress` solo durante `Connecting` y oculta el icono de acción en ese estado.
8. Asigna al botón un `PendingIntent.getBroadcast` con acción `WidgetIntents.ACTION_TOGGLE`, `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` y request code igual al ID del widget.

El texto de país se escribe solo si `countryLabel` no está vacío. Nunca se consulta red.

- [ ] **Step 6: implementa el provider**

`UsipipoWidgetProvider`:

- `onUpdate` renderiza los IDs recibidos.
- `onAppWidgetOptionsChanged` vuelve a renderizar ese ID para aplicar el layout ancho/estrecho.
- `onEnabled` renderiza todas las instancias.
- `onReceive` detecta `WidgetIntents.ACTION_TOGGLE`, llama `goAsync()`, ejecuta `QuickActionRouter.toggle()` en `container.applicationScope` y siempre llama `pendingResult.finish()` en `finally`.
- Las acciones normales se delegan en `super.onReceive(...)`.

- [ ] **Step 7: declara el receiver**

En `AndroidManifest.xml`:

```xml
<receiver
    android:name=".widget.UsipipoWidgetProvider"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/usipipo_widget_info" />
</receiver>
```

- [ ] **Step 8: verifica y commit**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.widget.SystemIntegrationResourceTest'
git add app/src/main/java/com/usipipo/vpn/widget/UsipipoWidgetProvider.kt \
        app/src/main/java/com/usipipo/vpn/widget/UsipipoWidgetRenderer.kt \
        app/src/main/res/layout/widget_usipipo.xml \
        app/src/main/res/layout/widget_usipipo_wide.xml \
        app/src/main/res/xml/usipipo_widget_info.xml \
        app/src/main/AndroidManifest.xml \
        app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt
git commit -m "feat(widget): widget de pantalla de inicio con RemoteViews"
```

---

### Task 5: mosaico de ajustes rápidos

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/widget/UsipipoTileService.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt`

**Interfaces:**
- Consumes: `QuickActionRouter`, `WidgetStateMapper` y `AppContainer.quickConnectController`.
- Produces: `UsipipoTileService.requestRefresh(context: Context)` para que `WidgetUpdater` solicite una actualización del mosaico.

- [ ] **Step 1: amplía el test de contrato de recursos**

Comprueba que el manifest declara:

- `.widget.UsipipoTileService`;
- `android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"`;
- `android:exported="true"`;
- `android:icon="@drawable/ic_shield_check"`;
- `android:label="@string/app_name"`.

- [ ] **Step 2: ejecuta el test y comprueba que falta el service**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.widget.SystemIntegrationResourceTest'
```

- [ ] **Step 3: implementa el service**

`UsipipoTileService` extiende `TileService`:

- `onCreate` asigna al `qsTile` un `PendingIntent.getService` con `WidgetIntents.ACTION_TOGGLE`, `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`.
- `onStartCommand`, cuando recibe la acción de toggle, ejecuta `QuickActionRouter.toggle()` en `container.applicationScope` y devuelve `START_NOT_STICKY`.
- `onTileAdded` y `onStartListening` llaman `renderTile()`.
- `renderTile()` lee `quickConnectController.state.value`, aplica el mapper y actualiza el `Tile`:
  - `WidgetStatus.Connected` → `Tile.STATE_ACTIVE`;
  - cualquier otro estado → `Tile.STATE_INACTIVE`;
  - `label` = `uSipipo VPN`;
  - `icon` = `Icon.createWithResource(context, R.drawable.ic_shield_check)`;
  - API 29 o superior: `stateDescription` = texto del estado y `subtitle` = nombre del perfil; si no hay perfil, `subtitle` usa el texto del estado;
  - API 26 a 28: omite `stateDescription` y `subtitle`; solo actualiza `label`, `icon`, estado y color nativo.
- `requestRefresh(context)` llama `requestListeningState(context, ComponentName(context, UsipipoTileService::class.java))`.

Durante `Connecting`, el mosaico queda inactivo con descripción «Conectando…», sin inventar un estado nativo adicional.

- [ ] **Step 4: declara el service y los textos**

Manifest:

```xml
<service
    android:name=".widget.UsipipoTileService"
    android:exported="true"
    android:icon="@drawable/ic_shield_check"
    android:label="@string/app_name"
    android:permission="android.permission.BIND_QUICK_SETTINGS_TILE" />
```

Strings:

```xml
<string name="tile_state_disconnected">Desconectado</string>
<string name="tile_state_connecting">Conectando…</string>
<string name="tile_state_connected">Activo</string>
<string name="tile_state_no_response">Sin respuesta</string>
<string name="tile_state_failed">Fallo</string>
```

- [ ] **Step 5: verifica y commit**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.widget.SystemIntegrationResourceTest'
git add app/src/main/java/com/usipipo/vpn/widget/UsipipoTileService.kt \
        app/src/main/AndroidManifest.xml \
        app/src/main/res/values/strings.xml \
        app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt
git commit -m "feat(widget): mosaico de ajustes rapidos"
```

---

### Task 6: actualizador reactivo, persistencia y arranque

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/widget/PersistedWidgetState.kt`
- Create: `app/src/main/java/com/usipipo/vpn/widget/WidgetStateStore.kt`
- Create: `app/src/main/java/com/usipipo/vpn/widget/SharedPreferencesWidgetStateStore.kt`
- Create: `app/src/main/java/com/usipipo/vpn/widget/WidgetUpdater.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/widget/UsipipoWidgetProvider.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/AppContainer.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/UsipipoApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/test/java/com/usipipo/vpn/widget/WidgetUpdaterTest.kt`

**Interfaces:**
- Consumes: `QuickConnectController.state`, `UsipipoWidgetRenderer` y `UsipipoTileService.requestRefresh`.
- Produces: `WidgetUpdater.start()` y `WidgetUpdater.refreshNow()`.
- Produces: `PersistedWidgetState(status: WidgetStatus, updatedAtEpochMs: Long)`.

- [ ] **Step 1: escribe el test del actualizador que falla**

`WidgetUpdaterTest.kt` usa un `MutableStateFlow<QuickConnectState>`, un renderer lambda que cuenta renders y un `WidgetStateStore` fake.

```kotlin
@Test
fun `un estado repetido no se dibuja dos veces`() = runTest {
    val states = MutableStateFlow(QuickConnectState(selectedConfig = casa))
    var renders = 0
    val updater = updater(states) { renders++ }

    updater.start()
    states.value = QuickConnectState(selectedConfig = casa)
    advanceUntilIdle()

    assertEquals(1, renders)
}

@Test
fun `un cambio dibuja y actualiza el mosaico`() = runTest {
    val states = MutableStateFlow(QuickConnectState(selectedConfig = casa))
    var renders = 0
    var tileRefreshes = 0
    val updater = updater(states, render = { renders++ }, refreshTile = { tileRefreshes++ })

    updater.start()
    states.value = QuickConnectState(
        tunnel = TunnelState.Connected("a"),
        selectedConfig = casa,
    )
    advanceUntilIdle()

    assertEquals(2, renders)
    assertEquals(2, tileRefreshes)
}

@Test
fun `arrancar dos veces no duplica la coleccion`() = runTest {
    val states = MutableStateFlow(QuickConnectState(selectedConfig = casa))
    var renders = 0
    val updater = updater(states) { renders++ }

    updater.start()
    updater.start()
    advanceUntilIdle()

    assertEquals(1, renders)
}

@Test
fun `el estado vivo desconectado invalida un estado activo persistido`() = runTest {
    val store = FakeWidgetStateStore(
        PersistedWidgetState(WidgetStatus.Connected, updatedAtEpochMs = 123L),
    )
    val states = MutableStateFlow(QuickConnectState())
    val updater = updater(states, store = store)

    updater.start()
    advanceUntilIdle()

    assertEquals(WidgetStatus.Disconnected, store.written?.status)
}
```

El helper fija `clock = { 456L }` y comprueba en otro test que el store recibe `updatedAtEpochMs == 456L`.

- [ ] **Step 2: ejecuta el test y comprueba que falla**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.widget.WidgetUpdaterTest'
```

- [ ] **Step 3: implementa persistencia mínima**

`PersistedWidgetState.kt`:

```kotlin
data class PersistedWidgetState(
    val status: WidgetStatus,
    val updatedAtEpochMs: Long,
)
```

`WidgetStateStore.kt`:

```kotlin
interface WidgetStateStore {
    fun read(): PersistedWidgetState?
    fun write(state: PersistedWidgetState)
}
```

`SharedPreferencesWidgetStateStore` usa preferencias privadas llamadas `usipipo_widget_state`. Guarda solo `status.name` y `updatedAtEpochMs`; no guarda perfil, país, endpoint ni claves. Un valor de status desconocido se trata como null.

- [ ] **Step 4: implementa el actualizador**

`WidgetUpdater` recibe:

```kotlin
class WidgetUpdater(
    scope: CoroutineScope,
    states: Flow<QuickConnectState>,
    renderWidget: (QuickConnectState) -> Unit,
    refreshTile: () -> Unit,
    store: WidgetStateStore,
    clock: () -> Long = System::currentTimeMillis,
)
```

- `start()` es idempotente y recoge `states.distinctUntilChanged()`.
- Por cada estado llama `WidgetStateMapper.map(state)`, dibuja el widget, solicita actualizar el mosaico y persiste `PersistedWidgetState(presentation.status, clock())`.
- Antes del primer render, lee el estado persistido. Si estaba activo y el estado vivo es `Disconnected`, registra que se invalidó; el estado vivo sigue mandando.
- `refreshNow()` obtiene el estado actual y fuerza un render explícito. Se usa para `onUpdate`, arranque y broadcasts del sistema; no añade un segundo flujo periódico.

- [ ] **Step 5: integra `AppContainer` y `UsipipoApp`**

En `AppContainer`:

```kotlin
val widgetUpdater: WidgetUpdater by lazy {
    WidgetUpdater(
        scope = applicationScope,
        states = quickConnectController.state,
        renderWidget = { UsipipoWidgetRenderer(appContext).render(it) },
        refreshTile = { UsipipoTileService.requestRefresh(appContext) },
        store = SharedPreferencesWidgetStateStore(appContext),
    )
}

fun startWidgetUpdater() {
    widgetUpdater.start()
}
```

En `UsipipoApp.onCreate`, llama `container.startWidgetUpdater()` después de `AppLog.init(...)` y antes de la comprobación de actualizaciones. Así el registro está listo antes del primer render.

- [ ] **Step 6: sincroniza el provider con el actualizador**

`UsipipoWidgetProvider.onUpdate` y `onAppWidgetOptionsChanged` llaman `container.widgetUpdater.refreshNow()` en vez de renderizar por su cuenta.

- [ ] **Step 7: añade arranque y actualización de app**

Manifest:

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

Añade al receiver del widget las acciones:

```xml
<action android:name="android.intent.action.BOOT_COMPLETED" />
<action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
```

En `UsipipoWidgetProvider.onReceive`, antes de delegar en `super`, si la acción es `BOOT_COMPLETED` o `MY_PACKAGE_REPLACED`, llama `container.widgetUpdater.refreshNow()` y termina.

- [ ] **Step 8: verifica y commit**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.widget.WidgetUpdaterTest' \
    --tests 'com.usipipo.vpn.widget.SystemIntegrationResourceTest'
git add app/src/main/java/com/usipipo/vpn/widget \
        app/src/main/java/com/usipipo/vpn/AppContainer.kt \
        app/src/main/java/com/usipipo/vpn/UsipipoApp.kt \
        app/src/main/AndroidManifest.xml \
        app/src/test/java/com/usipipo/vpn/widget/WidgetUpdaterTest.kt
git commit -m "feat(widget): actualizar widget y mosaico solo ante cambios"
```

---

### Task 7: pista de accesos rápidos en Diagnóstico

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/diagnostics/QuickAccessHint.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/test/java/com/usipipo/vpn/ui/diagnostics/QuickAccessHintTest.kt`

**Interfaces:**
- Produces: `shouldShowQuickAccessHint(sdkInt: Int): Boolean` y el composable `QuickAccessHint()`.

- [ ] **Step 1: escribe el test que falla**

```kotlin
@Test
fun `la pista aparece desde android 13`() {
    assertTrue(shouldShowQuickAccessHint(33))
    assertTrue(shouldShowQuickAccessHint(35))
}

@Test
fun `la pista no aparece antes de android 13`() {
    assertFalse(shouldShowQuickAccessHint(32))
    assertFalse(shouldShowQuickAccessHint(26))
}
```

- [ ] **Step 2: ejecuta el test y comprueba que falla**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.ui.diagnostics.QuickAccessHintTest'
```

- [ ] **Step 3: implementa la política y el componente**

`shouldShowQuickAccessHint(sdkInt)` devuelve `sdkInt >= Build.VERSION_CODES.TIRAMISU`.

`QuickAccessHint` es un composable pequeño con `UsipipoCard`, título y dos pasos:

1. Mantener pulsada la pantalla de inicio, abrir Widgets y añadir uSipipo VPN.
2. Abrir ajustes rápidos, tocar Editar y añadir uSipipo VPN.

Añade previews claro y oscuro. `DiagnosticsScreen` muestra el componente después de `InfoCard` solo cuando `shouldShowQuickAccessHint(Build.VERSION.SDK_INT)` sea true.

- [ ] **Step 4: añade los strings**

```xml
<string name="quick_access_title">Accesos rápidos</string>
<string name="quick_access_widget_hint">Mantén pulsada la pantalla de inicio, abre Widgets y añade uSipipo VPN.</string>
<string name="quick_access_tile_hint">Abre el panel de ajustes rápidos, toca Editar y añade uSipipo VPN.</string>
```

- [ ] **Step 5: verifica y commit**

```bash
./gradlew testDebugUnitTest --tests 'com.usipipo.vpn.ui.diagnostics.QuickAccessHintTest'
git add app/src/main/java/com/usipipo/vpn/ui/diagnostics/QuickAccessHint.kt \
        app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt \
        app/src/main/res/values/strings.xml \
        app/src/test/java/com/usipipo/vpn/ui/diagnostics/QuickAccessHintTest.kt
git commit -m "feat(ui): pista de accesos rapidos en Diagnostico"
```

---

### Task 8: verificación integral y matriz manual

**Files:**
- Verify: todos los archivos de las tareas 1–7.
- Modifica únicamente si una verificación identifica un defecto: cambia solo el archivo que posee ese defecto.

**Interfaces:**
- Consumes: el subsistema completo.
- Produces: APK verificable y checklist manual ejecutado.

- [ ] **Step 1: comprueba el árbol y el diff**

```bash
git status --short
git diff --check
git diff main...HEAD --stat
```

No debe haber archivos generados, claves, `.conf`, logs ni artefactos de build.

- [ ] **Step 2: ejecuta la suite completa en CI**

En una máquina con JDK:

```bash
./gradlew testDebugUnitTest assembleDebug
```

En este entorno sin JDK:

```bash
git push -u origin feat/mockup-fase6
gh workflow run android-ci.yml --ref feat/mockup-fase6
sleep 15
RUN_ID="$(gh run list --workflow android-ci.yml --branch feat/mockup-fase6 --limit 1 --json databaseId -q '.[0].databaseId')"
test -n "$RUN_ID"
gh run watch "$RUN_ID" --exit-status
```

Si el workflow ya corrió antes, borra y vuelve a lanzar el run o acota `gh run list` por el identificador recién creado; no uses un valor inventado. Un run verde exige `testDebugUnitTest` y `assembleDebug` exitosos.

- [ ] **Step 3: prueba el widget 2×2**

Con la APK del CI:

1. Añade el widget desde Widgets.
2. Verifica logo, estado, botón, perfil y país.
3. Con perfil sin país, verifica que solo aparece el nombre.
4. Con país cacheado, verifica bandera y texto.
5. Redimensiona a 4×2 y confirma que cambia al layout horizontal.

- [ ] **Step 4: prueba el flujo de conexión**

1. Con permiso VPN concedido, toca conectar: el widget debe pasar a «Conectando…» y luego «Activo».
2. Toca de nuevo: debe desconectar.
3. Toca durante «Conectando…»: debe desconectar y no iniciar una segunda conexión.
4. Prueba el mismo flujo desde el mosaico.
5. Revoca el permiso VPN, toca conectar y deniega el diálogo: debe quedar «Desconectado», la Activity cerrarse y Diagnóstico contener solo el metadato de denegación.

- [ ] **Step 5: prueba Importar y perfiles ausentes**

1. Borra todos los perfiles.
2. Toca el widget o mosaico: debe abrir la app directamente en Importar, sin pedir permiso VPN antes.
3. Importa un perfil y confirma que el widget lo refleja sin reiniciar la app.
4. Borra el perfil seleccionado mientras el túnel está desconectado: debe pasar al siguiente o a «Importar perfil».

- [ ] **Step 6: prueba muerte del proceso y cierre externo**

1. Conecta el túnel.
2. Mata solo el proceso con `adb shell am kill com.usipipo.vpn`; no uses `force-stop`, que inhibe broadcasts.
3. Toca el widget: debe tratar el túnel como desconectado y conectar, no desconectar.
4. Activa otra VPN: cuando el sistema cierre el túnel, el widget y el mosaico deben reflejar «Desconectado».

- [ ] **Step 7: prueba arranque y actualización**

1. Reinicia el dispositivo: tras `BOOT_COMPLETED`, el widget no debe conservar un estado conectado falso.
2. Instala una versión posterior: tras `MY_PACKAGE_REPLACED`, el widget se refresca con el estado vivo.
3. Comprueba que no hay actualización periódica observable y que el consumo en reposo es estable.

- [ ] **Step 8: verifica temas y accesibilidad**

1. Cambia a tema oscuro y revisa los fondos normal, conectado y de fallo.
2. Confirma que el texto no se recorta en el layout 4×2.
3. Confirma que el perfil largo se recorta con elipsis.
4. Confirma que el registro no contiene claves, endpoint, IP ni texto `.conf`.

- [ ] **Step 9: revisión final de rama**

```bash
git log --oneline main..HEAD
git diff main...HEAD
```

Cada commit debe corresponder a una tarea del plan y ningún diff puede incluir cambios en `app/src/main/java/com/usipipo/vpn/tunnel/`.

---

## Criterios de aceptación finales

- El widget 2×2 y el layout 4×2 muestran el estado, perfil y país cacheado correctos.
- Widget y mosaico conectan y desconectan el mismo perfil seleccionado que Inicio.
- Conectar no requiere abrir la UI de la app; solo el permiso VPN puede mostrar una Activity transparente.
- Sin perfiles se abre Importar directamente.
- Un perfil ilegible muestra Fallo y el siguiente toque reintenta.
- El estado vivo corrige estados obsoletos tras muerte del proceso, arranque o actualización.
- El registro solo contiene metadatos.
- Los tests JVM nuevos y existentes pasan y CI genera la APK.
- No se modifica la lógica de `tunnel/` ni se añaden dependencias.
