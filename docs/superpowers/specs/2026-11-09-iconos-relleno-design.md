# Spec de diseño — Iconos: paso a relleno sólido

**Fecha:** 2026-11-09
**Estado:** implementado · rama `feat/iconos-relleno` · PR #30 (2026-11-10)
**Proyecto:** uSipipo VPN
**Alcance:** migrar 11 drawables del set propio de iconos (`res/drawable/ic_*.xml`) de contorno a relleno sólido, usando como fuente la librería `iconmcp`. Es la continuación natural del cierre del hallazgo 5 de la auditoría MASVS, y la razón por la que aquel plan dejó fuera «los 3 iconos del mosaico y el contrato de trazo»: **es este trabajo, no aquel**.

**Fuente de verdad del `pathData`:** `docs/design/iconos-relleno-pathdata.md`

---

## 1. Objetivo

El set de iconos propio se dibujó a contorno: trazo de 1.75 sobre relleno transparente. El
contrato que lo fija (`IconSetStyleTest`) lleva ese estilo en su KDoc como si fuera ley del
género, y ya no lo es: la dirección visual de la app es relleno sólido. Los once iconos de
este spec pasan a relleno; los demás se quedan como están.

| Comportamiento | Antes | Después |
|---|---|---|
| Masa visual | Contorno de 1.75, huecos por dentro | Relleno sólido, silueta maciza |
| Barra inferior (`nav_home`, `nav_profiles`) | Techo y puerta a trazo | Casa sólida; cabeza + torso sólidos, 3 rayas a trazo |
| Favorito (`star` / `star_filled`) | Par de contornos con la misma geometría | Par que **sigue** compartiendo geometría: el outline de `iconmcp` sobre el sólido |
| Escudos (`shield_check`, `empty_shield`) | Contorno + check en negativo | Escudo relleno con el check en negativo |
| Acciones de Ajustes (`bubble`, `more`, `widget`, `qr`, `notifications`) | Contorno | Relleno sólido |
| 3 iconos del mosaico + 4 del widget | Color de estado dentro del vector | **Intactos** (ver §5) |

**Qué cambia en pantalla:** la geometría, no el color. Los once se piden con
`Icon(..., tint = onPrimaryContainer)`, y un `tint` de Compose aplica un `ColorFilter` que
pisa todos los colores internos del vector (`ui/components/IconTile.kt` es la prueba). Así
que el `#FFFFFFFF` de dentro es una formalidad; lo que el usuario verá es una silueta más
gruesa.

---

## 2. Decisiones de la sección

| Tema | Decisión | Por qué |
|---|---|---|
| Librería de origen | `iconmcp` (7.609 iconos, 58 sets, grid de 16 unidades, tres estilos: outline / solid / duotone) | Ya está configurado como MCP global en esta máquina; el usuario lo pidió |
| Alcance | Los 11 que son **realmente** sólidos, no los 31 del set | Comprobado uno a uno con `get_icon`: el parámetro `style=solid` de `iconmcp` no siempre da relleno real. `power` sale puro trazo, `home` sale relleno de verdad. Migrar 31 habría sido migrar 21 iconos que siguen siendo de contorno con otro `pathData` |
| Cuadrícula | `viewportWidth/Height = 16`, `width/height = 24dp` | Todos los SVG de `iconmcp` salen con `viewBox="0 0 16 16"`. Mantener `24dp` conserva el tamaño en pantalla de cada punto de uso; solo cambia el sistema de coordenadas interno |
| `stroke-width="0.833"` del SVG | **No se traduce literalamente**: se escribe `strokeWidth="1.75"` | Es una decisión, no una conversión. Ver §4.3: cambiar de cuadrícula cambia el grosor físico del trazo, y hay dos formas de hacerlo |
| `currentColor` | Se sustituye por un color concreto | `VectorDrawable` no entiende `currentColor` (a diferencia de `Icon` en Compose). Los once van teñidos por Compose, así que `#FFFFFFFF` es correcto y no inventa un color nuevo |
| `fillType` | `evenOdd` donde el SVG declara `fill-rule="evenodd"` (`home`, `shield-check`); `nonZero` en el resto | Necesario para que los subpaths en negativo (la puerta de la casa, el check dentro del escudo) salgan huecos y no tapados |
| Contrato de estilo | **No se toca ninguna aserción** de `IconSetStyleTest` | El contrato ya acepta relleno (exige `fillColor` declarado y distinto de `#FF000000`; las aserciones de trazo son condicionales a que exista `strokeColor`). Lo único desactualizado es su KDoc, que habla de «relleno transparente» |
| Los 7 con color de estado | Fuera de alcance | Se pintan en `RemoteViews`, que **no** teñe: el color tiene que ir dentro del vector. Su contrato (`SystemIntegrationResourceTest.TILE_ICONS`) exige `strokeColor` igual al acento, sin cláusula de escape. No se mezclan dos rediseños en una rama |

