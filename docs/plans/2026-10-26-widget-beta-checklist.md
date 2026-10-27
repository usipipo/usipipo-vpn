# uSipipo VPN: matriz manual del widget y el mosaico

Fecha: 2026-10-26
Rama: `feat/mockup-fase6`
Paquete: `com.usipipo.vpn`

Esta matriz la ejecuta una persona en un dispositivo real. Todo lo automático ya está
cubierto por la CI (tests JVM y generación de APK); lo que queda aquí es lo que solo se
puede ver en un launcher, en un panel de ajustes rápidos y con una VPN de verdad.

## Qué se prueba

- **Widget de pantalla de inicio**: tarjeta 2×2 con logo, píldora de estado, botón de
  conectar/desconectar, nombre del perfil y país cacheado.
- **Mosaico de ajustes rápidos**: acceso desde el panel desplegable del sistema para
  conectar y desconectar el perfil seleccionado.

## APK

- Descarga el artefacto `usipipo-vpn-debug-apk` de la última CI verde de la rama
  `feat/mockup-fase6`: <https://github.com/usipipo/usipipo-vpn/actions>
- Instálala con `adb install -r app-debug.apk` o copiándola al dispositivo y abriéndola.
- El paquete es `com.usipipo.vpn`. Si tenías una versión anterior instalada, actualiza
  encima: el widget y el mosaico se refrescan solos tras la actualización.

## Cómo preparar el dispositivo

1. Instala la APK y ábrela una vez. Concede el permiso de VPN cuando lo pida (es lo
   único que muestra una pantalla del sistema).
2. **Widget**: mantén pulsada una zona vacía de la pantalla de inicio → **Widgets** →
   busca **uSipipo VPN** → arrastra el widget a la pantalla.
3. **Mosaico**: abre **Ajustes** → panel de ajustes rápidos (el tirador de arriba a la
   derecha, o dos dedos hacia abajo) → **Editar** (icono de lápiz) → busca **uSipipo VPN**
   → toca y mantenlo pulsado para moverlo a la posición que prefieras.
4. Importa al menos un perfil válido (QR o archivo `.conf`) para poder probar la
   conexión. Deja también un segundo perfil con un nombre largo para la prueba de recorte.

## Cómo reportar

Una línea por comprobación: **OK**, **falla** o **no aplica**, más una nota breve cuando
falla. Si falla, anota:

- Estado visible en el widget o en el mosaico (la palabra exacta de la píldora).
- Nombre del perfil seleccionado.
- Versión de Android (`Ajustes → Acerca del teléfono`).
- Nombre y versión del launcher (launcher de fábrica, Pixel Launcher, Nova, etc.).
- Si el fallo aparece solo con el mosaico, con el widget o con ambos.

---

## 1. Presentación del widget

| Comprobación | Resultado esperado |
|---|---|
| Widget recién añadido, sin perfil importado | Botón con un **+**, píldora «Importar perfil», sin nombre ni país |
| Con un perfil importado | Se ve el logo «u», la píldora de estado, el botón de power y el **nombre del perfil** |
| Con un perfil que tiene país en caché | Bajo el nombre aparece **bandera + nombre del país** |
| Con un perfil sin país en caché | Solo aparece el nombre del perfil, sin hueco vacío ni bandera suelta |
| Perfil con nombre muy largo (más de 25 caracteres) | El nombre se recorta con **elipsis («…») en una sola línea**, sin cortarse a media palabra ni desbordar la tarjeta |
| Tocar el botón de power | No abre la app; cambia el estado del widget |

## 2. Tamaños

| Comprobación | Resultado esperado |
|---|---|
| Tamaño por defecto (2×2) | Logo arriba a la izquierda, píldora arriba a la derecha, botón en el centro, perfil y país abajo |
| Arrastrar el borde hasta hacerlo ancho (≥ 250 dp) | El diseño cambia a **horizontal**: el botón a la izquierda y el texto a la derecha |
| Redimensionar a un tamaño estrecho | La tarjeta sigue mostrando el botón y el estado; anota si el launcher deja el widget más pequeño que 2×2 |
| Girar el dispositivo con el widget puesto | El widget se recoloca y mantiene su contenido |

## 3. Conexión y desconexión

| Comprobación | Resultado esperado |
|---|---|
| Widget, sin conexión | «Desconectado» → al tocar: «Conectando…» → «Activo» |
| Widget, conectado | Al tocar de nuevo: «Desconectado» |
| Tocar **durante** «Conectando…» | **Desconecta**. No debe duplicar la conexión ni dejar dos túneles |
| Mosaico, sin conexión | Icono apagado → al tocar: se conecta y el mosaico queda encendido |
| Mosaico, conectado | Al tocar: se desconecta y el mosaico queda apagado |
| Mosaico durante «Conectando…» | Desconecta, igual que el widget |
| Ambos a la vez | Si tocas el widget y el mosaico casi a la vez, el estado real manda: solo hay un túnel |

## 4. Permiso VPN

