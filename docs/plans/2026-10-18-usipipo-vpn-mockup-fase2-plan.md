# uSipipo VPN «Mockup» — Fase 2: datos reales y pantalla Detalles — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Lee antes `AGENTS.md` y `CODING_STANDARDS.md`.

**Goal:** Mostrar datos reales del túnel: velocidad de descarga y subida (con gráfica), tiempo activo, protocolo, endpoint e IPv6; añadir la pantalla Detalles (con botón Desconectar y lápiz para renombrar el perfil) y las tarjetas de estadísticas en Inicio. Antes, corregir los hallazgos de la revisión de la fase 1.

**Architecture:** El túnel gana una lectura de contadores (`TunnelManager.counters()`) y el estado `Connected` recuerda cuándo se conectó (`sinceMs`). Un `StatsViewModel` convierte los contadores en velocidad con un calculador puro y suavizado. La lectura solo corre mientras Inicio o Detalles están visibles (`WhileSubscribed`). Detalles no es una pestaña: es una subpantalla de Inicio que se cierra con Atrás, al cambiar de pestaña o al caer el túnel. El `.conf` se lee solo para extraer endpoint e IPv6 y no se conserva.

**Tech Stack:** Kotlin 2.3.21, Jetpack Compose (BOM 2026.08.00), Material 3, AGP 9.3.3, minSdk 26. **Sin dependencias nuevas.**

**Spec:** `docs/plans/2026-10-18-usipipo-vpn-mockup-design.md` (sección 5). Fase anterior: `docs/plans/2026-10-18-usipipo-vpn-mockup-fase1-plan.md`.

## Autorización explícita para tocar `tunnel/`

`AGENTS.md` y `CODING_STANDARDS.md` prohíben tocar `tunnel/` salvo que la fase lo autorice. **Esta fase lo autoriza, y solo para esto:**

1. `TunnelManager`: método nuevo `counters()` con implementación por defecto vacía.
2. `WireGuardTunnelManager`: implementar `counters()` y rellenar `sinceMs` al emitir `Connected`.
3. `domain/Models.kt`: `TunnelState.Connected` gana `sinceMs` con valor por defecto.

**No cambia** `connect`, `disconnect`, el sondeo de handshake, los tiempos ni la gestión de errores.

## Global Constraints

- Rama: `feat/mockup-fase2`, desde `main` **después de mergear la fase 1**. Commits en español, convencionales y minúsculas, un cambio lógico por commit (`CONTRIBUTING.md`).
- Verificación: `./gradlew testDebugUnitTest` en local si hay JDK; si no, ciclo CI: `gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2` y `gh run watch <id> --exit-status`. Los comandos de CI de cada tarea valen para los dos casos.
- TDD (red → green): en las tareas con lógica, el test se escribe y se ve fallar antes del código.
- Estándares: imports ordenados y sin uso, tipos explícitos en firmas públicas, sin `!!`, sin colores fijos (salvo el azul de marca ya existente), `Modifier` tras los datos y con valor por defecto, `@Preview` claro y oscuro de cada pantalla y estado. Constantes nuevas dentro de `companion object` privado, como en el código existente.
- Iconos: `material-icons-core` para símbolos estándar (`Edit`, `ArrowBack`); vectores propios para el resto.
- Datos del túnel: **nada de tráfico en el registro de diagnóstico**, ni claves, ni el `.conf`. Solo se lee el `.conf` para endpoint e IPv6.
- Fuera de alcance: país, bandera y ping (fase 3).
- Las etiquetas de velocidad indican que es la **velocidad actual del túnel, no una prueba de velocidad**.
- Los tests existentes deben seguir en verde. Se permite ampliar `Fakes.kt` (helpers de tests) para los métodos nuevos del contrato.

## Review Focus

Modos de fallo que ningún test unitario cubre; cada línea indica dónde queda fijado:

1. **Lectura solo con pantalla visible:** el `StateFlow` de estadísticas se recoge dentro de la rama `Home` de `UsipipoRoot`, no en la raíz. Si se recogiera arriba, el sondeo correría en todas las pestañas. Fijado en la tarea 8 (paso 3) y en la checklist manual (batería con la app en otra pestaña).
2. **Contadores que se reinician** al reconectar: el delta negativo debe valer 0, no una velocidad negativa. Fijado en `SpeedCalculatorTest` (tarea 2).
3. **`sinceMs` en 0** (estado construido sin hora): debe mostrar `--:--:--`, no un tiempo enorme. Fijado en `FormattersTest` (tarea 2).
4. **Detalles abierto y el túnel cae** (otro VPN, permiso revocado): debe cerrarse solo. Fijado en la tarea 8 (`LaunchedEffect`) y en la checklist manual.
5. **Atrás con Detalles abierto:** cierra Detalles y no sale de la app. Fijado en la tarea 8 (segundo `BackHandler`).
6. **Renombrar:** nombre vacío, duplicado de otro perfil (sin distinguir mayúsculas) y cambio solo de mayúsculas del propio nombre (válido). Fijado en `EncryptedFileConfigStoreRenameTest` (tarea 4).
7. **`getStatistics` falla** una vez: no se emite muestra y se conserva la última velocidad; no debe romper el flujo. Fijado en la tarea 3 (`readTotals` devuelve `null`).
8. **Rotación en Detalles:** `showDetails` y el texto del diálogo sobreviven (`rememberSaveable`). Fijado en la checklist manual.
9. **Renombrar el perfil activo conectado:** el nombre cambia en Inicio, Configuraciones y Detalles sin reconectar. Fijado en la checklist manual.

---

### Task 1: Correcciones de la revisión de la fase 1

Cuatro commits pequeños, uno por cambio lógico.

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/configs/ConfigsScreen.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/data/EncryptedFileConfigStore.kt`
- Modify: `AGENTS.md`

- [ ] **Step 0: Rama**

```bash
git switch main && git pull && git switch -c feat/mockup-fase2 && git push -u origin feat/mockup-fase2
```

- [ ] **Step 1: La píldora de la pestaña activa usa un color que el tema no define**

`NavigationBarItem` pinta el indicador con `secondaryContainer`, que ni `LightColors` ni `DarkColors` definen: sale con el lila/gris de Material por defecto, fuera de la paleta. En `Theme.kt` añadir a cada esquema:

```kotlin
// LightColors
    secondaryContainer = Color(0xFFD6E6FB),
    onSecondaryContainer = Navy,
// DarkColors
    secondaryContainer = Color(0xFF14386B),
    onSecondaryContainer = Color(0xFFE6F0FF),