---

## 3. El contrato que hay que satisfacer

`app/src/test/java/com/usipipo/vpn/ui/IconSetStyleTest.kt`, cuatro tests, sin cambios:

1. `todos los iconos declaran su relleno y ninguno se rellena de negro opaco` — cada
   `<path>` declara `android:fillColor`, y ninguno vale `#FF000000`.
2. `todo trazo del set usa el grosor uniforme y las tapas redondeadas` — **condicional**:
   `if (atributo(bloque, "strokeColor").isEmpty()) return@forEachIndexed`. Los paths solo
   rellenos se saltan; los mixtos (`bell`, `member-list`) deben declarar
   `strokeWidth="1.75"` y `strokeLineCap="round"`.
3. `el set de iconos no esta vacio` — el directorio sigue teniendo `ic_*.xml`.
4. `el set cubre los cuatro iconos de la barra y los de acciones` — comprueba que existen
   `nav_home`, `nav_profiles`, `nav_import`, `nav_settings` y los de acciones. Los once
   migrados **siguen existiendo con el mismo nombre**, así que este test no se entera.

Dos restricciones de formato que el test impone sin decirlo, y que hay que respetar al
escribir el XML:

- `private val PATH = Regex("<path\\s[^>]*?/>")` — el elemento tiene que empezar por
  `<path` seguido de un espacio, no contener ningún `>` hasta el cierre, y cerrar con `/>`
  **autocontenido**. Es decir: un `<path>` por línea, con sus atributos juntos, nunca
  partido en varias líneas ni con hijos.
- `atributo()` lee `android:NOMBRE="..."`. Los atributos van con el prefijo `android:`.

**Sí hay que cambiar** el KDoc de la clase (líneas 9-12): hoy afirma «trazo de 1.75, tapas
redondeadas y relleno transparente», que un rediseño a relleno vuelve falsa. Se reescribe
para que describa el invariante real: *relleno declarado explícito, nunca negro opaco; y el
trazo, cuando lo hay, con el grosor y las tapas del set*. El porqué del `fillColor`
explícito se mantiene: sin él Android rellena de negro opaco.

---

## 4. Los once iconos y su conversión

Fuente: `.superpowers/sdd/2026-11-09-iconos-relleno-plan/fuente-pathdata.md`, con el
`pathData` literal ya extraído y verificado de `get_icon`.

| Fichero destino | icono `iconmcp` | estilo | particularidad |
|---|---|---|---|
| `ic_nav_home.xml` | `home` | solid | 2 subpaths, `fill-rule="evenodd"` |
| `ic_nav_profiles.xml` | `member-list` | solid | círculo + path rellenos + **3 `<line>` a trazo** |
| `ic_star.xml` | `star` | **outline** | sin relleno: `fillColor="#00000000"` + trazo 1.75 |
| `ic_star_filled.xml` | `star` | solid | el **mismo** pathData que `ic_star.xml`, rellenado |
| `ic_empty_shield.xml` | `shield` | solid | 1 path |
| `ic_shield_check.xml` | `shield-check` | solid | 2 paths: relleno `evenodd` + contorno a trazo |
| `ic_qr.xml` | `qr-code` | solid | 3 `<rect>` + 4 `<circle>` |
| `ic_settings_notifications.xml` | `bell` | solid | path relleno + 1 `<line>` a trazo |
| `ic_settings_bubble.xml` | `message-circle` | solid | 1 path, con un arco `A` de `large-arc-flag=1` |
| `ic_settings_more.xml` | `more-horizontal` | solid | 3 `<circle>` |
| `ic_settings_widget.xml` | `widget` | solid | 3 `<rect>` + 1 path relleno |

