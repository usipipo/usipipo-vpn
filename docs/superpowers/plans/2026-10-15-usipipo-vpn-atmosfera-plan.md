# uSipipo VPN «Atmósfera» — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dar personalidad visual a la app Android (Kotlin + Jetpack Compose) con la dirección «Atmósfera»: fondo degradado según el estado del túnel, botón con halo y anillos, tipografía propia, tarjetas tonales, estado vacío ilustrado y transiciones entre pantallas.

**Architecture:** Todo el trabajo vive en la capa `ui/` más recursos (`res/font`, `res/drawable`, `res/values/strings.xml`). Se añade un módulo de tema puro (paleta y fases de ambiente, testeable en JVM) y un canal de «reducir animaciones». No se toca ViewModel, túnel, almacenamiento ni diagnósticos (lógica). Las animaciones usan solo escala, transparencia, offset y color; la única animación continua es el halo del botón.

**Tech Stack:** Kotlin 2.3.21, Jetpack Compose (BOM 2026.08.00), Material 3, AGP 9.3.3, minSdk 26. Única dependencia nueva: `androidx.compose.ui:ui-tooling-preview` (anotaciones `@Preview`, gestionada por el BOM).

**Spec:** `docs/plans/2026-10-15-usipipo-vpn-atmosfera-design.md`

## Global Constraints

- **No hay JDK ni Android SDK en la máquina local** (confirmado). El compilador y el runner de tests efectivos son **GitHub Actions**. Cada tarea termina con un ciclo CI: `gh workflow run "Android CI - Build APK" --ref feat/atmosfera` y `gh run watch <id> --exit-status`. El workflow corre `testDebugUnitTest` y `assembleDebug`.
- Rama de trabajo: `feat/atmosfera`, creada desde `main` antes de la tarea 1.
- **Fuera de alcance (no tocar):** `ui/home/HomeViewModel.kt`, `ui/diagnostics/DiagnosticsViewModel.kt`, `ui/importer/ImportViewModel.kt`, `tunnel/**`, `data/**`, `diagnostics/**` (lógica), `update/**`, `AppContainer.kt`, `MainActivity.kt`, `UsipipoApp.kt`.
- Pantalla principal **sin** estadísticas, tiempo conectado ni extras (regla ya aprobada).
- Los colores de estado **siempre** van acompañados de texto; nunca solo color.
- Animaciones ligeras: máximo **una** animación continua simultánea (el halo del botón). Nada de blur en tiempo real ni sombras animadas. Solo escala, transparencia, offset y color.
- Con «reducir animaciones» del sistema activo: halo fijo, sin anillos ni temblor, cambio de icono instantáneo, fondo sin transición y cambio de pantalla directo.
- El degradado de fondo se usa **solo en Home**; las demás pantallas llevan fondo plano (`colorScheme.background`).
- Todo en español: strings, comentarios y mensajes de commit (estilo del repo: `feat(ui): …` en minúscula). Nombres de API en inglés.
- Los tests unitarios existentes deben seguir en verde sin modificarlos.
- La fuente Manrope (OFL) se empaqueta en la APK; unos cientos de KB extra son aceptables (riesgo aceptado en la spec).

## Review Focus

Clases de entrada / modos de fallo que la spec implica y que ningún test unitario cubre; cada línea indica dónde queda fijada:

1. `TunnelState.Failed(configId = null)` (fallo antes de asociar config, p. ej. permiso denegado): debe pintar el mismo ambiente «alerta» que `Failed(id)`. Fijado en `AtmosphereTest` (tarea 1).
2. `NoResponse`: mismo ambiente alerta que `Failed` pero **sin** temblor continuo, y el texto de ayuda (`hint_no_response`) debe seguir legible sobre el tinte rosa. Fijado en el test de contraste (tarea 1) y en la preview `SinRespuesta` (tarea 2).
3. «Reducir animaciones» activo con estado `Connecting`: nada se mueve (halo fijo, sin anillos), y con `Connected` el icono cambia al instante. Fijado en la preview `ConectandoReducido` (tarea 2) y en el paso de fondo sin transición (tarea 3) y transiciones directas (tarea 6).
4. Tema oscuro con túnel conectado: el texto `onBackground` (#E6F0FF) sobre el extremo del degradado (#114C40) debe mantener contraste ≥ 4.5:1. Fijado en `AtmosphereTest` (tarea 1).
5. Rotación de pantalla en `Connecting`/`Failed`: las animaciones no deben reiniciarse de forma extraña ni el temblor repetirse por recomposición. Fijado con `rememberSaveable` en el paso 4 de la tarea 2 y en la checklist manual final (tarea 9).

---

### Task 1: Fundaciones — paleta de ambiente, fases, tipografía Manrope y canal de movimiento reducido

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/theme/Atmosphere.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/theme/Motion.kt`
- Create: `app/src/test/java/com/usipipo/vpn/ui/theme/AtmosphereTest.kt`
- Create: `app/src/main/res/font/manrope.ttf` (descargado, no escrito a mano)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/theme/Theme.kt`
- Modify: `gradle/libs.versions.toml` (añadir `ui-tooling-preview`)
- Modify: `app/build.gradle.kts` (añadir `implementation(libs.androidx.compose.ui.tooling.preview)`)

**Interfaces:**
- Consumes: `com.usipipo.vpn.domain.TunnelState` (existente); `UsipipoTheme` (existente).
- Produces (usados por las tareas 2, 3 y 6):
  - `object AtmospherePalette`: `const val : Long` — `LIGHT_CALM_TOP/BOTTOM`, `LIGHT_DRIFTING_TOP/BOTTOM`, `LIGHT_FLOWING_TOP/BOTTOM`, `LIGHT_ALERT_TOP/BOTTOM`, `DARK_*` (ídem), `TEXT_LIGHT`, `TEXT_DARK`, `TEXT_LIGHT_SECONDARY`, `TEXT_DARK_SECONDARY`, `BUTTON_LIGHT_IDLE/CONNECTED/ERROR`, `BUTTON_DARK_IDLE/CONNECTED/ERROR`.
  - `enum class AtmospherePhase { Calm, Drifting, Flowing, Alert }`
  - `fun atmospherePhase(state: TunnelState): AtmospherePhase`
  - En `Theme.kt`: `@Immutable data class Atmosphere(val top: Color, val bottom: Color)`; `@Immutable data class AtmosphereColors(val calm: Atmosphere, val drifting: Atmosphere, val flowing: Atmosphere, val alert: Atmosphere)` con `fun forPhase(phase: AtmospherePhase): Atmosphere`; `val LocalAtmosphere: ProvidableCompositionLocal<AtmosphereColors>`.
  - En `Motion.kt`: `val LocalReduceMotion: ProvidableCompositionLocal<Boolean?>` (`null` = leer del sistema); `@Composable fun rememberReduceMotion(): Boolean`.
  - `UsipipoTheme` mantiene firma `(darkTheme: Boolean = isSystemInDarkTheme(), content)` y ahora aplica `UsipipoTypography` (Manrope) y `LocalAtmosphere`; `StatusColors.connected` pasa a turquesa.

- [ ] **Step 0: Crear la rama y confirmar CI base en verde**

```bash
git switch main && git pull && git switch -c feat/atmosfera && git push -u origin feat/atmosfera
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: run `success` (base verde antes de tocar nada).

- [ ] **Step 1: Descargar la fuente variable Manrope a `res/font`**

```bash
mkdir -p app/src/main/res/font
curl -fL --retry 3 -o app/src/main/res/font/manrope.ttf \
  "https://raw.githubusercontent.com/google/fonts/main/ofl/manrope/Manrope%5Bwght%5D.ttf"
stat -c%s app/src/main/res/font/manrope.ttf && od -An -t x1 -N 4 app/src/main/res/font/manrope.ttf
```

Esperado: tamaño entre ~100 000 y ~400 000 bytes y primeros bytes `00 01 00 00` (TrueType). Si la red corporativa bloquea GitHub, usar alternativa `https://fonts.google.com/download?family=Manrope` (zip: extraer el `.ttf` variable y renombrarlo `manrope.ttf`). Manrope es OFL: se puede empaquetar.

- [ ] **Step 2: Escribir los tests que fallan (`AtmosphereTest.kt`)**

```kotlin
package com.usipipo.vpn.ui.theme

import com.usipipo.vpn.domain.TunnelState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class AtmosphereTest {

    @Test
    fun `cada estado del tunel tiene su fase de ambiente`() {
        assertEquals(AtmospherePhase.Calm, atmospherePhase(TunnelState.Disconnected))
        assertEquals(AtmospherePhase.Drifting, atmospherePhase(TunnelState.Connecting("a")))
        assertEquals(AtmospherePhase.Flowing, atmospherePhase(TunnelState.Connected("a")))
        assertEquals(AtmospherePhase.Alert, atmospherePhase(TunnelState.NoResponse("a")))
        assertEquals(AtmospherePhase.Alert, atmospherePhase(TunnelState.Failed("a")))
        assertEquals(AtmospherePhase.Alert, atmospherePhase(TunnelState.Failed(null)))
    }

    @Test
    fun `el texto mantiene contraste 4 punto 5 sobre cada degradado`() {
        val lightGradients = listOf(
            AtmospherePalette.LIGHT_CALM_TOP to AtmospherePalette.LIGHT_CALM_BOTTOM,
            AtmospherePalette.LIGHT_DRIFTING_TOP to AtmospherePalette.LIGHT_DRIFTING_BOTTOM,
            AtmospherePalette.LIGHT_FLOWING_TOP to AtmospherePalette.LIGHT_FLOWING_BOTTOM,
            AtmospherePalette.LIGHT_ALERT_TOP to AtmospherePalette.LIGHT_ALERT_BOTTOM,
        )
        val darkGradients = listOf(
            AtmospherePalette.DARK_CALM_TOP to AtmospherePalette.DARK_CALM_BOTTOM,
            AtmospherePalette.DARK_DRIFTING_TOP to AtmospherePalette.DARK_DRIFTING_BOTTOM,
            AtmospherePalette.DARK_FLOWING_TOP to AtmospherePalette.DARK_FLOWING_BOTTOM,
            AtmospherePalette.DARK_ALERT_TOP to AtmospherePalette.DARK_ALERT_BOTTOM,
        )
        lightGradients.forEach { (top, bottom) ->
            assertTrue("texto sobre $top", contrast(AtmospherePalette.TEXT_LIGHT, top) >= 4.5)
            assertTrue("texto sobre $bottom", contrast(AtmospherePalette.TEXT_LIGHT, bottom) >= 4.5)
        }
        darkGradients.forEach { (top, bottom) ->
            assertTrue("texto sobre $top", contrast(AtmospherePalette.TEXT_DARK, top) >= 4.5)
            assertTrue("texto sobre $bottom", contrast(AtmospherePalette.TEXT_DARK, bottom) >= 4.5)
        }
    }

    @Test
    fun `el texto secundario mantiene contraste 4 punto 5 donde aparece`() {
        // El texto secundario solo se usa sobre Calm (estado vacío) y Alert (aviso sin
        // respuesta); no se exige sobre Flowing, donde no llega a mostrarse.
        val lightSurfaces = listOf(
            AtmospherePalette.LIGHT_CALM_TOP, AtmospherePalette.LIGHT_CALM_BOTTOM,
            AtmospherePalette.LIGHT_ALERT_TOP, AtmospherePalette.LIGHT_ALERT_BOTTOM,
        )
        val darkSurfaces = listOf(
            AtmospherePalette.DARK_CALM_TOP, AtmospherePalette.DARK_CALM_BOTTOM,
            AtmospherePalette.DARK_ALERT_TOP, AtmospherePalette.DARK_ALERT_BOTTOM,
        )
        lightSurfaces.forEach {
            assertTrue("secundario sobre $it", contrast(AtmospherePalette.TEXT_LIGHT_SECONDARY, it) >= 4.5)
        }
        darkSurfaces.forEach {
            assertTrue("secundario sobre $it", contrast(AtmospherePalette.TEXT_DARK_SECONDARY, it) >= 4.5)
        }
    }

    @Test
    fun `el icono del boton mantiene contraste sobre cada color de estado`() {
        val buttons = listOf(
            AtmospherePalette.BUTTON_LIGHT_IDLE, AtmospherePalette.BUTTON_LIGHT_CONNECTED,
            AtmospherePalette.BUTTON_LIGHT_ERROR, AtmospherePalette.BUTTON_DARK_IDLE,
            AtmospherePalette.BUTTON_DARK_CONNECTED, AtmospherePalette.BUTTON_DARK_ERROR,
        )
        buttons.forEach { color ->
            assertTrue("icono sobre $color", contrast(AtmospherePalette.TEXT_LIGHT, color) >= 4.5)
        }
    }

    // Contraste WCAG 2.1 a partir de constantes ARGB (sin tipos de Compose: testeable en JVM).
    private fun contrast(a: Long, b: Long): Double {
        val l1 = max(luminance(a), luminance(b))
        val l2 = min(luminance(a), luminance(b))
        return (l1 + 0.05) / (l2 + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(v: Long): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }
}
```

- [ ] **Step 3: Implementar `Atmosphere.kt` (puro Kotlin, sin imports de Compose)**

Valores fijados por la spec (los tonos de turquesa/menta se fijan aquí y quedan validados por el test de contraste):

```kotlin
package com.usipipo.vpn.ui.theme

import com.usipipo.vpn.domain.TunnelState

/**
 * Paleta de «Atmósfera» en ARGB (sin tipos de Compose, para poder testearla en JVM).
 * El texto del diseño exige: claro = degradado azul pastel → turquesa/menta, oscuro = azul
 * noche → verde azulado profundo (no una inversión de la paleta clara).
 */
object AtmospherePalette {
    // Fondos (claro)
    const val LIGHT_CALM_TOP: Long = 0xFFF3F8FF
    const val LIGHT_CALM_BOTTOM: Long = 0xFFD6E6FB
    const val LIGHT_DRIFTING_TOP: Long = 0xFFEDF7FD
    const val LIGHT_DRIFTING_BOTTOM: Long = 0xFFC6E9E0
    const val LIGHT_FLOWING_TOP: Long = 0xFFE3F1FE
    const val LIGHT_FLOWING_BOTTOM: Long = 0xFFA9E5D8
    const val LIGHT_ALERT_TOP: Long = 0xFFFDF6F7
    const val LIGHT_ALERT_BOTTOM: Long = 0xFFF7DCE2
    // Fondos (oscuro)
    const val DARK_CALM_TOP: Long = 0xFF0E1A2B
    const val DARK_CALM_BOTTOM: Long = 0xFF17293D
    const val DARK_DRIFTING_TOP: Long = 0xFF0D1F2E
    const val DARK_DRIFTING_BOTTOM: Long = 0xFF15403C
    const val DARK_FLOWING_TOP: Long = 0xFF0E2436
    const val DARK_FLOWING_BOTTOM: Long = 0xFF114C40
    const val DARK_ALERT_TOP: Long = 0xFF231620
    const val DARK_ALERT_BOTTOM: Long = 0xFF32202B
    // Texto e iconos
    const val TEXT_LIGHT: Long = 0xFF0B2545 // Navy
    const val TEXT_DARK: Long = 0xFFE6F0FF
    const val TEXT_LIGHT_SECONDARY: Long = 0xFF4A6283 // onSurfaceVariant claro
    const val TEXT_DARK_SECONDARY: Long = 0xFFB7C9E2 // onSurfaceVariant oscuro
    // Botón según estado
    const val BUTTON_LIGHT_IDLE: Long = 0xFFA8CCFF
    const val BUTTON_LIGHT_CONNECTED: Long = 0xFF6FCFC0 // turquesa pastel
    const val BUTTON_LIGHT_ERROR: Long = 0xFFF4A3A3
    const val BUTTON_DARK_IDLE: Long = 0xFF8DBBFF
    const val BUTTON_DARK_CONNECTED: Long = 0xFF4FB3A6 // turquesa profundo
    const val BUTTON_DARK_ERROR: Long = 0xFFF4A3A3
}

/** Fase visual de la pantalla; los estados de error comparten «Alert». */
enum class AtmospherePhase { Calm, Drifting, Flowing, Alert }

fun atmospherePhase(state: TunnelState): AtmospherePhase = when (state) {
    TunnelState.Disconnected -> AtmospherePhase.Calm
    is TunnelState.Connecting -> AtmospherePhase.Drifting
    is TunnelState.Connected -> AtmospherePhase.Flowing
    is TunnelState.NoResponse -> AtmospherePhase.Alert
    is TunnelState.Failed -> AtmospherePhase.Alert
}
```

- [ ] **Step 4: Implementar `Motion.kt` (canal de movimiento reducido)**

```kotlin
package com.usipipo.vpn.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * `null` = leer el ajuste del sistema; un valor fijo permite previsualizar ambos modos
 * en @Preview y probar el camino reducido sin cambiar ajustes del teléfono.
 */
val LocalReduceMotion = staticCompositionLocalOf<Boolean?> { null }

@Composable
fun rememberReduceMotion(): Boolean {
    LocalReduceMotion.current?.let { return it }
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
```

(La lógica del ajuste es la del helper privado actual de `ConnectButton.kt`; se mueve aquí y allí se borra.)

- [ ] **Step 5: Extender `Theme.kt`**

Cambios exactos:

1. Sustituir los tres valores `Color(0xFFA8CCFF)`, etc. de `StatusColors` por referencias a `AtmospherePalette` (idle/error iguales; `connected` pasa a turquesa): `LightStatus.connected = Color(AtmospherePalette.BUTTON_LIGHT_CONNECTED)`, `DarkStatus.connected = Color(AtmospherePalette.BUTTON_DARK_CONNECTED)`.
2. Añadir tipografía con la fuente variable:

```kotlin
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.usipipo.vpn.R

private val Manrope = FontFamily(
    Font(R.font.manrope, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.manrope, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.manrope, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private val UsipipoTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
        headlineSmall = headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = bodyMedium.copy(fontFamily = Manrope),
        labelLarge = labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    )
}
```

3. Añadir colores de ambiente y su CompositionLocal:

```kotlin
@Immutable
data class Atmosphere(val top: Color, val bottom: Color)

@Immutable
data class AtmosphereColors(
    val calm: Atmosphere,
    val drifting: Atmosphere,
    val flowing: Atmosphere,
    val alert: Atmosphere,
) {
    fun forPhase(phase: AtmospherePhase): Atmosphere = when (phase) {
        AtmospherePhase.Calm -> calm
        AtmospherePhase.Drifting -> drifting
        AtmospherePhase.Flowing -> flowing
        AtmospherePhase.Alert -> alert
    }
}

private val LightAtmosphere = AtmosphereColors(
    calm = Atmosphere(Color(AtmospherePalette.LIGHT_CALM_TOP), Color(AtmospherePalette.LIGHT_CALM_BOTTOM)),
    drifting = Atmosphere(Color(AtmospherePalette.LIGHT_DRIFTING_TOP), Color(AtmospherePalette.LIGHT_DRIFTING_BOTTOM)),
    flowing = Atmosphere(Color(AtmospherePalette.LIGHT_FLOWING_TOP), Color(AtmospherePalette.LIGHT_FLOWING_BOTTOM)),
    alert = Atmosphere(Color(AtmospherePalette.LIGHT_ALERT_TOP), Color(AtmospherePalette.LIGHT_ALERT_BOTTOM)),
)

private val DarkAtmosphere = AtmosphereColors(
    calm = Atmosphere(Color(AtmospherePalette.DARK_CALM_TOP), Color(AtmospherePalette.DARK_CALM_BOTTOM)),
    drifting = Atmosphere(Color(AtmospherePalette.DARK_DRIFTING_TOP), Color(AtmospherePalette.DARK_DRIFTING_BOTTOM)),
    flowing = Atmosphere(Color(AtmospherePalette.DARK_FLOWING_TOP), Color(AtmospherePalette.DARK_FLOWING_BOTTOM)),
    alert = Atmosphere(Color(AtmospherePalette.DARK_ALERT_TOP), Color(AtmospherePalette.DARK_ALERT_BOTTOM)),
)

val LocalAtmosphere = staticCompositionLocalOf { LightAtmosphere }
```

4. En `UsipipoTheme`, el `CompositionLocalProvider` provee además `LocalAtmosphere` (claro/oscuro) y `MaterialTheme` usa `typography = UsipipoTypography`.

- [ ] **Step 6: Añadir `ui-tooling-preview`**

En `gradle/libs.versions.toml`, junto a las otras entradas de Compose (sin versión: la fija el BOM):

```toml
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
```

En `app/build.gradle.kts`, sección de dependencias, junto a `androidx-compose-ui-tooling`:

```kotlin
implementation(libs.androidx.compose.ui.tooling.preview)
```

(Necesario en `implementation`, no `debugImplementation`, para que `@Preview` compile también en release.)

- [ ] **Step 7: Verificar en CI**

```bash
git add app/src/main/res/font/manrope.ttf app/src/main/java/com/usipipo/vpn/ui/theme/Atmosphere.kt \
  app/src/main/java/com/usipipo/vpn/ui/theme/Motion.kt app/src/main/java/com/usipipo/vpn/ui/theme/Theme.kt \
  app/src/test/java/com/usipipo/vpn/ui/theme/AtmosphereTest.kt gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat(ui): ambiente por estado, tipografia Manrope y canal de movimiento reducido"
git push
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: `success`; `testDebugUnitTest` incluye `AtmosphereTest` con 5 tests en verde y los tests existentes siguen pasando. Si CI falla: corregir, commit nuevo (nunca amend sobre lo ya empujado), push y repetir.

---

### Task 2: Botón de conectar — halo, anillos, icono de escudo, temblor y háptica

**Files:**
- Create: `app/src/main/res/drawable/ic_shield_check.xml`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/components/ConnectButton.kt`
- Test: `app/src/main/java/com/usipipo/vpn/ui/components/ConnectButton.kt` (previews al final del archivo)

**Interfaces:**
- Consumes: `AtmospherePhase`, `atmospherePhase(state)` (tarea 1); `LocalReduceMotion` / `rememberReduceMotion()` (tarea 1); `LocalStatusColors` (existente).
- Produces: `ConnectButton(state: TunnelState, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)` — **firma sin cambios**; `R.drawable.ic_shield_check` (la usa el propio botón). El helper privado `rememberReduceMotion` de este archivo se elimina (vive en `Motion.kt`).

- [ ] **Step 1: Crear `ic_shield_check.xml`** (escudo de trazo con check; el tinte de `Icon()` lo recolorea)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M12,2.8 L5.2,5.6 v5.2 c0,4.4 2.9,8.2 6.8,9.4 c3.9,-1.2 6.8,-5 6.8,-9.4 V5.6 Z"
        android:fillColor="#00000000"
        android:strokeColor="#FF000000"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M9.1,12 l2,2 3.8,-3.8"
        android:fillColor="#00000000"
        android:strokeColor="#FF000000"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

- [ ] **Step 2: Reestructurar `ConnectButton.kt`**

Estructura final (mantener el archivo enfocado; borrar `PulseRing` y el helper `rememberReduceMotion` privados):

1. `val phase = atmospherePhase(state)` y los colores como ahora (`animateColorAsState` de 300 ms hacia `status.connected` / `status.error` / `status.idle`).
2. Box exterior `size(200.dp)` centrado con, de atrás a delante:
   - **Halo**: Box `size(188.dp)` círculo con `background(container.copy(alpha = haloAlpha), CircleShape)` y `graphicsLayer { scaleX = haloScale; scaleY = haloScale }`. Valores por fase:
     - `Calm` / `Alert`: alpha 0.35 fijo, escala 1 (estático).
     - `Drifting`: respirar: escala 1 → 1.14, `infiniteRepeatable(tween(1200, FastOutSlowInEasing), Reverse)`; alpha 0.35 fijo.
     - `Flowing`: respirar lento: escala 1 → 1.04 y alpha 0.50 → 0.65, `infiniteRepeatable(tween(4000, FastOutSlowInEasing), Reverse)`.
     - Con `reduceMotion`: usar los valores estáticos de `Calm` (escala 1, alpha 0.35 en Drifting; escala 1.04, alpha 0.65 fijo en Flowing) y **no** crear el `rememberInfiniteTransition`.
   - **Anillo expansivo** (solo `Drifting`, y solo sin `reduceMotion`): Box `size(168.dp)` círculo; `infiniteRepeatable(tween(1200, LinearEasing), Restart)` sobre un progreso 0 → 1 mapeado a escala 1 → 1.30 y alpha 0.35 → 0.
   - **Botón**: `Surface` como ahora (`CircleShape`, `size(168.dp)`, `shadowElevation = 4.dp`, `alpha(if (enabled) 1f else 0.5f)`), más `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)` (el anillo exterior fino que separa del fondo).
3. **Icono con transición**: `AnimatedContent(targetState = state is TunnelState.Connected, transitionSpec = ...)` — entrar/salir con `fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f)` / `fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f)`; con `reduceMotion` usar `EnterTransition.None togetherWith ExitTransition.None`. Pintor: `R.drawable.ic_shield_check` si conectado, `R.drawable.ic_power` si no. `contentDescription` = `actionLabel` como ahora.
4. **Temblor** (una sola vez al entrar en `Alert`, solo sin `reduceMotion`; ver Review Focus 5):

```kotlin
val offsetX = remember { Animatable(0f) }
var shook by rememberSaveable(state is TunnelState.NoResponse || state is TunnelState.Failed) { mutableStateOf(false) }
LaunchedEffect(phase) {
    if (phase == AtmospherePhase.Alert && !shook && !reduceMotion) {
        shook = true
        val amplitude = 10.dp.toPx() // con with(LocalDensity.current) dentro del composable
        offsetX.animateTo(0f, keyframes {
            durationMillis = 360
            0f at 0; -amplitude at 60; amplitude at 120; -amplitude * 0.7f at 180
            amplitude * 0.7f at 240; -amplitude * 0.3f at 300; 0f at 360
        })
    }
    if (phase != AtmospherePhase.Alert) shook = false
}
```

El offset se aplica al Box exterior con `graphicsLayer { translationX = offsetX.value }`. Nota: `rememberSaveable` con clave booleana evita que una rotación a mitad del error repita el temblor.

5. **Háptica** en el clic (dentro de `onClick` del `Surface`, antes de delegar):

```kotlin
val haptics = LocalHapticFeedback.current
onClick = {
    if (state.isActive) haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
    else haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
    onClick()
}
```

`HapticFeedbackType.ToggleOn/ToggleOff` existen desde Compose UI 1.7 (BOM 2026.08.00 los incluye) y respetan el ajuste de vibración del sistema; no hace falta comprobación extra.

- [ ] **Step 3: Añadir previews al final de `ConnectButton.kt`**

Un preview por estado con doble anotación claro/oscuro (patrón `@Preview(name = "…")` + `@Preview(name = "… oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)`), todos envueltos en `UsipipoTheme { Surface { … } }`:

- `PreviewDesconectado` → `TunnelState.Disconnected`, enabled = true
- `PreviewConectando` → `TunnelState.Connecting("preview")`
- `PreviewConectado` → `TunnelState.Connected("preview")`
- `PreviewSinRespuesta` → `TunnelState.NoResponse("preview")`
- `PreviewFallo` → `TunnelState.Failed("preview")`
- `PreviewConectandoReducido` → `TunnelState.Connecting("preview")` envuelto además en `CompositionLocalProvider(LocalReduceMotion provides true)` (Review Focus 3)

No hace falta capturar `onClick` (usar `{}`).

- [ ] **Step 4: Verificar en CI**

```bash
git add app/src/main/res/drawable/ic_shield_check.xml app/src/main/java/com/usipipo/vpn/ui/components/ConnectButton.kt
git commit -m "feat(ui): boton de conectar con halo, anillos, temblor y haptica"
git push
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: `success` (compila `ConnectButton.kt` con las previews incluidas y los tests siguen verdes). Este archivo no tiene tests unitarios: la comprobación es de compilación + previews + la prueba manual de la tarea 9.

---

### Task 3: Home — fondo degradado animado por estado

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt`
- Test: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt` (previews del fondo al final)

**Interfaces:**
- Consumes: `LocalAtmosphere`, `atmospherePhase(state)`, `AtmosphereColors.forPhase(phase)`, `rememberReduceMotion()` (tarea 1).
- Produces: composable privado `HomeBackground(phase: AtmospherePhase, content: @Composable () -> Unit)`; `HomeScreen(vm, onImport, onAbout)` — firma sin cambios.

- [ ] **Step 1: Envolver el `Scaffold` con el fondo degradado**

En `HomeScreen`, el `Scaffold` pasa a `containerColor = Color.Transparent` y todo el cuerpo queda dentro de:

```kotlin
val reduceMotion = rememberReduceMotion()
val phase = atmospherePhase(state.tunnel)
HomeBackground(phase = phase, reduceMotion = reduceMotion) {
    Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        // LazyColumn actual, sin cambios
    }
}
```

`HomeBackground` (privado, mismo archivo):

```kotlin
@Composable
private fun HomeBackground(
    phase: AtmospherePhase,
    reduceMotion: Boolean = rememberReduceMotion(),
    content: @Composable () -> Unit,
) {
    val atmosphere = LocalAtmosphere.current.forPhase(phase)
    val spec = tween<Color>(durationMillis = 600)
    val top = if (reduceMotion) atmosphere.top else animateColorAsState(atmosphere.top, spec, label = "atmosphereTop").value
    val bottom = if (reduceMotion) atmosphere.bottom else animateColorAsState(atmosphere.bottom, spec, label = "atmosphereBottom").value
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))) {
        content()
    }
}
```

(Una sola interpolación de color de 600 ms cuando cambia el estado; sin transición con «reducir animaciones».)

- [ ] **Step 2: Estado de conexión más grande**

En `ConnectSection`, el texto de estado (`statusLabel`) pasa de `titleMedium` a `headlineSmall` (la tipografía Manrope ya viene del tema). El `hint_no_response` se mantiene en `bodyMedium` (su contraste sobre el tinte rosa está cubierto por el test de la tarea 1).

- [ ] **Step 3: Previews del fondo**

Añadir al final del archivo cuatro previews `HomeBackground` (contenido de ejemplo: `Text("Conectado", style = headlineSmall)` centrado) por cada fase, cada uno con doble anotación claro/oscuro:

- `PreviewFondoTranquilo` (Calm), `PreviewFondoDerivando` (Drifting), `PreviewFondoFluyendo` (Flowing), `PreviewFondoAlerta` (Alert).

Cada preview envuelve en `UsipipoTheme { Surface { HomeBackground(phase = …, reduceMotion = true) { … } } }` (estático en preview basta).

- [ ] **Step 4: Verificar en CI**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt
git commit -m "feat(ui): fondo de inicio con degradado animado por estado"
git push
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: `success`.

---

### Task 4: Home — tarjetas de configuración tonales con chip «Activo» y deslizar para borrar

**Files:**
- Create: `app/src/main/res/drawable/ic_server.xml`
- Modify: `app/src/main/res/values/strings.xml` (nueva string `config_active`)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt` (`ConfigCard`)