```

Comprobar en una `@Preview` de `UsipipoBottomBar` (claro y oscuro) que el indicador es azul. Commit: `fix(ui): definir secondaryContainer para el indicador de la barra inferior`.

- [ ] **Step 2: Barra de estado con relleno doble y degradado cortado**

`UsipipoRoot` aplica `windowInsetsPadding(WindowInsets.statusBars)` a la columna, y además `HomeScreen`, `ConfigsScreen`, `ImportScreen` y `DiagnosticsScreen` ya reservan la barra de estado en su `Scaffold` (`contentWindowInsets = WindowInsets.statusBars`). Lo más probable es un hueco doble arriba; y en Inicio el degradado no cubre la franja de la barra de estado (se ve el fondo plano). El plan de la fase 1 dejaba esa responsabilidad solo en cada pantalla.

En `UsipipoRoot.kt`: quitar `.windowInsetsPadding(WindowInsets.statusBars)` de la `Column` y los imports `statusBars` y `windowInsetsPadding` que queden sin uso. Se mantienen `imePadding()` y `isImeVisible`.

Verificación en el teléfono (APK del CI): en Inicio el degradado llega hasta arriba, detrás de la barra de estado; en las otras pestañas el título no queda más abajo de lo debido. Commit: `fix(ui): dejar la barra de estado a cada pantalla`.

- [ ] **Step 3: Strings sin uso y orden de imports**

Strings sin uso (comprobado con `grep -rn "R.string.<nombre>\b"` y `@string/`): `configs_title`, `import_footer_title`, `import_footer_body`, `about_updates_title`, `support_label`. Eliminarlas de `strings.xml`. **No** eliminar `cd_back`: lo usa Detalles en la tarea 8.

`ConfigsScreen.kt`: ordenar los imports alfabéticamente por bloque (`android.*`, `androidx.*`, `com.usipipo.vpn.*`) —hoy `Preview`, `clip`, `Configuration` y `Surface` están intercalados— y sustituir los nombres calificados en línea (`androidx.compose.foundation.shape.RoundedCornerShape`, `androidx.compose.ui.graphics.Color`) por imports. `EncryptedFileConfigStore.kt`: mover `setFavorite` **antes** del `companion object` (el companion va al final de la clase).

Commit: `refactor(ui): quitar strings sin uso y ordenar imports`.

- [ ] **Step 4: Documentación de la estructura**

`AGENTS.md` dice que `UsipipoRoot` y `UsipipoApp` viven en `ui/nav/` y `ConnectButton` en `ui/home/`. En realidad: `UsipipoRoot.kt` está en `ui/`, `UsipipoApp.kt` en la raíz del paquete y `ConnectButton.kt` en `ui/components/`. Corregir el bloque «Estructura del código». Añadir `ui/details/` (Detalles y estadísticas) para que quede listo para esta fase. Commit: `docs: corregir la estructura de código en AGENTS.md`.

- [ ] **Step 5: Verificación**

```bash
./gradlew testDebugUnitTest
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase2 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

Esperado: `success`, mismos 79 tests de la fase 1 en verde.

---

### Task 2: Utilidades puras — contadores, velocidad, resumen del `.conf` y formato

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/domain/TunnelCounters.kt`
- Create: `app/src/main/java/com/usipipo/vpn/tunnel/SpeedCalculator.kt`
- Create: `app/src/main/java/com/usipipo/vpn/data/ConfSummary.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/details/Formatters.kt`
- Create: `app/src/test/java/com/usipipo/vpn/tunnel/SpeedCalculatorTest.kt`
- Create: `app/src/test/java/com/usipipo/vpn/data/ConfSummaryTest.kt`
- Create: `app/src/test/java/com/usipipo/vpn/ui/details/FormattersTest.kt`

**Interfaces (usadas por las tareas 3, 5, 7 y 8):**
- `data class TunnelCounters(val rxBytes: Long, val txBytes: Long, val atMs: Long)`
- `data class Throughput(val downBps: Long, val upBps: Long)` y `class SpeedCalculator(window: Int = 3)` con `add(counters): Throughput` y `reset()`. Las velocidades son **bytes por segundo**.
- `data class ConfSummary(val endpoint: String?, val routesIpv6: Boolean)` con `ConfSummary.parse(conf: String)`.
- `formatRate(bytesPerSecond, locale)`, `formatUptime(millis)`, `uptimeText(sinceMs, nowMs)`.

- [ ] **Step 1: Tests que fallan (red)**

`SpeedCalculatorTest.kt`:

```kotlin
package com.usipipo.vpn.tunnel

