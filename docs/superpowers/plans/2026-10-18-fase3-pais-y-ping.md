# Fase 3 — País y Ping: Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir a uSipipo VPN la visualización de país/bandera (mediante ipapi.co sobre la VPN) y ping real (cada 5 s dentro del túnel) en la pantalla Detalles y bandera en el ProfileCard de Inicio, con caché de país por perfil (TTL 24 h) para minimizar llamadas a la API externa.

**Architecture:** Capa de datos (`data/geo/`) con `CountryRepository` (caché + HTTP a ipapi.co) y `PingRepository` (ping nativo con fallback `isReachable`). Capa de presentación (`ui/details/`) con `CountryPingViewModel` que orquesta país (una vez al conectar) y ping (loop cada 5 s solo mientras Detalles es visible). UI: `CountryPingRow` en Detalles, bandera en `ProfileCard` de Inicio. Cableado en `AppContainer` y `UsipipoRoot`.

**Tech Stack:** Kotlin, Jetpack Compose, kotlinx.coroutines (StateFlow, viewModelScope), HttpURLConnection (estándar, sin dependencias nuevas), ProcessBuilder para `ping`, JUnit 4 para tests JVM, Compose previews.

**Spec:** `docs/superpowers/specs/2026-10-18-usipipo-vpn-fase3-pais-y-ping-design.md`

## Global Constraints

- Máximo 1 llamada a ipapi.co por cambio de IP o expiración de caché (TTL 24 h). Capa gratuita: 1,000 req/día, HTTPS obligatorio.
- Timeout de 3 s por consulta de país; 1 intento, sin reintentos en bucle.
- Ping cada 5 s, solo con túnel conectado y pantalla Detalles visible.
- País y ping son opcionales: nunca bloquean conexión ni ninguna pantalla.
- Fallos de geolocalización se anotan en `AppLog` con IP abreviada, sin claves ni IPs completas del usuario.
- Todo acceso de red fuera del hilo principal (Dispatchers.IO).
- Cada fase debe compilar y pasar tests por separado (`./gradlew testDebugUnitTest` verde).
- No tocar la lógica de conexión del túnel (`WireGuardTunnelManager` se lee solo).
- Tema claro y oscuro; previews de Compose en ambos temas.

## Review Focus

1. IP del endpoint ausente o sin puerto → país no se consulta, se muestra globo sin romper nada.
2. ipapi.co timeout (3 s) o HTTP 429 → `CountryInfo(error=true)`, globo en UI, log en AppLog, sin reintento.
3. Binario `ping` ausente o bloqueado por fabricante → fallback `InetAddress.isReachable`; si falla, «—».
4. Perfil existente sin campos de país (migración) → campos `null`, no rompe carga del almacén.
5. Túnel se desconecta mientras Detalles abierto → job de ping se cancela, UI muestra «—».

---

