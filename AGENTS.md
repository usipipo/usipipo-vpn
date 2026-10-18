# uSipipo VPN — AGENTS.md

Guía de operación para agentes que trabajan en este repositorio. Lee este archivo antes de tocar código. Todo lo que no cabe aquí vive detrás de context pointers nombrados; ábrelos solo cuando la tarea lo requiera.

## Qué es este proyecto

**uSipipo VPN** es una app Android (Kotlin + Jetpack Compose) que gestiona túneles WireGuard. Pantallas: Inicio (conectar/desconectar, tarjeta del perfil activo), Configuraciones (mis perfiles / favoritos), Importar (QR y archivo `.conf`), Diagnóstico (registro, acerca de, actualizaciones). Tema claro y oscuro con paleta azul noche / azules pastel. Navegación por barra inferior de 4 pestañas sin librería de navegación.

## Stack y versiones (fuente de verdad: entorno)

No restates lo que el entorno ya confiesa. Consulta directamente:

- `gradle/libs.versions.toml` — AGP 9.3.3, Kotlin 2.3.21, Compose BOM 2026.08.00, dependencias
- `app/build.gradle.kts` — compileSdk 37, minSdk 26, targetSdk 36, Java 17, Compose habilitado
- `.github/workflows/android-ci.yml` — CI: `./gradlew testDebugUnitTest` + `assembleDebug`

## Estructura del código (paquete `com.usipipo.vpn`)

```
domain/          Modelos de dominio (SavedConfig, TunnelState, HomeMessage…)
data/            Almacén cifrado (ConfigStore, EncryptedFileConfigStore, Crypto, ConfigImporter)
tunnel/          Motor del túnel (TunnelManager, WireGuardTunnelManager)
diagnostics/     Registro y diagnóstico (AppLog, LogRedactor, ReportBuilder…)
update/          Comprobación de actualizaciones (UpdateChecker, ReleaseInfo…)
ui/
  home/          Pantalla Inicio (HomeScreen, HomeViewModel, ConnectButton)
  configs/       Pantalla Configuraciones (ConfigsScreen, ConfigCard, ConfigsFilter)
  importer/      Pantalla Importar (ImportScreen, ImportViewModel, QR + archivo)
  diagnostics/   Pantalla Diagnóstico (DiagnosticsScreen, AboutSection)
  components/    Componentes compartidos (UsipipoCard, AppHeader, AppLogo, IconTile, UsipipoBottomBar)
  nav/           Navegación (MainTab, UsipipoRoot, UsipipoApp)
  theme/         Tema (Theme.kt, Atmosphere.kt, Motion.kt, colores claro/oscuro)
```

Regla de co-locación: cada pantalla vive en su propio paquete bajo `ui/` con su ViewModel, su Screen y sus componentes privados. Los componentes compartidos viven en `ui/components/`.

## Cómo trabajar (steps)

1. **Lee el plan antes de implementar.** Las fases están en `docs/plans/`. Cada fase compila y se prueba por separado.
2. **Crea una rama por fase.** `feat/mockup-fase<N>` desde `main`. No hagas push a `main` directamente.
3. **Escribe tests antes del código** (TDD) cuando la tarea lo requiera. Los tests corren en JVM con JUnit 4.
4. **Ejecuta la suite completa antes de commitear:** `./gradlew testDebugUnitTest`
5. **Commitea en español con convencional commits:** `feat(ui): …`, `fix(ui): …`, `docs: …`
6. **No toques la lógica de conexión del túnel** salvo que la fase lo autorice explícitamente.

## Context pointers (abre solo cuando la tarea lo requiera)

| Pointer | Qué contiene | Cuando usarlo |
|---------|-------------|---------------|
| `CODING_STANDARDS.md` | Estándares de código Kotlin + Compose, reglas de nombrado, imports, Compose | Cada vez que escribas o modifies código Kotlin |
| `CONTRIBUTING.md` | Flujo de ramas, commits, CI, revisión de pull requests | Antes de abrir una PR o crear una rama |
| `docs/plans/` | Planes de implementación por fase (mockup design, fase 1, fase 2…) | Al iniciar una nueva fase o tarea |
| `docs/design/` | Mockups, assets, paletas de color, logo | Cuando necesites referencias visuales exactas |

## Leading words (usar en prompts y commits)

- **tight**: bucle o cambio pequeño, determinista, sin sobreconstrucción
- **red**: estado de fallo observable y binario (un test que falla antes de implementar)
- **green**: tests pasando, build verde

---

## Completion criteria (para este documento)

- [ ] Cada agente nuevo puede entender qué hace la app y dónde vive cada cosa en 5 minutos
- [ ] Cada cambio de código puede verificarse contra `CODING_STANDARDS.md`
- [ ] Cada contribución puede seguirse contra `CONTRIBUTING.md`