import com.usipipo.vpn.domain.TunnelCounters
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedCalculatorTest {

    @Test
    fun `una sola muestra no da velocidad`() {
        val calculator = SpeedCalculator()

        assertEquals(Throughput(0L, 0L), calculator.add(TunnelCounters(1_000L, 500L, 0L)))
    }

    @Test
    fun `velocidad constante entre dos muestras`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(0L, 0L, 0L))

        val result = calculator.add(TunnelCounters(2_000_000L, 500_000L, 1_000L))

        assertEquals(Throughput(2_000_000L, 500_000L), result)
    }

    @Test
    fun `suaviza con la ventana de muestras`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(0L, 0L, 0L))
        calculator.add(TunnelCounters(1_000_000L, 0L, 1_000L))
        calculator.add(TunnelCounters(1_000_000L, 0L, 2_000L))

        val result = calculator.add(TunnelCounters(4_000_000L, 0L, 3_000L))

        // 4 MB en 3 s, no los 3 MB del último segundo.
        assertEquals(1_333_333L, result.downBps)
    }

    @Test
    fun `descarta las muestras mas alla de la ventana`() {
        val calculator = SpeedCalculator()
        listOf(0L, 1_000_000L, 2_000_000L, 3_000_000L).forEachIndexed { i, rx ->
            calculator.add(TunnelCounters(rx, 0L, i * 1_000L))
        }

        val result = calculator.add(TunnelCounters(10_000_000L, 0L, 4_000L))

        // Ventana de 4 muestras: de 1 MB a 10 MB en 3 s.
        assertEquals(3_000_000L, result.downBps)
    }

    @Test
    fun `contadores que se reinician no dan velocidad negativa`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(5_000_000L, 5_000_000L, 0L))

        val result = calculator.add(TunnelCounters(100L, 100L, 1_000L))

        assertEquals(Throughput(0L, 0L), result)
    }

    @Test
    fun `dos muestras en el mismo instante no dividen entre cero`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(0L, 0L, 1_000L))

        assertEquals(Throughput(0L, 0L), calculator.add(TunnelCounters(500L, 500L, 1_000L)))
    }

    @Test
    fun `reset olvida las muestras anteriores`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(0L, 0L, 0L))
        calculator.add(TunnelCounters(1_000_000L, 0L, 1_000L))

        calculator.reset()

        assertEquals(Throughput(0L, 0L), calculator.add(TunnelCounters(9_000_000L, 0L, 2_000L)))
    }
}
```

`ConfSummaryTest.kt`:

```kotlin
package com.usipipo.vpn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfSummaryTest {

    private val conf = """
        [Interface]
        PrivateKey = clave-de-prueba
        Address = 10.66.66.2/32, fd42:42:42::2/128

        [Peer]
        PublicKey = otra-clave-de-prueba
        Endpoint = vpn.example.com:51820
        AllowedIPs = 0.0.0.0/0, ::/0
    """.trimIndent()

    @Test
    fun `extrae el endpoint`() {
        assertEquals("vpn.example.com:51820", ConfSummary.parse(conf).endpoint)
    }

    @Test
    fun `detecta que enruta IPv6`() {
        assertTrue(ConfSummary.parse(conf).routesIpv6)
    }

    @Test
    fun `sin la ruta IPv6 no la marca`() {
        val onlyV4 = conf.replace("0.0.0.0/0, ::/0", "0.0.0.0/0")

        assertFalse(ConfSummary.parse(onlyV4).routesIpv6)
    }

    @Test
    fun `sin endpoint devuelve null`() {
        val noEndpoint = conf.lines().filterNot { it.startsWith("Endpoint") }.joinToString("\n")

        assertNull(ConfSummary.parse(noEndpoint).endpoint)
    }

    @Test
    fun `no distingue mayusculas ni espacios en las claves`() {
        val messy = "  endpoint=1.2.3.4:51820  \nallowedips =  ::/0 "

        val summary = ConfSummary.parse(messy)

        assertEquals("1.2.3.4:51820", summary.endpoint)
        assertTrue(summary.routesIpv6)
    }

    @Test
    fun `un texto vacio no rompe nada`() {
        assertEquals(ConfSummary(endpoint = null, routesIpv6 = false), ConfSummary.parse(""))
    }
}
```

`FormattersTest.kt`:

```kotlin
package com.usipipo.vpn.ui.details

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormattersTest {

    @Test
    fun `una velocidad alta se muestra en Mbps`() {
        assertEquals("60.00 Mbps", formatRate(7_500_000L, Locale.US))
    }

    @Test
    fun `una velocidad baja se muestra en Kbps`() {
        assertEquals("500 Kbps", formatRate(62_500L, Locale.US))
    }

    @Test
    fun `sin trafico se muestra cero`() {
        assertEquals("0 Kbps", formatRate(0L, Locale.US))
    }

    @Test
    fun `justo bajo 1 Mbps no muestra mil Kbps`() {
        assertEquals("1.00 Mbps", formatRate(124_999L, Locale.US))
    }

    @Test
    fun `el tiempo se muestra como horas minutos y segundos`() {
        assertEquals("00:00:00", formatUptime(0L))
        assertEquals("00:28:15", formatUptime(28 * 60_000L + 15_000L))
        assertEquals("100:00:00", formatUptime(100 * 3_600_000L))
    }

    @Test
    fun `un tiempo negativo se muestra como cero`() {
        assertEquals("00:00:00", formatUptime(-5_000L))
    }

    @Test
    fun `sin hora de conexion no se inventa un tiempo`() {
        assertEquals("--:--:--", uptimeText(sinceMs = 0L, nowMs = 999_999L))
    }

    @Test
    fun `con hora de conexion calcula el tiempo transcurrido`() {
        assertEquals("00:01:05", uptimeText(sinceMs = 10_000L, nowMs = 75_000L))
    }
}
```

Esperado: no compila (red): faltan los tipos.

- [ ] **Step 2: Implementación (green)**

`TunnelCounters.kt`:

```kotlin
package com.usipipo.vpn.domain

/** Bytes acumulados por el túnel en un instante; [atMs] viene de `SystemClock.elapsedRealtime()`. */
data class TunnelCounters(val rxBytes: Long, val txBytes: Long, val atMs: Long)
```

`SpeedCalculator.kt`:

```kotlin
package com.usipipo.vpn.tunnel

import com.usipipo.vpn.domain.TunnelCounters

/** Velocidad en bytes por segundo. */
data class Throughput(val downBps: Long, val upBps: Long)

/**
 * Convierte contadores acumulados en velocidad, promediando las últimas [window] muestras
 * para que la gráfica no salte. Un contador que baja (reconexión) cuenta como 0.
 */
class SpeedCalculator(private val window: Int = 3) {

    private val samples = ArrayDeque<TunnelCounters>()

    fun add(counters: TunnelCounters): Throughput {
        samples.addLast(counters)
        while (samples.size > window + 1) samples.removeFirst()

        val first = samples.first()
        val last = samples.last()
        val elapsedMs = last.atMs - first.atMs
        if (elapsedMs <= 0L) return Throughput(0L, 0L)

        return Throughput(
            downBps = (last.rxBytes - first.rxBytes).coerceAtLeast(0L) * 1_000L / elapsedMs,
            upBps = (last.txBytes - first.txBytes).coerceAtLeast(0L) * 1_000L / elapsedMs,
        )
    }

    fun reset() = samples.clear()
}
```

`ConfSummary.kt`:

```kotlin
package com.usipipo.vpn.data

/** Lo único que la interfaz necesita saber de un `.conf`: nunca claves. */
data class ConfSummary(val endpoint: String?, val routesIpv6: Boolean) {

    companion object {
        fun parse(conf: String): ConfSummary {
            var endpoint: String? = null
            var routesIpv6 = false
            conf.lineSequence().forEach { raw ->
                val separator = raw.indexOf('=')
                if (separator < 0) return@forEach
                val key = raw.substring(0, separator).trim().lowercase()
                val value = raw.substring(separator + 1).trim()
                when (key) {
                    "endpoint" -> if (endpoint == null) endpoint = value
                    "allowedips" -> if (value.split(',').any { it.trim() == "::/0" }) routesIpv6 = true
                }
            }
            return ConfSummary(endpoint = endpoint, routesIpv6 = routesIpv6)
        }
    }
}
```

`Formatters.kt`:

```kotlin
package com.usipipo.vpn.ui.details

import java.util.Locale

/** Por encima de este umbral (en bits/s) se muestra en Mbps; evita «1000 Kbps». */
private const val MBPS_THRESHOLD_BITS = 999_500L

/** Velocidad en bytes por segundo, mostrada en bits como en el mockup («60.00 Mbps»). */
fun formatRate(bytesPerSecond: Long, locale: Locale = Locale.getDefault()): String {
    val bits = bytesPerSecond * 8L
    return if (bits >= MBPS_THRESHOLD_BITS) {
        String.format(locale, "%.2f Mbps", bits / 1_000_000.0)
    } else {
        String.format(locale, "%.0f Kbps", bits / 1_000.0)
    }
}

/** `HH:MM:SS`; las horas pueden pasar de 99. */
fun formatUptime(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    return String.format(
        Locale.US,
        "%02d:%02d:%02d",
        totalSeconds / 3_600L,
        (totalSeconds / 60L) % 60L,
        totalSeconds % 60L,
    )
}

/** Sin hora de conexión (`sinceMs` en 0) no se inventa un tiempo. */
fun uptimeText(sinceMs: Long, nowMs: Long): String =
    if (sinceMs <= 0L) "--:--:--" else formatUptime(nowMs - sinceMs)