**Interfaces:**
- Consumes: `SavedConfig` (existente), `stringResource(R.string.cd_delete, name)` (existente), `pendingDelete` (estado local existente de `HomeScreen`).
- Produces: `ConfigCard(config, selected, selectEnabled, onSelect, onDeleteRequest)` privado; el diálogo de confirmación existente no cambia y se dispara desde `onDeleteRequest` (que hace `pendingDelete = config`). `R.drawable.ic_server`, `R.string.config_active`.

- [ ] **Step 1: Crear `ic_server.xml`** (dos barras de servidor con orificio; un solo path con `evenOdd`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M6,3 h12 a2.5,2.5 0 0 1 2.5,2.5 v2 A2.5,2.5 0 0 1 18,10 H6 A2.5,2.5 0 0 1 3.5,7.5 v-2 A2.5,2.5 0 0 1 6,3 z M7,6.5 m-1.3,0 a1.3,1.3 0 1 0 2.6,0 a1.3,1.3 0 1 0 -2.6,0 M6,14 h12 a2.5,2.5 0 0 1 2.5,2.5 v2 A2.5,2.5 0 0 1 18,21 H6 A2.5,2.5 0 0 1 3.5,18.5 v-2 A2.5,2.5 0 0 1 6,14 z M7,17.5 m-1.3,0 a1.3,1.3 0 1 0 2.6,0 a1.3,1.3 0 1 0 -2.6,0"
        android:fillColor="#FF000000"
        android:fillType="evenOdd" />
</vector>
```

- [ ] **Step 2: Nueva string** en `res/values/strings.xml`, sección «Inicio», junto a `configs_title`:

```xml
<string name="config_active">Activo</string>
```

- [ ] **Step 3: Reescribir `ConfigCard`**

Decisiones fijadas:

- Sin `RadioButton`, sin `BorderStroke`, sin `IconButton` de borrar: borrar es deslizar (con alternativa accesible, ver abajo).
- `shape = RoundedCornerShape(20.dp)`.
- Colores: seleccionada → `containerColor = MaterialTheme.colorScheme.primaryContainer`; no seleccionada → `surfaceVariant`. Sin borde.
- Fila `heightIn(min = 64.dp)`, `padding(horizontal = 16.dp)`, `verticalAlignment = CenterVertically`:
  - `Icon(painterResource(R.drawable.ic_server), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))`
  - `Text(config.name, bodyLarge, onSurface, weight(1f), padding start 12.dp)`
  - Si seleccionada: chip «Activo» → `Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary) { Text(stringResource(R.string.config_active), style = labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) }`
- La fila conserva `selectable(selected, enabled = selectEnabled, role = Role.RadioButton, onClick = onSelect)` (el rol semántico sigue siendo correcto sin el dibujo del RadioButton). Con el túnel activo, `selectEnabled = false` bloquea la selección como hasta ahora.
- Accesibilidad del borrado (TalkBack no puede deslizar): sobre el contenido de la tarjeta,

```kotlin
.semantics {
    customActions = listOf(
        CustomAccessibilityAction(label = deleteLabel) { onDeleteRequest(); true },
    )
}
```

donde `deleteLabel = stringResource(R.string.cd_delete, config.name)`.
- Swipe: envolver la `Card` en un `SwipeToDismissBox` (añadir `@OptIn(ExperimentalMaterial3Api::class)` por si la API sigue marcada como experimental):

```kotlin
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
        false // la tarjeta vuelve a su sitio; el borrado real pasa por el diálogo
    },
)
SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = false,
    backgroundContent = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(end = 24.dp),
            )
        }
    },
) { /* Card + fila de arriba */ }
```

Devolver `false` en `confirmValueChange` hace que la tarjeta rebote a su lugar y el diálogo de confirmación actual (`pendingDelete`) decida; cancelar no deja rastro visual. Umbral de deslizamiento: el por defecto de Material 3.

- [ ] **Step 4: Verificar en CI**

```bash
git add app/src/main/res/drawable/ic_server.xml app/src/main/res/values/strings.xml \
  app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt
