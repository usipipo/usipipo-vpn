# Fuente de verdad — `pathData` de iconmcp para el paso a relleno (verificado el 2026-11-10)

Servidor MCP `iconmcp`, estilo pedido con el parámetro `style`, formato `svg`.
**Todos los SVG salen con `viewBox="0 0 16 16"`** (grid de 16 unidades), NO 24.
El parámetro `size` solo cambia `width`/`height` del `<svg>`; no reescala el viewBox.

Acompaña a `docs/superpowers/specs/2026-11-09-iconos-relleno-design.md`. Este fichero está
versionado a propósito: `.superpowers/` está en `.gitignore`, así que una fuente de verdad
guardada allí no llegaría al worktree del que la implementa.

Cada bloque de abajo es el contenido literal del `<g>` del icono, copiado de la
respuesta de `get_icon`. Es la fuente de verdad: el implementador convierte ESTO y no
vuelve a pedir el icono.

Convención de este fichero:
- `relleno:` → el `<path>` lleva `fill="currentColor"` (o `fill-rule="evenodd"`)
- `trazo:` → `<line>` o `<path>` sin fill, hereda `fill="none"` del `<svg>`
- `primitiva:` → `<rect>` / `<circle>`, que **no existen en VectorDrawable**

---

## `home` — solid → `ic_nav_home.xml`

Un solo path, relleno con `fill-rule="evenodd"` (la puerta sale en negativo).

```
relleno evenodd:
M2.569 6.249 L2.625 6.176 L2.936 5.859 L2.97 5.828 L6.47 2.964 L6.509 2.935 L6.901 2.684
L6.918 2.674 L6.988 2.642 L7.416 2.491 L7.507 2.47 L7.953 2.42 L8 2.417 L8.047 2.42
L8.493 2.47 L8.584 2.491 L9.012 2.642 L9.082 2.674 L9.099 2.684 L9.491 2.935 L9.53 2.964
L13.03 5.828 L13.064 5.859 L13.375 6.176 L13.431 6.249 L13.668 6.633 L13.701 6.705
L13.845 7.12 L13.865 7.21 L13.914 7.652 L13.917 7.698 L13.917 11.5 L13.914 11.548
L13.861 12.006 L13.84 12.099 L13.689 12.52 L13.658 12.588 L13.648 12.603 L13.412 12.975
L13.355 13.046 L13.046 13.355 L12.975 13.412 L12.603 13.648 L12.588 13.658 L12.52 13.689
L12.099 13.84 L12.006 13.861 L11.548 13.914 L11.5 13.917 L4.5 13.917 L4.452 13.914
L3.994 13.861 L3.901 13.84 L3.48 13.689 L3.412 13.658 L3.397 13.648 L3.025 13.412
L2.954 13.355 L2.645 13.046 L2.588 12.975 L2.352 12.603 L2.342 12.588 L2.311 12.52
L2.16 12.099 L2.139 12.006 L2.086 11.548 L2.083 11.5 L2.083 7.698 L2.086 7.652 L2.135 7.21
L2.155 7.12 L2.299 6.705 L2.332 6.633 Z
M6.667 13.5 L6.667 11.228 L6.698 10.992 L6.777 10.806 L6.898 10.648 L7.056 10.527
L7.242 10.448 L7.478 10.417 L8.522 10.417 L8.758 10.448 L8.944 10.527 L9.102 10.648
L9.223 10.806 L9.302 10.992 L9.333 11.228 L9.333 13.5 L9.389 13.708 L9.542 13.861
L9.75 13.917 L9.958 13.861 L10.111 13.708 L10.167 13.5 L10.167 11.2 L10.163 11.145
L10.12 10.825 L10.091 10.719 L9.97 10.432 L9.947 10.386 L9.916 10.34 L9.728 10.097
L9.653 10.022 L9.41 9.834 L9.364 9.803 L9.318 9.78 L9.031 9.659 L8.925 9.63 L8.605 9.587
L8.55 9.583 L7.45 9.583 L7.395 9.587 L7.075 9.63 L6.969 9.659 L6.682 9.78 L6.636 9.803
L6.59 9.834 L6.347 10.022 L6.272 10.097 L6.084 10.34 L6.053 10.386 L6.03 10.432
L5.909 10.719 L5.88 10.825 L5.837 11.145 L5.833 11.2 L5.833 13.5 L5.889 13.708
L6.042 13.861 L6.25 13.917 L6.458 13.861 L6.611 13.708 Z
```