```

(Constante a nivel de archivo, `private`: es una excepción a la regla de evitar `UPPER_SNAKE` nuevas porque `const val` exige nivel de archivo u objeto; si el equipo prefiere, moverla a un `private object`.)

- [ ] **Step 3: Verificación y commit**

```bash
./gradlew testDebugUnitTest
git add -A && git commit -m "feat(tunnel): utilidades de velocidad, resumen del conf y formato de estadísticas" \
  -m "Lógica pura y probada en JVM: sirve a la lectura de contadores y a las pantallas de la fase 2."
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2
```

Esperado: 21 tests nuevos en verde (7 + 6 + 8).

---

### Task 3: Contador del túnel y hora de conexión (autorizado en `tunnel/`)

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/domain/Models.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/tunnel/TunnelManager.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/tunnel/WireGuardTunnelManager.kt`
- Modify: `app/src/test/java/com/usipipo/vpn/ui/home/Fakes.kt`

**Interfaces (usadas por las tareas 5, 7 y 8):** `TunnelState.Connected(configId, sinceMs = 0L)`; `TunnelManager.counters(): Flow<TunnelCounters>`.

- [ ] **Step 1: `Connected` recuerda cuándo se conectó**

`Models.kt`:

```kotlin
    /** Túnel arriba y con handshake recibido. [sinceMs] es `SystemClock.elapsedRealtime()` en ese momento. */
    data class Connected(val configId: String, val sinceMs: Long = 0L) : TunnelState
```

El valor por defecto mantiene compilando todo el código y los tests existentes (`Connected("a")`).

- [ ] **Step 2: Contrato**

`TunnelManager.kt`:

```kotlin
import com.usipipo.vpn.domain.TunnelCounters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

    /**
     * Contadores acumulados del túnel, una muestra por segundo mientras alguien recoge el flujo.
     * Por defecto no emite nada (implementaciones de prueba).
     */
    fun counters(): Flow<TunnelCounters> = emptyFlow()
```

- [ ] **Step 3: Implementación en `WireGuardTunnelManager`**

En `monitorHandshake`, sustituir solo la emisión de `Connected`:

```kotlin
                setState(TunnelState.Connected(configId, SystemClock.elapsedRealtime()))
```

Añadir (junto a `hasHandshake`):

```kotlin
    override fun counters(): Flow<TunnelCounters> = flow {
        while (true) {
            val tunnel = active?.tunnel
            if (tunnel != null) {
                val totals = withContext(Dispatchers.IO) { readTotals(tunnel) }
                if (totals != null) {
                    emit(TunnelCounters(totals.first, totals.second, SystemClock.elapsedRealtime()))
                }
            }
            delay(STATS_POLL_MS)
        }
    }

    /** Suma recibidos y enviados de todos los peers; null si la librería falla esta vez. */
    private fun readTotals(tunnel: Tunnel): Pair<Long, Long>? = try {
        val stats = backend.getStatistics(tunnel)
        var rx = 0L
        var tx = 0L
        stats.peers().forEach { key ->
            stats.peer(key)?.let {
                rx += it.rxBytes()
                tx += it.txBytes()
            }
        }
        rx to tx
    } catch (e: Exception) {
        AppLog.d(TAG, "getStatistics falló (${e.javaClass.simpleName})")
        null
    }
```

En el `companion object`: `const val STATS_POLL_MS = 1_000L`. Imports nuevos: `com.usipipo.vpn.domain.TunnelCounters`, `kotlinx.coroutines.flow.Flow`, `kotlinx.coroutines.flow.flow`. **No se registran cifras de tráfico.**

- [ ] **Step 4: Fakes**

En `Fakes.kt`, dentro de `FakeTunnelManager`:

```kotlin
    val counterSamples = MutableSharedFlow<TunnelCounters>(extraBufferCapacity = 16)

    override fun counters(): Flow<TunnelCounters> = counterSamples
```

con imports `com.usipipo.vpn.domain.TunnelCounters`, `kotlinx.coroutines.flow.Flow`, `kotlinx.coroutines.flow.MutableSharedFlow`.

- [ ] **Step 5: Verificación y commit**

```bash
./gradlew testDebugUnitTest
git add -A && git commit -m "feat(tunnel): leer contadores del túnel y guardar la hora de conexión" \
  -m "Autorizado por el plan de la fase 2: solo añade lectura; no cambia connect, disconnect ni el sondeo de handshake."
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2
```

Esperado: `success`, sin cambios en ningún test existente.

---

### Task 4: Renombrar perfiles

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/data/ConfigStore.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/data/EncryptedFileConfigStore.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeViewModel.kt`
- Modify: `app/src/test/java/com/usipipo/vpn/ui/home/Fakes.kt`
- Modify: `app/src/test/java/com/usipipo/vpn/data/EncryptedFileConfigStoreFavoritesTest.kt` (extraer helper)
- Create: `app/src/test/java/com/usipipo/vpn/data/IdentityCrypto.kt`
- Create: `app/src/test/java/com/usipipo/vpn/data/EncryptedFileConfigStoreRenameTest.kt`
- Create: `app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelRenameTest.kt`

**Interfaces (usadas por la tarea 8):** `ConfigStore.rename(id, name): Result<SavedConfig>`; `HomeViewModel.rename(id, name): Result<SavedConfig>` (suspend).

- [ ] **Step 1: Helper de tests compartido (refactor)**

`IdentityCrypto.kt`:

```kotlin
package com.usipipo.vpn.data

/** Sin cifrado: en los tests del almacén solo importa el índice, no el Keystore. */
internal class IdentityCrypto : Crypto {
    override fun encrypt(plain: ByteArray): ByteArray = plain

