# uSipipo VPN «Mockup» — Fase 1: estructura y estilo — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Llevar a código la estructura y el estilo del mockup: tema oscuro azul noche (y claro adaptado), barra inferior de 4 pestañas (Inicio, Configuraciones, Importar, Diagnóstico), pestaña Configuraciones con «Mis perfiles» y «Favoritos», Importar con dos tarjetas, «Acerca de» dentro de Diagnóstico, y logo nuevo (icono y cabecera).

**Architecture:** Se sustituye la navegación por pantallas (`Screen`) por una navegación por pestañas (`MainTab`) sin librería. Cada perfil guarda un campo `favorite` en el índice del almacén cifrado. `HomeViewModel` se comparte entre Inicio y Configuraciones (mismo `ViewModelStoreOwner`). El túnel, `AppContainer`, diagnóstico (lógica) y actualizaciones no cambian.

**Tech Stack:** Kotlin 2.3.21, Jetpack Compose (BOM 2026.08.00), Material 3, AGP 9.3.3, minSdk 26. Única dependencia nueva, solo de tests: `org.json:json` (para probar el índice del almacén en JVM).

**Spec:** `docs/plans/2026-10-18-usipipo-vpn-mockup-design.md` (secciones 2, 3 y 4)

## Global Constraints

- **No hay JDK ni Android SDK en la máquina local.** El compilador y el runner de tests son **GitHub Actions**. Cada tarea termina con un ciclo CI: `gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1` y `gh run watch <id> --exit-status`. El workflow corre `testDebugUnitTest` y `assembleDebug`.
- Rama de trabajo: `feat/mockup-fase1`, creada desde `main` antes de la tarea 1.
- **Fuera de alcance (no tocar):** `tunnel/**`, `AppContainer.kt`, `MainActivity.kt`, `UsipipoApp.kt`, `update/**`, `diagnostics/**` (lógica), `DiagnosticsViewModel.kt`, `ImportViewModel.kt`, `ConnectButton.kt`.
- **Fuera de alcance de la fase 1:** estadísticas, «Ver detalles», pantalla Detalles, renombrar perfil (fase 2); país y ping (fase 3). No dejar huecos vacíos ni datos de relleno.
- Los colores de estado **siempre** van acompañados de texto.
- Animaciones ligeras: sin blur ni sombras animadas. El halo del botón sigue siendo la única animación continua. Con «reducir animaciones» no hay transición entre pestañas.
- Todo en español: strings, comentarios y mensajes de commit (`feat(ui): …` en minúscula). Nombres de API en inglés.
- Los tests existentes deben seguir en verde. Solo se permite ampliar `FakeConfigStore` (helper de tests) para el método nuevo del contrato.
- Iconos: solo `material-icons-core` (ya es dependencia). Lo que no exista allí se dibuja como vector propio en `res/drawable`.

## Review Focus

Modos de fallo que ningún test unitario cubre; cada línea indica dónde queda fijado:

1. **Insets duplicados:** cada pantalla de pestaña lleva su propio `Scaffold`, y la barra inferior ya consume el inset de navegación. Si no se limita a `statusBars`, aparece un hueco doble sobre la barra. Fijado en la tarea 3 (paso 5) y en la checklist manual (navegación por gestos y por 3 botones).
2. **Teclado en el paso «Nombre» de Importar:** la barra no debe subir encima del teclado. Fijado en la tarea 3 (oculta la barra si el IME está visible).
3. **Índice antiguo sin `favorite`:** los perfiles ya guardados deben cargar como no favoritos. Fijado en `EncryptedFileConfigStoreFavoritesTest` (tarea 1).
4. **Atrás en el paso «Nombre»:** debe cancelar el paso y no salir de la pestaña. Fijado en la tarea 6 (`BackHandler`).
5. **Borrar el perfil activo con el túnel conectado:** debe desconectar antes. Ya lo hace `HomeViewModel.delete`; la tarea 5 conserva esa llamada.
6. **Favoritos vacío:** debe mostrar un mensaje, no una lista en blanco. Fijado en la tarea 5.
7. **Rotación de pantalla:** pestaña activa y pestaña de Configuraciones sobreviven (`rememberSaveable`). Fijado en la checklist manual.
8. **Tema claro:** la paleta nueva toca el esquema oscuro; el claro debe quedar igual salvo tarjetas con borde. Fijado en las previews de la tarea 2.

---

### Task 1: Datos — perfiles favoritos

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/domain/Models.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/data/ConfigStore.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/data/EncryptedFileConfigStore.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeViewModel.kt`
- Modify: `app/src/test/java/com/usipipo/vpn/ui/home/Fakes.kt`
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Create: `app/src/test/java/com/usipipo/vpn/data/EncryptedFileConfigStoreFavoritesTest.kt`
- Create: `app/src/test/java/com/usipipo/vpn/ui/home/HomeViewModelFavoritesTest.kt`

**Interfaces:**
- Produces (usado por las tareas 4 y 5): `SavedConfig(id, name, favorite = false)`; `ConfigStore.setFavorite(id: String, favorite: Boolean)`; `HomeViewModel.toggleFavorite(id: String)`.

- [ ] **Step 0: Crear la rama y confirmar CI base en verde**

```bash
git switch main && git pull && git switch -c feat/mockup-fase1 && git push -u origin feat/mockup-fase1
gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1
sleep 15 && gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase1 --limit 1
gh run watch <id-mostrado> --exit-status
```

Esperado: run `success`.

- [ ] **Step 1: Escribir los tests que fallan**

`HomeViewModelFavoritesTest.kt`:

```kotlin
package com.usipipo.vpn.ui.home

