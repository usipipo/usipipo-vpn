# uSipipo VPN — Iconos: paso a relleno sólido — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reescribir los 11 drawables del set propio que tienen un equivalente sólido real en `iconmcp`, de contorno a relleno, sin tocar el contrato de estilo ni los 7 iconos con color de estado.

**Architecture:** Cinco tareas, una por técnica de conversión de SVG a `VectorDrawable`: copia literal del `pathData`, `fillType` con subpaths negativos, mezcla de relleno y línea, y primitivas `<rect>` / `<circle>` convertidas a `pathData`. La Task 1 añade a `IconSetStyleTest` las cinco guardas que las demás tareas necesitan para ser verificables. Ninguna tarea toca código Kotlin de la app.

**Tech Stack:** XML de `VectorDrawable` y JUnit 4 sobre JVM. Sin dependencias nuevas: `android:fillType` existe desde la API 24 y `minSdk` es 26.

**Spec:** `docs/superpowers/specs/2026-11-09-iconos-relleno-design.md`
**Fuente de verdad del `pathData`:** `docs/design/iconos-relleno-pathdata.md`
**Rama:** `feat/iconos-relleno` · **Worktree:** `.worktrees/iconos-relleno`

## Global Constraints

1. **`pathData` literal, sin aritmética.** Decisión A del spec §4.2: los números se copian de la fuente de verdad. Ni multiplicar, ni redondear, ni reordenar.
2. **Cabecera única en los once:** `android:width="24dp" android:height="24dp" android:viewportWidth="16" android:viewportHeight="16"`.
3. **Todo `<path>` declara `fillColor`**, y ninguno vale `#FF000000`.
4. **Todo `<path>` que declare `strokeColor` declara también `strokeWidth="1.75"`, `strokeLineCap="round"` y `strokeLineJoin="round"`.**
5. **Cada `<path>` se escribe en una sola línea** y se cierra con `/>`. El contrato lo exige: `PATH = Regex("<path\s[^>]*?/>")` falla sobre un elemento partido en varias líneas o con hijos.
6. **Los 7 iconos con color de estado no se tocan:** `ic_tile_shield_connected`, `ic_tile_shield_disconnected`, `ic_tile_shield_failed`, `ic_widget_power`, `ic_widget_power_failed`, `ic_widget_add`, `ic_widget_shield_check`. Tampoco `ic_logo_u.xml` ni `ic_launcher_foreground.xml`.
7. **`IconSetStyleTest`: se reescribe su KDoc y se añaden tests. Ninguna aserción de los cuatro tests existentes se modifica.**
8. **No se toca la lógica del túnel** ni `MainActivity.kt` (`AGENTS.md`, paso 6).
9. **Esta máquina no tiene JDK, ni Android SDK, ni Gradle, y no se instala nada.** El código se escribe y se comprueba leyendo ficheros; el veredicto es de la CI.
10. **Nada se inventa** (`AGENTS.md`, paso 7): cada atributo, cada número y cada comando viene de la fuente de verdad, del spec o de un fichero del repo que se haya leído.

## Review Focus

| # | Lo que puede romperse y ningún test existente lo ve | Test que lo fija | Task |
|---|---|---|---|
| 1 | **Un `pathData` mal formado revienta en tiempo de ejecución, no en el build.** Android parsea el `pathData` al inflar el vector, así que ni `testDebugUnitTest` ni `assembleDebug` lo detectan: un número que falta, una letra de comando que sobra o un arco con un flag de menos tumban la app cuando se dibuja el icono | `todo pathData del set declara comandos validos y argumentos completos` | 1 |
| 2 | **Una primitiva SVG sin convertir es invisible para el contrato actual.** `PATH` solo casa con `<path`: un `<rect>` o un `<circle>` colados dentro del `<vector>` no casan con nada, así que los cuatro tests existentes pasan en verde sobre un drawable roto | `ningun icono del set usa primitivas que VectorDrawable no tiene` | 1 |
| 3 | **`fillType` mal puesto tapa los subpaths negativos.** En `home` la puerta y en `shield-check` el check salen de un segundo subpath en negativo: sin `evenOdd` se rellenan, y la casa sale con un portal macizo y el escudo con un cuadro blanco | `los iconos con subpaths negativos declaran el fillType par` | 1 |
| 4 | **El par de la estrella se desajusta y el favorito deja de leerse como el mismo glifo.** Hoy los dos comparten borde exterior (19,75 y 19,74 en la cuadrícula de 24, comprobado). Si se sustituyen por glifos distintos, el icono cambia de tamaño al marcar favorito | `la estrella vacia y la llena comparten pathData` | 1 |
| 5 | **Los cuatro iconos mixtos pierden el trazo y salen como manchas.** Son los únicos que ejercitan la rama condicional del test de trazo; si en la migración se quedan solo con relleno, esa rama muere sin que nadie lo note | `los cuatro iconos mixtos declaran su trazo` | 1 |

**Lo que ningún test cubre y es del usuario:** la lectura visual. Esta máquina no renderiza y el usuario no puede abrir enlaces en el PC. La Task 5 acaba con el recorrido que debe hacer en dispositivo.

---