### Task 1: Domain models CountryInfo, PingResult, y campos de país en SavedConfig

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/domain/CountryInfo.kt`
- Create: `app/src/main/java/com/usipipo/vpn/domain/PingResult.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/domain/Models.kt`
- Test: `app/src/test/java/com/usipipo/vpn/domain/CountryInfoTest.kt`
- Test: `app/src/test/java/com/usipipo/vpn/domain/PingResultTest.kt`
- Test: `app/src/test/java/com/usipipo/vpn/domain/SavedConfigCountryTest.kt`

**Interfaces:**
- Produces: `CountryInfo(ip: String, countryCode: String?, countryName: String?, fetchedAt: Long, error: Boolean = false)`
- Produces: `PingResult(ms: Long?, error: Boolean = false)`
- Produces: `SavedConfig(id, name, favorite = false, countryCode: String? = null, countryName: String? = null, countryIp: String? = null, countryFetchedAt: Long? = null)`

- [ ] **Step 1: Write failing test — SavedConfig copia con campos de país conserva el resto**

```kotlin
@Test
fun `copia con pais conserva campos existentes`() {
    val original = SavedConfig("id-1", "Mi VPN", favorite = true)
    val copy = original.copy(countryCode = "DE", countryName = "Germany")
    assertEquals("DE", copy.countryCode)
    assertEquals("Germany", copy.countryName)
    assertTrue(copy.favorite)
    assertEquals("id-1", copy.id)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.domain.SavedConfigCountryTest"`
Expected: FAIL (campos no existen en SavedConfig)

- [ ] **Step 2: Implement fields en `SavedConfig`**

En `Models.kt`, agregar a `data class SavedConfig` los parámetros `countryCode`, `countryName`, `countryIp`, `countryFetchedAt` (todos `? = null`).

- [ ] **Step 3: Run test — SavedConfig pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.domain.SavedConfigCountryTest"`
Expected: PASS

- [ ] **Step 4: Write failing tests — CountryInfo y PingResult constructores**

En `CountryInfoTest.kt` y `PingResultTest.kt`, tests básicos de constructor y default `error = false`.

- [ ] **Step 5: Crear `CountryInfo.kt` y `PingResult.kt`**

- [ ] **Step 6: Run tests — domain pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.domain.*"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/domain/
git add app/src/test/java/com/usipipo/vpn/domain/
git commit -m "feat(domain): modelos CountryInfo, PingResult y campos de pais en SavedConfig"
```

---

### Task 2: ConfigStore.updateCountry y persistencia en EncryptedFileConfigStore

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/data/ConfigStore.kt` (agregar `updateCountry`)
- Modify: `app/src/main/java/com/usipipo/vpn/data/EncryptedFileConfigStore.kt` (implementar + leer/escribir campos en index.json)
- Test: `app/src/test/java/com/usipipo/vpn/data/EncryptedFileConfigStoreCountryTest.kt`

**Interfaces:**
- Consumes: `SavedConfig` con campos de país (Task 1)
- Produces: `suspend fun updateCountry(id: String, countryCode: String?, countryName: String?, countryIp: String?, countryFetchedAt: Long?)` en `ConfigStore`

- [ ] **Step 1: Write failing test — guardar y leer campos de país en el almacén**

```kotlin
@Test
fun `guarda y lee campos de pais de un perfil`() = runBlocking {
    val store = FakeConfigStoreWithCountry() // o EncryptedFileConfigStore real con dir temporal
    store.updateCountry("id-1", "DE", "Germany", "1.2.3.4", 1000L)
    val cfg = store.configs.value.first { it.id == "id-1" }
    assertEquals("DE", cfg.countryCode)
    assertEquals("Germany", cfg.countryName)
    assertEquals("1.2.3.4", cfg.countryIp)
    assertEquals(1000L, cfg.countryFetchedAt)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.data.EncryptedFileConfigStoreCountryTest"`
Expected: FAIL (`updateCountry` no existe en ConfigStore)

- [ ] **Step 2: Agregar `updateCountry` a la interfaz `ConfigStore`**

- [ ] **Step 3: Implementar `updateCountry` en `EncryptedFileConfigStore`**

Seguir el patrón de `setFavorite`: mutex, Dispatchers.IO, mapear `_configs.value`, `persistIndex()`. En `persistIndex` agregar los 4 campos al JSONObject. En `loadIndex` leerlos con `optString`/`optLong` y defaults null.

- [ ] **Step 4: Run test — almacén de país pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.data.EncryptedFileConfigStoreCountryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/data/ConfigStore.kt
git add app/src/main/java/com/usipipo/vpn/data/EncryptedFileConfigStore.kt
git add app/src/test/java/com/usipipo/vpn/data/EncryptedFileConfigStoreCountryTest.kt
git commit -m "feat(data): persistencia de campos de pais en ConfigStore"
```

---

### Task 3: CountryRepository — caché + llamada a ipapi.co

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/data/geo/CountryRepository.kt`
- Create: `app/src/main/java/com/usipipo/vpn/data/geo/IpApiClient.kt` (cliente HTTP mínimo, HttpURLConnection)
- Test: `app/src/test/java/com/usipipo/vpn/data/geo/CountryRepositoryTest.kt`

**Interfaces:**
- Consumes: `ConfigStore` (Task 2), `CountryInfo` (Task 1)
- Produces: `class CountryRepository(private val store: ConfigStore)`
- Produces: `suspend fun countryFor(configId: String, ip: String): CountryInfo`

- [ ] **Step 1: Write failing test — caché hit con misma IP y <24 h no llama a la API**

Usar un `FakeConfigStore` y un `HttpClient` falso que falle si se llama.

```kotlin
@Test
fun `cache hit misma ip no llama a la api`() = runBlocking {
    val store = FakeCountryStore(configs = listOf(
        SavedConfig("c1", "x", countryIp = "1.2.3.4", countryCode = "DE", countryName = "Germany", countryFetchedAt = SystemClock.elapsedRealtime() - 3600_000)
    ))
    var httpCalled = false
    val repo = CountryRepository(store, http = object : FakeHttp { override suspend fun get(ip: String) { httpCalled = true; throw AssertionError("no debe llamar") } })
    val result = repo.countryFor("c1", "1.2.3.4")
    assertFalse(httpCalled)
    assertEquals("DE", result.countryCode)
    assertFalse(result.error)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.data.geo.CountryRepositoryTest"`
Expected: FAIL (CountryRepository no existe)

- [ ] **Step 2: Crear `IpApiClient` (HttpURLConnection, timeout 3 s, GET https://ipapi.co/json/{ip}/, parsear country_name/country_code)**

- [ ] **Step 3: Crear `CountryRepository.countryFor`**

Lógica: leer SavedConfig del configId → si `countryIp == ip` y edad < 24 h → retornar caché. Si miss → llamar a IpApiClient → éxito: guardar en store via `updateCountry`, retornar CountryInfo. Fallo: retornar `CountryInfo(ip = ip, error = true)`, log en AppLog con IP abreviada (primeros 2 octetos).

- [ ] **Step 4: Run tests — CountryRepository pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.data.geo.CountryRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/data/geo/
git add app/src/test/java/com/usipipo/vpn/data/geo/CountryRepositoryTest.kt
git commit -m "feat(data/geo): CountryRepository con cache 24h y ipapi.co"
```

---

### Task 4: PingRepository — ping nativo con fallback isReachable

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/data/geo/PingRepository.kt`
- Test: `app/src/test/java/com/usipipo/vpn/data/geo/PingRepositoryTest.kt`

**Interfaces:**
- Produces: `class PingRepository`
- Produces: `suspend fun ping(target: String): PingResult`

- [ ] **Step 1: Write failing test — parsear salida de ping `time=23.4 ms`**

```kotlin
@Test
fun `parsea time de salida de ping`() = runBlocking {
    val repo = PingRepository(process = FakeProcess(output = "64 bytes from 10.5.0.1: icmp_seq=1 ttl=64 time=23.4 ms\n"))
    val result = repo.ping("10.5.0.1")
    assertEquals(23L, result.ms) // o 23.4 truncado/redondeado, definir en impl
    assertFalse(result.error)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.data.geo.PingRepositoryTest"`
Expected: FAIL (PingRepository no existe)

- [ ] **Step 2: Crear `PingRepository.ping`**

Ejecutar `ping -c 1 -W 1000 <target>` vía ProcessBuilder en Dispatchers.IO. Parsear `time=(\d+(?:\.\d+)?)` de la salida. Si proceso falla o no existe → `InetAddress.isReachable(target, 1000)`: true → `PingResult(ms = null, error = false)`; false → `PingResult(ms = null, error = true)`.

- [ ] **Step 3: Run tests — PingRepository pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.data.geo.PingRepositoryTest"`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/data/geo/PingRepository.kt
git add app/src/test/java/com/usipipo/vpn/data/geo/PingRepositoryTest.kt
git commit -m "feat(data/geo): PingRepository nativo con fallback isReachable"
```

---

### Task 5: CountryPingUiState y CountryPingViewModel

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/details/CountryPingUiState.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/details/CountryPingViewModel.kt`
- Test: `app/src/test/java/com/usipipo/vpn/ui/details/CountryPingViewModelTest.kt`

**Interfaces:**
- Consumes: `CountryRepository` (Task 3), `PingRepository` (Task 4), `TunnelManager`, `ConfigStore`, `ConfSummary` (existente)
- Produces: `data class CountryPingUiState(val countryInfo: CountryInfo? = null, val pingResult: PingResult? = null, val isLoadingCountry: Boolean = false)`
- Produces: `class CountryPingViewModel(store, tunnelManager, countryRepository, pingRepository) : ViewModel()` con `val ui: StateFlow<CountryPingUiState>`

- [ ] **Step 1: Write failing test — al conectar con IP nueva consulta país una vez**

```kotlin
@Test
fun `al conectar consulta pais una vez y emite resultado`() = runBlocking {
    val repo = FakeCountryRepository(result = CountryInfo("1.2.3.4", "DE", "Germany", now, false))
    val vm = CountryPingViewModel(store, tunnelManager, repo, pingRepo)
    tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))
    // store.readText("c1") retorna conf con endpoint 1.2.3.4:51820
    val first = vm.ui.first()
    assertEquals("DE", first.countryInfo?.countryCode)
    assertFalse(first.countryInfo!!.error)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.ui.details.CountryPingViewModelTest"`
Expected: FAIL (CountryPingViewModel no existe)

- [ ] **Step 2: Crear `CountryPingUiState`**

- [ ] **Step 3: Crear `CountryPingViewModel`**

- `ui`: `tunnelManager.state` → filtrar `Connected(configId)` → extraer IP del endpoint vía `ConfSummary.parse(store.readText(configId))` → `countryRepository.countryFor(configId, ip)` (una vez, con `distinctUntilChanged` por configId e IP).
- Ping loop: mientras `ui` recolectado y `Connected` y pantalla visible → `pingRepository.ping(target)` cada 5 s. Usar `viewModelScope.launch` con `while (isActive)` o `flow { ... }.stateIn(WhileSubscribed)`.
- Target del ping: derivar `.1` del `Address` del cliente en el .conf (reusar `store.readText`), fallback host del endpoint.

- [ ] **Step 4: Run tests — CountryPingViewModel pasa**

Run: `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.ui.details.CountryPingViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/details/CountryPingUiState.kt
git add app/src/main/java/com/usipipo/vpn/ui/details/CountryPingViewModel.kt
git add app/src/test/java/com/usipipo/vpn/ui/details/CountryPingViewModelTest.kt
git commit -m "feat(ui/details): CountryPingViewModel con pais y ping"
```

---

### Task 6: CountryPingRow composable y filas en DetailsScreen

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/details/CountryPingRow.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/details/DetailsScreen.kt` (recibir countryPing, añadir filas)
- Test: Compose previews (sin test unitario, solo preview)

**Interfaces:**
- Consumes: `CountryPingUiState` (Task 5)
- Produces: `@Composable fun CountryPingRow(state: CountryPingUiState)`
- Produces: `DetailsScreen` recibe parámetro adicional `countryPing: CountryPingUiState`

- [ ] **Step 1: Crear `CountryPingRow`**

Bandera: emoji regional indicator de `countryCode` (dos caracteres Unicode U+1F1E6..U+1F1FF). Si `null` o `error` → globo `⏣`. Nombre de país al lado. Ping: badge coloreado (verde <100, ámbar 100-250, rojo >250, gris «—»).

- [ ] **Step 2: Modificar `DetailsScreen`**

Agregar parámetro `countryPing: CountryPingUiState`. Añadir dos `InfoRow`: país (bandera + nombre) y ping (valor con badge/color). Mantener firma existente para compatibilidad (parámetro con default o nuevo).

- [ ] **Step 3: Agregar previews Detalles claro/oscuro con país y ping**

En `PreviewDetailsScreen` agregar variantes con `countryPing` lleno y con `error = true`.

- [ ] **Step 4: Run previews / build**

Run: `./gradlew assembleDebug` (para verificar que compila Compose)
Expected: build verde

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/details/CountryPingRow.kt
git add app/src/main/java/com/usipipo/vpn/ui/details/DetailsScreen.kt
git commit -m "feat(ui/details): CountryPingRow y filas pais/ping en DetailsScreen"
```

---

### Task 7: Bandera de país en ProfileCard de Inicio

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt` (pasar countryCode al ProfileCard, mostrar bandera)
- Test: preview Inicio conectado con bandera

**Interfaces:**
- Consumes: `countryCode: String?` del perfil activo
- Produces: `ProfileCard` muestra emoji de bandera + país a la derecha del nombre, o globo si no hay dato

- [ ] **Step 1: Modificar `HomeScreen` para pasar `countryCode` a `ProfileCard`**

`HomeScreen` recibe `countryCode: String?` (nuevo parámetro con default null). `ProfileCard(config, countryCode)` muestra emoji de bandera o globo.

- [ ] **Step 2: Agregar preview Inicio conectado con bandera (claro/oscuro)**

- [ ] **Step 3: Run previews / build**

Run: `./gradlew assembleDebug`
Expected: build verde

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt
git commit -m "feat(ui/home): bandera de pais en ProfileCard de Inicio"
```

---

### Task 8: Cableado AppContainer, UsipipoRoot y DetailsScreen

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/AppContainer.kt` (agregar `countryRepository`, `pingRepository`)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt` (crear CountryPingViewModel, pasar a DetailsScreen y HomeScreen)
- Test: build + previews

**Interfaces:**
- Consumes: `CountryRepository` (Task 3), `PingRepository` (Task 4), `CountryPingViewModel` (Task 5)
- Produces: `AppContainer.countryRepository`, `AppContainer.pingRepository` (lazy)
- Produces: `UsipipoRoot` crea `CountryPingViewModel(container)` y lo pasa a `DetailsScreen`; `countryCode` del perfil activo a `HomeScreen`

- [ ] **Step 1: Agregar repositorios a `AppContainer`**

`val countryRepository: CountryRepository by lazy { CountryRepository(configStore, IpApiClient()) }`
`val pingRepository: PingRepository by lazy { PingRepository() }`

- [ ] **Step 2: Cablear en `UsipipoRoot`**

Crear `countryPingViewModel(container)` vía `viewModel(...)`. Pasar `countryPing = countryPingVm.ui.collectAsStateWithLifecycle().value` a `DetailsScreen`. Pasar `countryCode` del perfil activo (de `homeState`) a `HomeScreen`.

- [ ] **Step 3: Run build y previews**

Run: `./gradlew assembleDebug`
Expected: build verde

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/AppContainer.kt
git add app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt
git commit -m "feat(ui): cableado CountryPingViewModel en AppContainer y UsipipoRoot"
```

---

### Task 9: Tests JVM de caché y migración, suite completa

**Files:**
- Test: `app/src/test/java/com/usipipo/vpn/data/geo/CountryRepositoryTest.kt` (completar casos pendientes: caché expirado >24 h, IP diferente, API falla)
- Test: `app/src/test/java/com/usipipo/vpn/data/geo/PingRepositoryTest.kt` (completar: ping falla, target null, isReachable fallback)

**Interfaces:**
- Consumes: todo lo anterior

- [ ] **Step 1: Completar tests de CountryRepository (caché expirado, IP distinta, fallo de API)**

- [ ] **Step 2: Completar tests de PingRepository (salida inválida, target null, proceso fallido)**

- [ ] **Step 3: Run suite completa**

Run: `./gradlew testDebugUnitTest`
Expected: todos los tests pasan (verde)

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/usipipo/vpn/data/geo/
git commit -m "test(data/geo): tests de cache, migracion y ping"
```

---

## Criterios de aceptación (Definition of Done)

- [ ] `./gradlew testDebugUnitTest` → verde (todos los tests JVM, incluidos los nuevos).
- [ ] `./gradlew assembleDebug` → build verde.
- [ ] Previews de Compose: Detalles claro/oscuro con país y ping; Inicio conectado con bandera.
- [ ] País se consulta solo al conectar (o al cambiar de IP), caché 24 h, máximo 1 llamada a ipapi.co por miss.
- [ ] Ping cada 5 s solo en Detalles visible y túnel conectado.
- [ ] Fallos de país/ping muestran globo/«—» sin bloquear nada.
- [ ] Fallos de geolocalización anotados en `AppLog` con IP abreviada.

---

Plan completo y guardado en `docs/superpowers/plans/2026-10-18-fase3-pais-y-ping.md`. Por favor revisa el plan. ¿Captura lo que quieres?

¿Qué enfoque de ejecución prefieres?

- **Subagent-driven** — Un subagent implementa cada tarea y un reviewer fresco la revisa antes de la siguiente; review completo de rama al final. Más exhaustivo; contexto fresco por tarea y review.
- **Native** — Yo implemento todas las tareas en esta sesión, y un reviewer fresco revisa toda la rama al final. Más rápido y barato; review independiente solo al final.

Recomiendo **Native** para este plan: las tareas tienen interfaces bien definidas entre sí (Task 1→2→3→4→5 son lineales), son pocas (9 tareas) y el costo de un error enviado es bajo porque cada task tiene su propio test cycle. ¿Estás de acuerdo con Native, o prefieres Subagent-driven?