import com.usipipo.vpn.domain.SavedConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelFavoritesTest {

    private val casa = SavedConfig("a", "Casa")
    private val trabajo = SavedConfig("b", "Trabajo", favorite = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `marcar una configuracion como favorita se refleja en el estado`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.toggleFavorite("a")

        assertTrue(vm.uiState.value.configs.first { it.id == "a" }.favorite)
    }

    @Test
    fun `desmarcar una favorita la devuelve a no favorita`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.toggleFavorite("b")

        assertFalse(vm.uiState.value.configs.first { it.id == "b" }.favorite)
    }

    @Test
    fun `un id desconocido no cambia nada`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.toggleFavorite("zzz")

        assertEquals(listOf(casa, trabajo), vm.uiState.value.configs)
    }
}
```

`EncryptedFileConfigStoreFavoritesTest.kt`:

```kotlin
package com.usipipo.vpn.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EncryptedFileConfigStoreFavoritesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Sin cifrado: aquí solo importa el índice, no el Keystore. */
    private class IdentityCrypto : Crypto {
        override fun encrypt(plain: ByteArray) = plain
        override fun decrypt(blob: ByteArray) = blob
    }

    @Test
    fun `el favorito se conserva al reabrir el almacen`() = runTest {
        val dir = tmp.newFolder("configs")
        val first = EncryptedFileConfigStore(dir, IdentityCrypto())
        val saved = first.add("Casa", "conf").getOrThrow()

        first.setFavorite(saved.id, true)

        val second = EncryptedFileConfigStore(dir, IdentityCrypto())
        assertTrue(second.configs.value.single().favorite)
    }

    @Test
    fun `un indice antiguo sin favorito carga como no favorito`() {
        val dir = tmp.newFolder("configs")
        File(dir, "abc.enc").writeBytes(byteArrayOf(1))
        File(dir, "index.json").writeText(
            """{"configs":[{"id":"abc","name":"Casa"}],"lastUsedId":"abc"}""",
        )

        val store = EncryptedFileConfigStore(dir, IdentityCrypto())

        assertEquals("Casa", store.configs.value.single().name)
        assertFalse(store.configs.value.single().favorite)
    }
}
```

- [ ] **Step 2: Dependencia de tests para `org.json`**

En `gradle/libs.versions.toml`, sección `[libraries]`, añadir:

```toml
json = { group = "org.json", name = "json", version = "20240303" }
```

En `app/build.gradle.kts`, junto a los demás `testImplementation`:

```kotlin
testImplementation(libs.json)
```

(Sin esto, `org.json` de `android.jar` lanza «not mocked» en tests JVM.)

- [ ] **Step 3: Modelo y contrato**

`Models.kt`: cambiar la clase.

```kotlin
/** Configuración guardada. El contenido (claves) vive cifrado en el almacén, no aquí. */
data class SavedConfig(val id: String, val name: String, val favorite: Boolean = false)
```

`ConfigStore.kt`: añadir al final de la interfaz.

```kotlin
    /** Marca o desmarca una configuración como favorita. Ignora ids que no existen. */
    suspend fun setFavorite(id: String, favorite: Boolean)
```

- [ ] **Step 4: Persistencia en `EncryptedFileConfigStore`**

Añadir el método:

```kotlin
    override suspend fun setFavorite(id: String, favorite: Boolean) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (_configs.value.none { it.id == id }) return@withContext
                _configs.value = _configs.value.map { if (it.id == id) it.copy(favorite = favorite) else it }
                persistIndex()
            }
        }
    }
```

En `loadIndex()`, sustituir la construcción de `SavedConfig`:

```kotlin
                SavedConfig(
                    id = item.getString(KEY_ID),
                    name = item.getString(KEY_NAME),
                    favorite = item.optBoolean(KEY_FAVORITE, false),
                )
```

En `persistIndex()`, sustituir el `put` del bucle:

```kotlin
            array.put(
                JSONObject()
                    .put(KEY_ID, it.id)
                    .put(KEY_NAME, it.name)
                    .put(KEY_FAVORITE, it.favorite),
            )
```

Y en el `companion object` añadir `const val KEY_FAVORITE = "favorite"`.

- [ ] **Step 5: `HomeViewModel.toggleFavorite` y el fake**

En `HomeViewModel`, después de `select`:

```kotlin
    fun toggleFavorite(id: String) {
        val current = store.configs.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { store.setFavorite(id, !current.favorite) }
    }
```

En `Fakes.kt`, dentro de `FakeConfigStore`:

```kotlin
    override suspend fun setFavorite(id: String, favorite: Boolean) {
        _configs.value = _configs.value.map { if (it.id == id) it.copy(favorite = favorite) else it }
    }
```

- [ ] **Step 6: CI y commit**

```bash
git add -A && git commit -m "feat(data): perfiles favoritos en el almacén de configuraciones"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase1 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

Esperado: `success`, con los 5 tests nuevos en verde y los existentes intactos.

---

