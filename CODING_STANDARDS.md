# uSipipo VPN — Estándares de código

Referencia de código para Kotlin + Jetpack Compose en este proyecto. Se alcanza por pointer desde `AGENTS.md`. Cada regla es verificable: un agente debe poder juzgar si un cambio la cumple sin preguntar.

## 1. Nombrado (leading word: exactitud)

Los nombres revelan qué hace la cosa, no cómo funciona internamente.

- Clases, objetos, interfaces: `PascalCase` (`SavedConfig`, `WireGuardTunnelManager`, `UsipipoCard`).
- Funciones, variables, parámetros: `camelCase` (`toggleFavorite`, `uiState`, `selectedId`).
- Constantes a nivel de paquete: `UPPER_SNAKE_CASE` (reservado; evitar en código nuevo salvo que el entorno lo exija).
- Archivos: `UpperCamelCase.kt`, uno por símbolo público principal. Si un archivo contiene varios símbolos privados relacionados, el nombre es el del símbolo principal.
- Recursos drawable/vector: `snake_case` con prefijo `ic_` (`ic_delete`, `ic_star_filled`, `ic_arrow_right`).
- Recursos string: `snake_case` descriptivo (`tagline_connected`, `cd_delete`, `err_invalid_config`).

## 2. Imports

- Orden alfabético dentro de cada bloque, agrupados por origen: `androidx.*`, `com.usipipo.vpn.*`, librerías de terceros (`com.journeyapps…`, `com.wireguard.android…`).
- No imports comodín (`import *`).
- No imports sin uso. Si un símbolo deja de usarse, quita el import en el mismo commit.
- Alias `as` solo cuando dos símbolos colisionan por nombre (`Delete as FilledDelete`).

## 3. Compose

### Composable
- Un `@Composable` por responsabilidad clara. Si un composable crece más de lo que cabe en una pantalla de contexto, extrae partes privadas en el mismo archivo o en un archivo de componentes privados.
- Parámetros con valores por defecto al final de la lista de parámetros.
- `Modifier` como primer parámetro nombrado después de los de datos, con valor por defecto `Modifier`.
- Content lambda (`content: @Composable () -> Unit`) como último parámetro cuando el componente envuelve contenido.

### Material 3
- Usa `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.colorScheme.onBackground` etc. No hardcodees colores.
- `Surface`, `Card`, `Scaffold` con `containerColor` explícito cuando el fondo no sea el por defecto del tema.
- Iconos: `material-icons-core` para símbolos estándar; vectores propios en `app/src/main/res/drawable/` para símbolos no disponibles o personalizados (`ic_logo_u`, `ic_delete`, `ic_star`).

### Estado
- `remember`, `rememberSaveable`, `mutableStateOf` solo en UI layer. La lógica de negocio vive en ViewModel.
- `collectAsStateWithLifecycle()` para flujos de ViewModel en pantallas.
- No llames a ViewModel desde composables privados anidados sin pasar el valor como parámetro.

### Previews
- Cada pantalla y cada estado relevante tiene `@Preview` para tema claro y oscuro (`uiMode = Configuration.UI_MODE_NIGHT_NO` y `UI_MODE_NIGHT_YES`).
- Los previews renderizan subcomponentes puros cuando la pantalla completa requiere `ViewModel` o `LifecycleOwner`.

## 4. Kotlin

- Tipos explícitos en firmas de funciones públicas y en propiedades de clase.
- `val` por defecto; `var` solo cuando el estado realmente muta.
- Data classes para modelos de dominio inmutables (`SavedConfig`, `TunnelState` sealed class).
- `sealed class` / `sealed interface` para estados mutuamente excluyentes (`TunnelState`, `ImportStage`, `ImportError`).
- No uses `!!` salvo imposible evitarlo y justificado en comentario.
- `when` exhaustivo sobre sealed types sin rama `else` salvo que el dominio lo justifique.

## 5. Tests (JUnit 4, JVM)

- Un test por comportamiento, nombre descriptivo en español o inglés consistente con el archivo.
- Arrange-Act-Act-Assert implícito: dado un estado, cuando se llama, entonces se verifica.
- TDD: escribe el test que falla (red) antes del código (green), luego refactor.
- Prueba casos borde: lista vacía, un ítem, nulos, valores límite.

## 6. Commits

- Convencional commits en español, minúsculas: `feat(ui): …`, `fix(ui): …`, `docs: …`, `refactor(ui): …`.
- Un cambio lógico por commit. No mezcles refactor con feature.
- Cuerpo del commit: por qué, no solo qué.

## 7. Lo que NO se toca

- La lógica de conexión del túnel (`tunnel/`) salvo que la fase de implementación lo autorice explícitamente.
- No añadas dependencias sin actualizar `gradle/libs.versions.toml` y justificarlo en el commit.