### Task 1: Las cinco guardas del set

**Files:**
- Modify: `app/src/test/java/com/usipipo/vpn/ui/IconSetStyleTest.kt` (KDoc de las líneas 8-18; cinco tests nuevos; tres constantes y un helper nuevos en `companion object`)
- Test: el mismo (los cinco tests nuevos leen los drawables del disco)
- Untracked que hay que commitear primero: `docs/superpowers/specs/2026-11-09-iconos-relleno-design.md`, `docs/design/iconos-relleno-pathdata.md`, este plan

**Interfaces:**
- Consumes: `iconosDelSet()`, `trazados(nombre)`, `atributo(bloque, nombre)`, `DRAWABLES`, `PATH` — todos privados y ya existentes, no se tocan.
- Produces: `pathDataValido(pathData: String): Boolean` y las constantes `PRIMITIVAS_SVG`, `CON_SUBPATHS_NEGATIVOS`, `ICONOS_MIXTOS`, `ARIDADES`, `BANDERAS`, `TOKEN`. Las Tasks 2-5 no las usan directamente; las usan sus contratos.

**Dependencia de import:** `assertFalse` **no está importado** en `IconSetStyleTest.kt` (solo `assertEquals`, `assertTrue`, `Test` y `java.io.File`). Hay que añadir `import org.junit.Assert.assertFalse`. Comprobado el 2026-11-10 sobre el fichero real; si ya estuviera, el import no rompe nada.

- [ ] **Step 1: Commitear los documentos**

El spec, la fuente de verdad del `pathData` y este plan están sin trackear. Sin este commit
no viajan al worktree de la ejecución.

```bash
git add docs/superpowers/specs/2026-11-09-iconos-relleno-design.md \
        docs/design/iconos-relleno-pathdata.md \
        docs/superpowers/plans/2026-11-09-iconos-relleno-plan.md
git commit -m "docs: añade el spec y la fuente de verdad del paso a relleno de los iconos"
```

- [ ] **Step 2: Reescribir el KDoc de la clase**

Sustituir el bloque de las líneas 8-18 por este. El KDoc viejo afirma «relleno
transparente», que el rediseño vuelve falso; el nuevo describe los dos invariantes reales
y por qué existen los tres tests nuevos.

```kotlin
/**
 * El set de iconos propio declara un estilo uniforme. Desde el paso a relleno hay dos
 * invariantes: todo `<path>` declara su `fillColor` y ninguno vale `#FF000000`, y todo
 * `<path>` que declara trazo usa el grosor del set (1.75) con tapas redondeadas. Sin
 * `fillColor` explicito Android rellena de negro opaco, y un path abierto (el del boton
 * de power, el de actualizar) se cerraria y saldria un disco macizo en lugar de un
 * contorno: el relleno se declara tambien transparente, no se omite.
 *
 * Los tres tests siguientes fijan lo que el estilo no puede expresar y ningun otro sitio
 * comprueba: que no quede ninguna primitiva SVG sin convertir (`VectorDrawable` solo
 * tiene `<path>`), que cada `pathData` sea legible (Android lo parsea al inflar el vector,
 * no al compilar, asi que un numero de menos tumba la app y ni el build lo ve), y que los
 * pares que deben leerse como el mismo glifo en dos estados compartan geometria.
 *
 * El invariante se comprueba leyendo los vectores del disco, porque no hay forma de
 * assertar sobre el trazo de un `ImageVector` desde un test de JVM. El XML se lee con
 * expresiones regulares y no con el DOM: el parser de Java no resuelve los espacios de
 * nombres por defecto, asi que `getAttributeNS` devolveria la cadena vacia.
 */
```

- [ ] **Step 3: Añadir los cinco tests y sus constantes**

Cinco `@Test` nuevos, al final de la clase, antes de los privados. Texto exacto:

```kotlin
@Test
fun `ningun icono del set usa primitivas que VectorDrawable no tiene`() {
    iconosDelSet().forEach { nombre ->
        val contenido = File(DRAWABLES, nombre).readText()
        PRIMITIVAS_SVG.forEach { primitiva ->
            assertFalse(
                "$nombre: <$primitiva> no existe en VectorDrawable, hay que convertirlo a pathData",
                contenido.contains("<$primitiva"),
            )
        }
    }
}

@Test
fun `todo pathData del set declara comandos validos y argumentos completos`() {
    iconosDelSet().forEach { nombre ->
        trazados(nombre).forEachIndexed { indice, bloque ->
            val pathData = atributo(bloque, "pathData")
            assertTrue("$nombre: el trazado $indice no declara pathData", pathData.isNotEmpty())
            assertTrue(
                "$nombre: el trazado $indice tiene un pathData mal formado: \"$pathData\"",
                pathDataValido(pathData),
            )
        }
    }
}

@Test
fun `los iconos con subpaths negativos declaran el fillType par`() {
    CON_SUBPATHS_NEGATIVOS.forEach { nombre ->
        assertTrue(
            "$nombre: sin android:fillType=\"evenOdd\" los subpaths negativos se rellenan",
            File(DRAWABLES, nombre).readText().contains("android:fillType=\"evenOdd\""),
        )
    }
}

