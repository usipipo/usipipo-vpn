# Spec de diseño — Fase 3: País y Ping

**Fecha:** 2026-10-18  
**Estado:** borrador pendiente de revisión del usuario  
**Proyecto:** uSipipo VPN  
**Alcance:** Capa de datos + UI para mostrar país/bandera y ping real del túnel WireGuard activo, con caché de país por perfil y consumo mínimo de la API externa.

---

## 1. Objetivo

Cuando el túnel está conectado, la app muestra:
- **País y bandera** del servidor al que está conectado, consultados mediante geolocalización de IP sobre la VPN (por tanto el servicio externo ve la IP del servidor, no la real del teléfono).
- **Ping real** hacia el servidor dentro del túnel, cada 5 segundos mientras la pantalla de Detalles está visible.

Ambos son opcionales, nunca bloquean la conexión, y se degradan a «—» si fallan.

---

## 2. Decisiones tomadas (ya aprobadas)

| Decisión | Valor |
|----------|-------|
| Servicio de geolocalización | **ipapi.co** (`https://ipapi.co/json/{ip}/`), HTTPS, sin API key, 1,000 req/día gratuito |
| Estrategia de caché | Consultar caché primero; si `countryIp` == IP del endpoint actual y no expiró el TTL, usar caché. Solo llamar a la API en miss o cambio de IP |
| TTL del caché de país | **24 horas** |
| Ubicación del país | Detalles (pantalla principal de conexión) **y** bandera pequeña en `ProfileCard` de Inicio |
| Destino del ping | Dirección `.1` derivada del `Address` del cliente en el `.conf` (ej. `10.5.0.2/24` → `10.5.0.1`). Fallback: host del endpoint. Si no hay ninguno: «—» |
| Intervalo de ping | Cada 5 segundos, solo con pantalla Detalles visible y túnel conectado |
| Timeout de consulta de país | 3 segundos, 1 intento, sin reintentos en bucle |
| Fallo de país | Mostrar globo (⏣) y `null` en país; anotar en `AppLog` con IP abreviada |
| Fallo de ping | Mostrar «—» |

---

## 3. Requisitos funcionales

### RF-1 — Consulta de país al conectar
Cuando el túnel pasa a `TunnelState.Connected`, extraer la IP del endpoint del `.conf` activo. Si el caché tiene una entrada para ese `configId` con la misma IP y edad < 24 h, usarla. Si no, llamar a ipapi.co.

### RF-2 — Bandera en Inicio
En `ProfileCard` de Inicio, junto al nombre del perfil activo, mostrar la bandera (emoji ISO 2 letras) y el nombre del país cuando se conozca. Si no se conoce aún o falló, mostrar globo.

### RF-3 — Ping en Detalles
En la pantalla Detalles, mostrar el ping (ms) actualizado cada 5 s mientras el túnel esté conectado y la pantalla sea visible. Color del badge: verde (<100 ms), ámbar (100-250 ms), rojo (>250 ms), gris («—»).

### RF-4 — Caché persistente por perfil
El resultado de geolocalización (`countryCode`, `countryName`, `countryIp`, `countryFetchedAt`) se persiste en el índice cifrado de `EncryptedFileConfigStore` junto al `configId`, sobreviviendo reinicios de la app.

### RF-5 — No bloqueo
Nunca bloquear conexión, desconexión ni ninguna pantalla por fallo de país o ping.

### RF-6 — Registro de fallos
Los fallos de geolocalización se anotan en `AppLog` con la IP abreviada (primeros 2 octetos), sin claves ni IPs completas del usuario.

---

## 4. Requisitos no funcionales

- **RNF-1:** HTTP/HTTPS obligatorio para la llamada externa (ipapi.co soporta HTTPS en la capa gratuita).
- **RNF-2:** Máximo 1 llamada a ipapi.co por cambio de IP o expiración de caché (24 h). Con 1,000 req/día gratuito, suficiente para uso típico.
- **RNF-3:** Timeout de 3 s por consulta de país; sin reintentos automáticos.
- **RNF-4:** Ping nativo vía `ping -c 1 -W 1000` con fallback a `InetAddress.isReachable`.
- **RNF-5:** Todo el acceso de red fuera del hilo principal (Dispatchers.IO).
- **RNF-6:** Tests en JVM para cálculo de caché, lectura de salida de ping, migración de campos de caché en `SavedConfig`.
- **RNF-7:** Previews de Compose de Inicio y Detalles en tema claro y oscuro.