git commit -m "feat(ui): tarjetas de configuracion tonales con chip activo y deslizar para borrar"
git push
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: `success`. La lógica de borrado (desconectar si es la activa) ya está cubierta por `HomeViewModelTest` y no se toca.

---

### Task 5: Home — estado vacío con ilustración

**Files:**
- Create: `app/src/main/res/drawable/ic_empty_shield.xml`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt` (`EmptyCard` → `EmptyState`; `ConfigsHeader`)

**Interfaces:**
- Consumes: `R.string.empty_title`, `R.string.empty_body`, `R.string.import_action` (existentes), `onImport` (existente).
- Produces: `EmptyState(onImport: () -> Unit)` privado; `ConfigsHeader(onImport: () -> Unit, showImport: Boolean)` (segundo parámetro nuevo).

- [ ] **Step 1: Crear `ic_empty_shield.xml`** (escudo con candado, trazo)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M12,2.8 L5.2,5.6 v5.2 c0,4.4 2.9,8.2 6.8,9.4 c3.9,-1.2 6.8,-5 6.8,-9.4 V5.6 Z"
        android:fillColor="#00000000"
        android:strokeColor="#FF000000"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M9.4,11.3 h5.2 v4.6 h-5.2 z"
        android:fillColor="#00000000"
        android:strokeColor="#FF000000"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M10.6,11.3 v-1.2 a1.4,1.4 0 0 1 2.8,0 v1.2"
        android:fillColor="#00000000"
        android:strokeColor="#FF000000"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

- [ ] **Step 2: Sustituir `EmptyCard` por `EmptyState`**

`EmptyCard` se elimina. `EmptyState` es una `Column` centrada (`fillMaxWidth`, `padding(vertical = 24.dp)`, `horizontalAlignment = CenterHorizontally`) con:

1. `Icon(painterResource(R.drawable.ic_empty_shield), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(96.dp))`
2. `Spacer(12.dp)` + `Text(stringResource(R.string.empty_title), titleMedium, onBackground, textAlign = Center)`
3. `Spacer(4.dp)` + `Text(stringResource(R.string.empty_body), bodyMedium, onSurfaceVariant, textAlign = Center)`
4. `Spacer(16.dp)` + `FilledTonalButton(onClick = onImport) { Icon(Icons.Default.Add, contentDescription = null); Spacer(8.dp); Text(stringResource(R.string.import_action)) }`

Sin `Card`, sin borde, sin fondo: la ilustración y el botón viven directamente sobre el degradado.

- [ ] **Step 3: Evitar el doble botón de importar**

Con la lista vacía habría dos «Importar» (cabecera + estado vacío). `ConfigsHeader` gana `showImport: Boolean`; cuando `state.configs.isEmpty()` se llama con `showImport = false` (solo el título «Configuraciones») y cuando hay configuraciones con `showImport = true` (comportamiento actual).

- [ ] **Step 4: Añadir previews** al final del archivo: `PreviewVacio` / `PreviewVacioOscuro` → `UsipipoTheme { HomeBackground(phase = Calm, reduceMotion = true) { EmptyState(onImport = {}) } }` con la doble anotación claro/oscuro.

- [ ] **Step 5: Verificar en CI**

```bash
git add app/src/main/res/drawable/ic_empty_shield.xml app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt
git commit -m "feat(ui): estado vacio con ilustracion y boton de importar"
git push
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: `success`.