@Test
fun `la estrella vacia y la llena comparten pathData`() {
    val vacia = trazados("ic_star.xml").single()
    val llena = trazados("ic_star_filled.xml").single()
    assertEquals(
        "el par de la estrella debe compartir geometria para leerse como el mismo glifo",
        atributo(llena, "pathData"),
        atributo(vacia, "pathData"),
    )
}

@Test
fun `los cuatro iconos mixtos declaran su trazo`() {
    ICONOS_MIXTOS.forEach { nombre ->
        val delIcono = trazados(nombre)
        assertTrue(
            "$nombre deberia mezclar relleno y trazo, y no declara ningun trazo",
            delIcono.any { atributo(it, "strokeColor").isNotEmpty() },
        )
    }
}
```

Y en `companion object`, junto a `DRAWABLES` y `PATH`:

```kotlin
/** Primitivas SVG que `VectorDrawable` no tiene: hay que pasarlas a `pathData`. */
private val PRIMITIVAS_SVG = listOf("rect", "circle", "line")

/** Los dos glifos cuyo interior sale de un subpath en negativo. */
private val CON_SUBPATHS_NEGATIVOS = listOf("ic_nav_home.xml", "ic_shield_check.xml")

/** Los cuatro que mezclan silueta rellena y detalle a trazo. */
private val ICONOS_MIXTOS = listOf(
    "ic_settings_notifications.xml", "ic_nav_profiles.xml",
    "ic_shield_check.xml", "ic_star.xml",
)
```

Nota de nombres: en el último test la lista se llama `delIcono`, no `trazados`, porque una
`val trazados` taparía la función `trazados(nombre)` y el `trazados(nombre)` de la línea
anterior no compilaría.

- [ ] **Step 4: El helper `pathDataValido`**

Firma exacta, como miembro privado más de la clase (no del companion):

```kotlin
private fun pathDataValido(pathData: String): Boolean
```

Reglas que definen el `true`, todas obligatorias:

1. Solo se aceptan los comandos `M`, `L`, `C`, `A`, `V`, `H`, `Z` y sus minúsculas. Cualquier otra letra invalida.
2. Aridades: `M` 2, `L` 2, `C` 6, `A` 7, `V` 1, `H` 1, `Z` 0.
3. Tras cada comando, la cuenta de números debe ser **múltiplo exacto** de su aridad. Un número suelto al final invalida. La repetición implícita de SVG (varios grupos sin comando delante) **se admite**: cinco drawables del set la usan, entre ellos los cuatro del mosaico y del widget, que este plan no toca. Si se exigiera un grupo exacto, el test fallaría sobre iconos fuera de alcance.
4. En cada grupo de 7 de `A`, el 4.º y el 5.º valor solo pueden ser `0` o `1` (los flags de arco).
5. Un número es `[-+]?(\d+(\.\d*)?|\.\d+)`. La notación científica invalida.
6. La cadena vacía invalida. `Z` no consume números y puede ir seguido de otro comando.
7. Ningún comando fuera de `M L C A V H Z` y sus minúsculas. Un `Q`, `S`, `T` o una letra cualquiera entre los números invalida.

El implementador escribe el cuerpo; las reglas son el contrato.

- [ ] **Step 5: Empujar y dejar el rojo documentado**

```bash
git push -u origin feat/iconos-relleno
```

CI: `./gradlew testDebugUnitTest`.

Expected: **FAIL en dos tests**, y el build en verde:
- `los iconos con subpaths negativos declaran el fillType par` — hoy `ic_nav_home.xml` y `ic_shield_check.xml` son de trazo y no declaran `fillType`.
- `la estrella vacia y la llena comparten pathData` — hoy los `pathData` son distintos (comprobado: el outline está insetado medio trazo respecto al sólido).

Los otros tres pasan en verde sobre el set actual. **No es un rojo incompleto**: los tres son
profilácticos y su mordedura se prueba en el Step 6.

Anotar el resultado real de la CI en el informe. Si falla un cuarto test, el conjunto nuevo
está mal escrito, no el set.

- [ ] **Step 6: Probar por mutación que los tres verdes muerden**

Un commit de un solo uso, revertido antes de terminar la tarea. En
`app/src/main/res/drawable/ic_settings_more.xml` (hoy de trazo, sin primitivas):

1. añadir un elemento `<rect android:layout_width="1dp" />` dentro del `<vector>` — para el test de primitivas;
2. cambiar un número del `pathData` de ese mismo fichero por una letra (`L8,8` → `LX,8`) — para el test de gramática.

```bash
git push
```

Expected: **FAIL en los dos tests nuevos**, y el mensaje nombra `ic_settings_more.xml` y el
`pathData` roto. Si `assembleDebug` también falla por el `<rect>`, no invalida nada: es la
misma conclusión por otro camino. Revertir con `git revert` del commit de mutación (no
`reset`, la rama ya está empujada) y comprobar que el fichero queda byte a byte como
estaba.

**Hecho el 2026-11-10.** Run `42900561777`, disparado con `workflow_dispatch` sobre una rama
de un solo uso (`test/mutacion-guardas-iconos`, borrada al terminar): **489 tests, 2
fallaron**, y son los dos mutados. Los mensajes:

```
java.lang.AssertionError: ic_settings_more.xml: no existe en VectorDrawable,
hay que convertirlo a pathData                                        (IconSetStyleTest.kt:87)