    override fun decrypt(blob: ByteArray): ByteArray = blob
}
```

En `EncryptedFileConfigStoreFavoritesTest.kt` eliminar su `private class IdentityCrypto` (mismo paquete: usa el nuevo). Commit aparte: `refactor(data): compartir IdentityCrypto entre tests del almacén`.

- [ ] **Step 2: Tests que fallan (red)**

`EncryptedFileConfigStoreRenameTest.kt`:

```kotlin
package com.usipipo.vpn.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncryptedFileConfigStoreRenameTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `el nuevo nombre se conserva al reabrir el almacen`() = runTest {
        val dir = tmp.newFolder("configs")
        val first = EncryptedFileConfigStore(dir, IdentityCrypto())
        val saved = first.add("Casa", "conf").getOrThrow()

        val result = first.rename(saved.id, "  Oficina ")

        assertEquals("Oficina", result.getOrThrow().name)
        val second = EncryptedFileConfigStore(dir, IdentityCrypto())
        assertEquals("Oficina", second.configs.value.single().name)
    }

    @Test
    fun `usar el nombre de otro perfil falla por duplicado sin distinguir mayusculas`() = runTest {
        val store = EncryptedFileConfigStore(tmp.newFolder("configs"), IdentityCrypto())
        val casa = store.add("Casa", "conf").getOrThrow()
        store.add("Trabajo", "conf").getOrThrow()

        val result = store.rename(casa.id, "trabajo")

        assertTrue(result.exceptionOrNull() is DuplicateNameException)
        assertEquals("Casa", store.configs.value.first { it.id == casa.id }.name)
    }

    @Test
    fun `cambiar solo las mayusculas del propio nombre es valido`() = runTest {
        val store = EncryptedFileConfigStore(tmp.newFolder("configs"), IdentityCrypto())
        val casa = store.add("Casa", "conf").getOrThrow()

        assertEquals("CASA", store.rename(casa.id, "CASA").getOrThrow().name)
    }

    @Test
    fun `un nombre vacio falla`() = runTest {
        val store = EncryptedFileConfigStore(tmp.newFolder("configs"), IdentityCrypto())
        val casa = store.add("Casa", "conf").getOrThrow()

        assertTrue(store.rename(casa.id, "   ").exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `un id desconocido falla`() = runTest {
        val store = EncryptedFileConfigStore(tmp.newFolder("configs"), IdentityCrypto())

        assertTrue(store.rename("zzz", "Nuevo").isFailure)
    }
}
```

`HomeViewModelRenameTest.kt` (mismo patrón que `HomeViewModelFavoritesTest`: `Dispatchers.setMain(UnconfinedTestDispatcher())` en `@Before` y `resetMain()` en `@After`):

```kotlin
    @Test
    fun `renombrar actualiza el nombre en el estado`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val result = vm.rename("a", "Oficina")

        assertEquals("Oficina", result.getOrThrow().name)
        assertEquals("Oficina", vm.uiState.value.configs.first { it.id == "a" }.name)
    }

    @Test
    fun `renombrar con un nombre repetido devuelve el error de duplicado`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager())

        assertTrue(vm.rename("a", "Trabajo").exceptionOrNull() is DuplicateNameException)
    }

    @Test
    fun `renombrar con un nombre vacio devuelve error`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager())

        assertTrue(vm.rename("a", " ").exceptionOrNull() is IllegalArgumentException)
    }
```

(con `casa = SavedConfig("a", "Casa")`, `trabajo = SavedConfig("b", "Trabajo")` y los imports de `DuplicateNameException`, `assertTrue`, etc.)

- [ ] **Step 3: Contrato e implementación (green)**

`ConfigStore.kt`:

```kotlin
    /**
     * Cambia el nombre visible. Falla con [DuplicateNameException] si otro perfil ya lo usa
     * (sin distinguir mayúsculas) y con [IllegalArgumentException] si queda vacío.
     */
    suspend fun rename(id: String, name: String): Result<SavedConfig>
```

`EncryptedFileConfigStore.kt` (antes del `companion object`):

```kotlin
    override suspend fun rename(id: String, name: String): Result<SavedConfig> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val clean = name.trim()
                val current = _configs.value.firstOrNull { it.id == id }
                    ?: return@withContext Result.failure<SavedConfig>(NoSuchElementException())
                if (clean.isEmpty()) {
                    return@withContext Result.failure<SavedConfig>(IllegalArgumentException("Nombre vacío"))
                }
                if (_configs.value.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) {
                    return@withContext Result.failure<SavedConfig>(DuplicateNameException())
                }
                runCatching {
                    val renamed = current.copy(name = clean)
                    _configs.value = _configs.value.map { if (it.id == id) renamed else it }
                    persistIndex()
                    renamed
                }.onFailure { AppLog.e("Almacen", "No se pudo renombrar la configuración", it) }
            }
        }
```

`HomeViewModel.kt`:

```kotlin
    suspend fun rename(id: String, name: String): Result<SavedConfig> = store.rename(id, name)
```

`Fakes.kt` (`FakeConfigStore`): añadir la misma lógica de validación sobre `_configs` y, para las pruebas de la tarea 5, un parámetro con textos de `.conf`:

```kotlin
class FakeConfigStore(
    initial: List<SavedConfig> = emptyList(),
    private val texts: Map<String, String> = emptyMap(),
) : ConfigStore {
    // …
    override suspend fun readText(id: String): String? =
        texts[id] ?: if (_configs.value.any { it.id == id }) "conf-de-$id" else null

    override suspend fun rename(id: String, name: String): Result<SavedConfig> {
        val clean = name.trim()
        val current = _configs.value.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException())
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Nombre vacío"))
        if (_configs.value.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) {
            return Result.failure(DuplicateNameException())
        }
        val renamed = current.copy(name = clean)
        _configs.value = _configs.value.map { if (it.id == id) renamed else it }
        return Result.success(renamed)
    }
}
```

(importar `com.usipipo.vpn.data.DuplicateNameException`).

- [ ] **Step 4: Verificación y commit**

```bash
./gradlew testDebugUnitTest
git add -A && git commit -m "feat(data): renombrar perfiles con validación de duplicados"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2
```

Esperado: 8 tests nuevos en verde (5 + 3).

---

### Task 5: `StatsViewModel`

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/details/StatsViewModel.kt`
- Create: `app/src/test/java/com/usipipo/vpn/ui/details/StatsViewModelTest.kt`

**Interfaces (usadas por las tareas 7 y 8):** `StatsUiState`, `StatsViewModel(store, tunnelManager).ui: StateFlow<StatsUiState>`, `StatsViewModel.HISTORY_SIZE`.

- [ ] **Step 1: Test que falla (red)**

`StatsViewModelTest.kt`:

```kotlin
package com.usipipo.vpn.ui.details

import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelCounters
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.ui.home.FakeConfigStore
import com.usipipo.vpn.ui.home.FakeTunnelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val casa = SavedConfig("a", "Casa")
    private val conf = "[Peer]\nEndpoint = vpn.example.com:51820\nAllowedIPs = 0.0.0.0/0, ::/0\n"

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(tunnel: FakeTunnelManager): StatsViewModel {
        val vm = StatsViewModel(FakeConfigStore(listOf(casa), mapOf("a" to conf)), tunnel)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        return vm
    }

    @Test
    fun `sin conexion no hay datos`() = runTest {
        val vm = viewModel(FakeTunnelManager())

        assertEquals(StatsUiState(), vm.ui.value)
    }

    @Test
    fun `al conectar muestra el endpoint y el IPv6 del conf`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = viewModel(tunnel)

        tunnel.emit(TunnelState.Connected("a", sinceMs = 1L))

        assertEquals("vpn.example.com:51820", vm.ui.value.endpoint)
        assertTrue(vm.ui.value.routesIpv6)
    }

    @Test
    fun `calcula la velocidad con los contadores`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = viewModel(tunnel)
        tunnel.emit(TunnelState.Connected("a", sinceMs = 1L))

        tunnel.counterSamples.tryEmit(TunnelCounters(0L, 0L, 0L))
        tunnel.counterSamples.tryEmit(TunnelCounters(1_000_000L, 250_000L, 1_000L))

        assertEquals(1_000_000L, vm.ui.value.downBps)
        assertEquals(250_000L, vm.ui.value.upBps)
        assertEquals(listOf(0L, 1_000_000L), vm.ui.value.downHistory)
    }

    @Test
    fun `el historial no pasa del tamano maximo`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = viewModel(tunnel)
        tunnel.emit(TunnelState.Connected("a", sinceMs = 1L))

        repeat(StatsViewModel.HISTORY_SIZE + 10) { i ->
            tunnel.counterSamples.tryEmit(TunnelCounters(i * 1_000L, 0L, i * 1_000L))
        }

        assertEquals(StatsViewModel.HISTORY_SIZE, vm.ui.value.downHistory.size)
    }

    @Test
    fun `al desconectar vuelve al estado vacio`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = viewModel(tunnel)
        tunnel.emit(TunnelState.Connected("a", sinceMs = 1L))
        tunnel.counterSamples.tryEmit(TunnelCounters(0L, 0L, 0L))

        tunnel.emit(TunnelState.Disconnected)

        assertEquals(StatsUiState(), vm.ui.value)
    }
}
```

