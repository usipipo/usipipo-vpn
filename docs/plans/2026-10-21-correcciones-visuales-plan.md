# uSipipo VPN: correcciones visuales tras el rediseño del mockup — Plan de implementación

> **Para agentes workers:** SUB-SKILL OBLIGATORIA: usa `superpowers:subagent-driven-development`
> (recomendado) o `superpowers:executing-plans` para implementar este plan tarea por tarea.
> Los pasos usan casillas (`- [ ]`) para seguimiento.

**Objetivo:** que la APK se parezca al mockup `docs/design/` en cuatro puntos: barra inferior
visible en las cuatro pestañas, icono del launcher sin zoom ni recorte, set de iconos propio sin
fugas ni trazos rotos, y mapa de puntos en el fondo del Inicio.

**Arquitectura:** cuatro cambios de capa visual, independientes entre sí y en cuatro ramas
separadas. (1) La causa raíz de la barra invisible es de layout: el `AnimatedContent` de
`UsipipoRoot` vive en una `Column` sin `weight(1f)`, así que cada pantalla, que usa
`fillMaxSize()`, se come todo el alto y deja la barra con altura cero. (2) Los iconos pasan a un
set propio en `res/drawable`, con trazo uniforme, sustituyendo a los de Material y reparando los
dos rotos. (3) El icono adaptativo se rehace con la geometría medida sobre
`public/image/usipipo_logo.jpg`, que es la causa del zoom y el recorte. (4) El mapa es una
máscara de tierra de 72×36 celdas empaquetada en hexadecimal, dibujada con `Canvas` sobre el
degradado del Inicio.

**Stack:** Kotlin 2.3.21, Jetpack Compose (BOM 2026.08.00), Material 3, vector drawables XML,
JUnit 4 sobre JVM.

**Spec:** `docs/plans/2026-10-21-correcciones-visuales-design.md` (aprobado). Este plan ejecuta
esa spec; donde la spec y el mockup discrepan, manda la spec (ver «Divergencias aceptadas»).

---

## Restricciones globales

- Versiones y toolchain no se tocan: AGP 9.3.3, Kotlin 2.3.21, compileSdk 37, minSdk 26,
  targetSdk 36, Java 17. No se añade ninguna dependencia.
- La lógica del túnel (`tunnel/`), los datos, la geolocalización, el ping y el diagnóstico quedan
  intactos. Solo se toca la capa visual.
- Todo icono nuevo o rehacido vive en `app/src/main/res/drawable/` con prefijo `ic_` y nombre
  `snake_case`.
- Los colores nuevos se declaran como constantes ARGB en `AtmospherePalette` (para poder testear
  el contraste en JVM) y nunca se hardcodean en un composable.
- Commits en español, formato convencional, minúsculas, un cambio lógico por commit.
- Cada fase vive en su rama `feat/mockup-fase<N>` creada desde `main`. Nada se pushea a `main`.
- Cada pantalla y cada icono nuevo lleva `@Preview` en claro y en oscuro.
- No se usa `!!` sin justificación, no hay imports comodín, y todo import que quede sin uso se
  borra en el mismo commit.

## Protocolo de verificación (no hay JDK en la máquina)

En este entorno no hay JDK (`java` no existe), así que `./gradlew testDebugUnitTest` **no** se
puede ejecutar en local. La verificación es el CI de GitHub Actions, que corre
`testDebugUnitTest`, `assembleDebug` y `assembleRelease`.

```bash
git push -u origin feat/mockup-fase<N>
gh workflow run android-ci.yml --ref feat/mockup-fase<N>
gh run watch <run-id>          # id devuelto por el paso anterior
```

Un run verde es la prueba. Un run rojo se lee con `gh run view <run-id> --log-failed`.

## Divergencias aceptadas (spec > mockup)

Estas son divergencias deliberadas entre el mockup y la spec aprobada. No se "corrigen".

| Mockup | Decisión | Motivo |
|---|---|---|
| Logotipo de cabecera = glifo tipo power (⏻) cyan con glow, sin cuadro azul | La spec dice: «`ic_logo_u` (cabecera de la app) se genera a partir del mismo trazado» que `usipipo_logo.jpg`. Se rehace el glifo; se conserva el cuadro azul redondeado y el tinte blanco de `AppLogo`. | La spec es la fuente de verdad; cambiar el contenedor del logo es rediseño, no corrección. |
| Pestaña activa con icono relleno | Set en contorno, activo en azul e inactivo en gris azulado, como dice la spec. | La spec fija el estilo del set. |
| Barra azul noche solo en tema oscuro | Barra azul noche en oscuro y superficie clara equivalente en claro. | El mockup solo documenta el tema oscuro; el claro no puede quedar con fondo de la marca. |
| El mapa es un globo | Mapa equirectangular de 72×36. | La spec fija las dimensiones de la máscara. |

## Foco de revisión

Cinco entradas o condiciones que la spec implica y que ningún test del plan ejercita de forma
directa. Cada una tiene su prueba asignada a la tarea que posee el código. Las dos primeras no
son automatizables sin `compose-ui-test` (fuera del alcance de una corrección visual: obligaría a
añadir una dependencia de test nueva), así que se cierran con la comprobación manual del
teléfono de la sección de cierre.

1. **La barra aparece en las cuatro pestañas.** Con el contenido de las pestañas ocupando el
   espacio sobrante, la barra no puede quedar con altura cero en ninguna de ellas, y cada
   pestaña debe tener su icono y su etiqueta.
   → Tarea 2, `MainTabTest`: cada `MainTab` tiene icono y etiqueta propios y distintos.
2. **Teclado abierto (IME).** Con el teclado visible la barra se oculta; el contenido de la
   pestaña no debe quedar aplastado a una franja de un píxel ni tapado por el teclado.
   → Verificación manual, punto 3 del checklist del teléfono.
3. **Tema claro.** La barra debe seguir siendo legible con el icono activo y el inactivo por
   encima del umbral de contraste, no solo en el tema oscuro.
   → Tarea 1, `PaletteContrastTest`: contraste en los dos temas.
4. **Máscaras del icono adaptativo.** Las capas de personalización recortan fuera de la zona
   segura: círculo, cuadrado redondeado y «squircle» deben mostrar la «u» completa, con el guion
   bajo dentro.
   → Tarea 3, `LauncherIconSafeZoneTest`.
5. **Mapa sobre el contenido.** Los puntos del mapa no pueden quedar encima del texto de las
   tarjetas ni bajar del contraste del degradado; y una máscara corrupta no puede dejar el
   Inicio en negro.
   → Tarea 4, `WorldMapMaskTest` (tierra y mar presentes, tamaño correcto) y verificación
   manual, punto 6 del checklist del teléfono.

---

### Tarea 1: la barra inferior vuelve a verse

El síntoma principal del usuario: la barra inferior no aparece. La causa está en
`UsipipoRoot.kt`: el `AnimatedContent` de las pestañas está dentro de una `Column` sin
`weight(1f)`.

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt` (`AnimatedContent`, ~línea 105)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/theme/Atmosphere.kt` (constantes de la barra)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/theme/Theme.kt` (`BottomBarColors` y su `CompositionLocal`)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/components/UsipipoBottomBar.kt`
- Test: `app/src/test/java/com/usipipo/vpn/ui/theme/PaletteContrastTest.kt`

