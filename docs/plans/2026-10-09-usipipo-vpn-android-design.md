# uSipipo VPN (Android) — Documento de diseño

- **Fecha:** 2026-10-09
- **Estado:** diseño aprobado (secciones 1 a 4), sin código todavía
- **Soporte:** usipipo@gmail.com

## 1. Objetivo y alcance

App Android nativa en Kotlin, minimalista: importar el `.conf` generado por WireGuard en el VPS y conectarse con un solo botón.

**Versión 1 (este documento):**
- Importar configuraciones `.conf` desde archivo o escaneando un QR.
- Guardar varias configuraciones y elegir cuál usar desde una lista.
- Conectar y desconectar con un botón; mostrar el estado.

**Fase 2 (propuesta aprobada, ver sección 6):** modo administrador para crear, listar y revocar usuarios desde la app.

**Fuera de alcance de la v1:** estadísticas en pantalla, conexión automática al abrir, acceso directo a "VPN siempre activa", importar pegando texto, split tunneling.

## 2. Contexto del servidor (referencia)

- Interfaz `wg0`, red `10.66.66.1/24` y `fd42:42:42::1/64`, puerto `64000/udp`.
- DNS de los clientes: Blocky en `10.66.66.1`.
- Los `.conf` de cliente usan túnel completo (`AllowedIPs = 0.0.0.0/0,::/0`) e incluyen clave privada y PresharedKey.
- El script `wg-install.sh` (basado en angristan/wireguard-install) gestiona los clientes por consola y no tiene API.

## 3. Arquitectura y pantallas

**Enfoque elegido:** librería oficial `wireguard-android` (módulo `tunnel`, con `GoBackend`). Descartados: delegar en la app oficial por intents (frágil) y motor propio (demasiado trabajo).

**Base:** Kotlin, Jetpack Compose con Material 3, un solo módulo, estructura UI → ViewModel → repositorio.

**Desvío consciente (subsistema de actualización):** el estado de la comprobación de
actualizaciones (`UpdateStatus`) vive en `AppContainer` en lugar de en un ViewModel propio,
porque lo comparten Inicio y «Acerca de» sin añadir navegación ni duplicar el orquestador.
Coste aceptado: si otra pantalla necesitase compartir estado, copiar este patrón sube el
acoplamiento.

**Componentes:**
- **TunnelManager:** envuelve el motor de WireGuard; conecta, desconecta y expone el estado.
- **ConfigStore:** guarda las configuraciones cifradas con Android Keystore.
- **Importador:** lee el `.conf` con el selector de archivos del sistema o escanea el QR con un escáner offline (ZXing), sin depender de Google Play Services.

**Pantallas:**
1. **Inicio:** lista de configuraciones, botón grande de conectar y estado.
2. **Importar:** elegir archivo o escanear QR, con nombre editable.
3. **Acerca de:** versión y soporte (usipipo@gmail.com). En la fase 2 aloja el acceso al modo administrador.

La primera vez, Android pide el permiso de VPN. Mientras hay conexión, el sistema muestra su icono de VPN (llave) en la barra de estado; en la fase 0 la app no añade una notificación propia (se revisará en las pruebas en el teléfono).

## 4. Flujo de datos y manejo de errores

**Importar:** archivo o QR → texto → validación con el parser de la librería → nombre → cifrado y guardado → aparece en la lista.

**Conectar:** botón → permiso de VPN si falta → el túnel sube. Estados: *Desconectado → Conectando → Conectado*. Tocar de nuevo desconecta. La app recuerda la última configuración elegida.

**Handshake:** WireGuard no tiene conexión real, así que el estado *Conectado* solo se muestra cuando llega el primer handshake. Si en unos 10 segundos no llega, se muestra *Sin respuesta del servidor*. Los datos de tráfico solo se usan internamente.

**Errores (mensaje claro y una acción):**
- Archivo o QR que no es WireGuard: "Configuración no válida".
- Nombre repetido: pide otro nombre.
- Permiso de VPN denegado: explica por qué hace falta y permite reintentar.
- Sin handshake: reintentar o escribir a usipipo@gmail.com.
- Borrar una configuración: pide confirmación.

## 5. Diseño visual y pruebas

Se aplicaron las reglas de `SKILL.md` (UI/UX Pro Max). Los scripts de búsqueda de la skill no estaban incluidos, así que no hay consultas a su base de datos.

**Paleta (modo claro):**
- Fondo `#F3F8FF`; tarjetas blancas con borde `#D6E6FB`.
- Botón conectar `#A8CCFF` con texto azul marino `#0B2545` (meta de contraste: 4.5:1 o más, a verificar en implementación).
- Estados: conectado azul brillante suave, desconectado gris azulado, error coral suave. Siempre con texto e icono, nunca solo color.

**Modo oscuro (automático):** fondo `#0E1A2B`, superficie `#162843`, acento `#8DBBFF`.

**Interfaz:**
- Botón circular grande al centro con anillo suave mientras conecta; sin animación si el sistema pide reducir movimiento.
- Elementos táctiles de 48dp o más.
- Iconos vectoriales de Material, sin emojis.
- Fuente del sistema para mantener el APK ligero.

**Pruebas:**
- Unitarias: validación del `.conf`, cifrado y descifrado del almacén, y máquina de estados del ViewModel con un túnel falso.
- Manual en el teléfono contra el VPS: handshake, IP pública y DNS de Blocky.

## 6. Fase 2 — Modo administrador (propuesta aprobada)

Requiere un servicio pequeño en el VPS (Go o Python) que reutilice la lógica de `newClient` y `revokeClient` del script: generar claves, asignar IP, añadir el peer y ejecutar `wg syncconf`.

- **Transporte:** el servicio escucha solo en `10.66.66.1`, dentro del túnel. No abre puertos públicos; para administrar hay que estar conectado a la VPN. SSH queda como respaldo.
- **Autenticación:** par de claves Ed25519. La clave privada se genera en el Keystore del teléfono y no sale de él. La clave pública se registra en el VPS con un comando por SSH. Cada petición va firmada e incluye marca de tiempo para evitar reutilización. Si se pierde el teléfono, se elimina esa clave pública en el VPS.
- **Funciones:** listar usuarios, crear (nombre → el servidor devuelve el `.conf`, que la app importa directo o comparte por QR) y revocar.
- **Acceso en la app:** Acerca de → "Modo administrador".
- **Alternativa descartada:** token secreto compartido (más simple, pero deja un secreto reutilizable que puede filtrarse).

Esta fase tendrá su propio diseño detallado (puerto, formato de la API, comando de registro de claves, formato de firma) antes de implementarse.

## 7. Supuestos pendientes de confirmar

- Android 8.0 o superior (minSdk 26).
- Escáner QR offline con ZXing.
- Fuente del sistema (sin fuentes empaquetadas).
- Distribución como APK firmado; nombre de paquete por definir.
- Textos de la app en español.
