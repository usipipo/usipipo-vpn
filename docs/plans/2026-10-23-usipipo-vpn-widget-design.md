# uSipipo VPN: widget y mosaico de conexión rápida

Fecha: 2026-10-23
Estado: diseño aprobado, pendiente de plan de implementación

## Contexto

La app Kotlin (rediseño del mockup y correcciones visuales ya en el repositorio) solo
permite conectar o desconectar desde la pantalla Inicio. Se quiere conectar y desconectar
sin abrir la app, desde un widget de la pantalla de inicio y desde un mosaico del panel de
ajustes rápidos.

## Decisiones

| Tema | Decisión |
|---|---|
| Contenido del widget | Botón de conectar/desconectar, nombre del perfil activo y país (bandera + texto). Conecta el perfil activo. Sin selector de perfil. |
| Tecnología | Widget clásico con `RemoteViews` y `AppWidgetProvider`, sin Jetpack Glance. Sin dependencias nuevas. |
| Extras | Mosaico de ajustes rápidos (Quick Settings Tile) además del widget. |
| Animaciones | Ninguna en el widget (no es posible). Sin sombras ni desenfoques por las gamas bajas. |

## 1. Arquitectura

El widget y el mosaico no tocan WireGuard. Ambos usan `QuickConnectController`, expuesto en
`AppContainer`, que reutiliza lo existente:

- Perfil activo: `ConfigStore` (el mismo criterio que `HomeViewModel.selectedId`).
- Estado: `TunnelManager.state`.
- Acciones: `TunnelManager.connect` / `disconnect`.

Piezas nuevas:

- `UsipipoWidgetProvider`: widget de la pantalla de inicio.
- `UsipipoTileService`: mosaico de ajustes rápidos.
- `VpnPermissionActivity`: Activity transparente, solo cuando falta el permiso de VPN
  (`VpnService.prepare`). Lo pide, conecta y se cierra.
- `WidgetUpdater`: observa el estado del túnel y la lista de perfiles y redibuja widget y
  mosaico solo cuando algo cambia.
- `WidgetStateMapper`: función pura que traduce estado a texto, color e icono (testeable sin
  Android).

Flujo al tocar el botón o el mosaico:

1. Túnel activo (Connecting, Connected o NoResponse): se desconecta.
2. Si no, con permiso concedido: se conecta el perfil activo sin abrir la app.
3. Sin permiso: se abre `VpnPermissionActivity`.
4. Sin perfiles: se abre la app en Importar.

Estado desactualizado: el túnel vive en el proceso de la app. Si el proceso muere, el túnel
cae pero el widget podría seguir mostrando "Conectado". Se guarda la hora del último estado
y, al refrescarse (`onUpdate`, arranque del teléfono, actualización de la app) o al tocarlo,
se contrasta con el estado real. Sin proceso se asume "Desconectado".

Cambios en archivos existentes: `AndroidManifest.xml` (receiver, service del mosaico,
activity), `AppContainer.kt` (controlador) y `UsipipoApp.kt` (arrancar `WidgetUpdater`).
No se modifica la lógica del túnel.

## 2. Diseño visual

### Widget (2×2 por defecto, redimensionable a 4×2)

Tarjeta con esquinas de 28 dp, mismo lenguaje que Atmósfera, con drawables y colores.

- Arriba: la "u" del logo a la izquierda y píldora de estado a la derecha ("Desconectado",
  "Conectando…", "Activo", "Sin respuesta").
- Centro: botón circular de ~64 dp. Power; escudo con check al estar conectado.
- Abajo: nombre del perfil (una línea, elipsis) y bandera + país en texto pequeño. Sin país
  en caché, solo el perfil.
- 4×2: botón a la izquierda y texto a la derecha, con un `RemoteViews` de diseño distinto.
- Sin perfiles: botón "+" con "Importar perfil"; abre la app en Importar.

| Estado | Fondo | Botón |
|---|---|---|
| Desconectado | Azul pastel | Anillo azul, power |
| Conectando | Azul pastel | Anillo con indicador de progreso |
| Conectado | Degradado azul a turquesa/menta | Relleno turquesa, escudo con check |
| Sin respuesta / fallo | Tinte rosado leve | Anillo ámbar, power |

Tema oscuro: azul noche a verde azulado, con recursos `-night`.

### Mosaico de ajustes rápidos

- Icono del escudo de la app y nombre "uSipipo VPN".
- Subtítulo con el perfil activo.
- Estados nativos: apagado, encendido y en transición (conectando).
- Mismo flujo de toque que el widget, incluida la Activity transparente.

## 3. Actualización, errores y pruebas

Actualización:

- `WidgetUpdater` se inicia en `UsipipoApp.onCreate()` y redibuja solo ante cambios (sin
  refresco periódico ni servicio nuevo en segundo plano).
- El país sale de la caché de perfil existente; no hay consultas de red nuevas.

Errores y casos límite:

- Permiso de VPN denegado: la Activity se cierra y queda "Desconectado". Se anota en el
  registro de Diagnóstico.
- Perfil ilegible o fallo al conectar: estado "Fallo"; un segundo toque reintenta.
- Toques repetidos: se ignoran mientras el estado es "Conectando", salvo el toque que
  desconecta.
- Perfil activo borrado: pasa al siguiente perfil o a "Importar perfil" si no queda ninguno.
- Otra VPN toma el control: lo detecta `onStateChange` del túnel y el widget lo refleja.
- Android 13 o superior: pista en Diagnóstico sobre cómo añadir el widget y el mosaico.
- Registro: solo metadatos (acción, estado, id corto del perfil), sin claves ni texto del
  .conf.

Pruebas:

- Tests unitarios de `QuickConnectController`: toque en cada estado, sin permiso, sin
  perfiles, perfil borrado y toques repetidos, con los `Fakes` existentes.
- Test de `WidgetStateMapper`: texto, color e icono por estado.
- Comprobación de contraste de los colores nuevos, como `PaletteContrastTest`.
- Prueba manual con la APK del CI: añadir widget, conectar, desconectar, matar la app y
  mosaico con y sin permiso concedido.

## Fuera de alcance

Selector de perfil en el widget, estadísticas en vivo, pantalla de configuración del widget
y animaciones.