**Interfaces:**
- Consumes: nada de tareas anteriores.
- Produces: `AtmospherePalette.BOTTOM_BAR_DARK: Long`, `AtmospherePalette.BOTTOM_BAR_LIGHT: Long`,
  `AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK: Long`, `AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT: Long`,
  `AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK: Long`, `AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT: Long`,
  y en `com.usipipo.vpn.ui.theme` la clase `BottomBarColors(container, active, inactive)` con su
  `LocalBottomBarColors`. La Tarea 2 los usa para el tinte de los iconos de la barra.

- [ ] **Step 1: escribe el test que falla**

En `PaletteContrastTest.kt` añade:

```kotlin
@Test
fun `la barra inferior mantiene el icono activo por encima de 4 punto 5`() {
    assertTrue(
        contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >= 4.5,
    )
    assertTrue(
        contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >= 4.5,
    )
}

@Test
fun `la barra inferior mantiene el icono inactivo por encima de 3 punto 1`() {
    // Los iconos son elementos no textuales: la WCAG pide 3:1 para ellos.
    assertTrue(
        contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >= 3.0,
    )
    assertTrue(
        contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >= 3.0,
    )
}

@Test
fun `la etiqueta inactiva de la barra se lee como texto`() {
    assertTrue(
        contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >= 4.5,
    )
    assertTrue(
        contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >= 4.5,
    )
}

@Test
fun `la pestana activa destaca mas que la inactiva`() {
    // Si la inactiva contrasta mas que la activa, el estado seleccionado es el mas
    // apagado y la barra no dice que pestana esta activa.
    assertTrue(
        contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >
            contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK),
    )
    assertTrue(
        contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >
            contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT),
    )
}

@Test
fun `la barra inferior no pinta igual la pestana activa y la inactiva`() {
    // La distincion activa/inactiva es de tono, no solo de luminancia.
    assertTrue(
        AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK != AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK,
    )
    assertTrue(
        AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT != AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT,
    )
}
```

Los iconos de la barra son elementos no textuales: la WCAG pide 3:1 para ellos. Se exige 4.5:1
para el activo (además es el foco de la pestaña), para las etiquetas y para la inactiva, que
también es texto. El test que de verdad protege el diseño es el tercero: la pestaña activa tiene
que contrastar **más** que la inactiva contra el fondo de la barra. Con el set de iconos en
contorno —que es lo que fija la spec— el estado activo no se puede marcar por relleno, así que
lo marca el color.

- [ ] **Step 2: ejecuta el test y comprueba que falla**

Sin JDK local, el fallo se comprueba en el CI (protocolo de verificación). El test falla porque
`AtmospherePalette` no tiene esas constantes: error de compilación `unresolved reference`.

- [ ] **Step 3: declara las constantes de la barra**

En `AtmospherePalette` (`app/src/main/java/com/usipipo/vpn/ui/theme/Atmosphere.kt`), junto al
bloque «Esquema oscuro», añade exactamente:

```kotlin
// Barra inferior: azul noche del mockup en oscuro, superficie clara equivalente en claro.
const val BOTTOM_BAR_DARK: Long = 0xFF000A25
const val BOTTOM_BAR_LIGHT: Long = 0xFFE8F1FD
// La pestaña activa usa el cian del mockup; la inactiva, un gris azulado mas apagado
// para que el estado se lea de un vistazo.
const val BOTTOM_BAR_ACTIVE_DARK: Long = 0xFF06D3FE
const val BOTTOM_BAR_ACTIVE_LABEL_DARK: Long = 0xFF2DA3D3
const val BOTTOM_BAR_INACTIVE_DARK: Long = 0xFF627E93
const val BOTTOM_BAR_ACTIVE_LIGHT: Long = 0xFF0B57C9
const val BOTTOM_BAR_ACTIVE_LABEL_LIGHT: Long = 0xFF0B57C9
const val BOTTOM_BAR_INACTIVE_LIGHT: Long = 0xFF5B6E8C
```

Valores tomados del mockup, no inventados:

| Color | Origen | Contraste sobre su fondo |
|---|---|---|
| Fondo de la barra `#000A25` | muestreado del mockup (`rgb(0,10,37)`) | — |
| Icono activo `#06D3FE` | muestreado del mockup (`rgb(6,211,254)`, trazo del icono Inicio) | 10,99:1 |
| Etiqueta activa `#2DA3D3` | muestreado del mockup (`rgb(45,163,211)`) | 6,81:1 |
| Icono inactivo `#627E93` | gris azulado apagado a propósito: el del mockup (`#95BEF6`, 10,25:1) brilla mas que el activo, lo que hacia que el estado seleccionado pareciera el mas apagado | 4,60:1 |
| Fondo claro `#E8F1FD` | superficie clara del tema | — |
| Activo claro `#0B57C9` | azul saturado sobre superficie clara | 5,72:1 |
| Inactivo claro `#5B6E8C` | gris azulado sobre superficie clara | 4,55:1 |

El invariant que hay que respetar es que **la pestaña activa contrasta mas que la inactiva**
contra el fondo de la barra, en los dos temas. Con el set de iconos en contorno (que es lo que
pide la spec) el estado activo no se puede marcar por relleno, asi que lo marca el color.

- [ ] **Step 4: arregla el layout: el contenido de las pestañas cede el espacio sobrante**

En `UsipipoRoot.kt`, el `AnimatedContent` recibe `Modifier.weight(1f)`. El `Column` que lo
contiene sigue siendo `Modifier.fillMaxSize().imePadding()`; la barra se queda fuera del
`weight`, abajo del todo.

```kotlin
AnimatedContent(
    targetState = tab,
    modifier = Modifier.weight(1f),
    transitionSpec = { ... },
    label = "tab",
) { target -> ... }
```

`Modifier.weight(1f)` solo existe dentro de `ColumnScope`, y el `AnimatedContent` está
directamente dentro de la `Column`, así que compila sin receptor extra.

- [ ] **Step 5: dale a la barra el color del mockup y el tinte de la pestaña**

En `UsipipoBottomBar.kt`: el `NavigationBar` conserva `windowInsets` por defecto
(`NavigationBarDefaults.windowInsets`), que ya reserva la barra de gestos del sistema; el fondo
del `NavigationBar` se pinta con el color de la barra y llega hasta el borde inferior. En esta
tarea los cuatro ítems se escriben uno a uno (los iconos de Material que hay hoy); la Tarea 2
los convierte en un bucle sobre `MainTab.entries`.

Los colores no van en el esquema de Material (la barra lleva su propio azul de marca), sino en
un `CompositionLocal` propio, como ya hacen `LocalAtmosphere` y `LocalStatusColors`:

```kotlin
// Theme.kt
@Immutable
data class BottomBarColors(
    val container: Color,
    val active: Color,
    val activeLabel: Color,
    val inactive: Color,
)

private val LightBottomBar = BottomBarColors(
    container = Color(AtmospherePalette.BOTTOM_BAR_LIGHT),
    active = Color(AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT),
    activeLabel = Color(AtmospherePalette.BOTTOM_BAR_ACTIVE_LABEL_LIGHT),
    inactive = Color(AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT),
)

private val DarkBottomBar = BottomBarColors(
    container = Color(AtmospherePalette.BOTTOM_BAR_DARK),
    active = Color(AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK),
    activeLabel = Color(AtmospherePalette.BOTTOM_BAR_ACTIVE_LABEL_DARK),
    inactive = Color(AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK),
)

val LocalBottomBarColors = staticCompositionLocalOf { LightBottomBar }
```

