# Diseño: asunto y cabecera del correo de diagnóstico

Fecha: 2026-10-25
Estado: aprobado
Proyecto: uSipipo VPN (app Android Kotlin)

## Objetivo

Hacer que los correos de diagnóstico que la app envía a `usipipo@gmail.com` sean fáciles de filtrar y de agrupar (por versión de la app, modelo y versión de Android), tanto desde Gmail como al leerlos con Claude mediante el conector de Gmail.

## Decisiones

- El asunto lleva prefijo fijo, versión de la app, modelo y versión de Android.
- Además del asunto, el cuerpo empieza con una cabecera en formato `clave: valor`.
- Descartado: solo prefijo, prefijo + versión, y añadir el tipo de error al asunto.

## Formato

### Asunto

```
uSipipo-Diag | v0.4.0 | Samsung SM-A256E | Android 16
```

El prefijo no lleva corchetes porque el buscador de Gmail los ignora. Se filtra con `subject:uSipipo-Diag`.

### Cabecera (primeras líneas del cuerpo, antes del texto libre)

```
app: uSipipo VPN
version: 0.4.0
model: Samsung SM-A256E
android: 16
verbose: no
```

La cabecera contiene solo estos cinco campos.

## Componentes

`DiagnosticEmailBuilder`, en Kotlin puro (sin dependencias de Android):

- Entrada: versión de la app, marca, modelo, versión de Android y estado del modo detallado.
- Salida: asunto y cabecera.
- La pantalla "Diagnóstico y Ajustes avanzados" lo llama al enviar el correo. El informe adjunto no cambia.

## Privacidad

Solo se incluyen datos técnicos del dispositivo y de la app. Nunca claves, endpoints ni contenido de configuraciones.

## Casos límite y errores

- Marca o modelo vacíos: se usa `desconocido`.
- Marca repetida dentro del modelo: se unen sin duplicar.
- Se eliminan saltos de línea y el carácter `|` de los textos del dispositivo.
- Cada campo se recorta a unos 40 caracteres para que el asunto no se corte en Gmail.
- Si falla el armado de los datos, el correo se envía igual con el prefijo `uSipipo-Diag` y lo que se pudo leer. El correo nunca se pierde por un error de formato.

## Pruebas

Unitarias en Kotlin puro:

- Asunto y cabecera con datos normales.
- Datos vacíos, con `|`, con saltos de línea y demasiado largos.
- Marca repetida dentro del modelo.
- La cabecera nunca contiene nada fuera de los cinco campos.

Todo se compila y se prueba mediante el CI de GitHub del proyecto.

## Alcance

Incluido: armado del asunto y de la cabecera al enviar el diagnóstico.

Fuera de alcance: tipo de error en el asunto, automatizar la lectura de correos, nuevos campos. Se pueden añadir después sin romper este formato.

## Lado Gmail

Con el conector de Gmail conectado, Claude busca con `subject:uSipipo-Diag` y agrupa por versión y modelo leyendo la cabecera.