### 4.1 Reglas de conversión SVG → VectorDrawable

`VectorDrawable` solo tiene `<path>`, `<group>`, `<clip-path>` y `<aapt:attr>`. **No
existen `<line>`, `<circle>` ni `<rect>`**: hay que convertirlos a `pathData`. Las
fórmulas, todas sobre las coordenadas primitivas `x,y,w,h,r`:

**`<rect>` sin redondeo** (`rx = 0`):
```
M x,y L x+w,y L x+w,y+h L x,y+h Z
```

**`<rect>` con redondeo** (`rx`; los tres rects del spec tienen esquinas iguales en ambos
ejes, así que se usa `rx` para los cuatro vértices):
```
M x+rx,y L x+w-rx,y A rx,rx 0 0 1 x+w,y+rx
L x+w,y+h-rx A rx,rx 0 0 1 x+w-rx,y+h
L x+rx,y+h A rx,rx 0 0 1 x,y+h-rx
L x,y+rx A rx,rx 0 0 1 x+rx,y Z
```

**`<circle>`** (`cx,cy,r`), dos semicircunferencias con el mismo flag de barrido:
```
M cx-r,cy A r,r 0 1 0 cx+r,cy A r,r 0 1 0 cx-r,cy Z
```

**`<line>`** (`x1,y1 → x2,y2`): como path con trazo, que es la única forma de dibujarla:
```
M x1,y1 L x2,y2
```
con `android:fillColor="#00000000"` (para el test 1), `android:strokeColor="#FFFFFFFF"`,
`android:strokeWidth="1.75"` y `android:strokeLineCap="round"`. Todos los `<line>` del set
son horizontales.

**Todo lo demás se copia tal cual.** El `pathData` de `iconmcp` y el de Android comparten
sintaxis (`M L C A V H Z` y sus minúsculas, separados por espacios o comas), y los flags
de arco son 0 o 1 en los dos. El `<g>` que envuelve los paths del SVG no aporta atributos
heredables en ninguno de los once, así que se omite.