java.lang.AssertionError: ic_settings_more.xml: el trazado 0 tiene un pathData
mal formado: "MX.7,8 A0.55,0.55 0 1 0 3.8,8 A0.55,0.55 0 1 0 2.7,8 Z"  (IconSetStyleTest.kt:101)
```

Dos cosas cambiaron respecto al plan al ejecutarlo, porque para entonces la Task 5 ya había
migrado el fichero:

- El `pathData` ya no contiene `L8,8` (el icono es ahora tres círculos rellenos en la
  cuadrícula de 16), así que la mutación de gramática fue `M2.7,8` → `MX.7,8`. Es la misma
  clase de error: un dígito sustituido por una letra.
- La rama se borró en lugar de revertirse: no hacía falta un commit de reversión en el
  historial para dejar constancia de lo contrario. `testDebugUnitTest` corre **antes** que
  `assembleDebug` en la CI, así que el rojo de tests se vio igual pese al `<rect>`.

Lo que esta prueba **no** cubre: la tercera guarda profiláctica,
`los cuatro iconos mixtos declaran su trazo`. Es la única de las cinco cuyo mordisco no se
ha visto ni en rojo de CI ni por mutación. Su evidencia actual es solo que está en verde.

- [ ] **Step 7: Commit**

```bash
git add app/src/test/java/com/usipipo/vpn/ui/IconSetStyleTest.kt
git commit -m "test(ui): el set de iconos verifica primitivas, pathData y pares"
```

---

### Task 2: Copia literal del `pathData`

**Files:**
- Modify: `app/src/main/res/drawable/ic_star_filled.xml`, `ic_star.xml`, `ic_empty_shield.xml`, `ic_settings_bubble.xml`
- Test: `IconSetStyleTest.kt` — son las guardas de la Task 1 las que verifican; no se añade nada aquí

**Interfaces:**
- Consumes: la Task 1 (sus cinco guardas). Nada más.
- Produces: la cabecera de vector que las Tasks 3-5 reutilizan, y el par de la estrella con `pathData` compartido, que la Task 1 fija.

- [ ] **Step 1: Escribir los cuatro vectores**

Sustituir el contenido íntegro de cada fichero. La cabecera es la misma en los cuatro:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="16" android:viewportHeight="16">
```

El `pathData` de cada uno sale de `docs/design/iconos-relleno-pathdata.md`, literal:

| Fichero | `<path>` | `fillColor` | atributos de trazo |
|---|---|---|---|
| `ic_empty_shield.xml` | 1, el de `shield` | `#FFFFFFFF` | ninguno |
| `ic_settings_bubble.xml` | 1, el de `message-circle` | `#FFFFFFFF` | ninguno |
| `ic_star_filled.xml` | 1, el de `star` | `#FFFFFFFF` | ninguno |
| `ic_star.xml` | 1, **el mismo** de `star` | `#00000000` | `#FFFFFFFF`, `1.75`, `round`, `round` |

Orden de atributos dentro del `<path>`, el mismo que usa hoy el set (`ic_nav_home.xml`):
`android:pathData`, `android:fillColor` y después, si los hay, `android:strokeColor`,
`android:strokeWidth`, `android:strokeLineCap`, `android:strokeLineJoin`. Un solo
`<path>` por línea, cerrado con `/>` (restricción global 5).

- [ ] **Step 2: Comprobación estática**

Esta máquina no ejecuta tests. Releer los cuatro ficheros contra la fuente de verdad y
confirmar, con `grep`, una cosa cada uno:

```bash
for f in ic_star.xml ic_star_filled.xml; do
  grep -o 'android:pathData="[^"]*"' app/src/main/res/drawable/$f
done
```

Expected: **las dos salidas idénticas**, carácter a carácter. Si difieren en un solo
número, el par se ha desajustado y la Task 1 lo va a cazar.

```bash
grep -c 'viewportWidth="16"' app/src/main/res/drawable/ic_star.xml \
  app/src/main/res/drawable/ic_star_filled.xml \
  app/src/main/res/drawable/ic_empty_shield.xml \
  app/src/main/res/drawable/ic_settings_bubble.xml
```

Expected: `1` en los cuatro.

```bash
grep -rl '<rect\|<circle\|<line' app/src/main/res/drawable/ | grep -v ic_widget | grep -v ic_tile
```

Expected: vacío. Ninguno de los cuatro usa primitivas.

- [ ] **Step 3: Empujar y comprobar el verde**

```bash
git push
```

CI: `./gradlew testDebugUnitTest`.

Expected: **PASS** en los cinco tests de `IconSetStyleTest` — incluidos los dos que
quedaron rojos en la Task 1, que esta tarea arregla: el par de la estrella ya comparte
`pathData`. El de `fillType` seguirá rojo, lo arregla la Task 3.