y en `UsipipoTheme` se añade `LocalBottomBarColors provides if (darkTheme) DarkBottomBar else
LightBottomBar`.

En esta versión de Material 3 el `NavigationBar` **no** tiene parámetro `colors`: los colores
del ítem van en `NavigationBarItemDefaults.colors(...)`, que se pasa a cada `NavigationBarItem`.
El `indicatorColor = Color.Transparent` elimina la píldora de fondo, que no existe en el mockup.

Los cuatro ítems se construyen con un composable privado, para no repetir ocho condicionales
de color:

```kotlin
@Composable
private fun BottomBarItem(
    tab: MainTab,
    current: MainTab,
    colors: BottomBarColors,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    val selected = tab == current
    val iconColor = if (selected) colors.active else colors.inactive
    val labelColor = if (selected) colors.activeLabel else colors.inactive
    NavigationBarItem(
        icon = {
            CompositionLocalProvider(LocalContentColor provides iconColor) { icon() }
        },
        label = { Text(text = label, color = labelColor) },
        selected = selected,
        onClick = onClick,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.active,
            selectedTextColor = colors.activeLabel,
            indicatorColor = Color.Transparent,
            unselectedIconColor = colors.inactive,
            unselectedTextColor = colors.inactive,
        ),
    )
}
```

El tinte del icono y el color de la etiqueta se fijan además de pasarlos a Material, para que
el estado activo no dependa de cómo los envuelva. El icono recibe el tinte por
`LocalContentColor`, que es el valor por defecto de `Icon`.

Los colores salen de `AtmospherePalette` para que el test de contraste los pueda verificar.

- [ ] **Step 6: preview de la barra en claro y en oscuro**

Añade a `UsipipoBottomBar.kt`:

```kotlin
@Preview(name = "Barra inferior")
@Preview(name = "Barra inferior oscura", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUsipipoBottomBar() {
    UsipipoTheme {
        UsipipoBottomBar(tab = MainTab.Home, onTabClick = {})
    }
}
```

- [ ] **Step 7: commit**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt \
        app/src/main/java/com/usipipo/vpn/ui/components/UsipipoBottomBar.kt \
        app/src/main/java/com/usipipo/vpn/ui/theme/Atmosphere.kt \
        app/src/main/java/com/usipipo/vpn/ui/theme/Theme.kt \
        app/src/test/java/com/usipipo/vpn/ui/theme/PaletteContrastTest.kt
git commit -m "fix(ui): hacer visible la barra inferior de las cuatro pestañas

El AnimatedContent de las pestañas no tenia weight(1f) dentro de la Column de
UsipipoRoot, asi que cada pantalla, que usa fillMaxSize(), se comia todo el
alto y la barra inferior se quedaba con altura cero.

La barra toma ahora el azul noche del mockup, con la pestana activa en el cian
del mockup y las inactivas en gris azulado. Los colores viven en un
CompositionLocal propio (BottomBarColors) porque la barra lleva su azul de
marca y no encaja en el esquema de Material; el contraste esta verificado en el
test de paleta, incluido que la pestana activa destaque mas que la inactiva."
```

- [ ] **Step 8: verificación por CI**

```bash
git push -u origin feat/mockup-fase1
gh workflow run android-ci.yml --ref feat/mockup-fase1
```

---

### Tarea 2: set de iconos propio, en contorno y con trazo uniforme

Los cuatro iconos de Material (`Home`, `Settings`, `Build`, `Add`) se sustituyen por vectores
propios, y los dos rotos (`ic_arrow_right` dibuja una flecha hacia la izquierda, `ic_add` tiene
un trazo malformado) se rehacen. El resto del set pasa a un único estilo de contorno con grosor
de trazo `1.75`.

**Files:**
- Create: `app/src/main/res/drawable/ic_nav_home.xml`
- Create: `app/src/main/res/drawable/ic_nav_configs.xml`
- Create: `app/src/main/res/drawable/ic_nav_import.xml`
- Create: `app/src/main/res/drawable/ic_nav_diagnostics.xml`
- Create: `app/src/main/res/drawable/ic_download.xml`
- Create: `app/src/main/res/drawable/ic_upload.xml`
- Rewrite: `ic_arrow_right.xml`, `ic_arrow_back.xml`, `ic_add.xml`, `ic_star.xml`,
  `ic_star_filled.xml`, `ic_edit.xml`, `ic_delete.xml`, `ic_qr.xml`, `ic_clock.xml`,
  `ic_server.xml`, `ic_import.xml`
- Rewrite (extensión del alcance): `ic_power.xml`, `ic_update.xml`, `ic_shield_check.xml`,
  `ic_empty_shield.xml`
- Delete: `app/src/main/res/drawable/ic_arrow_down.xml`, `app/src/main/res/drawable/ic_arrow_up.xml`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/nav/MainTab.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/components/UsipipoBottomBar.kt`
- Create: `app/src/main/java/com/usipipo/vpn/ui/components/IconGallery.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt` (el `Icons.Default.Add` del estado vacío)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/ConnectionStats.kt` (descarga/subida)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/details/DetailsScreen.kt` (descarga/subida)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/components/StatTile.kt` (preview)
- Modify: `app/build.gradle.kts` y `gradle/libs.versions.toml` (se va `material-icons-core`)
- Test: `app/src/test/java/com/usipipo/vpn/ui/nav/MainTabTest.kt`
- Test: `app/src/test/java/com/usipipo/vpn/ui/IconSetStyleTest.kt` (nuevo)

**Interfaces:**
- Consumes: `AtmospherePalette.BOTTOM_BAR_*` de la Tarea 1.
- Produces: `MainTab.iconRes: Int` (`@DrawableRes`) y `MainTab.labelRes: Int` (`@StringRes`),
  ambos `internal`, en `com.usipipo.vpn.ui.nav`.

> Nota de nomenclatura: `ic_arrow_down`/`ic_arrow_up` se renombran a `ic_download`/`ic_upload`
> porque pasan a dibujar una flecha con bandeja, no una flecha suelta. Los nombres revelaban el
> dibujo anterior, no la función.

- [ ] **Step 1: escribe el test que falla**

In `MainTabTest.kt` añade:

```kotlin
@Test
fun `cada pestana tiene su propio icono del set propio`() {
    val iconos = MainTab.entries.map { it.iconRes }
    assertEquals(MainTab.entries.size, iconos.toSet().size)
    assertTrue("alguna pestana apunta a un recurso inexistente", iconos.all { it != 0 })
}