---

## 5. Arquitectura y componentes

### 5.1 Modelo de dominio (`domain/`)

**`CountryInfo.kt`** — resultado de geolocalización:
```kotlin
data class CountryInfo(
    val ip: String,
    val countryCode: String?,   // ISO 2 letras, ej. "DE"
    val countryName: String?,   // ej. "Germany"
    val fetchedAt: Long,        // System.currentTimeMillis()
    val error: Boolean = false  // true si la consulta falló
)
```

**`PingResult.kt`** — resultado de ping:
```kotlin
data class PingResult(
    val ms: Long?,
    val error: Boolean = false
)
```

**Reloj del TTL:** `fetchedAt` y `countryFetchedAt` usan el reloj de pared
(`System.currentTimeMillis()`), no `SystemClock.elapsedRealtime()`. El dato se persiste en
`index.json` y debe sobrevivir a reinicios; `elapsedRealtime()` se reinicia al apagar el
dispositivo y reiniciaría el TTL de 24 h, duplicando las llamadas a la API.

**Cambio a `SavedConfig`** (`domain/Models.kt`):
```kotlin
data class SavedConfig(
    val id: String,
    val name: String,
    val favorite: Boolean = false,
    val countryCode: String? = null,
    val countryName: String? = null,
    val countryIp: String? = null,
    val countryFetchedAt: Long? = null,
)
```

### 5.2 Capa de datos (`data/geo/`)

**`CountryRepository`** — responsable de la consulta de país con caché.
- `suspend fun countryFor(configId: String, ip: String): CountryInfo`
  1. Lee `SavedConfig` del `configId` desde `ConfigStore`.
  2. Si `countryIp == ip` y `countryFetchedAt` no supera 24 h → retorna caché (`CountryInfo` con datos guardados, `error = false`).
  3. Si miss o IP changed → `GET https://ipapi.co/json/{ip}/` (timeout 3 s, 1 intento).
  4. Si éxito → parsear `country_name` y `country_code`, guardar en `SavedConfig` (via `ConfigStore`), retornar `CountryInfo`.
  5. Si falla (timeout, 429, error de red, JSON inválido) → retornar `CountryInfo(ip = ip, error = true)`, anotar en `AppLog` con IP abreviada, no guardar nada nuevo (mantener caché anterior si existe).

**`PingRepository`** — responsable del ping nativo.
- `suspend fun ping(target: String): PingResult`
  1. Ejecutar `ping -c 1 -W 1000 <target>` vía `ProcessBuilder` en Dispatchers.IO.
  2. Parsear la salida para extraer `time=XX ms` y `time<1 ms` (el ping al `.1` del tunel
     suele ser menor de 1 ms).
  3. Si `ping` no existe o falla → `InetAddress.isReachable(target, 1000)` → si true, `ms = null` (solo sabemos que responde); si false, `error = true`.
  4. Retornar `PingResult(ms = ms, error = error)`.

**Cambio a `ConfigStore`** (`data/ConfigStore.kt`):
Agregar método `suspend fun updateCountry(id: String, countryInfo: CountryInfo)` que actualice los campos de caché del perfil. Implementación en `EncryptedFileConfigStore` persiste en `index.json` (misma estructura que `favorite`).

### 5.3 Capa de presentación (`ui/details/`)

**`CountryPingViewModel`**:
- Recibe `TunnelManager`, `ConfigStore`, `CountryRepository`, `PingRepository`.
- Expone `StateFlow<CountryPingUiState>` donde:
```kotlin
data class CountryPingUiState(
    val countryInfo: CountryInfo? = null,
    val pingResult: PingResult? = null,
    val isLoadingCountry: Boolean = false,
)
```
- Al detectar `TunnelState.Connected(configId)` → extraer IP del endpoint (via `ConfSummary.parse(store.readText(configId))`) → consultar país (una vez, al conectar; re-consultar si la IP cambia).
- Mientras la pantalla Detalles es visible y el túnel está conectado → lanzar coroutine que hace ping cada 5 s y emite `PingResult`.
- Cuando la pantalla deja de ser visible o el túnel se desconecta → cancelar el job de ping.