Además, `assembleDebug` compila: es la prueba de que la cabecera con `viewportWidth=16` y
el `pathData` de `message-circle` (con su arco `A5.75 5.75 0 1 0`) son legibles.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/drawable/ic_star.xml \
        app/src/main/res/drawable/ic_star_filled.xml \
        app/src/main/res/drawable/ic_empty_shield.xml \
        app/src/main/res/drawable/ic_settings_bubble.xml
git commit -m "feat(ui): estrella, escudo y burbuja pasan a relleno solido"
```

---

### Task 3: `fillType="evenOdd"` y subpaths negativos

**Files:**
- Modify: `app/src/main/res/drawable/ic_nav_home.xml`, `ic_shield_check.xml`
- Test: `IconSetStyleTest.kt` — la guarda `los iconos con subpaths negativos declaran el fillType par`, que la Task 1 dejó en rojo

**Interfaces:**
- Consumes: la Task 1 (la guarda del `fillType` y su constante `CON_SUBPATHS_NEGATIVOS`, que ya nombra estos dos ficheros).
- Produces: los dos drawables con `android:fillType="evenOdd"`. Con esto las cinco guardas de la Task 1 quedan en verde y el rojo de la Task 1 se cierra.

- [ ] **Step 1: Escribir `ic_nav_home.xml`**

Un solo `<path>`, con los **dos subpaths concatenados** de `home` en la fuente de verdad
(el contorno de la casa y la puerta en negativo), `android:fillType="evenOdd"` y
`android:fillColor="#FFFFFFFF"`. Sin atributos de trazo.

La cabecera es la de la Task 2. El orden de atributos del `<path>`:
`android:pathData`, `android:fillType`, `android:fillColor`.

- [ ] **Step 2: Escribir `ic_shield_check.xml`**

Dos `<path>`, y **no llevan el mismo `pathData`**.

1. **El relleno**: el bloque `relleno evenodd` de la fuente, que contiene los dos subpaths
   concatenados (el escudo completo y el escudo con el check recortado). Atributos:
   `android:pathData`, `android:fillType="evenOdd"`, `android:fillColor="#FFFFFFFF"`.
2. **El contorno**: el bloque `trazo` de la fuente, que es solo el contorno del escudo.
   Atributos: `android:pathData`, `android:fillColor="#00000000"`,
   `android:strokeColor="#FFFFFFFF"`, `android:strokeWidth="1.75"`,
   `android:strokeLineCap="round"`, `android:strokeLineJoin="round"`.

Es el patrón de la librería para glifos con detalle interior: el relleno debajo con el
hueco ya recortado, el contorno encima. Si los dos paths llevan el mismo `pathData`, el
check sale dos veces.

- [ ] **Step 3: Comprobación estática**

La guarda del `fillType` comprueba el atributo, no que el subpath negativo exista. Un
`pathData` al que le falte el segundo subpath pasa el test y dibuja una casa sin puerta y
un escudo sin check. Se comprueba contando comandos:

```bash
for f in ic_nav_home.xml ic_shield_check.xml; do
  echo -n "$f: M="; grep -o 'M' app/src/main/res/drawable/$f | wc -l | tr -d ' '
  echo -n "     Z="; grep -o 'Z' app/src/main/res/drawable/$f | wc -l | tr -d ' '
done
```

Expected: **`ic_nav_home.xml` → `M=2 Z=2`** y **`ic_shield_check.xml` → `M=3 Z=3`**.

El `grep` cuenta sobre el fichero entero, y los dos ficheros no tienen la misma forma:
`ic_nav_home.xml` es un solo `<path>` con dos subpaths (el contorno de la casa y la
puerta), y `ic_shield_check.xml` son dos `<path>`: el de relleno, con dos subpaths (el
escudo y el check), y el de contorno, con uno. Por eso el segundo suma un `M` y un `Z`
más. Lo que hay que mirar en realidad es por `<path>`: **el de relleno de cada uno tiene
`M=2 Z=2`**. Un path de relleno con `M=1 Z=1` significa que le falta el subpath negativo:
la casa sale sin puerta o el escudo sin check, y hay que volver a la fuente.

*Corrección del 2026-11-10: esta línea esperaba originalmente `M=1 Z=1` para
`ic_nav_home.xml` y `M=2 Z=2` para `ic_shield_check.xml`, y las dos estaban mal. La
primera porque la puerta de la casa es un subpath propio y también cierra; la segunda
porque el `grep` cuenta los dos `<path>` del fichero, no uno. Comprobado sobre los
ficheros escritos: `M=2 Z=2`, `M=3 Z=3`, y por path `2/2`, `2/2` y `1/1`.*

```bash
grep -c 'android:fillType="evenOdd"' app/src/main/res/drawable/ic_nav_home.xml \
  app/src/main/res/drawable/ic_shield_check.xml
```

Expected: `1` y `1` (el contorno de `shield_check` no lo lleva, y no debe).

- [ ] **Step 4: Empujar y comprobar el verde**

```bash
git push
```

CI: `./gradlew testDebugUnitTest`.

Expected: **PASS** en los cinco tests de `IconSetStyleTest`, y en los de
`SystemIntegrationResourceTest`. El rojo de la Task 1 queda cerrado aquí.

`assembleDebug` compila: `android:fillType` existe desde la API 24 y `minSdk` es 26, pero
la compilación es quien lo confirma sin suposiciones.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/drawable/ic_nav_home.xml \
        app/src/main/res/drawable/ic_shield_check.xml
git commit -m "feat(ui): la casa y el escudo recortan su interior con fillType par"
```