---

## `star` — solid → `ic_star_filled.xml`

Un solo path, relleno.

```
relleno:
M8.454 3.234 L9.646 5.819 C9.719 5.977 9.869 6.085 10.041 6.106 L12.868 6.441
C13.291 6.491 13.461 7.015 13.148 7.305 L11.059 9.237 C10.931 9.355 10.874 9.531
10.908 9.702 L11.462 12.493 C11.546 12.912 11.1 13.235 10.728 13.027 L8.244 11.637
C8.092 11.552 7.908 11.552 7.756 11.637 L5.272 13.027 C4.9 13.235 4.454 12.912
4.538 12.493 L5.092 9.702 C5.126 9.531 5.069 9.355 4.941 9.237 L2.852 7.305
C2.539 7.015 2.709 6.491 3.132 6.441 L5.959 6.106 C6.131 6.085 6.281 5.977
6.354 5.819 L7.546 3.234 C7.725 2.847 8.275 2.847 8.454 3.234 Z
```

## `star` — outline → `ic_star.xml`

**El mismo pathData exacto, sin relleno.** Es el par: el fichero `ic_star.xml` es este
mismo path con `fillColor="#00000000"` y `strokeColor="#FFFFFFFF"`. Así el par
`star` / `star_filled` comparte geometría, que es lo que hace que se lean como el
mismo glifo en dos estados.

---

## `shield` — solid → `ic_empty_shield.xml`

Un solo path, relleno.

```
relleno:
M 8 1.75 C 9.625 2.75 11.25 3.25 13 3.25 L 13 7.5 C 13 10.5 10.875 12.875 8 14.25
C 5.125 12.875 3 10.5 3 7.5 L 3 3.25 C 4.75 3.25 6.375 2.75 8 1.75 Z
```

---

## `shield-check` — solid → `ic_shield_check.xml`

Dos paths. **El primero es el relleno real** (`fill="currentColor" fill-rule="evenodd"`,
`stroke="none"`) y lleva dentro, concatenado tras la primera `Z`, el contorno del escudo
dibujado de nuevo para que el check salga en negativo. El segundo path es el mismo
contorno **sin fill**, que hereda `fill="none"` del `<svg>`.

```
relleno evenodd (contiene dos subpaths: escudo completo + escudo con el check recortado):
M5 2.5 L11 2.5 C12.105 2.5 13 3.395 13 4.5 L13 7.129 C13 7.374 12.955 7.617 12.867 7.847
L11.929 10.284 C11.812 10.59 11.62 10.862 11.372 11.077 L8 14 L4.628 11.077 C4.38 10.862
4.188 10.59 4.071 10.284 L3.133 7.847 C3.045 7.617 3 7.374 3 7.129 L3 4.5 C3 3.395
3.895 2.5 5 2.5 Z
M7.042 9.861 L7.25 9.917 L7.458 9.861 L7.545 9.795 L10.795 6.545 L10.861 6.458
L10.917 6.25 L10.861 6.042 L10.708 5.889 L10.5 5.833 L10.292 5.889 L10.205 5.955
L7.25 8.911 L5.795 7.455 L5.708 7.389 L5.5 7.333 L5.292 7.389 L5.139 7.542
L5.083 7.75 L5.139 7.958 L5.205 8.045 L6.955 9.795 Z

trazo (hereda fill="none"; contorno del escudo):
M5 2.5 L11 2.5 C12.105 2.5 13 3.395 13 4.5 L13 7.129 C13 7.374 12.955 7.617 12.867 7.847
L11.929 10.284 C11.812 10.59 11.62 10.862 11.372 11.077 L8 14 L4.628 11.077 C4.38 10.862
4.188 10.59 4.071 10.284 L3.133 7.847 C3.045 7.617 3 7.374 3 7.129 L3 4.5 C3 3.395
3.895 2.5 5 2.5 Z
```

