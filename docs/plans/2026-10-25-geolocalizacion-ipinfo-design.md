# Geolocalización del endpoint con IPinfo Lite — Diseño

Fecha: 2026-10-25 (revisado 2026-10-30)
App: uSipipo VPN (Android, Kotlin)
Estado: diseño aprobado, en implementación (rama `feat/fase8-ipinfo-geolocalizacion`)

## 1. Contexto y problema

La app consulta el país del endpoint del perfil activo para mostrar bandera y país
(fase 3 del rediseño). El informe de diagnóstico del 25 oct 2026 (app 0.4.0,
Xiaomi 24117RN76L, Android 16) muestra que la geolocalización con `ipapi.co`
**nunca ha funcionado**: 11 fallos desde el 21 oct (v0.3.0) y ningún éxito.

- 9 fallos: `ipapi.co respondió HTTP 404 para 165.140`. El mensaje muestra la IP
  cortada a dos octetos. Puede ser un bug al construir la URL o un artefacto del
  redactor de claves del registro; hay que comprobarlo en el código.
- 2 fallos: `SocketTimeoutException`, unos 10 s después de conectar.
- Un patrón parecido apareció el 15 oct en la comprobación de actualizaciones
  (timeout tras un handshake lento).

Además, consultar por el túnel hace que todos los usuarios compartan la IP de
salida del servidor, lo que agotaría cualquier cuota por IP de cliente.

## 2. Decisión

Usar **IPinfo Lite como proveedor principal** y **ipwho.is como proveedor de
respaldo** (solo de emergencia). Guardar el país en el perfil sigue siendo la
vía de lectura, no una vía para obtener el dato.

Hechos verificados en la documentación del proveedor el 30 oct 2026
(ipinfo.io/developers/lite-api):

- API de país y ASN **sin límite de cuota**: «IPinfo Lite offers unlimited
  access to our API», sin topes diarios ni por hora.
- Requiere token gratuito; se pasa como `?token=…` (también valen Basic Auth o
  Bearer).
- HTTPS: `https://api.ipinfo.io/lite/{ip}?token=...`
- Respuesta 200: `ip`, `asn`, `as_name`, `as_domain`, `country_code`,
  `country`, `continent_code`, `continent`. Una IP reservada responde
  `{"ip": …, "bogon": true}` **sin** `country_code`.
- Licencia con atribución obligatoria: basta con acreditar la fuente
  («IP address data powered by IPinfo»).

Sobre el proveedor de respaldo:

- **ipwho.is** sirve por HTTPS sin token y devuelve `country_code` y `country`.
  Su plan gratuito está limitado a **1.000 peticiones/día por IP de cliente** y
  sin SLA. Como el respaldo solo se usa cuando el primario falla, ese techo es
  aceptable: si se agota, la app degrada a «sin país» sin error visible.
- **ip-api.com** queda descartado: el plan gratuito no ofrece HTTPS
  («SSL encryption is not available for this free API»), limita a 45
  peticiones/minuto y prohíbe el uso comercial.

Por qué no un respaldo permanente: el primario es ilimitado y con atribución
simple; un segundo proveedor siempre activo solo añadiría cuota que agotar y
atribución que cumplir. El respaldo existe para el caso de caída del primario,
y se quita borrando una línea en `AppContainer`.

## 3. Diseño

### 3.1 Proveedor y petición

- `GET https://api.ipinfo.io/lite/{ip}?token=…` con timeout corto.
- De la respuesta solo se usan `country_code` y `country`. La bandera se
  calcula como emoji a partir del código.
- Una respuesta `bogon` (IP reservada) no es un fallo: es «sin país», y no se
  cachea.
- **Validación previa de la IP:** solo IPv4 o IPv6 completas y válidas. Se
  rechazan IPs cortadas, con puerto, con corchetes y dominios, sin hacer la
  petición y registrando el motivo.
- **Respaldo:** `GET https://ipwho.is/{ip}` (HTTPS, sin token), con los mismos
  timeout y campos (`country_code`, `country`). Se consulta **solo** si el
  primario falla por infraestructura (red, timeout, HTTP ≠ 2xx). Un `429` del
  primario no activa el respaldo: activa el enfriamiento (§3.2), porque el
  límite es del token, no del proveedor.
