# Release firmada distribuible y actualización desde la app — diseño

- Fecha: 2026-10-11
- Estado: propuesto (pendiente de revisión)
- Proyecto: uSipipo VPN (Android, Kotlin + Compose, AGP 9.3.3, Gradle 9.5.0)
- Repositorio: `usipipo/usipipo-vpn` (actualmente privado; pasa a público con este cambio)
- Diseños previos relacionados: `docs/plans/2026-10-09-usipipo-vpn-android-design.md`, `docs/plans/2026-10-11-usipipo-vpn-diagnostico-design.md`

## 1. Objetivo y contexto

Dos objetivos encadenados:

1. **Release distribuible**: al subir una etiqueta `vX.Y.Z`, GitHub Actions compila la variante
   release, la firma con un keystore propio y la publica como release de GitHub con la APK
   adjunta, de forma que cualquier persona pueda descargarla e instalarla.
2. **Actualización desde la app**: la aplicación detecta sola su versión instalada, consulta el
   release más reciente de GitHub y, si existe una versión más nueva, muestra una ventana con
   dos acciones: «Más tarde» (posponer el aviso de esa versión) y «Descargar» (abrir el
   navegador en la release).

Contexto y restricciones:

- La app es de uso personal/distribución directa (no Google Play). Las claves de VPN viven en
  el `.conf` de cada usuario; el repositorio no contiene secretos.
- El proyecto solo se ha compilado mediante GitHub Actions; no hay build local. Todo cambio se
  verifica por Actions.
- La app no tiene hoy dependencias de red. La paleta (azules pastel en `ui/theme/Theme.kt`) y
  el patrón de iconos vectoriales propios (`res/drawable/ic_power.xml`) son el estilo vigente.
- Textos de interfaz en español.
- `minSdk` 26, `targetSdk` 36, `compileSdk` 37.

## 2. Decisiones tomadas

1. **Repositorio público.** La API `GET /repos/{owner}/{repo}/releases/latest` y los enlaces
   `browser_download_url` solo son accesibles sin token en repositorios públicos. Hacer el
   repo público evita embebir credenciales en la APK.
2. **Cadencia por etiqueta `vX.Y.Z`.** Cada etiqueta publicada genera una release firmada.
   `versionName` y `versionCode` se derivan de la etiqueta.
3. **Ventana con dos acciones:** «Más tarde» pospone; «Descargar» abre el navegador en la
   release. La app no descarga ni instala la APK (fuera de alcance).
4. **Disparador híbrido:** comprobación automática al arrancar la app, con throttle de una vez
   cada 24 h, en segundo plano y silenciosa si falla; más una comprobación manual desde la
   pantalla «Acerca de».

## 3. Requisitos

### 3.1 Release firmada

- **R1.** Existe `.github/workflows/release.yml`, disparado por `push` de etiquetas `v*` y por
  `workflow_dispatch`.
- **R2.** El keystore nunca se commitea. Las credenciales viven en secretos del repositorio:
  `ANDROID_KEYSTORE_BASE64` (keystore en base64), `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- **R3.** `app/build.gradle.kts` define `signingConfigs.release` leyendo variables de entorno.
  Si las variables no están presentes (PR, build sin secretos), la variante release se compila
  sin firma; el build no debe fallar por ello.
- **R4.** `versionName` y `versionCode` se inyectan desde la etiqueta
  (`-PVERSION_NAME=...`, `-PVERSION_CODE=...`) y, si no se pasan, se mantienen los valores
  por defecto de `app/build.gradle.kts` (0.1.0 / 1), de modo que los build sin etiqueta no
  cambian. `versionCode` se calcula como `major * 1_000_000 + minor * 1_000 + patch`
  (por ejemplo `v0.2.3` → 2003); cada componente debe ser menor que 1000, y si no lo es el
  job falla con un mensaje claro. La aritmética usa `10#` para que un componente con cero
  inicial (`v1.08.3`) no se interprete en octal.
  *Enmienda 2026-10-12 (ronda de revisión, ruling R1): la fórmula original
  `major * 10000 + minor * 100 + patch` (`v0.2.3` → 203) producía colisiones entre
  versiones distintas (`v0.0.100` y `v0.1.0` daban ambas 100; `v1.100.0` y `v2.0.0`, 20000)
  y la aritmética bash sin `10#` rompía el job con ceros iniciales (`v1.08.3` → «value too
  great for base»). La fórmula nueva no colisiona mientras cada componente sea < 1000 y el
  resultado máximo (999.999.999) queda por debajo del límite de Android (2.100.000.000).*
- **R5.** El workflow publica una release de GitHub con la APK firmada
  (`app-release.apk`) adjunta y notas generadas automáticamente.
