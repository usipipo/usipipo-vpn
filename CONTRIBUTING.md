# uSipipo VPN — Contribuyendo

Flujo de trabajo para contribuir al repositorio: ramas, commits, CI y revisión de pull requests.

## Rama por fase

1. Crea una rama desde `main`: `feat/mockup-fase<N>` (o `fix/…`, `docs/…` según corresponda).
2. No hagas push a `main` directamente. Todas las cambios entran por rama + merge.
3. Cada fase compila y se prueba por separado antes de mergear.

## Commits

- Formato: convencional commits en español, minúsculas.
  - `feat(ui): …` — nueva funcionalidad de interfaz
  - `fix(ui): …` — corrección de interfaz
  - `docs: …` — documentación
  - `refactor(ui): …` — reestructuración sin cambio de comportamiento
- Un cambio lógico por commit. No mezcles refactor con feature.
- Cuerpo del commit: explica el porqué, no solo el qué.

## CI (GitHub Actions)

- `android-ci.yml` corre en push y PR a `main`, `master`, `develop`.
- Ejecuta `./gradlew testDebugUnitTest` y `assembleDebug`.
- La verificación ocurre en la CI: el toolchain (JDK 17, Android SDK, Gradle) vive en GitHub Actions, no en la máquina de trabajo. No se instala nada localmente para compilar; se sube la rama, se abre la PR y se lee el resultado del workflow.
- Si falla en CI, no abras la PR hasta que esté verde.

## Pull requests

1. Abre la PR desde tu rama hacia `main`.
2. La PR debe incluir descripción del cambio y el porqué.
3. Revisión por pares obligatoria antes del merge.
4. No hagas force push a una rama que ya tiene PR abierta sin avisar.

## Estándares de código

Todos los cambios deben cumplir `CODING_STANDARDS.md`. Si no hay una regla escrita para un caso, decide por la consistencia con el código existente y documenta la decisión si afecta a varios archivos.

## Límites

- No toques la lógica de conexión del túnel (`tunnel/`) salvo que la fase de implementación lo autorice explícitamente.
- No añadas dependencias sin actualizar `gradle/libs.versions.toml` y justificarlo en el commit.