---

### Task 6: Transiciones entre pantallas

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt`

**Interfaces:**
- Consumes: `rememberReduceMotion()` (tarea 1); `Screen` privado (existente).
- Produces: `UsipipoRoot(container: AppContainer)` — firma sin cambios.

- [ ] **Step 1: Envolver el `when (screen)` en `AnimatedContent`**

```kotlin
val reduceMotion = rememberReduceMotion()

AnimatedContent(
    targetState = screen,
    transitionSpec = {
        if (reduceMotion) {
            EnterTransition.None togetherWith ExitTransition.None
        } else {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            val spec = tween<IntOffset>(durationMillis = 250)
            val fade = tween<Float>(durationMillis = 250)
            (slideInHorizontally(spec) { it / 8 * direction } + fadeIn(fade)) togetherWith
                (slideOutHorizontally(spec) { -it / 8 * direction } + fadeOut(fade))
        }
    },
    label = "screen",
) { target ->
    when (target) {
        Screen.Home -> HomeScreen(/* igual que ahora */)
        Screen.Import -> ImportScreen(/* … */)
        Screen.About -> AboutScreen(/* … */)
        Screen.Diagnostics -> DiagnosticsScreen(/* … */)
    }
}
```

Notas: el orden del enum (`Home, Import, About, Diagnostics`) define la dirección (avanzar si `targetState.ordinal > initialState.ordinal`); `Import` vuelve a Home (retroceso). Deslizamiento corto (1/8 del ancho) con desvanecimiento, 250 ms. `BackHandler` y el `UpdateDialog` quedan **fuera** del `AnimatedContent`, sin cambios. Imports nuevos: `androidx.compose.animation.AnimatedContent`, `EnterTransition`, `ExitTransition`, `fadeIn`, `fadeOut`, `slideInHorizontally`, `slideOutHorizontally`, `togetherWith`, `androidx.compose.animation.core.tween`, `androidx.compose.ui.unit.IntOffset`, y `rememberReduceMotion`.

- [ ] **Step 2: Verificar en CI**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt
git commit -m "feat(ui): transiciones entre pantallas con desvanecimiento"
git push
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: `success`.

---

### Task 7: Acerca de — logo grande y secciones con encabezado

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (nuevas `about_updates_title`, `about_advanced_title`)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/about/AboutScreen.kt`