### 5.4 UI (`ui/details/`, `ui/home/`, `ui/`)

**`CountryPingRow`** (composable en `ui/details/`):
- Muestra bandera (emoji de `countryCode` → regional indicator) + nombre de país, o globo si `error` / `null`.
- Muestra ping con badge de color según ms, o «—».

**Cambios en `DetailsScreen`:**
- Recibir adicionalmente `countryPing: CountryPingUiState`.
- Añadir filas `InfoRow` para país (bandera + nombre) y ping (badge coloreado).

**Cambios en `HomeScreen` / `ProfileCard`:**
- Recibir `countryCode: String?`.
- Mostrar emoji de bandera + país a la derecha del nombre, o globo si no hay dato.

**Cambios en `UsipipoRoot`:**
- Crear `CountryPingViewModel` y pasarlo a `DetailsScreen`.
- Pasar `countryCode` del perfil activo a `HomeScreen`/`ProfileCard`.

**Cambios en `AppContainer`:**
- Agregar `countryRepository: CountryRepository` y `pingRepository: PingRepository` (lazy, construidos con `ConfigStore`, `TunnelManager`, `AppContext`).

---

## 6. Flujo de datos paso a paso

### País (al conectar)

```
TunnelManager.state → Connected(configId)
  → StatsViewModel ya lee el .conf para endpoint/IPv6 (flujo existente)
  → CountryPingViewModel lee ConfSummary.endpoint, extrae IP (sin puerto)
  → CountryRepository.countryFor(configId, ip)
      → caché hit (misma IP, <24h)? → retornar país guardado
      → caché miss → HTTPS GET https://ipapi.co/json/{ip}/ (3s timeout)
          → éxito → guardar en SavedConfig vía ConfigStore.updateCountry
          → fallo → CountryInfo(error=true), log en AppLog (IP abreviada)
  → Emitir CountryInfo al UI
  → UI: bandera + país en Detalles y ProfileCard de Inicio
```

### Ping (cada 5 s, solo Detalles visible)

```
Pantalla Detalles visible + TunnelState.Connected
  → lanzar coroutine loop:
      → target = derivar .1 del Address del cliente en .conf
          → fallback: host del endpoint
          → fallback: null → PingResult(error=true)
      → PingRepository.ping(target)
      → emitir PingResult
      → delay(5000)
  → cancelar al salir de Detalles o al desconectar
```

---

## 7. Derivación de la IP del endpoint y del .1

**IP del endpoint (para geolocalización):**
- `ConfSummary.endpoint` tiene forma `host:puerto` o `ip:puerto`.
- Extraer la parte antes del `:`. Si es un hostname, resolverlo a IP mediante `InetAddress.getByName` (best effort). Si no resuelve, **no se consulta el pais** y se muestra el globo: `ipapi.co` no acepta dominios en `/json/{ip}/`, y enviar el nombre de host consumiria cuota sin resultado.

**Destino del ping (`.1`):**
- Leer `Address` del cliente en el `.conf` (ej. `10.5.0.2/24`).
- Tomar la red base y reemplazar el último octeto por `1` → `10.5.0.1`.
- Si no se puede parsear, usar el host del endpoint.
- Si nada disponible → ping = «—».

---

## 8. Manejo de errores y degradación

| Escenario | Comportamiento |
|-----------|---------------|
| ipapi.co timeout (3 s) | `error = true`, globo en UI, log en AppLog, sin reintento |
| ipapi.co HTTP 429 (rate limit) | `error = true`, globo, log con IP abreviada. Se respeta `X-Ttl` (segundos hasta el reset): el repositorio entra en **cooldown** local y no vuelve a consultar la API hasta el reinicio del contador. No se guarda nada en cache |
| ipapi.co JSON inválido | `error = true`, globo, log |
| Sin internet | `error = true`, globo, log |
| Ping binario no existe | fallback `isReachable`, ms=null, «—» si false |
| Fabricante bloquea ping nativo | `isReachable` false → «—» |
| Túnel se desconecta | Cancelar job de ping; limpiar estado de UI a «—» |