### Task 2: Fundaciones visuales — paleta, logo y componentes base

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/theme/Atmosphere.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/theme/Theme.kt`
- Modify: `app/src/main/res/values/colors.xml`, `app/src/main/res/values-night/colors.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/drawable/ic_logo_u.xml`, `ic_import.xml`, `ic_qr.xml`
- Create: `app/src/main/java/com/usipipo/vpn/ui/components/UsipipoCard.kt`, `IconTile.kt`, `AppLogo.kt`, `AppHeader.kt`
- Create: `app/src/test/java/com/usipipo/vpn/ui/theme/PaletteContrastTest.kt`
- Create: `docs/design/mockup-ia.png` (copia del mockup, solo referencia)

**Interfaces:**
- Produces (usados por las tareas 3 a 7): `UsipipoCard(modifier, selected, onClick, content)`, `IconTile(icon: Int)`, `AppLogo(size: Dp)`, `AppHeader()`; consts `AtmospherePalette.DARK_BACKGROUND/SURFACE/SURFACE_VARIANT/PRIMARY/ON_PRIMARY/SECONDARY/ON_SECONDARY`.

- [ ] **Step 1: Guardar el mockup como referencia**

```bash
mkdir -p docs/design && cp <ruta-del-mockup>.png docs/design/mockup-ia.png
```

- [ ] **Step 2: Test de contraste de la paleta nueva (falla hasta el paso 3)**

`PaletteContrastTest.kt`:

```kotlin
package com.usipipo.vpn.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {

    @Test
    fun `el texto oscuro mantiene contraste 4 punto 5 sobre fondo y tarjetas`() {
        listOf(
            AtmospherePalette.DARK_BACKGROUND,
            AtmospherePalette.DARK_SURFACE,
            AtmospherePalette.DARK_SURFACE_VARIANT,
        ).forEach {
            assertTrue("texto sobre $it", contrast(AtmospherePalette.TEXT_DARK, it) >= 4.5)
            assertTrue("secundario sobre $it", contrast(AtmospherePalette.TEXT_DARK_SECONDARY, it) >= 4.5)
        }
    }

    @Test
    fun `los botones oscuros mantienen contraste 4 punto 5`() {
        assertTrue(contrast(AtmospherePalette.DARK_ON_PRIMARY, AtmospherePalette.DARK_PRIMARY) >= 4.5)
        assertTrue(contrast(AtmospherePalette.DARK_ON_SECONDARY, AtmospherePalette.DARK_SECONDARY) >= 4.5)
    }

    @Test
    fun `el acento oscuro se lee como texto de la barra inferior`() {
        assertTrue(contrast(AtmospherePalette.DARK_PRIMARY, AtmospherePalette.DARK_BACKGROUND) >= 4.5)
        assertTrue(contrast(AtmospherePalette.DARK_PRIMARY, AtmospherePalette.DARK_SURFACE) >= 4.5)
    }

    private fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val (hi, lo) = if (la >= lb) la to lb else lb to la
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(v: Long): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
```

- [ ] **Step 3: Paleta oscura del mockup**

En `AtmospherePalette` (`Atmosphere.kt`), sustituir los fondos oscuros y añadir el esquema. Valores calculados contra el mockup; todos cumplen ≥ 4.5:1 con `TEXT_DARK` y `TEXT_DARK_SECONDARY` (comprobado: mínimo 5.85 en el extremo «fluyendo»).

```kotlin
    // Fondos (oscuro): azul noche del mockup
    const val DARK_CALM_TOP: Long = 0xFF04132D
    const val DARK_CALM_BOTTOM: Long = 0xFF0B2147
    const val DARK_DRIFTING_TOP: Long = 0xFF06182E
    const val DARK_DRIFTING_BOTTOM: Long = 0xFF15403C
    const val DARK_FLOWING_TOP: Long = 0xFF071D33
    const val DARK_FLOWING_BOTTOM: Long = 0xFF114C40
    const val DARK_ALERT_TOP: Long = 0xFF231620
    const val DARK_ALERT_BOTTOM: Long = 0xFF32202B
    // Esquema oscuro (pantallas sin degradado y tarjetas)
    const val DARK_BACKGROUND: Long = 0xFF061631
    const val DARK_SURFACE: Long = 0xFF0E2447
    const val DARK_SURFACE_VARIANT: Long = 0xFF12305A
    const val DARK_PRIMARY: Long = 0xFF4C8DFF
    const val DARK_ON_PRIMARY: Long = 0xFF04122B
    const val DARK_SECONDARY: Long = 0xFF2DD4BF
    const val DARK_ON_SECONDARY: Long = 0xFF042A26
```

En `Theme.kt`, sustituir `DarkColors`:

```kotlin
private val DarkColors = darkColorScheme(
    primary = Color(AtmospherePalette.DARK_PRIMARY),
    onPrimary = Color(AtmospherePalette.DARK_ON_PRIMARY),
    primaryContainer = Color(0xFF14386B),
    onPrimaryContainer = Color(0xFFE6F0FF),
    secondary = Color(AtmospherePalette.DARK_SECONDARY),
    onSecondary = Color(AtmospherePalette.DARK_ON_SECONDARY),
    background = Color(AtmospherePalette.DARK_BACKGROUND),
    onBackground = Color(0xFFE6F0FF),
    surface = Color(AtmospherePalette.DARK_SURFACE),
    onSurface = Color(0xFFE6F0FF),
    surfaceVariant = Color(AtmospherePalette.DARK_SURFACE_VARIANT),
    onSurfaceVariant = Color(0xFFB7C9E2),
    outline = Color(0xFF244A7C),
    error = Color(0xFFF4A3A3),
    onError = Navy,
)
```

El esquema claro (`LightColors`) no cambia. Nota de diseño: en oscuro, el texto de los botones es azul noche sobre el azul brillante (no blanco, como en el mockup) porque el blanco no llega a 4.5:1.

`res/values-night/colors.xml`: `window_background` pasa a `#061631`. `res/values/colors.xml`: `ic_launcher_background` pasa a `#004AAD` (azul del logo).

- [ ] **Step 4: Logo como vector**

El jpg se analizó: fondo `#004AAD`, trazo blanco de ~45 px sobre lienzo 640×640, asta izquierda con curva que enlaza con una asta derecha más larga, y un guion bajo. `res/drawable/ic_logo_u.xml` (recorte de 400×400 centrado en el glifo, para usarlo a cualquier tamaño):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="400dp"
    android:height="400dp"
    android:viewportWidth="400"
    android:viewportHeight="400">
    <group android:translateX="-76" android:translateY="-133">
        <path
            android:pathData="M230,242 L230,328 C230,386 262,412 304,412 C344,412 378,394 406,358"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="45"
            android:strokeLineCap="round"
            android:strokeLineJoin="round" />
        <path
            android:pathData="M406,176 L406,462"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="45"
            android:strokeLineCap="round" />
        <path
            android:pathData="M120,498 L277,498"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="30" />
    </group>
</vector>
```

`ic_launcher_foreground.xml` (icono adaptable 108 dp; el glifo escalado a ~52 dp de alto, dentro de la zona segura):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="0.1453"
        android:scaleY="0.1453"
        android:translateX="13.9"
        android:translateY="5.6">
        <path
            android:pathData="M230,242 L230,328 C230,386 262,412 304,412 C344,412 378,394 406,358"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="45"
            android:strokeLineCap="round"
            android:strokeLineJoin="round" />
        <path
            android:pathData="M406,176 L406,462"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="45"
            android:strokeLineCap="round" />
        <path
            android:pathData="M120,498 L277,498"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="30" />
    </group>
</vector>
```

`ic_import.xml` y `ic_qr.xml` (24 dp, relleno, para tintar con `Icon`):

```xml
<!-- ic_import.xml: documento con flecha hacia abajo -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:fillType="evenOdd"
        android:pathData="M6,2h8l6,6v12a2,2 0 0 1 -2,2H6a2,2 0 0 1 -2,-2V4a2,2 0 0 1 2,-2zM13,3.5V9h5.5L13,3.5zM11,11h2v4.17l1.59,-1.58L16,15l-4,4 -4,-4 1.41,-1.41L11,15.17V11z" />
</vector>
```

```xml
<!-- ic_qr.xml: escáner de código QR -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M9.5,6.5v3h-3v-3H9.5M11,5H5v6h6V5L11,5zM9.5,14.5v3h-3v-3H9.5M11,13H5v6h6V13L11,13zM17.5,6.5v3h-3v-3H17.5M19,5h-6v6h6V5L19,5zM13,13h1.5v1.5H13V13zM14.5,14.5H16V16h-1.5V14.5zM16,13h1.5v1.5H16V13zM13,16h1.5v1.5H13V16zM14.5,17.5H16V19h-1.5V17.5zM16,16h1.5v1.5H16V16zM17.5,14.5H19V16h-1.5V14.5zM17.5,17.5H19V19h-1.5V17.5zM22,7h-2V4h-3V2h5V7zM22,22h-5v-2h3v-3h2V22zM2,22v-5h2v3h3v2H2zM2,2h5v2H4v3H2V2z" />
</vector>
```

- [ ] **Step 5: Componentes base**

`UsipipoCard.kt`:

```kotlin
package com.usipipo.vpn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Tarjeta del mockup: superficie tonal, borde fino y esquinas de 20 dp. */
@Composable
fun UsipipoCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val cardColors = CardDefaults.cardColors(
        containerColor = if (selected) colors.primaryContainer else colors.surface,
    )
    val border = BorderStroke(1.dp, if (selected) colors.primary else colors.outline)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = cardColors, border = border, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = cardColors, border = border, content = content)
    }
}
```

`IconTile.kt`:

```kotlin
package com.usipipo.vpn.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Icono dentro de un cuadro redondeado, como en las tarjetas del mockup. */
@Composable
fun IconTile(@DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}
```

`AppLogo.kt` y `AppHeader.kt`:

```kotlin
package com.usipipo.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R

private val BrandBlue = Color(0xFF004AAD)

/** El logo: la «u» blanca con guion bajo sobre el azul de marca. */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(BrandBlue),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_u),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.62f),
        )
    }
}

/** Cabecera de las pantallas principales: logo y nombre de la app. */
@Composable
fun AppHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLogo(size = 36.dp)
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
```

- [ ] **Step 6: Previews**

Añadir al final de `UsipipoCard.kt` una preview clara y oscura (`@Preview` y `@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)`) con una tarjeta normal y una `selected = true`, dentro de `UsipipoTheme`. Comparar con el mockup (`docs/design/mockup-ia.png`) en Android Studio o en la APK y ajustar solo `outline` y `primaryContainer` si el borde o el chip se ven muy distintos.

- [ ] **Step 7: CI y commit**

```bash
git add -A && git commit -m "feat(ui): paleta azul noche, logo vectorial y componentes base del mockup"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase1 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

Esperado: `success`; `AtmosphereTest` existente y `PaletteContrastTest` nuevo en verde.

---

### Task 3: Navegación por pestañas y barra inferior

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/nav/MainTab.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/components/UsipipoBottomBar.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/configs/ConfigsScreen.kt` (provisional; lo completa la tarea 5)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt`, `importer/ImportScreen.kt`, `diagnostics/DiagnosticsScreen.kt` (solo el `Scaffold`)
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/test/java/com/usipipo/vpn/ui/nav/MainTabTest.kt`

**Interfaces:**
- Produces: `enum class MainTab { Home, Configs, Import, Diagnostics }`; `fun backTarget(tab: MainTab): MainTab?`; `UsipipoBottomBar(selected, onSelect)`.

- [ ] **Step 1: Test que falla**

```kotlin
package com.usipipo.vpn.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainTabTest {

    @Test
    fun `atras desde Inicio sale de la app`() {
        assertNull(backTarget(MainTab.Home))
    }

    @Test
    fun `atras desde cualquier otra pestana vuelve a Inicio`() {
        listOf(MainTab.Configs, MainTab.Import, MainTab.Diagnostics).forEach {
            assertEquals(MainTab.Home, backTarget(it))
        }
    }
}
```

- [ ] **Step 2: `MainTab.kt`**

```kotlin
package com.usipipo.vpn.ui.nav

/** Las cuatro pestañas de la barra inferior. */
enum class MainTab { Home, Configs, Import, Diagnostics }

/** Destino del botón Atrás: Inicio desde cualquier otra pestaña; null en Inicio (sale de la app). */
fun backTarget(tab: MainTab): MainTab? = if (tab == MainTab.Home) null else MainTab.Home
```

- [ ] **Step 3: Strings de navegación**

En `strings.xml`:

```xml
    <!-- Navegación -->
    <string name="nav_home">Inicio</string>
    <string name="nav_configs">Configuraciones</string>
    <string name="nav_import">Importar</string>
    <string name="nav_diagnostics">Diagnóstico</string>
```

- [ ] **Step 4: `UsipipoBottomBar.kt`**

```kotlin
package com.usipipo.vpn.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.nav.MainTab

@Composable
fun UsipipoBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // Oscuro: acento brillante como en el mockup. Claro: azul marino sobre el pastel.
    val active = if (scheme.background.luminance() < 0.5f) scheme.primary else scheme.onPrimaryContainer

    NavigationBar(containerColor = scheme.surface) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(painter = tabIcon(tab), contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(tabLabel(tab)),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = active,
                    selectedTextColor = active,
                    indicatorColor = scheme.primaryContainer,
                    unselectedIconColor = scheme.onSurfaceVariant,
                    unselectedTextColor = scheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private fun tabLabel(tab: MainTab): Int = when (tab) {
    MainTab.Home -> R.string.nav_home
    MainTab.Configs -> R.string.nav_configs
    MainTab.Import -> R.string.nav_import
    MainTab.Diagnostics -> R.string.nav_diagnostics
}

@Composable
private fun tabIcon(tab: MainTab): Painter = when (tab) {
    MainTab.Home -> rememberVectorPainter(Icons.Default.Home)
    MainTab.Configs -> rememberVectorPainter(Icons.Default.Settings)
    MainTab.Import -> painterResource(R.drawable.ic_import)
    MainTab.Diagnostics -> rememberVectorPainter(Icons.Default.Build)
}
```

(Diagnóstico usa una llave en vez del segundo engranaje del mockup para distinguirlo de Configuraciones.)

- [ ] **Step 5: Un solo manejo de insets**

La barra inferior consume el inset de navegación; las pantallas de pestaña solo deben respetar la barra de estado. En los `Scaffold` de `HomeScreen`, `ImportScreen` y `DiagnosticsScreen` añadir el parámetro:

```kotlin
        contentWindowInsets = WindowInsets.statusBars,
```

con `import androidx.compose.foundation.layout.WindowInsets` e `import androidx.compose.foundation.layout.statusBars`. En `HomeScreen` además quitar `padding.calculateBottomPadding() + 24.dp` y dejar `24.dp` (ya no hay inset inferior que sumar).

- [ ] **Step 6: Stub provisional de Configuraciones**

`ConfigsScreen.kt` (se reemplaza completo en la tarea 5):

```kotlin
package com.usipipo.vpn.ui.configs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.home.HomeViewModel

@Composable
fun ConfigsScreen(vm: HomeViewModel, onImport: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.nav_configs), color = MaterialTheme.colorScheme.onBackground)
    }
}
```

- [ ] **Step 7: Reescribir `UsipipoRoot.kt`**

Se conserva intacto el bloque del diálogo de actualización, `openRelease` y las tres funciones de factoría de ViewModels; cambia el cuerpo de `UsipipoRoot`. Con esta tarea, **Acerca de deja de ser alcanzable** hasta la tarea 7 (rama de trabajo, no se publica).

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsipipoRoot(container: AppContainer) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    val updateStatus by container.updateStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var handledUpdate by rememberSaveable { mutableStateOf<String?>(null) }
    val reduceMotion = rememberReduceMotion()
    val imeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = backTarget(tab) != null) { tab = backTarget(tab) ?: tab }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    if (reduceMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    }
                },
                label = "tab",
            ) { target ->
                when (target) {
                    MainTab.Home -> HomeScreen(
                        vm = homeViewModel(container),
                        onImport = { tab = MainTab.Import },
                        onAbout = { tab = MainTab.Diagnostics }, // provisional (tarea 4 lo reemplaza)
                    )
                    MainTab.Configs -> ConfigsScreen(
                        vm = homeViewModel(container),
                        onImport = { tab = MainTab.Import },
                    )
                    MainTab.Import -> ImportScreen(
                        vm = importViewModel(container),
                        onBack = { tab = MainTab.Home },
                    )
                    MainTab.Diagnostics -> DiagnosticsScreen(
                        vm = diagnosticsViewModel(container),
                        onBack = { tab = MainTab.Home },
                    )
                }
            }
        }
        // Con el teclado abierto (paso «Nombre» de Importar) la barra no estorba.
        if (!imeVisible) {
            UsipipoBottomBar(selected = tab, onSelect = { tab = it })
        }
    }

    // ... aquí sigue, sin cambios, el bloque `val available = updateStatus as? UpdateStatus.UpdateAvailable` ...
}
```

Imports nuevos: `androidx.compose.foundation.background`, `foundation.layout.{Box, Column, WindowInsets, fillMaxSize, imePadding, isImeVisible}`, `androidx.compose.material3.MaterialTheme`, `com.usipipo.vpn.ui.components.UsipipoBottomBar`, `com.usipipo.vpn.ui.configs.ConfigsScreen`, `com.usipipo.vpn.ui.nav.{MainTab, backTarget}`. Se eliminan `Screen`, `slideInHorizontally`, `slideOutHorizontally`, `IntOffset`, `AboutScreen` y el uso de `container.checkForUpdates` por la lambda de About (vuelve en la tarea 7). `HomeScreen` y los demás conservan temporalmente su firma actual.

- [ ] **Step 8: CI y commit**

```bash
git add -A && git commit -m "feat(ui): navegación por pestañas con barra inferior"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase1 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

