# uSipipo VPN: correcciones visuales tras el rediseño del mockup

Fecha: 2026-10-21
Estado: diseño aprobado, pendiente de plan de implementación

## Contexto

El rediseño del mockup (fases 1 a 3) está implementado y el CI está en verde, pero al
probar la APK en el teléfono se ven cuatro diferencias con el mockup:

1. No aparece la barra inferior de 4 pestañas.
2. El icono de la app en la pantalla de inicio del dispositivo se ve con zoom y recortado.
3. Hay iconos rotos o incorrectos.
4. El Home no tiene el fondo con mapa del mundo del mockup.

La referencia visual son las cinco capturas del mockup en `docs/design/`:

| Captura | Pantalla |
|---|---|
| `Screenshot_2026-10-21-05-22-04-969_com.miui.gallery.jpg` | 1. Pantalla principal |
| `Screenshot_2026-10-21-05-22-24-600_com.miui.gallery.jpg` | 2. Gestión de configuraciones |
| `Screenshot_2026-10-21-05-22-39-673_com.miui.gallery.jpg` | 3. Importación |
| `Screenshot_2026-10-21-05-23-03-578_com.miui.gallery.jpg` | 4. Diagnóstico y ajustes avanzados |
| `Screenshot_2026-10-21-05-23-33-313_com.miui.gallery.jpg` | 5. Detalles del perfil |

> `docs/design/mockup-ia.png` está vacío (0 bytes) en el repositorio: la referencia real son las
> capturas de arriba.

## Causas encontradas en el código

| # | Síntoma | Causa |
|---|---------|-------|
| 1 | Sin barra inferior | En `UsipipoRoot.kt` el `AnimatedContent` está dentro de un `Column` sin `weight(1f)`. Cada pantalla usa `fillMaxSize()`, ocupa todo el alto y deja la barra con altura cero. |
| 1 | Iconos de la barra genéricos | `UsipipoBottomBar.kt` usa `Icons.Default.Home`, `Settings` y `Build` de Material. |
| 2 | Logo con zoom | `ic_launcher_foreground.xml` dibuja la "u" con trazos de 20 unidades y coordenadas hasta 104 sobre un lienzo de 108. Se sale de la zona segura (66 dp centrales) y su forma no coincide con `usipipo_logo.jpg`. |
| 3 | Flecha "←" en lugar de ">" | `ic_arrow_right.xml` dibuja una flecha hacia la izquierda. |
| 3 | "+" que se ve como "1" | `ic_add.xml` tiene un trazo mal formado (`M12,2v16h-2V6H6V4H12z`). |
| 4 | Sin mapa | No existe ningún recurso de mapa; `HomeBackground` solo dibuja un degradado vertical. |

## Decisiones

- **Mapa del Home:** mapa de puntos vectorial y ligero, sin desenfoques (descartados: imagen PNG/WebP y dejarlo sin mapa).
- **Iconos:** set propio en estilo contorno con trazo uniforme (descartados: `material-icons-extended` y arreglar solo los rotos).

## Diseño

### 1. Barra inferior e iconos

- En `UsipipoRoot.kt`, el contenido de las pestañas ocupa solo el espacio sobrante
  (`Modifier.weight(1f)`) y la barra queda fija abajo, respetando la barra de gestos del
  sistema. Visible en las cuatro pestañas y también en Detalles.
- Estilo: fondo azul noche (como el mockup), pestaña activa en azul con etiqueta resaltada,
  inactivas en gris azulado.
- Set de iconos propio, en contorno, con el mismo grosor de trazo (1.75 dp):
  - Barra: inicio, configuraciones, importar, diagnóstico.
  - Flechas: ">" para ir a un detalle y "←" para volver.
  - Acciones: "+", estrella (vacía y llena), lápiz, papelera, QR, reloj, descarga, subida, servidor.
- Se eliminan los iconos de Material (`Home`, `Settings`, `Build`, `Add`) y los dos iconos
  rotos (`ic_arrow_right`, `ic_add`).
- Previews de Compose de la barra y de cada icono, en claro y oscuro.

### 2. Logo del launcher y mapa del Home

**Logo**
- `ic_launcher_foreground.xml` se rehace con la forma exacta de `usipipo_logo.jpg`
  (la "u" con el rabo derecho alto y el guion bajo a la izquierda).
- La "u" se centra y se escala a unos 58 de los 108 dp, dentro de la zona segura.
- Fondo `#004AAD`; el icono monocromo usa la misma forma.
- `ic_logo_u` (cabecera de la app) se genera a partir del mismo trazado.

**Mapa**
- Mapa de puntos dibujado con `Canvas` a partir de una máscara de tierra de unos 72×36
  puntos guardada como texto (menos de 1 KB). Sin imágenes ni desenfoques.
- Se dibuja detrás del contenido y encima del degradado, centrado detrás del botón de
  conectar, con desvanecimiento hacia los bordes y hacia abajo.
- Estático, dibujado una vez por tamaño de pantalla (`drawWithCache`).
- Opacidad baja: más marcada en el tema oscuro (puntos turquesa-azulados), casi
  imperceptible en el claro.
- Solo en el Home. El degradado y las fases de Atmósfera (azul, turquesa, rosado en fallo)
  no cambian.

### 3. Alcance, orden y pruebas

**Alcance:** solo la capa visual: `UsipipoRoot.kt`, `UsipipoBottomBar.kt`, iconos en
`res/drawable`, icono del launcher, `AppLogo` y fondo del Home (archivo nuevo para el
mapa). No se toca la lógica del túnel, los datos, la geolocalización, el ping ni el
diagnóstico.

**Orden de trabajo**
1. Barra inferior visible.
2. Set de iconos propio y sustitución de los de Material y los rotos.
3. Icono del launcher y logo de la cabecera.
4. Mapa de puntos del Home.

**Pruebas**
- Test de JVM: la máscara del mapa tiene el tamaño correcto y contiene tierra y mar.
- Los tests existentes (`MainTabTest`, `PaletteContrastTest`, etc.) siguen pasando; lo
  confirma el CI de GitHub.
- Previews de Compose de la barra, de cada icono y del Home con el mapa, en claro y oscuro.
- Verificación manual en el teléfono con la APK del CI: barra visible en las 4 pestañas,
  icono del launcher sin zoom (círculo y cuadrado redondeado), flechas ">" y "+" correctas,
  mapa visible en el Home.

## Riesgos

- El diseño se escribió sin poder compilar ni ejecutar la app, por lo que la comprobación
  visual final depende de la APK del CI en el teléfono.
- Si el icono del launcher sigue sin verse bien en la capa de personalización de Xiaomi,
  se ajusta el tamaño de la "u" en una segunda pasada.