**Interfaces:**
- Consumes: `R.drawable.ic_launcher_foreground` (existente, logo de la app), `R.string.support_label`, `R.string.support_email`, `R.string.send_email`, `R.string.about_check_updates`, `R.string.diag_open` (existentes); callbacks `onBack`, `onDiagnostics`, `onCheckUpdates` (existentes).
- Produces: `AboutScreen(onBack, onDiagnostics, onCheckUpdates)` — firma sin cambios.

- [ ] **Step 1: Strings nuevas** en `res/values/strings.xml`, sección «Acerca de»:

```xml
<string name="about_updates_title">Actualizaciones</string>
<string name="about_advanced_title">Avanzado</string>
```

- [ ] **Step 2: Reestructurar la pantalla**

La `Column` actual pasa a (mantener el `Row` de cabecera con flecha + «Acerca de» tal cual):

1. **Cabecera de marca** (centrada, `fillMaxWidth`): `Icon(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(96.dp))`; `Text(stringResource(R.string.app_name), headlineSmall, onBackground)`; `Text(stringResource(R.string.version_label, version), bodyMedium, onSurfaceVariant)`. El tinte recolorea el trazo navy del logo para que funcione en modo oscuro.
2. **Sección «Soporte»**: `SectionHeader(stringResource(R.string.support_label))` + `Card(shape = RoundedCornerShape(20.dp), colors = cardColors(containerColor = surfaceVariant))` sin borde, con `padding(16.dp)`: email (`bodyLarge`, `onSurface`) y `FilledTonalButton` «Escribir a soporte» (`fillMaxWidth`, `heightIn(min = 56.dp)`).
3. **Sección «Actualizaciones»**: `SectionHeader(about_updates_title)` + card tonal con el `FilledTonalButton` de comprobar actualizaciones (mismo `onClick`/`checking` que ahora).
4. **Sección «Avanzado»**: `SectionHeader(about_advanced_title)` + card tonal con el `OutlinedButton` «Diagnóstico y ajustes avanzados».