**Cabecera del vector**, idéntica en los once (el `24dp` es lo que ya tienen hoy todos los
drawables del set):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="16" android:viewportHeight="16">
```

### 4.2 La decisión sobre el grosor del trazo

El `pathData` de la fuente está en una cuadrícula de 16. La cabecera de arriba declara
`viewportWidth=16`, así que la geometría entra **sin tocar un solo número**. El precio está
en los trazos, y es aritmético, no de gusto:

- Con `viewportWidth=16` y `width=24dp`, una unidad del vector son **1,5dp**. El
  `strokeWidth="1.75"` que exige el contrato sale a **2,625dp**.
- Los iconos que **no** se migran siguen en `viewportWidth=24` con `width=24dp`, donde una
  unidad es 1dp: su `strokeWidth="1.75"` sale a **1,75dp**.

O sea: los cuatro de los once que llevan trazo (`ic_star.xml`, el contorno de
`ic_shield_check.xml`, las 3 rayas de `ic_nav_profiles.xml`, el badajo de
`ic_settings_notifications.xml`) quedan con el detalle un 50% más grueso que el resto del
set. Los otros siete no tienen trazo, así que no les afecta.

La alternativa es mantener todo el set en una sola cuadrícula: `viewportWidth=24` y
multiplicar las 16 coordenadas por 1,5. Entonces el trazo sale a 1,75dp, igual que los no
migrados, y la geometría es la misma. El coste es que **cada número del `pathData` pasa por
una multiplicación y un redondeo** (varios cientos en los once), es decir exactamente la
clase de error de transcripción
que `AGENTS.md` paso 7 prohíbe (§8).

| Opción | Geometría | Trazo de los 4 | Coste |
|---|---|---|---|
| **A — viewport 16, pathData tal cual** | exactamente la de `iconmcp` | 2,625dp | ninguno: los números se copian |
| B — viewport 24, pathData × 1,5 | igual, en otra cuadrícula | 1,75dp | multiplicar y redondear ~200 números |

**Decisión: A**, aprobada por el usuario el 2026-11-10. Tres argumentos. Primero, contra una
silueta maciza un detalle de 1,75dp se ve pobre; 2,625dp sostiene el peso. Segundo, el
contrato se cumple igual en las dos (exige el número `1.75`, no los dp). Tercero y
principal: A no toca el `pathData`, así que no hay nada que transcribir mal.

Consecuencia que el plan debe respetar: **el `pathData` se copia literal de la fuente de
verdad, sin ninguna operación aritmética sobre sus números**. Si un implementador se ve
tentado de reescalar cualquier coordenada, está saliendo de lo aprobado.

### 4.3 Los dos casos que hay que mirar dos veces

**`ic_shield_check.xml`** — el relleno lleva, concatenado tras la primera `Z`, el contorno
del escudo dibujado otra vez. En Android eso son dos `<path>`: el de relleno con
`fillColor="#FFFFFFFF"` y `fillType="evenOdd"` más el `pathData` completo con los dos
subpaths, y el de contorno con `fillColor="#00000000"`, `strokeColor="#FFFFFFFF"`,
`strokeWidth="1.75"`, `strokeLineCap="round"` y el `pathData` del contorno. Es el patrón de
la librería para glifos con detalle interior: relleno debajo, contorno encima.

**`ic_star.xml` vs `ic_star_filled.xml`** — el par tiene que seguir leyéndose como el mismo
glifo en dos estados. Se consigue con el **mismo** `pathData` en los dos ficheros: relleno
transparente + trazo en `ic_star.xml`, relleno sólido en `ic_star_filled.xml`. No se
sustituye cada uno por un icono distinto.

---

## 5. Lo que queda intacto, a propósito

| Grupo | Ficheros | Por qué |
|---|---|---|
| Mosaico | `ic_tile_shield_connected.xml`, `ic_tile_shield_disconnected.xml`, `ic_tile_shield_failed.xml` | Se pintan en `RemoteViews`; el color de estado (conectado/desconectado/fallo) va **dentro** del vector. `SystemIntegrationResourceTest.TILE_ICONS` lo exige: cada `<path>` declara `strokeColor` igual al acento de su estado, sin excepción posible |
| Widget | `ic_widget_power.xml`, `ic_widget_power_failed.xml`, `ic_widget_add.xml`, `ic_widget_shield_check.xml` | Ídem: `RemoteViews` no teñe. Su contrato está en `SystemIntegrationResourceTest.WIDGET_ASSETS` |
| Marca | `ic_logo_u.xml`, `ic_launcher_foreground.xml` | Excluidos del contrato a propósito (`iconosDelSet()` los filtra) y con escala propia |

Antes de escribir el código, el implementador **debe** comprobar con `grep` que ninguno de
los once aparece en `app/src/main/res/layout/` ni en código de `RemoteViews`. Si aparece
alguno, ese icono **no se migra**: el `#FFFFFFFF` sería invisible sobre fondo claro. Hoy
ninguno aparece (son los siete de arriba los que están en el widget y el mosaico), pero es
una comprobación, no una suposición.

---

## 6. Verificación

**Esta máquina no tiene JDK, ni Android SDK, ni Gradle, y no se instala nada.** Ni con
`apt`, ni con `sdkmanager`, ni por descarga directa: el repo trae `gradlew` y su wrapper
porque son de CI. El agente escribe el código y la CI emite el veredicto
(`AGENTS.md` §«Entorno»). Nada de esta sección se ejecuta en local.