---

## 9. Cambios a archivos existentes

| Archivo | Cambio |
|---------|--------|
| `domain/Models.kt` | `SavedConfig` gana `countryCode`, `countryName`, `countryIp`, `countryFetchedAt` |
| `data/ConfigStore.kt` | Agregar `updateCountry(id, countryInfo)` a la interfaz |
| `data/EncryptedFileConfigStore.kt` | Implementar `updateCountry`; persistir nuevos campos en `index.json`; leerlos en `loadIndex` |
| `data/geo/CountryRepository.kt` | **Nuevo** |
| `data/geo/PingRepository.kt` | **Nuevo** |
| `ui/details/CountryPingViewModel.kt` | **Nuevo** |
| `ui/details/CountryPingUiState.kt` | **Nuevo** (data class) |
| `ui/details/CountryPingRow.kt` | **Nuevo** (composable) |
| `ui/details/DetailsScreen.kt` | Recibir `countryPing`, añadir filas país + ping |
| `ui/details/StatsViewModel.kt` | Sin cambios (ya lee endpoint; se reusa) |
| `ui/home/HomeScreen.kt` | Pasar `countryCode` al `ProfileCard` |
| `ui/home/ProfileCard` (privado en HomeScreen) | Mostrar bandera + país |
| `ui/UsipipoRoot.kt` | Crear y cablear `CountryPingViewModel` |
| `app/AppContainer.kt` | Agregar `countryRepository` y `pingRepository` |

---

## 10. Testing

### JVM (JUnit 4, sin Android)

1. **`CountryRepositoryTest`**
   - Caché hit con misma IP y <24 h → no llama a la API.
   - Caché miss (IP diferente) → llama a la API.
   - Caché expirada (>24 h) → llama a la API.
   - API falla → retorna `error = true`, no guarda nada nuevo.
   - IP abreviada en el log de fallo.

2. **`PingRepositoryTest`**
   - Salida `64 bytes from 10.5.0.1: icmp_seq=1 ttl=64 time=23.4 ms` → `ms = 23`.
   - Ping falla → fallback `isReachable`.
   - Target null → `error = true`.

3. **`EncryptedFileConfigStoreCountryTest`** (migración)
   - Perfiles existentes sin campos de país → `null`, no rompen.
   - Guardar y leer `countryCode`/`countryName`/`countryIp`/`countryFetchedAt`.

4. **`SavedConfigTest`** — copia con campos de país conserva los demás.

### Compose Previews

- `PreviewDetailsScreen` — con país y ping, tema claro y oscuro.
- `PreviewProfileCard` — con bandera, tema claro y oscuro.
- Estado «sin datos» (globo / «—»).

### Manual

- Prueba en gama baja y media (contraste, rendimiento del ping cada 5 s).
- Verificar que 1,000 req/día no se agotan en uso normal (caché 24 h).

---

## 11. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|-----------|
| ipapi.co cambia su capa gratuita o bloquea IP de la app | Diseñar `CountryRepository` detrás de interfaz; fácil de reemplazar por ipinfo.io/ipgeolocation.io |
| Algunos fabricantes bloquean `ping` | Fallback a `InetAddress.isReachable`; mostrar «—» |
| Hostname del endpoint no resuelve | No se consulta el pais (ipapi.co no acepta dominios en `/json/{ip}/`); se muestra el globo sin gastar cuota |
| Caché stale (servidor cambió de país) | TTL 24 h; re-consulta al cambiar de IP de endpoint |

---

## 12. Puntos abiertos (a resolver al implementar)

- Exacto formato del emoji de bandera en el `Text` de Compose (regional indicator indicators: `countryCode` a dos símbolos Unicode).
- Si se muestra el país también en la lista de «Mis perfiles» de Configuraciones (deferido a fase posterior).
- Migración de perfiles existentes: `countryCode` etc. son `null` por defecto, compatible hacia atrás.

---

Fin del spec. Por favor revíselo y confirme si aprueba o solicita cambios antes de pasar al plan de implementación.