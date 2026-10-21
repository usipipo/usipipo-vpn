# uSipipo VPN — Diseño: del mockup a la app real

Fecha: 2026-10-18
Estado: diseño aprobado (secciones 1 a 6). Fases 1, 2 y 3 implementadas y verificadas en CI.
Pendiente de diseño la fase 4 (modo administrador, ver sección 3).
Alcance: capa de interfaz de la app Android (Kotlin + Jetpack Compose), más los datos de túnel que la interfaz necesita

## 1. Objetivo

Llevar a código real las cinco pantallas del mockup generado con IA (principal, configuraciones, importar, diagnóstico y ajustes avanzados, detalles del perfil) para que la app se vea profesional y moderna. Se parte del proyecto actual, que ya incluye Atmósfera, diagnóstico, actualizaciones y el motor de túnel de WireGuard.

## 2. Decisiones tomadas

- Datos del mockup: se muestran datos reales (tiempo activo, descarga/subida, endpoint, protocolo) más país y ping. Se descarta rellenar con datos falsos.
- Tema: claro y oscuro. El oscuro es igual al mockup (azul noche); el claro es una adaptación con los azules pastel ya aprobados.
- Navegación: barra inferior de 4 pestañas (Inicio, Configuraciones, Importar, Diagnóstico). «Acerca de» y las actualizaciones pasan a vivir dentro de Diagnóstico.
- Importar: solo escanear QR e importar archivo `.conf`. No se añade «Pegar configuración» ni se menciona `.ovpn`.
- Configuraciones: pestañas «Mis perfiles» y «Favoritos», con estrella en cada perfil.
- Enfoque: por fases. Cada fase compila y se puede probar en el teléfono.

### Decisiones anteriores que este diseño reemplaza

- «Pantalla principal sin extras»: ahora Inicio muestra descarga, subida, tiempo activo y «Ver detalles» cuando el túnel está conectado (fase 2).
- «Lista de configuraciones en Inicio»: pasa a la pestaña Configuraciones.

## 3. Fuera de alcance

- Contador de «usuarios online» del mockup (no hay fuente de datos).
- Mapa de puntos de fondo y efectos de desenfoque (peso en gama baja).
- Modo administrador (fase posterior ya aprobada).
- Soporte de otros protocolos.

## 4. Fase 1 — Estructura y estilo

### Navegación

- Barra inferior Material 3 con 4 destinos, sin librería de navegación: un estado simple con la pestaña activa.
- Atrás desde cualquier pestaña vuelve a Inicio; desde Inicio sale de la app.

### Pantallas

- **Inicio:** botón de conectar con Atmósfera, estado y tarjeta con el perfil activo (nombre y protocolo). Tocar la tarjeta lleva a Configuraciones. La lista de perfiles sale de esta pantalla.
- **Configuraciones (nueva):** «Mis perfiles» y «Favoritos». Reutiliza las tarjetas actuales (chip «Activo», deslizar para borrar) y añade la estrella. Botón «+» y «Añadir configuración» llevan a Importar.
- **Importar:** dos tarjetas grandes, QR y archivo.
- **Diagnóstico:** la pantalla actual más la sección «Acerca de» y la comprobación de actualizaciones.

### Datos

- Cada perfil guarda un campo `favorito` en el almacén cifrado. Los perfiles existentes siguen funcionando, sin favorito por defecto.
- El túnel y la lógica de conexión no se tocan.

### Estilo