---

### Task 4: Mezcla de relleno y trazo

**Files:**
- Modify: `app/src/main/res/drawable/ic_settings_notifications.xml`, `ic_nav_profiles.xml`
- Test: `IconSetStyleTest.kt` — las guardas de la Task 1; esta tarea no añade tests

**Interfaces:**
- Consumes: la Task 1 (las cinco guardas, ya en verde) y la Task 2 (la cabecera y el orden de atributos).
- Produces: dos drawables que mezclan silueta rellena y detalle a trazo, más la conversión de `<line>` a `pathData` que la Task 5 reutiliza para los `<circle>`.

- [ ] **Step 1: Escribir `ic_settings_notifications.xml`**

Dos `<path>`:

1. **El cuerpo de la campana**: el bloque `relleno` de la fuente. Atributos:
   `android:pathData`, `android:fillColor="#FFFFFFFF"`.
2. **El badajo**: el `<line>` de la fuente (`x1=6.5 y1=13.75 x2=9.5 y2=13.75`) convertido a
   `pathData` con la fórmula del spec §4.1: `M6.5,13.75 L9.5,13.75`. Atributos:
   `android:pathData`, `android:fillColor="#00000000"`, `android:strokeColor="#FFFFFFFF"`,
   `android:strokeWidth="1.75"`, `android:strokeLineCap="round"`,
   `android:strokeLineJoin="round"`.

Es el primer `<line>` del plan. `VectorDrawable` no tiene elemento `<line>`: la fórmula
convierte sus dos extremos en un `M` y un `L`.

- [ ] **Step 2: Escribir `ic_nav_profiles.xml`**

Cinco `<path>`, en este orden:

1. **La cabeza**: el `<circle>` de la fuente (`cx=4.5 cy=5.25 r=2`) convertido con la
   fórmula del spec §4.1: `M2.5,5.25 A2,2 0 1 0 6.5,5.25 A2,2 0 1 0 2.5,5.25 Z`.
   Atributos: `android:pathData`, `android:fillColor="#FFFFFFFF"`.
2. **El torso**: el bloque `relleno (path)` de la fuente. Atributos:
   `android:pathData`, `android:fillColor="#FFFFFFFF"`.
3-5. **Las tres rayas de la lista**: los tres `<line>` de la fuente, convertidos:
   `M10,4.5 L14,4.5`, `M10,8 L14,8`, `M10,11.5 L14,11.5`. Cada uno con
   `android:fillColor="#00000000"`, `android:strokeColor="#FFFFFFFF"`,
   `android:strokeWidth="1.75"`, `android:strokeLineCap="round"`,
   `android:strokeLineJoin="round"`.

- [ ] **Step 3: Comprobación estática**

La cabeza es el primer `<circle>` del plan, así que se comprueba su conversión a mano:
`cx-r = 4.5-2 = 2.5`, `cx+r = 4.5+2 = 6.5`, y `cy = 5.25` en los cuatro sitios. Un
`cy` que no coincida entre el `M` y los dos `A` produce un path que no cierra.

```bash
grep -o 'android:pathData="[^"]*"' app/src/main/res/drawable/ic_nav_profiles.xml
```

Expected: cinco salidas. La primera debe ser exactamente
`M2.5,5.25 A2,2 0 1 0 6.5,5.25 A2,2 0 1 0 2.5,5.25 Z`.

```bash
grep -c 'android:strokeWidth="1.75"' app/src/main/res/drawable/ic_nav_profiles.xml \
  app/src/main/res/drawable/ic_settings_notifications.xml
```

Expected: `3` y `1` — las tres rayas y el badajo. Si `ic_nav_profiles.xml` diera `5`, la
cabeza o el torso llevan trazo y no deben.

- [ ] **Step 4: Empujar y comprobar el verde**

```bash
git push
```

CI: `./gradlew testDebugUnitTest`.

Expected: **PASS** en los cinco tests de `IconSetStyleTest`. En particular
`los cuatro iconos mixtos declaran su trazo`, que solo muerde si estos dos ficheros
declaran `strokeColor` en al menos un path.

`assembleDebug` compila: el arco de dos semicircunferencias del `<circle>` convertido es
legible.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/drawable/ic_settings_notifications.xml \
        app/src/main/res/drawable/ic_nav_profiles.xml