@Test
fun `cada pestana tiene su propia etiqueta`() {
    val etiquetas = MainTab.entries.map { it.labelRes }
    assertEquals(MainTab.entries.size, etiquetas.toSet().size)
    assertEquals(R.string.nav_home, MainTab.Home.labelRes)
    assertEquals(R.string.nav_configs, MainTab.Configs.labelRes)
    assertEquals(R.string.nav_import, MainTab.Import.labelRes)
    assertEquals(R.string.nav_diagnostics, MainTab.Diagnostics.labelRes)
}
```

El icono activo de la barra sale de aquí: cuatro pestañas con cuatro iconos distintos es lo que
impide que dos pestañas compartan dibujo. Que el recurso **exista** lo garantiza el
compilador: si `MainTab.iconRes` apunta a un `R.drawable` inexistente, el proyecto no compila.
Los identificadores de `R` son enteros reales en las pruebas unitarias de un módulo de
aplicación, así que la comparación de igualdad es válida.

- [ ] **Step 2: ejecuta el test y comprueba que falla**

Falla en el CI: `unresolved reference: iconRes`.

- [ ] **Step 3: crea los cuatro iconos de la barra**

Cada vector: `android:width="24dp"`, `android:height="24dp"`, `viewportWidth/Height="24"`,
`android:fillColor="#00000000"`, `android:strokeColor="#FFFFFFFF"`,
`android:strokeWidth="1.75"`, `android:strokeLineCap="round"`,
`android:strokeLineJoin="round"`. El blanco es el color base; Compose aplica el tinte.

`ic_nav_home.xml` (casa con arco de puerta):

```xml
<path android:pathData="M3.8,10.7 L12,4.1 L20.2,10.7"
    android:fillColor="#00000000" android:strokeColor="#FFFFFFFF"
    android:strokeWidth="1.75" android:strokeLineCap="round" android:strokeLineJoin="round" />
<path android:pathData="M6.1,9.2 L6.1,19.6 L17.9,19.6 L17.9,9.2" ... />
<path android:pathData="M9.9,19.6 L9.9,15.5 A2.1,2.1 0 0 1 14.1,15.5 L14.1,19.6" ... />
```

`ic_nav_configs.xml` (roseta de 8 lóbulos con anillo central, como el engranado redondeado del
mockup). La roseta es la curva polar `r(θ) = 7.4 + 1.9·cos(8θ)` muestreada en 96 puntos:

```xml
<path android:pathData="M9.55,7.35 L10.39,5.94 L11.85,5.16 L13.51,5.11 L15.05,5.77 L15.96,6.97 L16.23,8.4 L15.84,9.73 L16.05,11.16 L17.02,12.29 L17.32,13.74 L16.71,15.0 L15.48,15.72 L14.16,15.72 L12.93,16.24 L12.28,17.24 L10.84,17.34 L9.58,16.64 L8.85,15.44 L8.58,14.01 L8.97,12.68 L8.76,11.25 L7.79,10.12 L7.49,8.67 L8.1,7.41 L9.55,7.35 Z" ... />
<path android:pathData="M10.1,12 A1.9,1.9 0 1 0 13.9,12 A1.9,1.9 0 1 0 10.1,12" ... />
```

> La roseta se genera por fórmula, no a ojo: `r(θ) = 7.4 + 1.9·cos(8θ)` en 96 muestras. Si
> hace falta ajustarla, se regenera el `pathData` con esa fórmula; no se edita a mano.

`ic_nav_import.xml` (documento con esquina doblada y flecha hacia arriba):

```xml
<path android:pathData="M6.2,3.6 L13.8,3.6 L18.4,8.2 L18.4,20.4 L6.2,20.4 Z" ... />
<path android:pathData="M13.8,3.6 L13.8,8.2 L18.4,8.2" ... />
<path android:pathData="M12.3,17 L12.3,11.2" ... />
<path android:pathData="M9.5,14 L12.3,11.2 L15.1,14" ... />
```

`ic_nav_diagnostics.xml` (la misma roseta más pequeña con una herramienta vertical, como en el
mockup):

```xml
<path android:pathData="M10.13,6.55 L11.1,5.06 L12.66,4.29 L14.33,4.4 L15.79,5.21 L16.57,6.51 L16.72,7.96 L16.22,9.24 L16.33,10.65 L17.21,11.75 L17.41,13.19 L16.7,14.38 L15.41,15.0 L14.09,14.89 L12.89,15.32 L12.3,16.29 L10.9,16.28 L9.7,15.48 L9.07,14.2 L9.02,12.72 L9.62,11.48 L9.61,10.06 L8.83,8.9 L8.74,7.44 L9.55,6.55 Z" ... />
<path android:pathData="M12,4.4 L12,19.6" ... />
```

- [ ] **Step 4: rehaz los iconos de acciones**

Todos con el mismo estilo (24×24, trazo 1.75, tapas y uniones redondeadas):

| Recurso | Contenido |
|---|---|
| `ic_arrow_right` | chevron «>»: `M9.5,5 L16,12 L9.5,19` |
| `ic_arrow_back` | flecha «←»: `M20,12 L4.6,12` y `M10.6,6 L4.6,12 L10.6,18` |
| `ic_add` | «+»: `M12,5 L12,19` y `M5,12 L19,12` |
| `ic_star` | estrella de 5 puntas en contorno: `M12,3.2 L14.62,9.06 L21,9.9 L16.2,14.4 L17.6,20.8 L12,17.6 L6.4,20.8 L7.8,14.4 L3,9.9 L9.38,9.06 Z` |
| `ic_star_filled` | la misma estrella con `android:fillColor="#FFFFFFFF"` y sin trazo |
| `ic_edit` | lápiz: `M4,20 L4.8,16.3 L15.9,5.2 L18.8,8.1 L7.7,19.2 Z` y `M14.35,6.75 L17.25,9.65` |
| `ic_delete` | papelera: `M4.2,6.4 L19.8,6.4`, `M9.4,6.4 L9.4,4.9 A1.4,1.4 0 0 1 10.8,3.5 L13.2,3.5 A1.4,1.4 0 0 1 14.6,4.9 L14.6,6.4`, `M5.9,6.4 L6.9,19.3 A1.7,1.7 0 0 0 8.6,20.9 L15.4,20.9 A1.7,1.7 0 0 0 17.1,19.3 L18.1,6.4`, `M10,10.2 L10,17.4`, `M14,10.2 L14,17.4` |
| `ic_qr` | escáner: esquinas `M4,9.2 L4,6.2 A2.2,2.2 0 0 1 6.2,4 L9.2,4`, `M14.8,4 L17.8,4 A2.2,2.2 0 0 1 20,6.2 L20,9.2`, `M20,14.8 L20,17.8 A2.2,2.2 0 0 1 17.8,20 L14.8,20`, `M9.2,20 L6.2,20 A2.2,2.2 0 0 1 4,17.8 L4,14.8`; dentro `M7.6,7.6 L11,7.6 L11,11 L7.6,11 Z`, `M13.6,7.6 L16.4,7.6 L16.4,10.4 L13.6,10.4 Z`, `M7.6,13.6 L10.4,13.6 L10.4,16.4 L7.6,16.4 Z`, `M13.8,13.8 L16.2,16.2`, `M16.2,13.8 L13.8,16.2` |
| `ic_clock` | reloj: `M3.2,12 A8.8,8.8 0 1 0 20.8,12 A8.8,8.8 0 1 0 3.2,12` y `M12,6.8 L12,12 L15.6,14.3` |
| `ic_server` | dos bandejas: `M4.2,6.6 A2.1,2.1 0 0 1 6.3,4.5 L17.7,4.5 A2.1,2.1 0 0 1 19.8,6.6 L19.8,10 A2.1,2.1 0 0 1 17.7,12.1 L6.3,12.1 A2.1,2.1 0 0 1 4.2,10 Z`, `M4.2,16 A2.1,2.1 0 0 1 6.3,13.9 L17.7,13.9 A2.1,2.1 0 0 1 19.8,16 L19.8,19.4 A2.1,2.1 0 0 1 17.7,21.5 L6.3,21.5 A2.1,2.1 0 0 1 4.2,19.4 Z`, más los puntos `M6.45,8.3 A0.95,0.95 0 1 0 8.35,8.3 A0.95,0.95 0 1 0 6.45,8.3` y `M6.45,17.4 A0.95,0.95 0 1 0 8.35,17.4 A0.95,0.95 0 1 0 6.45,17.4` (rellenos) y las ranuras `M11,8.3 L17,8.3` y `M11,17.4 L17,17.4` |
| `ic_import` | documento sin flecha (es el icono de la tarjeta «Importar archivo»): `M6.2,3.6 L13.8,3.6 L18.4,8.2 L18.4,20.4 L6.2,20.4 Z` y `M13.8,3.6 L13.8,8.2 L18.4,8.2` |
| `ic_download` | flecha hacia abajo con bandeja: `M12,3.8 L12,15`, `M8.2,11.2 L12,15 L15.8,11.2`, `M4,19.6 L20,19.6` |
| `ic_upload` | flecha hacia arriba con bandeja: `M12,15.2 L12,3.8`, `M8.2,7.6 L12,3.8 L15.8,7.6`, `M4,19.6 L20,19.6` |

- [ ] **Step 5: borra los iconos sustituidos**

```bash
git rm app/src/main/res/drawable/ic_arrow_down.xml app/src/main/res/drawable/ic_arrow_up.xml
```

- [ ] **Step 5b: mete en el set los cuatro iconos que se quedaban fuera**

`ic_power`, `ic_update`, `ic_shield_check` e `ic_empty_shield` no estaban en la lista de la
spec, pero tienen un defecto de render real: **no declaran `android:fillColor`**, y el valor
por defecto de Android es negro opaco. Un path abierto (el arco del botón de power, el del
icono de actualizar) se cierra implícitamente al rellenarse, así que en la app salía un disco
macizo del color del tinte en lugar de un contorno. `ic_shield_check` e `ic_empty_shield` sí
declaran el relleno, pero con trazo negro y grosores 1.6/2, fuera del estilo del set.

Los cuatro pasan a trazo 1.75, tapas y uniones redondeadas, `fillColor="#00000000"` y trazo
blanco, y se añaden a la galería.

- [ ] **Step 5c: quita la dependencia que se queda sin uso**

`material-icons-core` solo servía para los iconos de Material, y ya no queda ninguno:

```bash
# app/build.gradle.kts: quitar implementation(libs.androidx.compose.material.icons.core)
# gradle/libs.versions.toml: quitar la entrada androidx-compose-material-icons-core
```

- [ ] **Step 5d: protege el invariante del set con un test**

El motivo del commit es «trazo uniforme», y sin un test que lo compruebe, un `strokeWidth="2"`
colado mañana en un icono pasaría el CI. El test lee los vectores del disco y comprueba que
todos declaran su relleno, que ninguno se rellena de negro opaco y que todo trazo usa el
grosor 1,75 con las tapas redondeadas.

El XML se lee con **expresiones regulares, no con el DOM**: el parser de Java no resuelve los
espacios de nombres por defecto, así que `getAttributeNS(uri, "fillColor")` devuelve la cadena
vacía y el test falla siempre. La comprobación se replica en Python antes de subirla.

`ic_launcher_foreground.xml` e `ic_logo_u.xml` quedan fuera del set: tienen su propia escala
(trazo 8.4 y 11.3) y no son iconos de interfaz.

- [ ] **Step 6: ata los iconos a las pestañas**

En `MainTab.kt` (`com.usipipo.vpn.ui.nav`) añade:

```kotlin
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.usipipo.vpn.R