`SectionHeader` es un privado nuevo: `Text(text, titleMedium, onBackground)`. Los `Card` usan `fillMaxWidth` y la `Column` conserva `spacedBy(16.dp)`; el orden de los botones y sus comportamientos no cambian.

- [ ] **Step 3: Verificar en CI**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/usipipo/vpn/ui/about/AboutScreen.kt
git commit -m "feat(ui): acerca de con logo y secciones"
git push
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: `success`.

---

### Task 8: Diagnóstico — secciones con encabezado y tarjetas tonales

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt`

**Interfaces:**
- Consumes: `R.string.diag_info_title`, `R.string.diag_verbose_title`, `R.string.diag_log_title` (existentes, pasan a ser encabezados); `DiagnosticsViewModel` y sus estados (sin cambios).
- Produces: composable privado `SectionHeader(text: String)`; `DiagnosticsScreen(vm, onBack)` — firma sin cambios.

- [ ] **Step 1: Convertir a secciones**

En la `Column` principal, sustituir la lista actual por:

1. `SectionHeader(stringResource(R.string.diag_info_title))` + `InfoCard(rows = ui.infoRows)` — la card pierde su título interno, el `BorderStroke` y `surface`; pasa a `RoundedCornerShape(20.dp)` + `containerColor = MaterialTheme.colorScheme.surfaceVariant`.
2. `SectionHeader(stringResource(R.string.diag_verbose_title))` + `VerboseRow(checked, onChange)` — la card tonal (mismo tratamiento); dentro queda solo el texto `diag_verbose_body` + `Switch` (el título interno se elimina). El `toggleable` de la fila y el rol `Switch` no cambian.
3. `SectionHeader(stringResource(R.string.diag_log_title))` + `LogCard(...)` — card tonal sin borde ni título interno; conserva la meta (`diag_log_meta`), el botón `diag_refresh`, el scroll y el `SelectionContainer`.
4. Botones `diag_send` / `diag_clear` y `CrashBanner`: sin cambios funcionales; solo `CrashBanner` ajusta su `shape` a `RoundedCornerShape(20.dp)` para acompañar.

`SectionHeader` (privado): `Text(text, titleMedium, onBackground)`.

- [ ] **Step 2: Verificar en CI**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt
git commit -m "feat(ui): diagnostico en secciones con tarjetas tonales"
git push
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: `success`.

---

### Task 9: Verificación final y entrega

**Files:**
- No modifica archivos (salvo correcciones si CI falla).

**Interfaces:**
- Consumes: todo lo anterior.
- Produces: rama `feat/atmosfera` en verde, lista para revisión y prueba manual.

- [ ] **Step 1: CI completo sobre la cabeza de la rama**

```bash
git status --short   # debe estar limpio
gh workflow run "Android CI - Build APK" --ref feat/atmosfera
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/atmosfera --limit 1
gh run watch <id-mostrado> --exit-status
gh run view <id> --log | grep -E "(BUILD SUCCESSFUL|tests completed)"
```

Esperado: `testDebugUnitTest` (todos los tests, incluidos `AtmosphereTest` y los de `HomeViewModel`, `update`, `diagnostics`) y `assembleDebug` en verde.

- [ ] **Step 2: Comprobar que las previews pedidas existen**

```bash
grep -c "@Preview" app/src/main/java/com/usipipo/vpn/ui/components/ConnectButton.kt   # 5 estados × 2 temas + 1 con movimiento reducido
grep -c "@Preview" app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt            # 4 fondos × 2 temas + vacío × 2
```

Esperado: todos los estados del diseño (desconectado, conectando, conectado, fallo/sin respuesta, vacío) en claro y oscuro.

- [ ] **Step 3: Entregar a prueba manual** (la persona, no el agente)

Checklist para el teléfono, según la spec (§7):
- Teléfono de gama baja y uno de gama media: el halo no debe tartamudear; si algo va lento, desactivar primero el «respirar» lento de conectado (revierte el paso 2 de la tarea 2 para `Flowing`).
- «Reducir animaciones» activado y desactivado en los dos.
- Rotación durante «Conectando» y tras un fallo (review focus 5).
- Modo claro y oscuro en los cinco estados.
- TalkBack: el botón conserva «Conectar/Desconectar»; cada tarjeta ofrece la acción «Eliminar».
- Confirmar peso de APK (la fuente añade unos cientos de KB, aceptado).

- [ ] **Step 4: Cierre**

Anotar en el mensaje de entrega: rama, resumen de commits y resultado de CI. La fusión a `main` la decide la persona (fuera del alcance del plan).