- **Token:** se inyecta en la compilación desde el secret `IPINFO_TOKEN` del CI
  de GitHub (BuildConfig). No va en el repositorio. Puede extraerse de la APK,
  pero Lite no tiene cuota que agotar. Si falta el token, el primario no
  consulta y el app recurre al respaldo.
- **Atribución:** línea «Geolocalización por IPinfo» en Diagnóstico → Acerca de.
- **Registro:** en modo detallado, código HTTP, tipo de fallo y plantilla de la
  URL, sin IP ni token.

### 3.2 Cuándo consultar, caché y fallos

- **Cuándo:** solo con el túnel Conectado, handshake recibido y la primera
  comprobación de conectividad superada. `TunnelState.Connected` ya significa
  «túnel arriba y con handshake recibido» (`TunnelState.NoResponse` es el caso
  sin handshake, y ahí no se consulta). La petición se cancela si el usuario
  desconecta. Solo con la app en primer plano.
- **Caché por perfil:** código, nombre y fecha junto al perfil. Se muestra al
  instante sin red. Se renueva si cambia el endpoint o pasados ~30 días.
- **Fallos:** un único reintento del par (primario + respaldo) tras ~3 s. Si
  vuelve a fallar, no se reintenta hasta la siguiente conexión, y el perfil
  entra en enfriamiento breve (~1 min) para no martillear la API. Un `429` del
  primario respeta su ventana de reintento y tampoco consulta el respaldo.
- **Interfaz:** el país nunca es un error visible. Sin caché ni respuesta, la
  tarjeta del perfil y Detalles muestran solo nombre y endpoint, sin bandera ni
  texto de error. El detalle del fallo queda solo en el registro de Diagnóstico.

### 3.3 Pruebas y alcance

- **Estructura:** `GeoLocator` (cadena de proveedores), `IpValidator` (validación
  de IP) y los clientes HTTP en Kotlin puro, con transporte HTTP inyectable,
  probables sin Android ni red.
- **Pruebas unitarias:**
  - Validación de IP: IPv4/IPv6 válidas pasan; cortadas (p. ej. `165.140`), con
    puerto, con corchetes o dominios se rechazan sin petición.
  - Respuestas: 200 correcto, JSON sin país, 401/403 (token inválido), 404, 429,
    5xx y timeout, con resultado y registro esperados.
  - Respaldo: el primario con fallo de infraestructura pasa al respaldo; con
    `429` no; si el primario responde bien no se consulta el respaldo.
  - Caché y reintentos: uso de caché, renovación por cambio de endpoint o 30
    días, un solo reintento, enfriamiento.
  - Bandera: de código a emoji, incluidos códigos desconocidos o vacíos.
  - Registro: nunca aparecen la IP completa ni el token.
- **Prueba manual** con la APK debug del CI: conectar, ver país y bandera,
  reconectar y confirmar que sale de caché; probar sin datos y con token
  inválido, sin errores visibles en pantalla.
- **CI:** todo se compila y prueba vía GitHub Actions. Hay que crear el secret
  `IPINFO_TOKEN`. Si falta, el build no falla: el primario se salta y el
  respaldo da el país.
- **Fuera de alcance:** otros proveedores, ping, mapa de ubicación, geolocalizar
  la IP del usuario, mostrar la ciudad. Solo se toca la geolocalización del
  endpoint, el secret del CI y la línea de atribución.

## 4. Primer paso de implementación (hecho el 30 oct 2026)

Causa raíz del 404 confirmada en el código:

- `IpApiClient.fetch` construía la URL como `https://ipapi.co/json/{ip}/`,
  pero ipapi.co sirve el país en `https://ipapi.co/{ip}/json/`. La ruta
  invertida devolvía **404 en todas las peticiones**, por eso la
  geolocalización nunca funcionó (9 de los 11 fallos del informe).
- El `165.140` que aparecía en el registro no era el redactor: era la
  abreviación a dos octetos que hace el propio cliente (`IpApiClient.abbreviate`)
  para no filtrar la IP. `LogRedactor` solo tapa claves de WireGuard.
- Los 2 fallos restantes eran `SocketTimeoutException`: el cliente usaba un
  timeout de 3 s y un solo intento, sin reintento.

Con la ruta corregida y IPinfo Lite (sin límite de cuota) el problema
desaparece; el respaldo cubre la caída del proveedor.
