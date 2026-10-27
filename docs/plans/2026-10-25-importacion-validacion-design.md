# uSipipo VPN · Mejora de importación y validación de configuraciones

Fecha: 2026-10-25
Estado: diseño aprobado, pendiente de plan de implementación
Alcance de protocolos: **solo WireGuard** por ahora; los nuevos protocolos llegarán en próximas actualizaciones.

## 1. Contexto

Un cliente envió un informe de diagnóstico de la app 0.4.0 (Samsung SM-A256E, Android 16). Al importar un archivo de 8912 caracteres, el registro solo decía "Configuración no válida (BadConfigException)", sin el motivo. Un `.conf` de cliente WireGuard normal ronda los 300–400 caracteres, así que el archivo elegido casi seguro no era una configuración de cliente (por ejemplo, el `wg0.conf` del servidor, un `.ovpn` u otro archivo).

El mismo informe mostró un fallo de red al consultar `api.github.com` (`UnknownHostException`). Es falta de conexión del teléfono en ese momento, la app lo gestionó bien y **no requiere cambios**.

## 2. Objetivos

1. Registrar el motivo exacto de un fallo de importación, sin exponer claves.
2. Validar el archivo antes de importarlo.
3. Mostrar al usuario mensajes claros, con consejo de qué hacer.

## 3. Fuera de alcance

- Soporte de otros protocolos (solo se deja la estructura preparada).
- Cambios en el túnel, el widget, el tema, la pantalla Home y el chequeo de actualizaciones.
- Cambios en el modo administrador.

## 4. Diseño

### 4.1 Error en la pantalla Importar

- **Tarjeta de error dentro de la pantalla**, bajo los botones "Escanear QR" e "Importar archivo". Queda visible mientras el usuario lee y decide.
- Tinte rosado leve del estado de fallo ya existente y un icono de contorno del set propio de iconos.
- Contenido: título corto, motivo y consejo. Ejemplos:
  - Archivo del servidor: "Este parece el archivo del servidor. Necesitas el de cliente (wg0-client-…conf)."
  - OpenVPN: "Esto es OpenVPN, aún no compatible. Por ahora solo WireGuard."
  - No es configuración: "Este archivo no parece una configuración."
  - Configuración con error: "Falta la clave del equipo (Interface → PrivateKey)."
- Acciones: **"Elegir otro archivo"** (principal) y **"Enviar diagnóstico"** (secundaria, abre el correo con el informe adjunto).
- La tarjeta se reemplaza en cada intento y desaparece al importar con éxito o al empezar otro intento.
- Aparece con un fundido corto; sin animación si el sistema reduce movimiento. Texto seleccionable y legible con TalkBack.
- Nunca muestra claves ni contenido del archivo; solo el tipo de problema y su ubicación.

### 4.2 `ImportValidator`

Componente en Kotlin puro (sin dependencias de Android). Lo usan por igual el archivo y el QR.

Pasos, en orden:

1. **Lectura con tope:** máximo 64 KB; más que eso, "no parece una configuración". El tamaño es solo una red de seguridad, no el criterio principal (el archivo del cliente, de 8912 caracteres, pasaría este filtro).
2. **¿Es texto?** Se rechaza si hay bytes nulos o demasiados caracteres no imprimibles (imagen, zip, PDF). Se limpia el BOM y se normalizan los saltos de línea.
3. **Detección de formato** con una lista de detectores:
   - WireGuard: contiene `[Interface]`.
   - OpenVPN: líneas como `client`, `remote`, `dev tun`, bloques `<ca>`.
   - Ninguno coincide: formato desconocido.
4. **¿Archivo del servidor?** WireGuard con `PostUp`, `PostDown` o `SaveConfig`, o varios `[Peer]` sin `Endpoint`.
5. **Análisis** con wireguard-android. Si lanza `BadConfigException`, se traducen su sección, ubicación y razón a un mensaje claro.

Resultado: tipo cerrado `ImportProblem`:

- `NoEsTexto`
- `DemasiadoGrande`
- `FormatoNoCompatible(nombre)`
- `ArchivoDeServidor`
- `ConfigInvalida(sección, campo, razón)`
- `Desconocido`

Cada uno tiene su texto en `strings.xml`, que alimenta la tarjeta de 4.1.

**Registro (AppLog):** una línea como
`Importar: formato=wireguard, tamaño=8912, problema=ArchivoDeServidor (Interface.PostUp)`.
Solo nombres de campos y razones; nunca valores ni claves.

**Futuros protocolos:** añadir uno nuevo es sumar su detector y su analizador a la lista. OpenVPN deja de ser "no compatible" sin tocar el resto.

### 4.3 Pruebas

Pruebas unitarias, con claves falsas en los datos de prueba:

- `.conf` de cliente válido: pasa.
- `.conf` con BOM y saltos CRLF: pasa.
- `wg0.conf` de servidor con `PostUp`/`PostDown` y varios `[Peer]`: `ArchivoDeServidor` (caso del cliente).
- Ejemplo de `.ovpn`: `FormatoNoCompatible("OpenVPN")`.
- Bytes de zip o imagen, archivo vacío y archivo de más de 64 KB: cada uno con su problema.
- Falta `PrivateKey`, clave inválida o `Endpoint` mal escrito: `ConfigInvalida` con sección y campo correctos.
- **Prueba de privacidad:** se importa un `.conf` con una clave falsa reconocible y se comprueba que la línea del registro no la contiene.

Previews de Compose: la tarjeta de error por cada tipo de problema, en tema claro y oscuro.

## 5. Todo se hace vía el CI de GitHub del proyecto

Nada depende de compilar o probar en local; el CI de GitHub es la fuente de verdad.

- **Compilación y pruebas:** el workflow `build-debug.yml` (en cada push, PR o ejecución manual) compila y ejecuta las pruebas unitarias del validador. Si aún no ejecuta las pruebas unitarias, se añade ese paso. Un fallo de pruebas o de compilación aparece en el resumen de errores del workflow.
- **APK para la prueba manual:** la APK debug que genera el CI es la que se instala en el teléfono para la verificación manual. No se compila ninguna APK en local.
- **Release:** solo al crear una etiqueta `v*`, con `release.yml` (APK firmada adjunta a una Release de GitHub). Esta mejora no cambia el flujo de release.
- **Criterio para dar por terminada cada etapa:** el CI en verde y la prueba manual con la APK del CI.

## 6. Prueba manual (con la APK del CI)

Importar el `wg0.conf` del servidor, un `.ovpn`, una foto y un `.conf` bueno. Luego usar "Enviar diagnóstico" y comprobar que el correo trae la línea nueva del registro.

## 7. Orden de implementación

1. `ImportValidator` y sus pruebas unitarias (CI en verde).
2. Registro en AppLog.
3. Tarjeta de error y textos en `strings.xml`.
4. Previews de Compose y prueba manual con la APK del CI.