git commit -m "feat(ui): campana y perfiles mezclan silueta rellena y detalle a trazo"
```

---

### Task 5: Primitivas `<rect>` y `<circle>` convertidas a `pathData`

**Files:**
- Modify: `app/src/main/res/drawable/ic_qr.xml`, `ic_settings_more.xml`, `ic_settings_widget.xml`
- Test: `IconSetStyleTest.kt` — las guardas de la Task 1; esta tarea no añade tests

**Interfaces:**
- Consumes: la Task 1 (las cinco guardas) y la Task 4 (la conversión de `<circle>` a `pathData`, ya probada con la cabeza de `member-list`).
- Produces: los tres últimos drawables, con los que el set queda completo. Es la tarea con más aritmética del plan, y la última.

- [ ] **Step 1: Escribir `ic_settings_more.xml`**

Tres `<path>`, los tres círculos de `more-horizontal`, todos con
`android:fillColor="#FFFFFFFF"` y sin trazo. `pathData` con la fórmula del spec §4.1:

| origen | `pathData` |
|---|---|
| `cx=3.25 cy=8 r=0.55` | `M2.7,8 A0.55,0.55 0 1 0 3.8,8 A0.55,0.55 0 1 0 2.7,8 Z` |
| `cx=8 cy=8 r=0.55` | `M7.45,8 A0.55,0.55 0 1 0 8.55,8 A0.55,0.55 0 1 0 7.45,8 Z` |
| `cx=12.75 cy=8 r=0.55` | `M12.2,8 A0.55,0.55 0 1 0 13.3,8 A0.55,0.55 0 1 0 12.2,8 Z` |

Los `pathData` de la tabla están calculados y son el valor exacto: se copian, no se recalculan.

- [ ] **Step 2: Escribir `ic_settings_widget.xml`**

Cuatro `<path>`, todos con `android:fillColor="#FFFFFFFF"` y sin trazo. Los tres `<rect>`
con `rx=1` convertidos con la fórmula de rect redondeado del spec §4.1, más el rombo, que
ya viene como `path` en la fuente y se copia literal:

| origen | `pathData` |
|---|---|
| `x=2.25 y=2.25 w=4.25 h=4.25 rx=1` | `M3.25,2.25 L5.5,2.25 A1,1 0 0 1 6.5,3.25 L6.5,5.5 A1,1 0 0 1 5.5,6.5 L3.25,6.5 A1,1 0 0 1 2.25,5.5 L2.25,3.25 A1,1 0 0 1 3.25,2.25 Z` |
| `x=2.25 y=9.5 w=4.25 h=4.25 rx=1` | `M3.25,9.5 L5.5,9.5 A1,1 0 0 1 6.5,10.5 L6.5,12.75 A1,1 0 0 1 5.5,13.75 L3.25,13.75 A1,1 0 0 1 2.25,12.75 L2.25,10.5 A1,1 0 0 1 3.25,9.5 Z` |
| `x=9.5 y=9.5 w=4.25 h=4.25 rx=1` | `M10.5,9.5 L12.75,9.5 A1,1 0 0 1 13.75,10.5 L13.75,12.75 A1,1 0 0 1 12.75,13.75 L10.5,13.75 A1,1 0 0 1 9.5,12.75 L9.5,10.5 A1,1 0 0 1 10.5,9.5 Z` |
| rombo | el bloque `relleno (path del rombo)` de la fuente, literal |

Orden: los tres cuadros y luego el rombo, el orden en que la fuente los lista.

- [ ] **Step 3: Escribir `ic_qr.xml`**

Siete `<path>`, todos con `android:fillColor="#FFFFFFFF"` y sin trazo. Los tres `<rect>`
con `rx=0.8` y los cuatro `<circle>` con `r=0.55`, convertidos:

| origen | `pathData` |
|---|---|
| `x=2.25 y=2.25 w=4.5 h=4.5 rx=0.8` | `M3.05,2.25 L5.95,2.25 A0.8,0.8 0 0 1 6.75,3.05 L6.75,5.95 A0.8,0.8 0 0 1 5.95,6.75 L3.05,6.75 A0.8,0.8 0 0 1 2.25,5.95 L2.25,3.05 A0.8,0.8 0 0 1 3.05,2.25 Z` |
| `x=9.25 y=2.25 w=4.5 h=4.5 rx=0.8` | `M10.05,2.25 L12.95,2.25 A0.8,0.8 0 0 1 13.75,3.05 L13.75,5.95 A0.8,0.8 0 0 1 12.95,6.75 L10.05,6.75 A0.8,0.8 0 0 1 9.25,5.95 L9.25,3.05 A0.8,0.8 0 0 1 10.05,2.25 Z` |
| `x=2.25 y=9.25 w=4.5 h=4.5 rx=0.8` | `M3.05,9.25 L5.95,9.25 A0.8,0.8 0 0 1 6.75,10.05 L6.75,12.95 A0.8,0.8 0 0 1 5.95,13.75 L3.05,13.75 A0.8,0.8 0 0 1 2.25,12.95 L2.25,10.05 A0.8,0.8 0 0 1 3.05,9.25 Z` |
| `cx=10.25 cy=10.25 r=0.55` | `M9.7,10.25 A0.55,0.55 0 1 0 10.8,10.25 A0.55,0.55 0 1 0 9.7,10.25 Z` |
| `cx=12.75 cy=10.25 r=0.55` | `M12.2,10.25 A0.55,0.55 0 1 0 13.3,10.25 A0.55,0.55 0 1 0 12.2,10.25 Z` |
| `cx=10.25 cy=12.75 r=0.55` | `M9.7,12.75 A0.55,0.55 0 1 0 10.8,12.75 A0.55,0.55 0 1 0 9.7,12.75 Z` |
| `cx=12.75 cy=12.75 r=0.55` | `M12.2,12.75 A0.55,0.55 0 1 0 13.3,12.75 A0.55,0.55 0 1 0 12.2,12.75 Z` |

Orden: los tres cuadrados y luego los cuatro puntos, el orden en que la fuente los lista.

- [ ] **Step 4: Comprobación estática**

```bash
grep -rl '<rect\|<circle\|<line' app/src/main/res/drawable/ | grep -vE 'ic_widget|ic_tile'
```

Expected: **vacío**. Es la misma guarda que corre el test, ejecutada a ojo sobre los tres
ficheros que acaban de escribirse. Si sale alguno de los tres, sobra una primitiva.

```bash
grep -c '<path' app/src/main/res/drawable/ic_settings_more.xml \
  app/src/main/res/drawable/ic_settings_widget.xml app/src/main/res/drawable/ic_qr.xml