| Comprobación | Resultado esperado |
|---|---|
| Sin permiso concedido, tocar conectar | Aparece el diálogo del sistema «Conexión VPN» |
| **Denegar** el permiso | El widget queda en «Desconectado» y **la app se cierra sola** (no se queda una pantalla transparente en primer plano) |
| Conceder el permiso | La conexión se completa y el widget pasa a «Activo» |
| Revocar el permiso en Ajustes y volver a tocar | Vuelve a pedir el permiso |

## 5. Sin perfiles (acceso directo a Importar)

| Comprobación | Resultado esperado |
|---|---|
| Sin ningún perfil guardado, tocar el widget | La app se abre **directamente en la pestaña Importar**, sin pedir antes el permiso VPN |
| Sin ningún perfil guardado, tocar el mosaico | Igual: se abre en **Importar** |
| Con la app ya abierta en otra pestaña, tocar el widget | La app vuelve a la pestaña **Importar** |
| Importar un perfil y volver al launcher | El widget muestra ese perfil **sin reiniciar la app** |

## 6. Perfil ilegible o borrado

| Comprobación | Resultado esperado |
|---|---|
| Perfil con el `.conf` dañado | El widget muestra **«Fallo»** (fondo rosado, botón ámbar) |
| Con el estado «Fallo», tocar el botón | **Reintenta**: si el archivo se restauró, conecta; si sigue roto, vuelve a «Fallo» |
| Borrar el perfil que estaba seleccionado | El widget pasa al siguiente perfil disponible |
| Borrar el último perfil | El widget vuelve a **«Importar perfil»** (botón «+») |
| Tocar el «+» del widget | Abre la app en Importar |

## 7. Muerte del proceso

| Comprobación | Resultado esperado |
|---|---|
| Con el túnel conectado, matar **solo** el proceso: `adb shell am kill com.usipipo.vpn` (**no** `force-stop`) | Al tocar el widget debe **conectar**, no desconectar |
| Con el túnel conectado y el proceso vivo, tocar el widget | Desconecta correctamente (no es lo mismo que el caso anterior) |

> Si al matar el proceso el widget sigue mostrando «Activo» hasta que lo tocas, anótalo:
> es el comportamiento esperado (no hay refresco periódico), pero conviene saberlo.

## 8. Cierre externo

| Comprobación | Resultado esperado |
|---|---|
| Activar otra app de VPN | El widget y el mosaico vuelven a **«Desconectado»** |
| Desactivar esa VPN | La app sigue en «Desconectado»; no se reconecta sola |

## 9. Arranque y actualización

| Comprobación | Resultado esperado |
|---|---|
| Reiniciar el dispositivo | Al volver a la pantalla de inicio **no** hay un estado «Activo» falso |
| Con el widget colocado, reiniciar y tocar el botón | Conecta de verdad (el túnel vive en el proceso de la app) |
| Instalar una versión posterior de la APK | El widget se refresca con el estado real, sin quedarse «Activo» |

## 10. Temas y accesibilidad

| Comprobación | Resultado esperado |
|---|---|
| Tema claro, sin conexión | Fondo azul pastel, píldora con texto legible |
| Tema claro, conectado | Degradado azul a turquesa, texto claro sobre el fondo |
| Tema claro, fallo | Tinte rosado, acento ámbar |
| Tema oscuro (las mismas cuatro situaciones) | Fondos oscuros equivalentes; **ningún texto ilegible ni invisible** |
| Tamaño de fuente grande del sistema | El texto sigue siendo legible; anota si se recorta de forma ilegible |
| TalkBack | El botón del widget tiene etiqueta y anuncia la acción; la píldora de estado se lee |

## 11. Mosaico según la versión de Android

| Comprobación | Resultado esperado |
|---|---|
| Android 10 o superior (API 29+) | El mosaico muestra **subtítulo** (perfil o estado) y **descripción de estado** |
| Android 9 o anteriores (API 26-28) | Solo icono, etiqueta «uSipipo VPN» y el estado nativo: **sin subtítulo**. Esto es una limitación de la API, no un fallo |
| Abrir el panel de ajustes rápidos | El mosaico se repinta con el estado actual |

## 12. Consumo en reposo

| Comprobación | Resultado esperado |
|---|---|
| Widget colocado, sin tocarlo en unas horas | **No hay actualización periódica** observable (ni parpadeo del icono) |
| Batería | El consumo es estable y no aumenta de forma visible |
| App cerrada por el sistema | El widget sigue mostrando el último estado hasta que se toca |

---

## Riesgos conocidos (anótalos, no son fallos)

- El widget declara `minHeight="190dp"`: el launcher debería darle esa altura para que se
  vean el nombre del perfil y el país. Si en algún launcher se ve cortado, anota el
  nombre y la versión del launcher.
- El layout cambia a horizontal a partir de **250 dp** de ancho. Con `minResizeWidth="110dp"`
  se puede estrechar bastante: si en un launcher el diseño horizontal queda muy apretado,
  anótalo.
- El mosaico en **API 26-28** no admite subtítulo ni descripción de estado (el umbral de la
  API real es API 29 / Android 10).
- El mosaico se repinta al abrir el panel de ajustes rápidos; **no hay refresco periódico**.
- Sin perfil con país en caché, el widget solo muestra el nombre del perfil (no es un
  fallo: el país no se consulta por red).
- Con el proceso muerto, el widget puede mostrar el último estado hasta que se toca.