| Comprobación | Quién | Cómo |
|---|---|---|
| Los cuatro tests de `IconSetStyleTest` | CI, `testDebugUnitTest` | Deben seguir en verde **sin cambiar ninguna aserción**. Si alguno se pone rojo, el XML está mal, no el test |
| Los 4 tests de seguridad + TILE_ICONS + WIDGET_ASSETS de `SystemIntegrationResourceTest` | CI, `testDebugUnitTest` | En verde. Confirman que los siete con color de estado quedaron intactos |
| Que los vectores parsean de verdad | CI, `assembleDebug` | La compilación es la prueba de que `pathData`, `fillType` y los arcos son legibles por Android. Un `pathData` mal formado rompe el build, no el test |
| Que no pierde tamaño | lectura del diff | `width/height` siguen siendo `24dp`; solo cambia el viewport |

**La verificación visual es del usuario, en su dispositivo.** Esta máquina no puede
renderizar (no hay SDK, y el usuario no puede abrir enlaces en el PC local). Lo que el
código puede garantizar es que los vectores parsean y que satisfacen el contrato; lo que
solo se ve en pantalla —si la silueta rellena lee bien a 24dp, si el par
`star`/`star_filled` sigue reconociéndose, si el check dentro del escudo sale hueco— lo
comprueba el usuario. El entregable incluye un recorrido sugerido: barra inferior
(Inicio/Configuraciones), favoritos en Configuraciones, las cinco acciones de Ajustes, y el
QR de Importar, en tema claro y oscuro.

---

## 7. Fuera de alcance

- Los otros 20 iconos del set `ic_*` (los que `iconmcp` da solo como contorno con `style=solid`).
- Los 3 del mosaico y los 4 del widget (§5).
- Cualquier cambio de `IconSetStyleTest` más allá del KDoc.
- Cualquier cambio de `MainActivity.kt`, del túnel o de `exported`.
- Cambiar el tamaño en pantalla de los iconos: siguen siendo 24dp.
- Nuevos iconos que no existan todavía en el set.

---

## 8. Riesgos y cómo se tratan

| Riesgo | Tratamiento |
|---|---|
| `pathData` mal copiado del fichero fuente | El fichero fuente es la única entrada. El implementador no vuelve a llamar a `get_icon` ni reescribe el path de memoria |
| Un `<rect>`/`<circle>`/`<line>` colado sin convertir | El build falla en CI: `VectorDrawable` rechaza el elemento. Es un rojo imposible de ignorar, que es exactamente lo que se quiere |
| `stroke-width="0.833"` traducido literalmente | El test 2 lo pilla: exige `"1.75"` |
| El KDoc sigue describiendo el estilo viejo | Es un cambio de este plan, no un recordatorio |
| El usuario no puede ver el resultado sin compilar | La rama se sube y la CI compila `assembleDebug`; el usuario instala ese APK y juzga en dispositivo |
| Migrar iconos que se usan en `RemoteViews` | §5, comprobación con `grep` antes de tocar nada |

---

## 9. Criterio de aceptación

1. Los once ficheros tienen el `pathData` de la fuente de verdad, `viewportWidth=16`, `width/height=24dp`, y ninguno declara `fillColor="#FF000000"`.
2. `ic_star.xml` e `ic_star_filled.xml` comparten `pathData`; se distinguen solo en relleno vs trazo.
3. Los siete iconos con color de estado están **byte a byte iguales** salvo que la rama no los toque (el diff lo prueba).
4. `IconSetStyleTest` en verde sin una sola aserción modificada; su KDoc describe el invariante nuevo.
5. `SystemIntegrationResourceTest` en verde.
6. `assembleDebug` compila: los vectores parsean.
7. El usuario ha visto el resultado en su dispositivo y lo acepta, o ha reportado qué glifo no lee bien.
