# uSipipo VPN (Android) — Diagnóstico y ajustes avanzados

- **Fecha:** 2026-10-11
- **Estado:** diseño aprobado e implementado (sin compilar el conjunto; ver README)
- **Complementa:** `2026-10-09-usipipo-vpn-android-design.md`

## 1. Objetivo

Recoger errores y eventos en los dispositivos para depurar antes y durante las pruebas, y poder
enviarlos por correo a usipipo@gmail.com sin filtrar claves ni configuraciones.

## 2. Decisiones aprobadas

- Salida del registro: **solo por correo** a usipipo@gmail.com (descartados portapapeles y compartir archivo).
- Pantalla con: info del dispositivo y de la app, modo detallado, visor del registro y borrar registro.
- Enfoque: **registro propio en archivo + volcado de logcat solo en modo detallado**.
  Descartado: servicio externo (Crashlytics/Sentry) por depender de internet, cuentas y Google Play Services.

## 3. El registro (AppLog)

- Objeto accesible desde cualquier capa; se inicia lo primero en `UsipipoApp`. Sin inicializar no hace nada.
- Archivo rotativo `usipipo.log` + `usipipo.log.1`, ~512 KB cada uno, en `filesDir/logs`.
  Escritura en un hilo propio para no frenar la interfaz.
- Siempre guarda INFO, WARN, ERROR y FATAL. DEBUG solo con el modo detallado (se activa en caliente).
- Formato: `fecha hora.ms NIVEL/Etiqueta: mensaje` + stack trace si hay excepción.
- **Privacidad:** `LogRedactor` tapa `PrivateKey`, `PresharedKey` y cualquier cadena con forma de clave
  de WireGuard (43 caracteres base64 + `=`). Nunca se registra el contenido de un `.conf` ni de un QR.
  Los mensajes de excepciones del parser de configuración no se registran (solo el tipo de error),
  porque pueden contener texto del `.conf`.
- **Cierres inesperados:** `CrashHandler` escribe el FATAL de forma síncrona, deja una marca y cede al
  manejador anterior del sistema. En el siguiente inicio, Diagnóstico muestra un aviso.
- **Qué se registra:** arranque y versión; cambios de estado del túnel y sus errores; tiempo hasta el
  primer handshake; permiso de VPN; importaciones (origen, resultado y motivo); errores del almacén y
  del cifrado. En modo detallado, además, cada sondeo de handshake y los tiempos.

## 4. La pantalla

Acceso: Acerca de → "Diagnóstico y ajustes avanzados".

1. Aviso de cierre inesperado (solo si ocurrió), con "Entendido".
2. Información: versión de la app, Android y API, fabricante y modelo, arquitectura, estado del túnel,
   permiso de VPN y número de configuraciones guardadas (sin nombres, IPs ni claves).
3. Interruptor del modo detallado.
4. Visor del registro: últimas ~300 líneas en fuente monoespaciada, con el nivel escrito (E, W, I, D),
   texto seleccionable y botón Actualizar.
5. "Enviar por correo" (con aviso previo de qué incluye) y "Borrar registro" (con confirmación).

**Envío:** se genera `diagnostico-AAAAMMDD-HHMM.txt` en la caché (solo se conserva el último) con
información, registro y, en modo detallado, logcat del propio proceso. Se adjunta con un
`FileProvider` a un correo dirigido a usipipo@gmail.com (`ACTION_SENDTO` con `mailto:`, que limita la
lista a apps de correo). El cuerpo repite la información del dispositivo por si el cliente ignora el
adjunto. Sin app de correo: mensaje claro.

## 5. Robustez y límites

- El registro nunca rompe la app: cualquier fallo al registrar se descarta en silencio.
- Sin registros: el visor dice "Aún no hay registros". Si logcat falla, el informe lo indica y sigue.
- El informe tiene un tope de ~1.5 MB; si se pasa, conserva lo más reciente y avisa del recorte.
- Fuera de alcance: subida automática a un servidor, compartir por otras vías, visor en vivo y filtros
  por nivel.

## 6. Pruebas

- Unitarias (JVM, sin Android): `LogRedactorTest`, `FileLogWriterTest` (rotación y borrado),
  `ReportBuilderTest` (secciones, logcat, redacción, recorte).
- Manual en el teléfono: forzar un fallo (importar un `.conf` roto, conectar sin internet), revisar el
  visor y enviar el informe a usipipo@gmail.com.
