# Geolocalización del endpoint con IPinfo Lite — Diseño

Fecha: 2026-10-25
App: uSipipo VPN (Android, Kotlin)
Estado: diseño aprobado, pendiente de plan de implementación

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

Usar **IPinfo Lite como proveedor único**, sin proveedor de respaldo.
Se descartaron ipwho.is (límite de 1.000 peticiones/día por IP de cliente) y
guardar el país en el perfil como vía principal.

Hechos del proveedor (a verificar en su documentación al implementar):

- API de país y ASN sin límite de cuota de peticiones.
- Requiere token gratuito.
- Licencia con atribución obligatoria.
- HTTPS: `https://api.ipinfo.io/lite/{ip}?token=...`

## 3. Diseño

### 3.1 Proveedor y petición

- `GET https://api.ipinfo.io/lite/{ip}` con token y timeout corto.
- De la respuesta solo se usan `country_code` y `country`. La bandera se
  calcula como emoji a partir del código.
- **Validación previa de la IP:** solo IPv4 o IPv6 completas y válidas. Se
  rechazan IPs cortadas, con puerto, con corchetes y dominios, sin hacer la
  petición y registrando el motivo.
- **Token:** se inyecta en la compilación desde el secret `IPINFO_TOKEN` del CI
  de GitHub (BuildConfig). No va en el repositorio. Puede extraerse de la APK,
  pero Lite no tiene cuota que agotar.
- **Atribución:** línea "Geolocalización por IPinfo" en Diagnóstico → Acerca de.
- **Registro:** en modo detallado, código HTTP, tipo de fallo y plantilla de la
  URL, sin IP ni token.

### 3.2 Cuándo consultar, caché y fallos

- **Cuándo:** solo con el túnel Conectado, handshake recibido y la primera
  comprobación de conectividad superada. La petición se cancela si el usuario
  desconecta. Solo con la app en primer plano.
- **Caché por perfil:** código, nombre y fecha junto al perfil. Se muestra al
  instante sin red. Se renueva si cambia el endpoint o pasados ~30 días.
- **Fallos:** un único reintento tras unos segundos. Si vuelve a fallar, no se
  reintenta hasta la siguiente conexión. Enfriamiento breve por perfil tras
  fallos seguidos.
- **Interfaz:** el país nunca es un error visible. Sin caché ni respuesta, la
  tarjeta del perfil y Detalles muestran solo nombre y endpoint, sin bandera ni
  texto de error. El detalle del fallo queda solo en el registro de Diagnóstico.

### 3.3 Pruebas y alcance

- **Estructura:** `GeoLocator` y `IpValidator` en Kotlin puro, con cliente HTTP
  inyectable, probables sin Android ni red.
- **Pruebas unitarias:**
  - Validación de IP: IPv4/IPv6 válidas pasan; cortadas (p. ej. `165.140`), con
    puerto, con corchetes o dominios se rechazan sin petición.
  - Respuestas: 200 correcto, JSON sin país, 401/403 (token inválido), 404, 429,
    5xx y timeout, con resultado y registro esperados.
  - Caché y reintentos: uso de caché, renovación por cambio de endpoint o 30
    días, un solo reintento, enfriamiento.
  - Bandera: de código a emoji, incluidos códigos desconocidos o vacíos.
  - Registro: nunca aparecen la IP completa ni el token.
- **Prueba manual** con la APK debug del CI: conectar, ver país y bandera,
  reconectar y confirmar que sale de caché; probar sin datos y con token
  inválido, sin errores visibles en pantalla.
- **CI:** todo se compila y prueba vía GitHub Actions. Hay que crear el secret
  `IPINFO_TOKEN`. Si falta, el build no falla y la app funciona sin país.
- **Fuera de alcance:** otros proveedores, ping, mapa de ubicación, geolocalizar
  la IP del usuario, mostrar la ciudad. Solo se toca la geolocalización del
  endpoint, el secret del CI y la línea de atribución.

## 4. Primer paso de implementación

Antes de escribir código nuevo, revisar en el código actual cómo se extrae la IP
del endpoint, cómo se construye la URL de `ipapi.co` y qué hace el redactor de
claves con las IPs, para confirmar la causa del 404.