Esperado: no compila (red).

- [ ] **Step 2: Implementación (green)**

```kotlin
package com.usipipo.vpn.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.data.ConfSummary
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.tunnel.SpeedCalculator
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estadísticas de la conexión activa. Las velocidades son bytes por segundo. */
data class StatsUiState(
    val downBps: Long = 0L,
    val upBps: Long = 0L,
    val downHistory: List<Long> = emptyList(),
    val upHistory: List<Long> = emptyList(),
    val endpoint: String? = null,
    val routesIpv6: Boolean = false,
)

/**
 * Convierte los contadores del túnel en velocidad. Solo lee mientras alguien recoge [ui]
 * (Inicio o Detalles visibles): `WhileSubscribed` apaga el sondeo cinco segundos después.
 */
class StatsViewModel(
    private val store: ConfigStore,
    private val tunnelManager: TunnelManager,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<StatsUiState> = tunnelManager.state
        .map { (it as? TunnelState.Connected)?.configId }
        .distinctUntilChanged()
        .flatMapLatest { configId ->
            if (configId == null) flowOf(StatsUiState()) else statsFor(configId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(),
        )

    private fun statsFor(configId: String): Flow<StatsUiState> = flow {
        // El .conf se lee solo para extraer endpoint e IPv6; el texto no se conserva.
        val summary = store.readText(configId)?.let { ConfSummary.parse(it) }
        var current = StatsUiState(
            endpoint = summary?.endpoint,
            routesIpv6 = summary?.routesIpv6 ?: false,
        )
        emit(current)

        val calculator = SpeedCalculator()
        tunnelManager.counters().collect { counters ->
            val throughput = calculator.add(counters)
            current = current.copy(
                downBps = throughput.downBps,
                upBps = throughput.upBps,
                downHistory = (current.downHistory + throughput.downBps).takeLast(HISTORY_SIZE),
                upHistory = (current.upHistory + throughput.upBps).takeLast(HISTORY_SIZE),
            )
            emit(current)
        }
    }

    companion object {
        /** Muestras de la gráfica: un minuto, a una por segundo. */
        const val HISTORY_SIZE = 60
    }
}
```

- [ ] **Step 3: Verificación y commit**

```bash
./gradlew testDebugUnitTest
git add -A && git commit -m "feat(ui): viewmodel de estadísticas de la conexión"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2
```

Esperado: 5 tests nuevos en verde.

---

### Task 6: Componentes — iconos, tarjeta de dato y gráfica

**Files:**
- Create: `app/src/main/res/drawable/ic_arrow_down.xml`, `ic_arrow_up.xml`, `ic_clock.xml`
- Create: `app/src/main/java/com/usipipo/vpn/ui/components/StatTile.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/details/ThroughputChart.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces (usadas por las tareas 7 y 8):** `StatTile(icon, label, value, modifier)`; `ThroughputChart(downHistory, upHistory, modifier)`.

- [ ] **Step 1: Iconos (24 dp, relleno, para tintar)**

```xml
<!-- ic_arrow_down.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M11,4h2v12.17l5.59,-5.58L20,12l-8,8 -8,-8 1.41,-1.41L11,16.17V4z" />
</vector>
```

```xml
<!-- ic_arrow_up.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M13,20h-2V7.83l-5.59,5.58L4,12l8,-8 8,8 -1.41,1.41L13,7.83V20z" />
</vector>
```

```xml
<!-- ic_clock.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M12,2C6.5,2 2,6.5 2,12s4.5,10 10,10 10,-4.5 10,-10S17.5,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67V7z" />
</vector>
```

- [ ] **Step 2: Strings**

```xml
    <!-- Estadísticas y detalles -->
    <string name="stat_download">Descarga</string>
    <string name="stat_upload">Subida</string>
    <string name="stat_protocol">Protocolo</string>
    <string name="stat_uptime">Tiempo activo</string>
    <string name="view_details">Ver detalles</string>
    <string name="details_title">Detalles</string>
    <string name="details_stats_title">Estadísticas de conexión</string>
    <string name="details_stats_note">Velocidad actual del túnel, no una prueba de velocidad.</string>
    <string name="details_connection_time">Tiempo de conexión</string>
    <string name="details_endpoint">Endpoint</string>
    <string name="details_ipv6">IPv6</string>
    <string name="ipv6_enabled">Habilitado</string>
    <string name="ipv6_disabled">Deshabilitado</string>
    <string name="endpoint_unknown">No disponible</string>
    <string name="rename_title">Renombrar perfil</string>
    <string name="cd_rename">Renombrar %1$s</string>
    <string name="cd_throughput_chart">Gráfica de la velocidad del último minuto</string>