Esperado: `success`; `MainTabTest` en verde.

---

### Task 4: Inicio

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt` (llamada)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `AppHeader`, `UsipipoCard`, `IconTile` (tarea 2); `HomeViewModel.uiState`.
- Produces: `HomeScreen(vm, onImport, onOpenConfigs)`.

- [ ] **Step 1: Strings**

```xml
    <string name="tagline_connected">Tu privacidad está protegida</string>
    <string name="protocol_wireguard">WireGuard</string>
```

- [ ] **Step 2: Cuerpo de `HomeScreen`**

Cambiar la firma a `HomeScreen(vm: HomeViewModel, onImport: () -> Unit, onOpenConfigs: () -> Unit)` y sustituir todo lo que hay dentro de `HomeBackground { … }` y el diálogo de borrado (el borrado se muda a Configuraciones). Se mantienen `permissionLauncher`, `messageText`, `onToggle`, `HomeBackground`, `GradientLayer` y `messageRes`.

```kotlin
    val selected = state.configs.firstOrNull { it.id == state.selectedId }

    HomeBackground(phase = phase, reduceMotion = reduceMotion) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppHeader()
                ConnectSection(
                    tunnel = state.tunnel,
                    enabled = selected != null,
                    onClick = { onToggle() },
                )
                if (selected == null) {
                    EmptyState(onImport = onImport)
                } else {
                    ProfileCard(config = selected, onClick = onOpenConfigs)
                }
            }
        }
    }