- **R6.** El keystore de release se genera con el workflow
  `.github/workflows/generate-keystore.yml` (disparo `workflow_dispatch`), que corre en el
  runner con JDK 17 y no necesita ninguna herramienta local: `keytool` lo genera con
  `-storepass:env`/`-keypass:env` (las contraseñas entran por variables de entorno, nunca
  por argv ni interpoladas en el script), se codifica en base64 y se guardan los cuatro
  secretos con `gh secret set NAME --repo "$GITHUB_REPOSITORY"` leyendo de ficheros o de
  stdin; al final `gh secret list` comprueba que los cuatro existen y un `trap … EXIT` borra
  el keystore y su base64 del runner. Para escribir secretos hace falta un token fino
  (PAT) con *Repository permissions → Secrets: Read and write* guardado una sola vez como
  secreto `GH_PAT`, porque el `GITHUB_TOKEN` automático no puede crearlos; el usuario lo
  revoca al terminar. Este workflow es el camino principal. El script wizard interactivo
  `scripts/setup-release-signing.sh` queda como alternativa para quien sí tenga un JDK en
  su máquina y prefiera no pasar por Actions. En ningún caso una contraseña ni material de
  clave se imprime en logs, viaja por argv o se escribe en ficheros temporales sin borrado.
  *Enmienda 2026-10-12 (segunda ronda): el repositorio vuelve a ser público y los inputs
  de un `workflow_dispatch` son visibles en el payload público del run, así que las
  contraseñas dejan de ser inputs del formulario. El usuario las crea antes en la web como
  dos secretos write-only, `TMP_STORE_PASSWORD` y `TMP_KEY_PASSWORD`; el workflow las lee
  de esos secretos (y falla con mensaje claro antes de generar nada si falta cualquiera de
  los dos), las exporta como variables de entorno del paso (`STORE_PASSWORD` /
  `KEY_PASSWORD`) y las borra con `gh secret delete` usando el PAT después de comprobar
  los cuatro secretos finales, en modo best-effort: si el borrado falla el run sigue
  correcto y el resumen avisa de que hay que eliminarlas a mano. Los únicos inputs del
  dispatch quedan `key_alias` (por defecto `release`), `validity_days` (por defecto
  `10000`) y `confirm` (obligatorio, exactamente `GENERAR`).*
- **R7.** El repositorio pasa a público (`gh repo edit --visibility public`).

### 3.2 Actualización desde la app

- **R8.** Al arrancar, la app consulta el release más reciente como máximo una vez cada 24 h
  (marca temporal en preferencias), en `Dispatchers.IO`, sin bloquear la interfaz.
- **R9.** La versión instalada se lee del sistema (`PackageManager.versionName`), con
  `BuildConfig.VERSION_NAME` como respaldo. La versión remota se toma de `tag_name` del
  release.
- **R10.** La comparación es numérica por componentes (`major.minor.patch`). El prefijo `v`
  del tag se ignora. Los componentes ausentes se tratan como 0 (`1.2` equivale a `1.2.0`). Si
  la versión remota es mayor que la instalada, hay actualización.
- **R11.** La ventana muestra: icono, título, texto con la versión nueva, botón «Descargar»
  (abre `Intent(ACTION_VIEW)` con `html_url` del release) y botón «Más tarde».
- **R12.** «Más tarde» guarda la versión pospuesta. No se vuelve a avisar de esa versión
  concreta; una versión superior sí vuelve a avisar.
- **R13.** Cualquier fallo (sin red, error HTTP, JSON inesperado, rate limit) es silencioso
  para el usuario: se registra en `AppLog` y se reintenta en el siguiente arranque.
- **R14.** La pantalla «Acerca de» gana un botón «Buscar actualizaciones» que ejecuta la
  comprobación sin throttle y muestra el resultado: al día, disponible (abriendo la ventana) o
  error.
- **R15.** Los textos nuevos van en `strings.xml` en español, con el tono de los existentes.

## 4. Arquitectura

Módulos pequeños de una sola responsabilidad; la lógica pura (sin Android) se prueba en JVM,
igual que `diagnostics/`.

```
app/src/main/java/com/usipipo/vpn/update/
├── Version.kt                 data class Version(major, minor, patch) con
│                              Version.parse(String); comparación por componentes
├── ReleaseInfo.kt             data class ReleaseInfo(version: Version, htmlUrl: String, name: String?)
├── ReleaseParser.kt           fun parse(json: String): ReleaseInfo?  (puro, sin org.json)
├── ReleaseSource.kt           interface ReleaseSource { suspend fun latest(): ReleaseInfo? }
│                              + GitHubReleaseSource (HttpURLConnection, corrutinas)
├── UpdateStateStore.kt        interface UpdateStateStore { skippedVersion, lastCheckAt }
│                              + PrefsUpdateStateStore (SharedPreferences)
├── UpdateChecker.kt           class UpdateChecker(source, store, clock) con
│                              suspend fun check(currentVersion, force): UpdateStatus
├── UpdateStatus.kt            sealed interface: UpToDate | UpdateAvailable(info) |
│                              Postponed | Failed | SkippedThrottled
```

`UpdateStatus` tipado y estable; la UI solo consume ese sellado. La fuente de red y el reloj se
inyectan, de modo que las pruebas no tocan red ni calendario real. `Version` es `data class` y no
`value class` porque un `value class` solo admite un parámetro en el constructor primario y hace
falta guardar tres componentes. La versión instalada entra por parámetro (`currentVersion`):
resolverla desde el sistema es dependencia de plataforma, y así el núcleo se mantiene puro y se
prueba en la JVM.