**Ojo con este icono**: el segundo path es geometría duplicada del primero. En
VectorDrawable son dos `<path>` con el mismo `android:pathData` y distinto
`fillColor`/`strokeColor`. Es el patrón de esta librería para los glifos cuyo interior
lleva detalle: el relleno por debajo, el contorno por arriba. El `<g>` que envuelve
ambos paths no aporta atributos heredables, así que se puede omitir en Android.

---

## `bell` — solid → `ic_settings_notifications.xml`

Cuerpo relleno + badajo en trazo.

```
relleno:
M3.75 10.75 V6.5 A4.25 4.25 0 0 1 12.25 6.5 V10.75 L13.25 11.75 H2.75 Z

trazo (line):
x1=6.5 y1=13.75 x2=9.5 y2=13.75
```

---

## `more-horizontal` — solid → `ic_settings_more.xml`

 Tres círculos, todos relleno.

```
primitiva circle: cx=3.25  cy=8 r=0.55
primitiva circle: cx=8     cy=8 r=0.55
primitiva circle: cx=12.75 cy=8 r=0.55
```

---

## `message-circle` — solid → `ic_settings_bubble.xml`

Un solo path, relleno.

```
relleno:
M6.3 13.15 A5.75 5.75 0 1 0 2.85 9.7 L2.25 13.75 Z
```

---

## `member-list` — solid → `ic_nav_profiles.xml`

 Cabeza y torso rellenos; las tres rayas de la lista en trazo.

```
relleno (circle): cx=4.5 cy=5.25 r=2
relleno (path):   M1.5 13 C1.8 10.75 3 9.25 4.5 9.25 C6 9.25 7.2 10.75 7.5 13

trazo (line): x1=10 y1=4.5  x2=14 y2=4.5
trazo (line): x1=10 y1=8    x2=14 y2=8
trazo (line): x1=10 y1=11.5 x2=14 y2=11.5
```

---

## `qr-code` — solid → `ic_qr.xml`

Tres cuadrados rellenos + cuatro puntos rellenos.

```
primitiva rect: x=2.25  y=2.25 w=4.5 h=4.5 rx=0.8
primitiva rect: x=9.25  y=2.25 w=4.5 h=4.5 rx=0.8
primitiva rect: x=2.25  y=9.25 w=4.5 h=4.5 rx=0.8
primitiva circle: cx=10.25 cy=10.25 r=0.55
primitiva circle: cx=12.75 cy=10.25 r=0.55
primitiva circle: cx=10.25 cy=12.75 r=0.55
primitiva circle: cx=12.75 cy=12.75 r=0.55
```

---

## `widget` — solid → `ic_settings_widget.xml`

Tres cuadros rellenos + un rombo relleno.

```
primitiva rect: x=2.25 y=2.25 w=4.25 h=4.25 rx=1
primitiva rect: x=2.25 y=9.5  w=4.25 h=4.25 rx=1
primitiva rect: x=9.5  y=9.5  w=4.25 h=4.25 rx=1

relleno (path del rombo, esquinas redondeadas):
M12.191 2.316 L13.684 3.809 C13.997 4.122 13.997 4.628 13.684 4.941 L12.191 6.434
C11.878 6.747 11.372 6.747 11.059 6.434 L9.566 4.941 C9.253 4.628 9.253 4.122
9.566 3.809 L11.059 2.316 C11.372 2.003 11.878 2.003 12.191 2.316 Z
```

---

## Los siete que NO se tocan

Con color de estado dentro del vector (RemoteViews no tiñe), se quedan como están:

`ic_tile_shield_connected.xml`, `ic_tile_shield_disconnected.xml`,
`ic_tile_shield_failed.xml`, `ic_widget_power.xml`, `ic_widget_power_failed.xml`,
`ic_widget_add.xml`, `ic_widget_shield_check.xml`.

Y la marca, excluida del contrato a propósito: `ic_logo_u.xml`,
`ic_launcher_foreground.xml`.