```

- [ ] **Step 3: `ConnectSection` con subtítulo y `ProfileCard`**

En `ConnectSection`, justo debajo del `Text` de `statusLabel`, añadir:

```kotlin
        if (tunnel is TunnelState.Connected) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.tagline_connected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
```

Nuevo composable:

```kotlin
@Composable
private fun ProfileCard(config: SavedConfig, onClick: () -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(R.drawable.ic_server)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.protocol_wireguard),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 4: Limpiar lo que ya no se usa**

Eliminar de `HomeScreen.kt`: `Header` (reemplazada por `AppHeader`), `ConfigsHeader`, `ConfigCard` (pasa a `ui/configs` en la tarea 5; **copiar su código antes de borrarlo**), el estado `pendingDelete` y su `AlertDialog`, y los imports que queden sin uso (`LazyColumn`, `items`, `SwipeToDismissBox…`, `Card…`, `Info`, `Delete`, `selectable`, `CustomAccessibilityAction`…). `EmptyState` y las previews se conservan; `PreviewVacio` sigue válida.

- [ ] **Step 5: Llamada en `UsipipoRoot`**

```kotlin
                    MainTab.Home -> HomeScreen(
                        vm = homeViewModel(container),
                        onImport = { tab = MainTab.Import },
                        onOpenConfigs = { tab = MainTab.Configs },
                    )
```