Integración:

- `AppContainer` crea `UpdateChecker` con `GitHubReleaseSource` y `PrefsUpdateStateStore`, y
  expone `updateStatus: StateFlow<UpdateStatus>`.
- `UsipipoApp.onCreate` lanza una comprobación no bloqueante con throttle (una vez por
  arranque bastaría; el throttle de 24 h vive en el store).
- `UsipipoRoot` muestra `ui/update/UpdateDialog.kt` cuando el estado es `UpdateAvailable`.
- `AboutScreen` gana el botón de comprobación manual (R14) que llama al checker con
  `force = true` y refleja el resultado.

## 5. Diseño de interfaz

Paleta vigente en `ui/theme/Theme.kt` (azules pastel):

- Claro: `primary #A8CCFF`, `onPrimary` navy `#0B2545`, `background #F3F8FF`,
  `surface` blanco, `outline #D6E6FB`, error `#F4A3A3`.
- Oscuro: `primary #8DBBFF`, `onPrimary` navy, `background #0E1A2B`, `surface #162843`,
  `outline #2B4468`.

Ventana de actualización (`ui/update/UpdateDialog.kt`):

- `AlertDialog` de Material3 con `icon` = vectorial propio `ic_update.xml` (flecha circular,
  trazo redondeado, mismo estilo que `ic_power.xml`; sin dependencias nuevas de iconos).
- Título: «Nueva versión disponible». Texto: versión nueva y una línea de contexto
  («Puedes descargarla desde GitHub.»).
- Botón de confirmación «Descargar» en el color primario; botón de descarte «Más tarde».
- `contentDescription` en el icono; contraste de texto sobre fondos pastel usando navy
  (igual que el resto de la app).
- Respeta el tema claro/oscuro por herencia de `MaterialTheme`.

## 6. Manejo de errores y límites

- Sin red, HTTP distinto de 200, JSON sin `tag_name`, o versión remota no parseable:
  `UpdateStatus.Failed`, registro en `AppLog`, sin ventana.
- Rate limit de la API sin token: 60 peticiones/hora por IP. El throttle de 24 h lo hace
  irrelevante en la práctica.
- `versionName` ausente o malformado: se usa `BuildConfig.VERSION_NAME`; si tampoco es
  parseable, la comprobación se descarta sin crash.
- La consulta nunca bloquea el primer fotograma; si falla, no hay ningún cambio visible.

## 7. Pruebas

Pruebas unitarias en JVM (sin Android), siguiendo el estilo de `diagnostics/`:

- `VersionTest`: parse de `"0.1.0"`, `"v0.2.3"`, con espacios, malformado; comparaciones
  mayor/menor/igual, componentes de distinta longitud (`1.2` vs `1.2.0`).
- `ReleaseParserTest`: JSON válido de GitHub, sin `body`, `tag_name` sin `v`, JSON roto,
  campos ausentes.
- `UpdateCheckerTest` (con fuente y store falsos, reloj inyectado): hay actualización, no hay
  actualización, versión pospuesta, throttle activo, `force = true` ignora el throttle, fallo
  de red.
- `UpdateStateStoreTest`: con implementación en memoria.

Las pruebas de integración reales contra GitHub quedan fuera de alcance; la verificación
end-to-end se hace con el run de Actions y una etiqueta de prueba.

## 8. Migración y advertencias

- **Mismo certificado para instalar encima.** Las compilaciones debug actuales usan el
  keystore de depuración de Android. La primera APK firmada con el keystore de release **no**
  podrá instalarse sobre una debug: hay que desinstalarla una vez. Este punto se documenta
  en el README.
- El repositorio pasa de privado a público.

## 9. Versionado dinámico

La versión de cada release se deriva del historial de git mediante
`scripts/next-version.sh`; nunca se escribe a mano.

- Sin ninguna etiqueta `vX.Y.Z`, la versión es la inicial `0.1.0`.
- Con etiqueta base (la más alta por semver), se cuentan los commits convencionales desde
  ella y manda el cambio más grande: `!`/`BREAKING CHANGE` → mayor (en 0.x salta el menor);
  si no, `feat:` → menor; si no, `fix:` o el resto → parche.
- El camino `workflow_dispatch` de `release.yml` calcula la versión, crea la etiqueta
  `vX.Y.Z` con la API de GitHub (para no auto-dispararse) y publica firmada; si la etiqueta
  calculada ya existe, falla sin republicar.
- Las reglas viven en la cabecera del script y se prueban en `scripts/next-version.test.sh`.

## 10. Fuera de alcance

- Descarga e instalación de la APK desde la app (permiso de orígenes desconocidos,
  `FileProvider`).
- Actualización forzada o bloqueo de versiones antiguas.
- Publicación en Google Play, firma de App Bundle, Play App Signing.
- Renderizado de las notas de la release (markdown) dentro de la app.
- Traducción a otros idiomas.