/** Icono propio de la barra inferior para cada pestaña. */
internal val MainTab.iconRes: Int
    @DrawableRes get() = when (this) {
        MainTab.Home -> R.drawable.ic_nav_home
        MainTab.Configs -> R.drawable.ic_nav_configs
        MainTab.Import -> R.drawable.ic_nav_import
        MainTab.Diagnostics -> R.drawable.ic_nav_diagnostics
    }

/** Etiqueta de la barra inferior para cada pestaña. */
internal val MainTab.labelRes: Int
    @StringRes get() = when (this) {
        MainTab.Home -> R.string.nav_home
        MainTab.Configs -> R.string.nav_configs
        MainTab.Import -> R.string.nav_import
        MainTab.Diagnostics -> R.string.nav_diagnostics
    }
```

`internal` es visible desde el conjunto de pruebas unitarias en Android Gradle (la compilación de
pruebas está asociada a la de `main`).

- [ ] **Step 7: usa los iconos propios en la barra y quita los de Material**

En `UsipipoBottomBar.kt` sustituye los cuatro `NavigationBarItem` por el bucle sobre
`MainTab.entries` que usa `entry.iconRes` y `entry.labelRes` (ya escrito en el Step 5 de la
Tarea 1), y borra los imports `androidx.compose.material.icons.Icons`,
`androidx.compose.material.icons.filled.Build`, `...filled.Home`, `...filled.Settings`.

- [ ] **Step 8: quita el último icono de Material de la app**

En `HomeScreen.kt`, el botón del estado vacío usa `Icons.Default.Add`. Sustitúyelo por
`painterResource(R.drawable.ic_add)` y borra los imports `androidx.compose.material.icons.Icons`
y `androidx.compose.material.icons.filled.Add`. Añade el import de `painterResource` si no está.

Con esto no queda ningún `Icons.Default` en el proyecto.

- [ ] **Step 9: renombra descarga y subida en sus usos**

Sustituye `R.drawable.ic_arrow_down` por `R.drawable.ic_download` y `R.drawable.ic_arrow_up` por
`R.drawable.ic_upload` en `ConnectionStats.kt`, `DetailsScreen.kt` y el preview de `StatTile.kt`.

- [ ] **Step 10: previews de los iconos**

Añade en `ui/components/` un archivo nuevo `IconGallery.kt` con un `@Preview` que muestre los
16 iconos en claro y en oscuro, para poder revisarlos de un vistazo:

```kotlin
@Preview(name = "Set de iconos")
@Preview(name = "Set de iconos oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewIconGallery() {
    UsipipoTheme {
        Surface {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    R.drawable.ic_nav_home, R.drawable.ic_nav_configs,
                    R.drawable.ic_nav_import, R.drawable.ic_nav_diagnostics,
                    R.drawable.ic_arrow_right, R.drawable.ic_arrow_back,
                    R.drawable.ic_add, R.drawable.ic_star, R.drawable.ic_star_filled,
                    R.drawable.ic_edit, R.drawable.ic_delete, R.drawable.ic_qr,
                    R.drawable.ic_clock, R.drawable.ic_download, R.drawable.ic_upload,
                    R.drawable.ic_server,
                ).forEach { res ->
                    Icon(painterResource(res), contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
```

`FlowRow` es `androidx.compose.foundation.layout.FlowRow` y necesita
`@OptIn(ExperimentalLayoutApi::class)`.

- [ ] **Step 11: commit**

```bash
git add app/src/main/res/drawable app/src/main/java/com/usipipo/vpn/ui/nav/MainTab.kt \
        app/src/main/java/com/usipipo/vpn/ui/components app/src/main/java/com/usipipo/vpn/ui/home \
        app/src/main/java/com/usipipo/vpn/ui/details app/src/test/java/com/usipipo/vpn/ui/nav/MainTabTest.kt
git commit -m "feat(ui): set de iconos propio en contorno con trazo uniforme

Los iconos de la barra eran los genericos de Material y dos vectores propios
estaban rotos: ic_arrow_right dibujaba una flecha hacia la izquierda e
ic_add tenia un trazo malformado que se leia como un 1.

Se sustituyen por un set propio de 16 iconos en contorno con grosor 1.75, se
renombran ic_arrow_down/up a ic_download/ic_upload porque pasan a dibujar
flecha con bandeja, y la app se queda sin ningun Icons.Default."
```

- [ ] **Step 12: verificación por CI**

```bash
git push -u origin feat/mockup-fase2
gh workflow run android-ci.yml --ref feat/mockup-fase2
```

---

### Tarea 3: icono del launcher y logo de la cabecera

El icono de la app se ve con zoom y recortado en la pantalla de inicio del dispositivo. La causa:
`ic_launcher_foreground.xml` dibuja la «u» con trazos de 20 unidades y coordenadas hasta 104
sobre un lienzo de 108, así que se sale de la zona segura (los 66 dp centrales) y el guion bajo,
en `y=104`, queda literalmente fuera del lienzo.

**Files:**
- Rewrite: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Rewrite: `app/src/main/res/drawable/ic_logo_u.xml`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/components/AppLogo.kt`
- Test: `app/src/test/java/com/usipipo/vpn/ui/LauncherIconSafeZoneTest.kt` (nuevo)

**Interfaces:**
- Consumes: nada de tareas anteriores.
- Produces: nada que consuman otras tareas.

> **Geometría de origen.** La forma se mide sobre `public/image/usipipo_logo.jpg` (640×640),
> azul de marca `rgb(0,74,173)` = `#004AAD`. La «u» se compone de un brazo izquierdo con la
> punta en `y=218`, un cuenco cuyo punto más bajo está en `x≈300, y≈433`, y un asta derecha más
> alta que arranca en `y=154` y baja hasta `y=489`; el trazo mide 52 px. El guion bajo es un
> rectángulo `x 120..278, y 484..513`.

- [ ] **Step 1: escribe el test que falla**

Crea `app/src/test/java/com/usipipo/vpn/ui/LauncherIconSafeZoneTest.kt`:

```kotlin
package com.usipipo.vpn.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El icono adaptativo se recorta fuera de la zona segura: el lienzo del
 * foreground es 108x108 y solo los 66x66 centrales (21..87) sobreviven a las
 * máscaras del sistema. Este test lee el vector y comprueba que nada se sale.
 *
 * Se miran solo las coordenadas de `pathData`: el resto de numeros del archivo
 * (el `1.0` de la declaracion XML, el `108` del tamano del lienzo, el grosor
 * de trazo) no son coordenadas y no dicen nada de la zona segura.
 */
class LauncherIconSafeZoneTest {

    private val foreground = File("src/main/res/drawable/ic_launcher_foreground.xml")

    @Test
    fun `el icono del launcher existe y dibuja dentro de la zona segura`() {
        assertTrue("falta ${foreground.path}", foreground.exists())
        val contenido = foreground.readText()
        val trazados = Regex("android:pathData=\"([^\"]*)\"")
            .findAll(contenido)
            .map { it.groupValues[1] }
            .toList()
        assertTrue("el vector no dibuja ninguna ruta", trazados.isNotEmpty())
        val numeros = Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(trazados.joinToString(" "))
            .map { it.value.toDouble() }
            .toList()
        assertTrue("el vector no tiene coordenadas", numeros.isNotEmpty())
        val fuera = numeros.filter { it < SAFE_MIN || it > SAFE_MAX }
        assertTrue("coordenadas fuera de la zona segura: $fuera", fuera.isEmpty())
    }

    private companion object {
        const val SAFE_MIN = 21.0
        const val SAFE_MAX = 87.0
    }
}
```

Las coordenadas se sacan de los atributos `pathData`, no de todo el archivo: el `1.0` de la
declaración XML, el `108` del tamaño del lienzo, el grosor de trazo `8.4` y el `0` de
`#00000000` no son coordenadas y quedarían fuera de 21..87 aunque el dibujo fuese correcto.

- [ ] **Step 2: ejecuta el test y comprueba que falla**

Falla en el CI: el vector actual tiene coordenadas `104` (el guion bajo), `102`, `100`, `92` y
`88` fuera de 21..87.

- [ ] **Step 3: rehaz el icono del launcher**

`ic_launcher_foreground.xml` con lienzo 108×108, trazo 8.4 y tapas redondeadas. La «u» está
centrada y ocupa de `y=25` a `y=83`, dentro de la zona segura:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:pathData="M46.9,34.5 L46.9,48.4 C46.9,56.1 52.2,60.8 59,60.8 C65.4,60.7 71.9,59.3 76.1,50.8"
        android:fillColor="#00000000"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="8.4"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M76.1,29.2 L76.1,74.9"
        android:fillColor="#00000000"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="8.4"
        android:strokeLineCap="round" />
    <path
        android:pathData="M28.8,78.3 L54.3,78.3 L54.3,83 L28.8,83 Z"
        android:fillColor="#FFFFFFFF" />
</vector>
```

Comprobación de la zona segura con trazo 8.4 (medio trazo 4.2):

| Elemento | Centro | Borde exterior | ¿Dentro de 21..87? |
|---|---|---|---|
| Asta derecha (x=76.1) | 76.1 | 80.3 | sí |
| Punta superior del asta (y=29.2) | 29.2 | 25.0 | sí |
| Punta inferior del asta (y=74.9) | 74.9 | 79.1 | sí |
| Brazo izquierdo (x=46.9) | 46.9 | 42.7 | sí |
| Guion bajo (relleno) | 78.3..83.0 | — | sí |

El fondo del icono adaptativo ya es `#004AAD` (`res/values/colors.xml`,
`ic_launcher_background`) y el monocromo usa el mismo `foreground`, así que no hay que tocar
`mipmap-anydpi-v26/ic_launcher.xml` ni `ic_launcher_round.xml`.

- [ ] **Step 4: genera `ic_logo_u` a partir del mismo trazado**

`ic_logo_u.xml` lleva el mismo trazado en un lienzo de 100×100, escalado por 1.345 y desplazado
para que el glifo ocupe del 15% al 89% del lienzo, con trazo 11.3:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="100dp"
    android:height="100dp"
    android:viewportWidth="100"
    android:viewportHeight="100">
    <path
        android:pathData="M39.7,23.8 L39.7,42.5 C39.7,52.9 46.8,59.2 56,59.2 C64.6,59 73.3,57.2 79,45.7"
        android:fillColor="#00000000"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="11.3"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M79,16.7 L79,78.1"
        android:fillColor="#00000000"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="11.3"
        android:strokeLineCap="round" />
    <path
        android:pathData="M15.3,82.7 L49.6,82.7 L49.6,89 L15.3,89 Z"
        android:fillColor="#FFFFFFFF" />
</vector>
```

- [ ] **Step 5: ajusta el tamaño del glifo en la cabecera**

En `AppLogo.kt`, el `Icon` de `ic_logo_u` pasa de `size * 0.62f` a `size * 0.85f`, porque el
vector nuevo ya trae su propio margen y el glifo debe llenar el cuadro azul como en el mockup.

- [ ] **Step 6: preview del logo en claro y en oscuro**

Añade a `AppLogo.kt`:

```kotlin
@Preview(name = "Logo")
@Preview(name = "Logo oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppLogo() {
    UsipipoTheme {
        Surface {
            AppLogo(size = 56.dp, modifier = Modifier.padding(16.dp))
        }
    }
}
```

- [ ] **Step 7: commit**

```bash
git add app/src/main/res/drawable/ic_launcher_foreground.xml \
        app/src/main/res/drawable/ic_logo_u.xml \
        app/src/main/java/com/usipipo/vpn/ui/components/AppLogo.kt \
        app/src/test/java/com/usipipo/vpn/ui/LauncherIconSafeZoneTest.kt
git commit -m "fix(ui): rehacer el icono del launcher dentro de la zona segura

El foreground dibujaba la u con trazos de 20 unidades y coordenadas hasta 104
sobre un lienzo de 108: el guion bajo caia fuera del lienzo y el resto se
salia de la zona segura de 66 dp, por eso el icono se veia con zoom y
recortado en la pantalla de inicio.

La forma se ha medido sobre usipipo_logo.jpg y se ha reescalado a 58 de los
108 dp, con el guion bajo dentro. ic_logo_u sale del mismo trazado."
```

- [ ] **Step 8: verificación por CI**

```bash
git push -u origin feat/mockup-fase3
gh workflow run android-ci.yml --ref feat/mockup-fase3
```

---

### Tarea 4: mapa de puntos en el fondo del Inicio

El Inicio del mockup lleva un mapa del mundo punteado detrás del botón de conectar. La spec pide
un mapa vectorial ligero, sin imágenes ni desenfoques: una máscara de tierra de 72×36 puntos
guardada como texto y dibujada con `Canvas`.

**Files:**
- Create: `app/src/main/java/com/usipipo/vpn/ui/home/WorldMap.kt`
- Modify: `app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt` (`HomeBackground`)
- Modify: `app/src/main/java/com/usipipo/vpn/ui/theme/Atmosphere.kt` (color de los puntos)
- Test: `app/src/test/java/com/usipipo/vpn/ui/home/WorldMapMaskTest.kt` (nuevo)

**Interfaces:**
- Consumes: nada de tareas anteriores.
- Produces: `internal const val MAP_COLUMNS = 72`, `internal const val MAP_ROWS = 36`,
  `internal fun mapMaskRows(): List<String>`, `internal fun isLand(column: Int, row: Int): Boolean`,
  en `com.usipipo.vpn.ui.home`.

> **Origen de la máscara.** Rasterizado de las costas de Natural Earth 110m/50m
> (`ne_50m_land`), que es dominio público. La rejilla cubre 360° de longitud y 145° de latitud
> (de 87.5°N a 57.5°S, es decir, sin la Antártida); 776 de 2592 celdas son tierra (29.9 %).

- [ ] **Step 1: escribe el test que falla**

Crea `app/src/test/java/com/usipipo/vpn/ui/home/WorldMapMaskTest.kt`:

```kotlin
package com.usipipo.vpn.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldMapMaskTest {

    @Test
    fun `la mascara tiene el tamano previsto`() {
        assertEquals(36, MAP_ROWS)
        assertEquals(72, MAP_COLUMNS)
        val filas = mapMaskRows()
        assertEquals(MAP_ROWS, filas.size)
        filas.forEach { fila ->
            assertEquals(MAP_COLUMNS / 4, fila.length)
            assertTrue("fila no hexadecimal: $fila", fila.all { it in "0123456789ABCDEF" })
        }
    }

    @Test
    fun `la mascara contiene tierra y mar`() {
        var tierra = 0
        var mar = 0
        for (row in 0 until MAP_ROWS) {
            for (column in 0 until MAP_COLUMNS) {
                if (isLand(column, row)) tierra++ else mar++
            }
        }
        assertTrue("no hay tierra", tierra > 0)
        assertTrue("no hay mar", mar > 0)
        assertTrue("la mascara esta vacia o llena", tierra in 400..1200)
    }

    @Test
    fun `las celdas fuera de la mascara se resuelven como mar`() {
        assertTrue(!isLand(-1, 0))
        assertTrue(!isLand(0, -1))
        assertTrue(!isLand(MAP_COLUMNS, 0))
        assertTrue(!isLand(0, MAP_ROWS))
    }
}
```

- [ ] **Step 2: ejecuta el test y comprueba que falla**

Falla en el CI: `unresolved reference: MAP_ROWS`.

- [ ] **Step 3: crea `WorldMap.kt` con la máscara y el decodificador**

```kotlin
package com.usipipo.vpn.ui.home

/** Columnas de la máscara del mapa (5° de longitud por celda). */
internal const val MAP_COLUMNS = 72

/** Filas de la máscara del mapa. */
internal const val MAP_ROWS = 36

/**
 * Máscara de tierra en hexadecimal: una fila por línea, 18 dígitos por fila
 * (4 celdas por dígito; el bit 3 es la columna 0 de cada grupo).
 *
 * Costa rasterizada sobre Natural Earth 50m land (dominio público), de 87.5°N
 * a 57.5°S. 776 de 2592 celdas son tierra.
 */
private const val MAP_MASK_HEX = """
    000000000000000000
    00003EFF4001000000
    ...
    000000000000000000
""".trimIndent()

/** Las 36 filas de la máscara, en hexadecimal. */
internal fun mapMaskRows(): List<String> = MAP_MASK_HEX.lines()

/** true si la celda (columna, fila) es tierra. Fuera de la máscara, mar. */
internal fun isLand(column: Int, row: Int): Boolean {
    if (column !in 0 until MAP_COLUMNS || row !in 0 until MAP_ROWS) return false
    val fila = mapMaskRows()[row]
    val digito = fila[column / 4]
    val bit = 3 - (column % 4)
    return (digito.digitToInt(16) shr bit) and 1 == 1
}
```

La máscara completa son las 36 líneas del bloque `MAP_MASK_HEX`, sin modificar:

```
000000000000000000
00003EFF4001000000
000431FF0180008000
0018F87F00000FB480
1F77D63E01E0BFFFFF
5FFFE670836FFFFFFF
1FFF8C2006FFFFFFE6
087FC70012FFFFFF18
007FFF8037FFFFFF90
001FFD800FFFFFFF80
001FFF000DDFFFFE00
001FF80030EFFFFC00
000FF80010FFFFFC00
0007F0003C1FFFF200
000390003FFFFFF000
000380007FFC7FF000
0001A8007FEF1B8000
0000C0007FF6118000
000020007FF810C000
000017007FFC100000
000007C027FC019000
00000FC003F800B800
00000FF003F000A100
00000FF801F0006080
00000FF801F0000080
000007F003F4000680
000003F001E4000F80
000003E001E4001FC0
000003C001E0001FE0
000003C000C0001FE0
0000038000000000C0
000007000000000000
000006000000000002
000006000000000000
000004000000000000
000000000000000000
```

Son 683 bytes: la máscara completa en UTF-8 cabe de sobra en el «menos de 1 KB» que pide la spec.

- [ ] **Step 4: dibuja el mapa con `Canvas`**

En `WorldMap.kt` añade la capa de dibujo. Va **debajo** del contenido y **encima** del
degradado, y solo en el Inicio.

```kotlin
/** Opacidad base de los puntos sobre el degradado. */
private const val DOT_ALPHA_DARK = 0.30f
private const val DOT_ALPHA_LIGHT = 0.12f

@Composable
internal fun WorldMapLayer(
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (dark) {
        Color(AtmospherePalette.MAP_DOT_DARK)
    } else {
        Color(AtmospherePalette.MAP_DOT_LIGHT)
    }
    val alpha = if (dark) DOT_ALPHA_DARK else DOT_ALPHA_LIGHT
    val filas = remember { mapMaskRows() }
    Canvas(
        modifier = modifier.fillMaxSize().drawWithCache {
            val cell = size.width * MAP_WIDTH_FRACTION / MAP_COLUMNS
            val radius = cell * 0.22f
            val mapWidth = cell * MAP_COLUMNS
            val mapHeight = cell * MAP_ROWS
            val left = (size.width - mapWidth) / 2f
            val top = size.height * MAP_VERTICAL_CENTER - mapHeight / 2f
            onDrawBehind {
                for (row in 0 until MAP_ROWS) {
                    val fila = filas[row]
                    for (column in 0 until MAP_COLUMNS) {
                        val digito = fila[column / 4].digitToInt(16)
                        if ((digito shr (3 - column % 4)) and 1 != 1) continue
                        val cx = left + cell * (column + 0.5f)
                        val cy = top + cell * (row + 0.5f)
                        // Desvanecido hacia los bordes (seno) y hacia abajo.
                        val nx = (cx - left) / mapWidth
                        val ny = (cy - top) / mapHeight
                        val edge = sin(PI * nx.toDouble()).toFloat()
                        val down = 1f - ny * 0.85f
                        drawCircle(
                            color = color.copy(alpha = alpha * edge * down),
                            radius = radius,
                            center = Offset(cx, cy),
                        )
                    }
                }
            }
        },
    )
}
```

Con `MAP_WIDTH_FRACTION = 0.98f` (la red ocupa el 98 % del ancho) y
`MAP_VERTICAL_CENTER = 0.34f` (el centro vertical del mapa queda al 34 % del alto, detrás del
botón de conectar).

- [ ] **Step 5: declara el color de los puntos**

En `AtmospherePalette`:

```kotlin
// Puntos del mapa del Inicio (turquesa azulado en oscuro, azul casi invisible en claro)
const val MAP_DOT_DARK: Long = 0xFF2DD4BF
const val MAP_DOT_LIGHT: Long = 0xFF1B4FA0
```

El alfa se aplica al dibujar (`color.copy(alpha = …)`), no en la constante ARGB.

- [ ] **Step 6: monta el mapa en el fondo del Inicio**

En `HomeScreen.kt`, `HomeBackground` pasa a tres capas:

```kotlin
@Composable
private fun HomeBackground(
    phase: AtmospherePhase,
    reduceMotion: Boolean = rememberReduceMotion(),
    content: @Composable () -> Unit,
) {
    val atmosphere = LocalAtmosphere.current.forPhase(phase)
    val dark = isSystemInDarkTheme()
    Box(modifier = Modifier.fillMaxSize()) {
        GradientLayer(top = atmosphere.top, bottom = atmosphere.bottom, reduceMotion = reduceMotion)
        WorldMapLayer(dark = dark)
        content()
    }
}
```

Ni el degradado ni las fases de `Atmosphere` cambian: el mapa es una capa aditiva encima.

- [ ] **Step 7: preview del Inicio con el mapa**

Añade a `HomeScreen.kt` un preview del fondo con mapa, en claro y en oscuro:

```kotlin
@Preview(name = "Fondo con mapa")
@Preview(name = "Fondo con mapa oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFondoConMapa() {
    UsipipoTheme {
        Surface {
            HomeBackground(phase = AtmospherePhase.Calm, reduceMotion = true) {
                PreviewBackgroundContent()
            }
        }
    }
}
```

- [ ] **Step 8: commit**

```bash
git add app/src/main/java/com/usipipo/vpn/ui/home/WorldMap.kt \
        app/src/main/java/com/usipipo/vpn/ui/home/HomeScreen.kt \
        app/src/main/java/com/usipipo/vpn/ui/theme/Atmosphere.kt \
        app/src/test/java/com/usipipo/vpn/ui/home/WorldMapMaskTest.kt
git commit -m "feat(ui): mapa de puntos vectorial en el fondo del Inicio

El fondo del Inicio solo pintaba el degradado de la atmosfera y se quedaba
lejos del mockup, que lleva un mapa del mundo punteado detras del boton de
conectar.

La costa se rasteriza en una mascara de 72x36 celdas guardada en hexadecimal
(683 bytes) y se dibuja con Canvas en una sola pasada por tamano de pantalla,
encima del degradado y detras del contenido, con desvanecido hacia los bordes
y hacia abajo. El degradado y las fases de atmosfera no cambian."
```

- [ ] **Step 9: verificación por CI**

```bash
git push -u origin feat/mockup-fase4
gh workflow run android-ci.yml --ref feat/mockup-fase4
```

---

## Cierre: revisión y apertura de las pull requests

- [ ] **Step 1: revisión de código por fase**

Por cada rama, lanzar un subagente de revisión con el diff contra `main` y el design doc como
referencia. Comprueba en particular:

- que ningún `Icons.Default` sobrevive en `app/src/main`;
- que no quedan referencias a `ic_arrow_down`, `ic_arrow_up`, `ic_arrow_right` con la geometría
  antigua;
- que no hay imports sin usar ni imports fuera de orden;
- que el mapa no se dibuja fuera del Inicio;
- que ningún archivo del `tunnel/`, `data/`, `diagnostics/` o `update/` ha cambiado.

- [ ] **Step 2: subagente de calidad**

Un subagente distinto revisa la calidad global del resultado: coherencia del set de iconos
(grosor de trazo uniforme, tapas redondeadas, escala visual parecida entre iconos), legibilidad
de la barra en ambos temas, contraste de los colores nuevos y limpieza del código tocado.

- [ ] **Step 3: pull request por fase**

Una PR por rama hacia `main`, con el porqué del cambio y la evidencia del CI verde:

```bash
gh pr create --base main --head feat/mockup-fase<N> \
  --title "fix(ui): correcciones visuales fase <N>" \
  --body "$(cat <<'EOF'
## Qué

<qué cambia y por qué>

## Verificación

- `testDebugUnitTest` y `assembleDebug` verdes en CI (run <id>).
- Comprobado en la APK del CI en el teléfono: <lista de comprobaciones manuales>.
EOF
)"
```

- [ ] **Step 4: lista de comprobaciones manuales en el teléfono**

Con la APK del CI instalada:

1. La barra inferior aparece en Inicio, Configuraciones, Importar y Diagnóstico, y también en
   Detalles.
2. La barra no se solapa con la barra de gestos del sistema.
3. Con el teclado abierto (campo «Pegar configuración»), la barra se oculta y el contenido no se
   aplasta.
4. El icono de la app en la pantalla de inicio se ve sin zoom, tanto en la máscara circular como
   en el cuadrado redondeado, y el guion bajo de la «u» se ve completo.
5. La flecha de las tarjetas apunta a la derecha («>») y el «+» de Configuraciones se ve como una
   cruz.
6. El mapa de puntos se ve detrás del botón de conectar en el Inicio, y no aparece en las otras
   pestañas.
7. Los cuatro iconos de la barra tienen el mismo grosor de trazo entre sí.