- [ ] **Step 6: Previews**

Añadir `PreviewInicioConectado` (claro y oscuro) que componga `ConnectSection(tunnel = Connected("a"), enabled = true, onClick = {})` y `ProfileCard(SavedConfig("a", "client-mowgli"), {})` sobre `HomeBackground(Flowing)`, con `reduceMotion = true`.

- [ ] **Step 7: CI y commit**

```bash
git add -A && git commit -m "feat(ui): inicio con tarjeta del perfil activo"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase1 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

---

### Task 5: Configuraciones con «Mis perfiles» y «Favoritos»

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/configs/ConfigsFilter.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/configs/ConfigCard.kt`
- Replace: `app/src/main/java/com/usipipo/vpn/ui/configs/ConfigsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/test/java/com/usipipo/vpn/ui/configs/ConfigsFilterTest.kt`

- [ ] **Step 1: Test que falla**

```kotlin
package com.usipipo.vpn.ui.configs

import com.usipipo.vpn.domain.SavedConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigsFilterTest {

    private val casa = SavedConfig("a", "Casa")
    private val trabajo = SavedConfig("b", "Trabajo", favorite = true)

    @Test
    fun `Mis perfiles muestra todos`() {
        assertEquals(listOf(casa, trabajo), listOf(casa, trabajo).forTab(ConfigsTab.Mine))
    }

    @Test
    fun `Favoritos muestra solo los marcados`() {
        assertEquals(listOf(trabajo), listOf(casa, trabajo).forTab(ConfigsTab.Favorites))
    }

    @Test
    fun `Favoritos sin ninguno marcado queda vacio`() {
        assertEquals(emptyList<SavedConfig>(), listOf(casa).forTab(ConfigsTab.Favorites))
    }
}
```

- [ ] **Step 2: `ConfigsFilter.kt`**

```kotlin
package com.usipipo.vpn.ui.configs

import com.usipipo.vpn.domain.SavedConfig

enum class ConfigsTab { Mine, Favorites }

fun List<SavedConfig>.forTab(tab: ConfigsTab): List<SavedConfig> =
    if (tab == ConfigsTab.Favorites) filter { it.favorite } else this
```

- [ ] **Step 3: Strings**

```xml
    <string name="tab_my_profiles">Mis perfiles</string>
    <string name="tab_favorites">Favoritos</string>
    <string name="add_config">Añadir configuración</string>
    <string name="favorites_empty_title">Aún no tienes favoritos</string>
    <string name="favorites_empty_body">Marca con la estrella los perfiles que más usas.</string>
    <string name="cd_favorite_add">Marcar %1$s como favorito</string>
    <string name="cd_favorite_remove">Quitar %1$s de favoritos</string>
```

- [ ] **Step 4: `ConfigCard.kt`**

Mover aquí el `ConfigCard` de `HomeScreen.kt` (guardado en la tarea 4, paso 4) con estos cambios: pasa a `internal`, su `Card` se sustituye por `UsipipoCard(selected = selected)`, el icono por `IconTile(R.drawable.ic_server)`, el nombre ocupa una `Column` con el protocolo debajo, y se añade la estrella al final. Se conservan el `SwipeToDismissBox`, la acción de accesibilidad de borrar, `selectable(role = RadioButton)` y el chip «Activo». Firma y estrella:

```kotlin
@Composable
internal fun ConfigCard(
    config: SavedConfig,
    selected: Boolean,
    selectEnabled: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    // … SwipeToDismissBox y fondo de borrado, como antes …
    // Contenido: UsipipoCard(selected = selected) { Row(…selectable…) {
    //     IconTile(R.drawable.ic_server)
    //     Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { nombre (bodyLarge) + protocolo (bodyMedium) }
    //     if (selected) { chip «Activo» como antes }
    //     IconButton(onClick = onToggleFavorite) { … } } }
}
```