```

Expected: `3`, `4` y `7`. Un número menor significa que un `<path>` se ha partido en varias
líneas y el `Regex` del contrato no lo verá.

```bash
grep -L 'android:fillType' app/src/main/res/drawable/ic_settings_more.xml \
  app/src/main/res/drawable/ic_settings_widget.xml app/src/main/res/drawable/ic_qr.xml
```

Expected: los tres nombres. Ninguno de estos tres lleva `fillType`: son formas convexas
sin subpaths negativos, y el `nonZero` por defecto las dibuja bien.

- [ ] **Step 5: Empujar, verde de CI y PR**

```bash
git push
gh pr create --base main --head feat/iconos-relleno \
  --title "feat(ui): los once iconos de relleno pasan a silueta solida"
```

CI: `./gradlew testDebugUnitTest` + `assembleDebug` + `assembleRelease`.

Expected: los tres en verde. **`assembleDebug` es la prueba de que los quince `pathData`
convertidos de esta tarea parsean**: es la primera vez en el plan que un arito de radio
0,55 y un rect de esquina 0,8 se compilan de verdad.

- [ ] **Step 6: Cerrar el spec y commit**

Actualizar `docs/superpowers/specs/2026-11-09-iconos-relleno-design.md`, línea de estado:

```
**Estado:** implementado · rama `feat/iconos-relleno` · PR #<n> (2026-11-10)
```

El número de PR se rellena con el real, no se inventa.

```bash
git add app/src/main/res/drawable/ic_qr.xml \
        app/src/main/res/drawable/ic_settings_more.xml \
        app/src/main/res/drawable/ic_settings_widget.xml \
        docs/superpowers/specs/2026-11-09-iconos-relleno-design.md
git commit -m "feat(ui): qr, ajustes y widget convierten sus primitivas a pathData"
```

---

## Recorrido que el usuario debe hacer en dispositivo

Ningún test cubre la lectura visual. Después del merge, el usuario instala el `assembleDebug`
de la CI y comprueba, **en tema claro y oscuro**:

1. **Barra inferior** — Inicio (la casa con la puerta recortada) y Configuraciones (la cabeza
   rellena con las tres rayas). Ahí se ve el peso nuevo: es el sitio donde más superficie
   ocupan.
2. **Favoritos en Configuraciones** — marcar y desmarcar el mismo perfil. La estrella vacía
   y la llena comparten `pathData`, así que la vacía se ve ahora un poco más grande que
   antes (su borde exterior queda 1,75 unidades por fuera). Si ese salto molestara al
   marcar, es el único punto del spec que se decidió por descarte y se puede revisar.
3. **Las cinco acciones de Ajustes** — notificaciones (campana con el badajo a trazo),
   burbuja de tráfico, más, widget y el QR de Importar.
4. **El escudo con check** — en la tarjeta del perfil activo y donde aparezca el check: hay
   que confirmar que el check sale hueco y no como un cuadro blanco. Es lo que fija el
   `evenOdd`, y es lo único que un error aquí rompería sin que nada lo detecte.

## Criterio de aceptación

1. Los once ficheros tienen el `pathData` de la fuente de verdad, `viewportWidth=16`,
   `width/height=24dp`, y ninguno declara `fillColor="#FF000000"`.
2. `ic_star.xml` e `ic_star_filled.xml` comparten `pathData`; se distinguen solo en relleno
   frente a trazo.
3. Los siete iconos con color de estado y la marca quedan intactos: el diff no los nombra.
4. `IconSetStyleTest` en verde con sus cuatro tests originales **sin una aserción modificada**,
   los cinco nuevos en verde, y un KDoc que describe el invariante real.
5. `SystemIntegrationResourceTest` en verde.
6. `assembleDebug` y `assembleRelease` compilan.
7. El usuario ha hecho el recorrido de arriba y lo acepta, o ha reportado qué glifo no lee bien.

## Fuera de alcance

- Los otros 20 iconos del set `ic_*` que `iconmcp` sigue dando solo como contorno.
- Los 3 del mosaico y los 4 del widget.
- Cualquier cambio de `MainActivity.kt`, del túnel o de `exported`.
- Cambiar el tamaño en pantalla de los iconos: siguen siendo 24dp.