```

- [ ] **Step 3: `StatTile.kt`**

```kotlin
package com.usipipo.vpn.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Dato corto con icono, como las tarjetas de descarga, subida, protocolo y tiempo del mockup. */
@Composable
fun StatTile(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    UsipipoCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = icon, modifier = Modifier.size(36.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
```

(`IconTile` ya recibe `modifier`; como el de quien llama va primero, su `size(36.dp)` gana sobre el interno de 44 dp.) Añadir `@Preview` claro y oscuro con `StatTile(R.drawable.ic_arrow_down, "Descarga", "60.00 Mbps")`.

- [ ] **Step 4: `ThroughputChart.kt`**

```kotlin
package com.usipipo.vpn.ui.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R

/**
 * Gráfica de líneas de descarga y subida del último minuto, dibujada con Canvas.
 * Las muestras nuevas entran por la derecha; las dos series comparten escala.
 */
@Composable
fun ThroughputChart(
    downHistory: List<Long>,
    upHistory: List<Long>,
    modifier: Modifier = Modifier,
) {
    val downColor = MaterialTheme.colorScheme.secondary
    val upColor = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outline
    val description = stringResource(R.string.cd_throughput_chart)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = description },
    ) {
        val peak = maxOf(downHistory.maxOrNull() ?: 0L, upHistory.maxOrNull() ?: 0L, 1L).toFloat()
        drawLine(
            color = baseline,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        drawSeries(downHistory, peak, downColor)
        drawSeries(upHistory, peak, upColor)
    }
}

private fun DrawScope.drawSeries(values: List<Long>, peak: Float, color: Color) {
    if (values.size < 2) return
    val step = size.width / (StatsViewModel.HISTORY_SIZE - 1)
    val padding = 4.dp.toPx()
    val usableHeight = size.height - padding * 2
    val path = Path()
    values.forEachIndexed { index, value ->
        val x = size.width - (values.size - 1 - index) * step
        val y = size.height - padding - (value / peak) * usableHeight
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
}
```

Previews (claro y oscuro): una con 60 muestras de una onda (`List(60) { (it % 20) * 500_000L }`) y otra vacía.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): tarjeta de dato, gráfica de velocidad e iconos de estadísticas"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2
```

Esperado: `success`.

---

### Task 7: Inicio con estadísticas

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/details/UptimeText.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/home/ConnectionStats.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt`

**Interfaces:** `HomeScreen(vm, stats, onImport, onOpenConfigs, onOpenDetails)`.

- [ ] **Step 1: Texto del tiempo activo que avanza solo**

`UptimeText.kt`:

```kotlin
package com.usipipo.vpn.ui.details

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Tiempo conectado, recalculado cada segundo mientras el composable está en pantalla. */
@Composable
fun rememberUptimeText(sinceMs: Long): String {
    var nowMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(sinceMs) {
        while (true) {
            nowMs = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    return uptimeText(sinceMs = sinceMs, nowMs = nowMs)
}
```

- [ ] **Step 2: `ConnectionStats.kt`**

```kotlin
package com.usipipo.vpn.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.StatTile
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.details.StatsUiState
import com.usipipo.vpn.ui.details.formatRate
import com.usipipo.vpn.ui.details.rememberUptimeText

/** Tarjetas de Inicio con el túnel conectado: protocolo, tiempo, velocidades y acceso a Detalles. */
@Composable
internal fun ConnectionStats(
    sinceMs: Long,
    stats: StatsUiState,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = R.drawable.ic_shield_check,
                label = stringResource(R.string.stat_protocol),
                value = stringResource(R.string.protocol_wireguard),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = R.drawable.ic_clock,
                label = stringResource(R.string.stat_uptime),
                value = rememberUptimeText(sinceMs),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = R.drawable.ic_arrow_down,
                label = stringResource(R.string.stat_download),
                value = formatRate(stats.downBps),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = R.drawable.ic_arrow_up,
                label = stringResource(R.string.stat_upload),
                value = formatRate(stats.upBps),
                modifier = Modifier.weight(1f),
            )
        }
        UsipipoCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenDetails) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.view_details),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
```

Previews claro y oscuro con `StatsUiState(downBps = 7_500_000L, upBps = 5_000_000L)` y `sinceMs = 0L`.

- [ ] **Step 3: Integrar en `HomeScreen`**

Nueva firma (los datos primero, los callbacks después):

```kotlin
fun HomeScreen(
    vm: HomeViewModel,
    stats: StatsUiState,
    onImport: () -> Unit,
    onOpenConfigs: () -> Unit,
    onOpenDetails: () -> Unit,
)
```

Justo después de `ProfileCard(config = selected, onClick = onOpenConfigs)`:

```kotlin
                    (state.tunnel as? TunnelState.Connected)?.let { connected ->
                        ConnectionStats(
                            sinceMs = connected.sinceMs,
                            stats = stats,
                            onOpenDetails = onOpenDetails,
                        )
                    }
```

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(ui): estadísticas de la conexión en inicio"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2
```

Esperado: compila una vez hecha la tarea 8 (el `UsipipoRoot` aún llama a `HomeScreen` con la firma vieja). **Hacer esta tarea y la 8 en la misma rama sin esperar CI entre ellas**; el commit de esta tarea puede incluir el cambio mínimo de la llamada en `UsipipoRoot` (`stats = StatsUiState()`, `onOpenDetails = {}`) para no dejar la rama sin compilar.

---

### Task 8: Pantalla Detalles y navegación

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/details/DetailsScreen.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt`

**Interfaces:** `DetailsScreen(config, tunnel, stats, onClose, onDisconnect, onRename)`.

- [ ] **Step 1: `DetailsScreen.kt`**

Estructura (un `Scaffold` con `contentWindowInsets = WindowInsets.statusBars`, fondo `colorScheme.background`, columna con `verticalScroll`):

```kotlin
@Composable
fun DetailsScreen(
    config: SavedConfig?,
    tunnel: TunnelState,
    stats: StatsUiState,
    onClose: () -> Unit,
    onDisconnect: () -> Unit,
    onRename: suspend (String) -> Result<SavedConfig>,
) {
    val connected = tunnel as? TunnelState.Connected
    val unavailable = connected == null || config == null
    // Si el túnel cae mientras se ve esta pantalla (otro VPN, permiso revocado), se cierra sola.
    LaunchedEffect(unavailable) { if (unavailable) onClose() }
    if (connected == null || config == null) return

    var renaming by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1) Fila superior: IconButton con Icons.AutoMirrored.Filled.ArrowBack
            //    (contentDescription = R.string.cd_back, onClick = onClose) y el título
            //    R.string.details_title (titleLarge, SemiBold, onBackground).
            // 2) UsipipoCard con el perfil: chip «Activo» (R.string.config_active, mismo
            //    estilo que ConfigCard), nombre (titleMedium), protocolo (bodyMedium) y un
            //    IconButton con Icons.Default.Edit (cd_rename con config.name) que pone
            //    renaming = true.
            // 3) UsipipoCard «Estadísticas de conexión»: título (R.string.details_stats_title),
            //    ThroughputChart(stats.downHistory, stats.upHistory), dos StatTile
            //    (descarga y subida con formatRate) y la nota R.string.details_stats_note
            //    (bodySmall, onSurfaceVariant).
            // 4) UsipipoCard con cuatro filas etiqueta/valor (InfoRow): tiempo de conexión
            //    (rememberUptimeText(connected.sinceMs)), protocolo (R.string.protocol_wireguard),
            //    endpoint (stats.endpoint ?: R.string.endpoint_unknown) e IPv6
            //    (R.string.ipv6_enabled / R.string.ipv6_disabled según stats.routesIpv6).
            // 5) Button «Desconectar» (R.string.disconnect), heightIn(min = 56.dp),
            //    containerColor = colorScheme.error, contentColor = colorScheme.onError,
            //    onClick = onDisconnect.
        }
    }

    if (renaming) {
        RenameDialog(
            currentName = config.name,
            onRename = { onRename(it) },
            onDismiss = { renaming = false },
        )
    }
}
```

`InfoRow` privado: `Row` con la etiqueta (`bodyMedium`, `onSurfaceVariant`) a la izquierda y el valor (`bodyMedium`, `onSurface`, `maxLines = 1`, `overflow = Ellipsis`, `textAlign = End`) ocupando el resto; `Modifier.weight(1f)` en el valor para que un endpoint largo no empuje la etiqueta.

`RenameDialog` (privado, mismo archivo):

```kotlin
@Composable
private fun RenameDialog(
    currentName: String,
    onRename: suspend (String) -> Result<SavedConfig>,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val currentError = error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text(stringResource(R.string.name_label)) },
                singleLine = true,
                isError = currentError != null,
                supportingText = if (currentError != null) {
                    { Text(stringResource(currentError)) }
                } else {
                    null
                },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    onRename(name).fold(
                        onSuccess = { onDismiss() },
                        onFailure = { error = renameErrorRes(it) },
                    )
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@StringRes
private fun renameErrorRes(error: Throwable): Int = when (error) {
    is DuplicateNameException -> R.string.err_name_duplicate
    is IllegalArgumentException -> R.string.err_name_empty
    else -> R.string.err_save_failed
}
```

Previews de `DetailsScreen` (claro y oscuro) con `TunnelState.Connected("a", sinceMs = 1L)`, un `SavedConfig("a", "client-mowgli")` y `StatsUiState` con historiales de 60 muestras, endpoint `frankfurt.example.com:51820` e `routesIpv6 = false`.

- [ ] **Step 2: Estado y atrás en `UsipipoRoot`**

Junto a `tab`:

```kotlin
    var showDetails by rememberSaveable { mutableStateOf(false) }