```kotlin
IconButton(onClick = onToggleFavorite) {
    Icon(
        imageVector = if (config.favorite) Icons.Filled.Star else Icons.Outlined.Star,
        contentDescription = stringResource(
            if (config.favorite) R.string.cd_favorite_remove else R.string.cd_favorite_add,
            config.name,
        ),
        tint = if (config.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
```

- [ ] **Step 5: `ConfigsScreen.kt` (reemplaza el stub)**

```kotlin
@Composable
fun ConfigsScreen(vm: HomeViewModel, onImport: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(ConfigsTab.Mine) }
    var pendingDelete by remember { mutableStateOf<SavedConfig?>(null) }
    val visible = state.configs.forTab(tab)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nav_configs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = onImport) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_config))
                }
            }

            TabPills(selected = tab, onSelect = { tab = it })

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (visible.isEmpty()) {
                    item { EmptyList(tab) }
                } else {
                    items(visible, key = { it.id }) { config ->
                        ConfigCard(
                            config = config,
                            selected = config.id == state.selectedId,
                            selectEnabled = !state.tunnel.isActive,
                            onSelect = { vm.select(config.id) },
                            onToggleFavorite = { vm.toggleFavorite(config.id) },
                            onDeleteRequest = { pendingDelete = config },
                        )
                    }
                }
            }

            Button(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_config))
            }
        }
    }

    pendingDelete?.let { config ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body, config.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(config.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun TabPills(selected: ConfigsTab, onSelect: (ConfigsTab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceVariant)
            .padding(4.dp),
    ) {
        ConfigsTab.entries.forEach { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) scheme.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(item) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (item == ConfigsTab.Mine) R.string.tab_my_profiles else R.string.tab_favorites,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyList(tab: ConfigsTab) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(if (tab == ConfigsTab.Favorites) R.string.favorites_empty_title else R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(if (tab == ConfigsTab.Favorites) R.string.favorites_empty_body else R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
```

- [ ] **Step 6: Previews**

`PreviewConfigsConFavorito` (claro y oscuro): dos `ConfigCard` (una con `selected = true`, otra con `favorite = true`) sobre `Surface`.

- [ ] **Step 7: CI y commit**

```bash
git add -A && git commit -m "feat(ui): configuraciones con pestañas y favoritos"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase1 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

Esperado: `success`; `ConfigsFilterTest` (3) en verde.

---

### Task 6: Importar con dos tarjetas

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/importer/ImportScreen.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt` (llamada)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `ImportScreen(vm, onDone)`. `ImportViewModel` no cambia.

- [ ] **Step 1: Strings**

```xml
    <string name="import_qr_subtitle">Importa una configuración desde un código QR.</string>
    <string name="import_file_title">Importar archivo</string>
    <string name="import_file_subtitle">Selecciona un archivo .conf de tu dispositivo.</string>
    <string name="import_footer_title">Rápido, fácil y seguro</string>
    <string name="import_footer_body">Tienes dos formas de importar tus configuraciones VPN.</string>
```

Se elimina `pick_file` (sustituido por `import_file_title`).

- [ ] **Step 2: Estructura de la pantalla**

Cambiar la firma a `ImportScreen(vm: ImportViewModel, onDone: () -> Unit)`. Sustituir `leave()` y el `LaunchedEffect`:

```kotlin
    LaunchedEffect(ui.done) {
        if (ui.done) {
            vm.reset()
            onDone()
        }
    }

    // Atrás en el paso «Nombre» cancela el paso; en «Elegir», lo gestiona la navegación.
    BackHandler(enabled = ui.stage == ImportStage.Name) { vm.reset() }
```

Quitar la fila con la flecha atrás: ahora es una pestaña. La cabecera queda:

```kotlin
            Text(
                text = stringResource(R.string.import_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
```

- [ ] **Step 3: Paso «Elegir» con tarjetas**

Sustituir los dos botones por:

```kotlin
                ImportStage.Choose -> {
                    ImportOptionCard(
                        icon = R.drawable.ic_qr,
                        title = R.string.scan_qr,
                        subtitle = R.string.import_qr_subtitle,
                        onClick = {
                            scanner.launch(
                                ScanOptions()
                                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    .setPrompt(scanPrompt)
                                    .setBeepEnabled(false)
                                    .setOrientationLocked(false)
                                    .setCaptureActivity(PortraitCaptureActivity::class.java),
                            )
                        },
                    )
                    ImportOptionCard(
                        icon = R.drawable.ic_import,
                        title = R.string.import_file_title,
                        subtitle = R.string.import_file_subtitle,
                        // Los .conf no tienen un tipo MIME fiable: se permite cualquier archivo.
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                    )

                    ui.error?.let { error ->
                        Text(
                            text = stringResource(errorRes(error)),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.import_footer_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(R.string.import_footer_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
```

```kotlin
@Composable
private fun ImportOptionCard(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    onClick: () -> Unit,
) {
    UsipipoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 4: Paso «Nombre» con Cancelar**

Tras el botón «Guardar», añadir:

```kotlin
                    TextButton(
                        onClick = vm::reset,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.cancel)) }
```

- [ ] **Step 5: Llamada en `UsipipoRoot`**

```kotlin
                    MainTab.Import -> ImportScreen(
                        vm = importViewModel(container),
                        onDone = { tab = MainTab.Home },
                    )
```

Tras guardar, se vuelve a Inicio con el perfil nuevo ya seleccionado (el almacén lo deja como «última usada»).

- [ ] **Step 6: CI y commit**

```bash
git add -A && git commit -m "feat(ui): importar con dos tarjetas y cancelar en el paso de nombre"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase1 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

