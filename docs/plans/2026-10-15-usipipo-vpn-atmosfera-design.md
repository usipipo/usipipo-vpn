# uSipipo VPN — Diseño visual «Atmósfera»

Fecha: 2026-10-15
Estado: diseño aprobado (secciones 1 a 4), pendiente de plan de implementación
Alcance: solo la capa visual de la app Android (Kotlin + Jetpack Compose)

## 1. Objetivo

Dar personalidad a la app sin romper su minimalismo. La app hoy es limpia y funcional, pero se ve como Material 3 de fábrica: botón de color plano, `Typography()` por defecto, tarjetas con borde y cambios de pantalla sin transición.

La dirección elegida se llama **Atmósfera**: el estado del túnel se siente en todo el ambiente de la pantalla (fondo, halo y botón), no solo en el texto.

## 2. Decisiones tomadas

- Dirección visual: Atmósfera (fondo dinámico, halos y animaciones del botón, además de tipografía, tarjetas y transiciones).
- Ambiente al estar conectado: degradado de azul a turquesa/menta.
- Público: mezcla de gamas de teléfonos, así que las animaciones deben ser ligeras.
- Se mantiene la regla ya aprobada: pantalla principal sin estadísticas, tiempo conectado ni otros extras.
- Los colores de estado siempre van acompañados de texto.

## 3. Fuera de alcance

- Cambios en el ViewModel, el túnel, el almacenamiento o los diagnósticos.
- Estadísticas o información adicional en la pantalla principal.
- Modo administrador (fase posterior).
- Logo nuevo y sistema de iconos propio (dirección «Marca», descartada por ahora).

## 4. Sección 1 — Ambiente y fondo según el estado

El fondo de la pantalla principal es un degradado vertical que cambia con el estado del túnel.

| Estado | Fondo | Botón y halo |
|---|---|---|
| Desconectado | Degradado azul pastel muy suave (de `#F3F8FF` hacia `#D6E6FB`) | Azul pastel, halo estático |
| Conectando | El fondo empieza a derivar hacia el turquesa | El halo «respira» |
| Conectado | Degradado de azul a turquesa/menta | Halo más brillante, casi estático |
| Sin respuesta o fallo | Tinte rosado pastel muy leve, sin animación | Rosa pastel |

- Modo oscuro: del azul noche (`#0E1A2B`) a un verde azulado profundo. No es una inversión de la paleta clara.
- Los tonos exactos de turquesa/menta (claro y oscuro) se fijan en el plan de implementación y se validan con la comprobación de contraste.

**Rendimiento**

- El degradado es estático. Solo se interpola el color, una vez, en unos 600 ms cuando cambia el estado.
- La única animación continua es el halo del botón (escala y transparencia).
- Con el ajuste «reducir animaciones» del sistema, el halo queda fijo y el fondo cambia sin transición.

## 5. Sección 2 — Botón de conectar

- Círculo de 168 dp como ahora, con un anillo exterior fino que lo separa del fondo.
- **Desconectado:** azul pastel, icono de power, halo de sombra de color suave y estático.
- **Conectando:** un anillo adicional se expande y se desvanece de forma repetida, además del halo que respira (escala y transparencia).
- **Conectado:** tono turquesa; el icono pasa de «power» a «escudo con check»; halo brillante casi estático con un «respirar» muy lento (unos 4 segundos).
- **Fallo o sin respuesta:** rosa pastel, con un temblor horizontal de una sola vez y sin animación continua.
- **Vibración suave** al conectar y otra distinta al desconectar. Se omite si el sistema la tiene desactivada.
- **Accesibilidad:** se conserva la descripción del botón («Conectar» o «Desconectar») y el texto de estado debajo. Con «reducir animaciones» activo, el cambio de icono es instantáneo y no hay anillos ni temblor.
- Todo se hace con escala y transparencia. No hay blur en tiempo real ni sombras animadas.

## 6. Sección 3 — Tipografía, tarjetas y transiciones

**Tipografía**

- Una sola fuente con carácter (por ejemplo Manrope o Plus Jakarta Sans), incluida dentro de la APK para no depender de internet.
- «uSipipo VPN» en peso alto; estado de conexión en tamaño grande y mediano; resto con la escala normal de Material.

**Lista de configuraciones**

- Se elimina el RadioButton y el borde. Cada configuración es una tarjeta tonal con esquinas más redondeadas y un icono de servidor.
- La seleccionada lleva un chip «Activo» y un fondo azul pastel algo más marcado.
- Borrar: deslizar la tarjeta hacia un lado. Se mantiene el diálogo de confirmación actual.
- Con el túnel activo, la selección sigue bloqueada.

**Estado vacío**

- Ilustración vectorial simple (escudo o candado) con el botón de importar debajo. Sustituye a la tarjeta de texto actual.

**Transiciones y otras pantallas**

- Entre Home, Importar, Acerca de y Diagnóstico: transición corta de deslizamiento con desvanecimiento (unos 250 ms). Con «reducir animaciones» activo, el cambio es directo.
- Acerca de y Diagnóstico se agrupan en secciones con encabezado y tarjetas tonales; Acerca de lleva el logo grande arriba.
- El degradado de fondo se usa solo en Home. Las demás pantallas llevan fondo plano.

## 7. Sección 4 — Alcance técnico, pruebas y riesgos

**Archivos afectados**

- `ui/theme/Theme.kt`: colores de ambiente por estado (claro y oscuro) y nueva tipografía.
- `ui/components/ConnectButton.kt`: anillos, transición de icono y temblor.
- `ui/home/HomeScreen.kt`: fondo con degradado, tarjetas nuevas, deslizar para borrar y estado vacío.
- `ui/UsipipoRoot.kt`: transiciones entre pantallas.
- Archivos nuevos: la fuente (`res/font`) y los iconos vectoriales (escudo con check e ilustración del estado vacío).

**Pruebas**

- Las pruebas unitarias existentes siguen siendo válidas porque no se toca la lógica.
- Previews de Compose de cada estado (desconectado, conectando, conectado, fallo, vacío) en modo claro y oscuro.
- Comprobación de contraste del texto de estado sobre el degradado, en los dos modos.
- Prueba manual en un teléfono de gama baja y en uno de gama media, con «reducir animaciones» activado y desactivado.

**Riesgos y mitigación**

| Riesgo | Mitigación |
|---|---|
| Contraste del texto sobre el degradado | Texto en navy oscuro fijo; se mide en cada tono |
| Fluidez en gama baja | Si algo se nota lento, se desactiva primero el «respirar» lento del estado conectado; la app funciona igual |
| Peso de la APK | La fuente añade unos cientos de KB |
| Crecimiento del alcance | Nada de estadísticas, tiempo conectado ni extras en la pantalla principal |

## 8. Siguiente paso

Escribir el plan de implementación (tareas ordenadas y verificables) a partir de este documento.