```

Los `BackHandler` (el segundo, registrado después, gana: Atrás cierra primero Detalles):

```kotlin
    BackHandler(enabled = backTarget(tab) != null) {
        backTarget(tab)?.let { tab = it }
    }
    BackHandler(enabled = showDetails) { showDetails = false }
```

La barra inferior también cierra Detalles:

```kotlin
                UsipipoBottomBar(
                    tab = tab,
                    onTabClick = {
                        tab = it
                        showDetails = false
                    },
                )
```

- [ ] **Step 3: La rama `Home` recoge las estadísticas (solo aquí)**

```kotlin
                    MainTab.Home -> {
                        // Se recoge aquí y no en la raíz: así el sondeo del túnel solo corre
                        // mientras Inicio o Detalles están en pantalla.
                        val homeVm = homeViewModel(container)
                        val stats by statsViewModel(container).ui.collectAsStateWithLifecycle()
                        if (showDetails) {
                            val homeState by homeVm.uiState.collectAsStateWithLifecycle()
                            val activeId = (homeState.tunnel as? TunnelState.Connected)?.configId
                            DetailsScreen(
                                config = homeState.configs.firstOrNull { it.id == activeId },
                                tunnel = homeState.tunnel,
                                stats = stats,
                                onClose = { showDetails = false },
                                onDisconnect = { homeVm.disconnect() },
                                onRename = { name -> homeVm.rename(activeId.orEmpty(), name) },
                            )
                        } else {
                            HomeScreen(
                                vm = homeVm,
                                stats = stats,
                                onImport = { tab = MainTab.Import },
                                onOpenConfigs = { tab = MainTab.Configs },
                                onOpenDetails = { showDetails = true },
                            )
                        }
                    }
```

Y la factoría, junto a las demás:

```kotlin
@Composable
private fun statsViewModel(container: AppContainer): StatsViewModel = viewModel(
    factory = viewModelFactory {
        initializer { StatsViewModel(container.configStore, container.tunnelManager) }
    },
)
```

Imports nuevos: `androidx.compose.runtime.getValue` (ya está), `com.usipipo.vpn.domain.TunnelState`, `com.usipipo.vpn.ui.details.DetailsScreen`, `com.usipipo.vpn.ui.details.StatsViewModel`.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(ui): pantalla de detalles con gráfica, desconectar y renombrar"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase2
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase2 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

Esperado: `success`, todos los tests (79 de la fase 1 + 34 nuevos) en verde.

---

### Task 9: Cierre de la fase 2

**Files:**
- Modify: `README.md`, `AGENTS.md`

- [ ] **Step 1: Documentación**

`README.md`: añadir a «Estructura» `ui/details/` (Detalles, estadísticas, gráfica y formato) y `data/ConfSummary.kt`; mencionar `TunnelManager.counters()`. `AGENTS.md`: añadir `ui/details/` a la estructura (si no se hizo en la tarea 1) y anotar que la fase 2 autorizó el cambio en `tunnel/`. Commit: `docs: actualizar README y AGENTS.md con la fase 2`.

- [ ] **Step 2: Checklist manual en el teléfono (APK debug del run verde)**

1. Inicio desconectado: sin tarjetas de estadísticas. Conectar: aparecen protocolo, tiempo activo, descarga y subida, y «Ver detalles».
2. Mover tráfico (navegar o descargar algo): descarga y subida suben y vuelven a bajar; con el teléfono quieto, caen a casi 0.
3. El tiempo activo avanza cada segundo y no se reinicia al cambiar de pestaña y volver.
4. Ver detalles: gráfica con las dos líneas, velocidades, tiempo, protocolo, endpoint e IPv6 (con un `.conf` con y sin `::/0`).
5. Atrás en Detalles vuelve a Inicio (no sale de la app). Tocar otra pestaña con Detalles abierto: cambia de pestaña y Detalles queda cerrado al volver a Inicio.
6. «Desconectar» en Detalles: vuelve a Inicio, desconectado.
7. Con Detalles abierto, apagar el túnel desde fuera (otro VPN o Ajustes del sistema): Detalles se cierra solo.
8. Renombrar con el lápiz: nombre vacío (error), nombre de otro perfil (error), cambiar solo las mayúsculas del propio (válido), nombre nuevo (se ve en Inicio, Configuraciones y Detalles sin reconectar). Rotar con el diálogo abierto: conserva el texto.
9. Rotar en Detalles: sigue en Detalles.
10. Batería: con el túnel conectado, ir a Configuraciones o Diagnóstico y comprobar (en el registro, modo detallado, o con `adb shell dumpsys batterystats`) que no se sigue consultando el túnel pasados ~5 s. Volver a Inicio: se reanuda.
11. Reconectar el túnel: el tiempo vuelve a 00:00:00 y la gráfica empieza de nuevo; la velocidad no es negativa.
12. Tema claro y oscuro en Inicio conectado y en Detalles; gama baja sin tirones al actualizar la gráfica.
13. Registro de diagnóstico: no aparecen cifras de tráfico, claves ni el endpoint.

- [ ] **Step 3: Pull request**

```bash
git push && gh pr create --base main --head feat/mockup-fase2 \
  --title "feat: fase 2 del mockup (datos reales y detalles)" \
  --body "Ver docs/plans/2026-10-18-usipipo-vpn-mockup-fase2-plan.md. Incluye correcciones de la revisión de la fase 1 y un cambio autorizado en tunnel/ (solo lectura de contadores)."
```

Tras revisión y la checklist, merge. La fase 3 (país y ping) parte de `main` y se planifica contra el código ya mergeado.