---

### Task 7: Diagnóstico con «Acerca de» y actualizaciones

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/about/AboutSection.kt`
- Delete: `app/src/main/java/com/usipipo/vpn/ui/about/AboutScreen.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `AboutSection(onCheckUpdates: suspend () -> UpdateStatus, snackbar: SnackbarHostState)`; `DiagnosticsScreen(vm, onCheckUpdates)`.

- [ ] **Step 1: `AboutSection.kt`**

Se parte de `AboutScreen.kt`: se conserva todo su contenido salvo el `Scaffold`, la fila de «atrás», la tarjeta «Avanzado» (con su botón a Diagnóstico) y el `SnackbarHostState` propio, que ahora llega por parámetro. El logo pasa a `AppLogo(64.dp)`. Las funciones privadas `SectionHeader`, `openSupportEmail` y `appVersion` se mueven tal cual.

```kotlin
@Composable
fun AboutSection(
    onCheckUpdates: suspend () -> UpdateStatus,
    snackbar: SnackbarHostState,
) {
    val context = LocalContext.current
    val version = remember { appVersion(context) }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    val upToDateMessage = stringResource(R.string.update_status_ok)
    val postponedMessage = stringResource(R.string.update_status_postponed)
    val errorMessage = stringResource(R.string.update_status_error)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(text = stringResource(R.string.about_title))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo(size = 64.dp)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.version_label, version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Tarjeta «Soporte» y tarjeta «Actualizaciones»: el mismo código de AboutScreen,
        // con UsipipoCard en lugar de Card, y `scope.launch { … snackbar.showSnackbar(message) }`
        // usando el `snackbar` recibido.
    }
}
```

- [ ] **Step 2: Integrarlo en `DiagnosticsScreen`**

- Firma: `DiagnosticsScreen(vm: DiagnosticsViewModel, onCheckUpdates: suspend () -> UpdateStatus)`.
- Quitar la fila con `IconButton` de atrás; dejar solo `Text(stringResource(R.string.diag_title), …)` como título.
- Localizar el final de la `Column` con `verticalScroll` (`view` de `DiagnosticsScreen.kt` líneas 135–210) y añadir como **último hijo**:

```kotlin
            AboutSection(onCheckUpdates = onCheckUpdates, snackbar = snackbar)
```

- [ ] **Step 3: Llamada en `UsipipoRoot`**

```kotlin
                    MainTab.Diagnostics -> DiagnosticsScreen(
                        vm = diagnosticsViewModel(container),
                        onCheckUpdates = {
                            // La comprobación manual siempre vuelve a mostrar la ventana si hay release.
                            handledUpdate = null
                            container.checkForUpdates(force = true)
                        },
                    )
```

En `HomeScreen` ya no existe `onAbout`: quitar el argumento provisional de la tarea 3.

- [ ] **Step 4: Strings sobrantes**

Eliminar de `strings.xml`: `pick_file`, `cd_about`, `diag_open`, `about_advanced_title` (ya sin uso). Comprobar con `grep -rn "R.string.<nombre>" app/src` antes de borrar cada una.

- [ ] **Step 5: CI y commit**

```bash
git rm app/src/main/java/com/usipipo/vpn/ui/about/AboutScreen.kt
git add -A && git commit -m "feat(ui): acerca de y actualizaciones dentro de diagnóstico"
git push && gh workflow run "Android CI - Build APK" --ref feat/mockup-fase1
sleep 15 && gh run watch $(gh run list --workflow "Android CI - Build APK" --branch feat/mockup-fase1 --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

---

### Task 8: Cierre de la fase 1

**Files:**
- Modify: `README.md`

- [ ] **Step 1: README**

Actualizar la sección «Estructura» (`ui/nav`, `ui/configs`, `ui/components` con `UsipipoCard`, `IconTile`, `AppLogo`, `UsipipoBottomBar`), la línea de navegación («4 pestañas») y añadir `docs/plans/2026-10-18-usipipo-vpn-mockup-design.md` a la lista de diseños aprobados.

- [ ] **Step 2: Checklist manual en el teléfono (APK debug del run verde)**

1. Instalar la APK; abrir en tema claro y en tema oscuro; comparar con `docs/design/mockup-ia.png`.
2. Con 3 perfiles: elegir uno en Configuraciones, marcarlo favorito, ver la pestaña Favoritos, desmarcarlo y ver el mensaje de vacío.
3. Deslizar para borrar un perfil y confirmar; borrar el perfil activo estando conectado (debe desconectar).
4. Conectar desde Inicio: fondo, halo, texto «Tu privacidad está protegida» y tarjeta del perfil.
5. Tocar la tarjeta del perfil: abre Configuraciones.
6. Importar por QR y por archivo; en el paso «Nombre» abrir el teclado (la barra desaparece) y pulsar Atrás (cancela el paso).
7. Diagnóstico: ver el registro, buscar actualizaciones desde «Acerca de» y probar «Escribir a soporte».
8. Rotar la pantalla en cada pestaña (se conserva pestaña y filtro).
9. Navegación por gestos y por 3 botones: sin hueco doble sobre la barra.
10. «Reducir animaciones» activado: sin transición entre pestañas.
11. Prueba en un teléfono de gama baja y otro de gama media: fluidez al cambiar de pestaña y con el halo.
12. Confirmar que el icono de la app es la «u» blanca sobre azul y que la cabecera usa el mismo logo.

- [ ] **Step 3: Merge**

```bash
git add -A && git commit -m "docs: actualizar README con la estructura de la fase 1"
git push && gh pr create --base main --head feat/mockup-fase1 --title "feat: fase 1 del mockup (estructura y estilo)" --body "Ver docs/superpowers/plans/2026-10-18-usipipo-vpn-mockup-fase1-plan.md"
```

Tras revisar el PR y la checklist, hacer merge. La fase 2 parte de `main`.