- Oscuro: fondo azul noche con degradado vertical; Atmósfera sigue mandando en Inicio (azul noche al desconectar, deriva a verde azulado al conectar, tinte rosado leve en fallo). Tarjetas en azul más claro con borde fino y esquinas redondeadas; iconos en cuadro redondeado. Acento azul brillante, turquesa para «conectado», rojo para desconectar. Los tonos exactos se ajustan sobre la imagen del mockup.
- Claro: mismas formas y estructura con los azules pastel aprobados, tarjetas blancas con borde suave.
- Barra inferior con icono y etiqueta; pestaña activa con acento y pastilla suave. Cabecera con logo y título en las pantallas principales.
- Se mantiene Manrope y la comprobación de contraste en ambos temas.
- Logo: icono de la app a partir del jpg entregado (la «u» blanca con guion bajo sobre azul), convertido a icono adaptable. La cabecera usa esa misma «u», no el símbolo de encendido del mockup.
- El brillo del botón sigue siendo la única animación continua; se respeta «reducir animaciones».

## 5. Fase 2 — Datos reales y pantalla Detalles

### Origen de los datos

- **Descarga/subida:** bytes recibidos y enviados que ya expone la librería de WireGuard. La velocidad es la diferencia entre lecturas, suavizada con las últimas 3 muestras. Es la velocidad actual del túnel, no un test de velocidad, y las etiquetas lo indican.
- **Tiempo activo:** desde el primer handshake.
- **Endpoint, protocolo e IPv6:** del propio `.conf`. El protocolo es siempre WireGuard. IPv6 aparece «Habilitado» si el perfil enruta `::/0`.

### Consulta

- Solo con el túnel conectado y la pantalla visible, una vez por segundo.
- Gráfica con las últimas ~60 muestras, dibujada con Canvas, sin librerías.

### Pantallas

- **Inicio:** al conectar aparecen tarjetas de descarga y subida, tiempo activo y «Ver detalles».
- **Detalles:** perfil activo con chip «Activo», estadísticas con gráfica, tiempo, protocolo, endpoint, IPv6 y botón rojo «Desconectar». Solo existe para el perfil conectado. Incluye el lápiz del mockup para renombrar el perfil.

### Cambios de código

- El contrato `TunnelManager` gana un flujo de estadísticas. La lógica de conexión no cambia.
- El registro de diagnóstico no guarda cifras de tráfico, salvo en modo detallado y sin claves ni IPs completas.

## 6. Fase 3 — País y ping — **implementada**

Especificación detallada: `docs/superpowers/specs/2026-10-18-usipipo-vpn-fase3-pais-y-ping-design.md`
Plan: `docs/superpowers/plans/2026-10-18-fase3-pais-y-ping.md`

### País y bandera

- Se consulta un servicio público de geolocalización con la IP del endpoint, solo con el túnel conectado. La consulta sale por la VPN, de modo que el servicio no ve la IP real del teléfono.
- Resultado en caché por perfil.
- La bandera es un emoji derivado del código de país. Si la consulta falla, se muestra un globo.
- El servicio concreto se elige al implementar, comprobando límites vigentes y soporte de HTTPS.

### Ping

- Ping real hacia el servidor dentro del túnel, cada 5 segundos y solo con la pantalla visible.
- Destino: la dirección `.1` de la red del túnel (la misma que usa el diseño del modo admin). Funciona con el script `wg-install.sh`; si otro servidor no responde, se muestra «—».
- Limitación conocida: algunos fabricantes bloquean el ping nativo; en ese caso también se muestra «—».

## 7. Errores

- País y ping son opcionales y nunca bloquean la conexión.
- Tiempo máximo de 3 s por consulta, sin reintentos en bucle.
- Los fallos se anotan en el registro de diagnóstico con la IP abreviada.

## 8. Pruebas

- Tests en JVM: cálculo de velocidad, lectura de la salida del ping, caché de país y migración del campo favorito.
- Previews de Compose de cada pantalla y estado, en tema claro y oscuro.
- Comprobación de contraste y prueba manual en gama baja y media.
- Los tests corren en el workflow de GitHub Actions existente.

## 9. Puntos a resolver en el plan de implementación

- Tonos exactos de los temas (muestreados del mockup).
- Servicio de geolocalización y sus límites.
- Conversión del jpg del logo a icono adaptable y a vector para la cabecera.
- Orden de commits por fase para que cada una compile por separado.
