# uSipipo VPN (Android) — Fase 0 + Diagnóstico

App Android nativa en Kotlin que importa el `.conf` de WireGuard de tu VPS y conecta con un botón.
Este paquete es la **fase 0: el esqueleto de la arquitectura**, con el flujo de la v1 ya cableado
de punta a punta (importar → guardar cifrado → conectar → estado).

Diseños aprobados:
- [`docs/plans/2026-10-09-usipipo-vpn-android-design.md`](docs/plans/2026-10-09-usipipo-vpn-android-design.md)
- [`docs/plans/2026-10-11-usipipo-vpn-diagnostico-design.md`](docs/plans/2026-10-11-usipipo-vpn-diagnostico-design.md) (Diagnóstico y ajustes avanzados)

## Antes de compilar

> **El proyecto Android no se ha compilado ni ejecutado todavía.** Se escribió sin acceso al SDK de Android.
> Espera algún ajuste menor en la primera compilación (ver "Puntos a vigilar").
>
> Lo único verificado: la lógica pura del registro (`LogRedactor`, `FileLogWriter`, `ReportBuilder`, `AppLog`)
> se compiló con kotlinc 2.3.21 y sus 16 pruebas pasaron, más una prueba de humo de `AppLog`.

- Android Studio con soporte para AGP 9.3.x, JDK 17 o superior y Android SDK 37 (`compileSdk`).
  Si tu Studio es más antiguo, baja las versiones en `gradle/libs.versions.toml` o actualiza Studio.
- Abre la carpeta del proyecto. El wrapper descarga Gradle 9.5.0 solo.
- Ejecuta en un teléfono real: el túnel de VPN no funciona bien en el emulador.

## Estructura

```
app/src/main/java/com/usipipo/vpn/
├── UsipipoApp.kt, AppContainer.kt   Dependencias a mano (sin Hilt, a propósito)
├── MainActivity.kt
├── domain/Models.kt                 SavedConfig y TunnelState (Desconectado, Conectando, Conectado,
│                                    SinRespuesta, Fallo)
├── data/
│   ├── Crypto.kt                    AES-256-GCM con clave del Android Keystore
│   ├── ConfigStore.kt               Contrato del almacén
│   ├── EncryptedFileConfigStore.kt  Un archivo cifrado por configuración + índice sin claves
│   └── ConfigImporter.kt            Valida el .conf con el parser oficial de WireGuard
├── diagnostics/                     Registro propio: AppLog, LogRedactor (tapa claves), FileLogWriter
│                                    (archivo rotativo), CrashHandler, informe y envío por correo
├── tunnel/
│   ├── TunnelManager.kt             Contrato: state, connect, disconnect
│   └── WireGuardTunnelManager.kt    GoBackend; "Conectado" solo tras el primer handshake
└── ui/
    ├── UsipipoRoot.kt               Navegación por estado (3 pantallas)
    ├── theme/Theme.kt               Paleta de azules pastel, claro y oscuro
    ├── components/ConnectButton.kt  Botón circular con anillo (respeta "reducir movimiento")
    ├── home/                        Lista de configuraciones + botón + estado
    ├── importer/                    Archivo .conf o QR (escáner ZXing offline)
    ├── about/                       Versión, soporte (usipipo@gmail.com) y acceso a Diagnóstico
    └── diagnostics/                 Diagnóstico y ajustes avanzados
```

## Qué incluye la fase 0

- Build completo (Gradle Kotlin DSL, catálogo de versiones, wrapper).
- Las tres capas (UI → ViewModel → datos/túnel) con contratos por interfaz.
- Tema y pantallas de la v1; textos en español.
- Pruebas unitarias: `HomeViewModel` (7, con un túnel y un almacén falsos) y el registro (16).
- **Diagnóstico y ajustes avanzados:** registro en archivo con rotación, claves tapadas automáticamente,
  captura de cierres inesperados, modo detallado (con logcat), visor, borrado y envío del informe por
  correo a usipipo@gmail.com.

## Qué NO incluye (según el diseño)

Estadísticas en pantalla, conexión automática, acceso a "VPN siempre activa", pegar texto,
split tunneling, ícono definitivo, minificación y todo el **modo administrador**
(fase 2: servicio en el VPS dentro del túnel + clave Ed25519 del Keystore).
La firma de la versión `release` ya está resuelta (ver «Release firmada»).

## Supuestos que elegí yo (cámbialos si no te sirven)

- Paquete y `applicationId`: `com.usipipo.vpn`.
- `minSdk` 26 (Android 8.0), `targetSdk` 36, `compileSdk` 37.
- Versiones: AGP 9.3.3, Kotlin 2.3.21, Compose BOM 2026.08.00, `wireguard-android:tunnel` 1.0.20251231.
  Las demás (core-ktx, activity, lifecycle, coroutines, ZXing) son versiones conocidas, no necesariamente
  las últimas.
- Sin notificación propia mientras hay conexión: el sistema ya muestra el icono de VPN.

## Puntos a vigilar en la primera compilación

1. **Desugaring:** la librería de WireGuard lo pide; ya está activado en `app/build.gradle.kts`.
2. **Kotlin integrado de AGP 9:** por eso no se aplica `org.jetbrains.kotlin.android`.
3. **API de la librería de WireGuard:** `Statistics`/`PeerStats.latestHandshakeEpochMillis()` y
   `Tunnel.onStateChange` se escribieron de memoria; si el compilador se queja, el cambio es local a
   `WireGuardTunnelManager.kt`.
4. **ZXing:** `PortraitCaptureActivity` usa el tema `zxing_CaptureTheme` de la librería.
5. **Nombre de interfaz del túnel:** `usp` + 10 caracteres del id (WireGuard limita a 15).
6. **Adjunto del correo:** se usa `ACTION_SENDTO` + `FileProvider`. Algunos clientes de correo ignoran el
   adjunto; por eso el cuerpo repite la información del dispositivo.
7. **logcat:** se lee con `logcat --pid` del propio proceso (API 24+); en algunos fabricantes puede venir vacío.

## Siguiente paso sugerido

Compilar, probar en tu teléfono con el `.conf` de tu VPS (handshake, IP pública, DNS de Blocky) y
corregir lo que salga. Después, el plan de la fase 1.

## Compilar la APK con GitHub Actions

El repositorio incluye `.github/workflows/android-ci.yml`. Al subir cambios a `main`, `master` o
`develop`, al abrir/actualizar un Pull Request hacia esas ramas, o al ejecutarlo manualmente,
GitHub Actions ejecutará las pruebas unitarias y compilará la APK de depuración.

### Descargar la APK

**Versión para distribuir (firmada):** las releases se crean al subir una etiqueta `vX.Y.Z`
(firmadas con el keystore de release y publicadas por `.github/workflows/release.yml`). Aparecen
en la [página de Releases](https://github.com/usipipo/usipipo-vpn/releases) con la APK
`app-release.apk` adjunta; el funcionamiento está explicado en «Release firmada», más abajo.

**Sin esperar a una release, desde el run de Actions:**

1. Abre la pestaña **Actions** y entra en la ejecución más reciente de **Android CI - Build APK**.
2. Cuando termine correctamente, baja hasta **Artifacts** y descarga `usipipo-vpn-debug-apk`.
3. Descomprime el artefacto y encontrarás la APK para instalar en un dispositivo Android.
   El artefacto se conserva 14 días.

La APK del artefacto es de **depuración** y no está firmada para distribución pública. El
keystore de release y sus contraseñas nunca se guardan en el repositorio: viven en secretos de
GitHub.

> **Ojo con el cambio de certificado:** la APK de depuración está firmada con el keystore de
> depuración de Android y la APK de release con el keystore de release. La primera APK firmada
> **no** se instala encima de la de depuración: desinstálala una vez (se avisa también en
> «Release firmada»).

> Nota: el proyecto declara `compileSdk = 37`. El workflow instala la plataforma Android 37;
> si esa plataforma aún no estuviera disponible en el SDK de GitHub Actions, habrá que ajustar
> `compileSdk` a una plataforma disponible o actualizar el entorno de compilación.

## Release firmada

Las versiones que se distribuyen (instalables en cualquier teléfono, no solo para probar) se
publican con una **etiqueta** `vX.Y.Z`. El workflow `.github/workflows/release.yml` compila la
variante release, la firma con tu keystore y la publica en
[Releases](https://github.com/usipipo/usipipo-vpn/releases) con la APK adjunta
(`app-release.apk`) y las notas generadas automáticamente. Antes de publicar, el workflow
ejecuta las pruebas unitarias y verifica la firma con `apksigner`. El `versionName` y el
`versionCode` se calculan solos a partir de la etiqueta (`v0.2.3` → `0.2.3` / `2003`, con
`major×1.000.000 + minor×1.000 + patch`; cada número debe ser menor que 1000).

### 1. Configurar la firma (solo la primera vez)

En tu máquina, dentro de la carpeta del repositorio, ejecuta el asistente:

```bash
scripts/setup-release-signing.sh
```

Necesitas `keytool` (viene con el JDK 17) y la CLI de GitHub `gh` con la sesión iniciada
(`gh auth login`). El asistente te pregunta la ruta del keystore (por defecto
`~/usipipo-release.jks`), el alias de la clave (`release`) y las contraseñas; ninguna se
muestra por pantalla, se guarda en el historial del shell ni viaja por la línea de comandos
(`keytool` las lee de variables de entorno con `-storepass:env` y `gh` las recibe por
stdin). Después genera el keystore, lo convierte a base64 y guarda los cuatro secretos del
repositorio (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD`).

**Guarda una copia de seguridad del keystore y de sus contraseñas.** Si se pierden no podrás
publicar actualizaciones que se instalen encima de las anteriores: Android exige que todas las
versiones de una app vengan firmadas con el mismo certificado.

Si algo queda a medias (por ejemplo, falla uno de los `gh secret set`), vuelve a ejecutar el
asistente con `--solo-secretos`: reutiliza el keystore que ya tienes y vuelve a subir los
cuatro secretos sin generar uno nuevo:

```bash
scripts/setup-release-signing.sh --solo-secretos
```

### 2. Crear y subir la etiqueta

```bash
git tag v0.2.0
git push origin v0.2.0
```

Al subir la etiqueta se dispara el workflow y, si termina bien, la release aparece en la
página de Releases con la APK firmada lista para descargar. Si subes la misma etiqueta otra
vez (o el run se reintenta), la release se actualiza con el nuevo adjunto en lugar de
quedar rota. Si los secretos de firma faltan o la etiqueta no tiene el formato `vX.Y.Z`, el
workflow falla sin publicar nada: nunca se distribuye una APK sin firmar como release.

### 3. O lanzar la release sin escribir la versión a mano (recomendado)

En la pestaña **Actions** → **Release firmada** → **Run workflow** (sin etiqueta), el
workflow calcula solo la versión y la publica firmada: no hay que teclear ni el número de
versión ni crear la etiqueta. Lanzarlo así publica una **release real y firmada**, así que
exige los cuatro secretos de firma configurados (ver sección 1): ya no produce una
compilación sin firmar con solo el artefacto.

### 4. Versionado dinámico: la versión sale del historial

La versión nunca se escribe a mano: la calcula `scripts/next-version.sh` a partir de las
etiquetas `vX.Y.Z` y de los commits convencionales que las siguen (las reglas exactas están
en la cabecera del script, y `scripts/next-version.test.sh` las prueba).

- **Sin ninguna etiqueta `vX.Y.Z`, la versión es la inicial: `0.1.0`** (y se queda así hasta
  que exista una etiqueta base).
- Con etiqueta base (la más alta por semver, no por fecha), se cuentan los commits desde
  ella y manda el cambio más grande del conjunto:

| Commits desde la etiqueta base | Siguiente versión |
|---|---|
| uno con `!` (`feat!:`) o pie `BREAKING CHANGE:` | salto mayor (salvo en 0.x: salta el menor, para no llegar a 1.0.0 solo) |
| cualquier `feat:` | salto menor |
| cualquier `fix:` | salto de parche |
| solo `docs:`, `chore:`, `ci:`, `test:`… | salto de parche |

Así, la primera release del repositorio es `v0.1.0` y las siguientes se numeran solas según
lo que se vaya cometiendo. Si la etiqueta calculada ya existe, el workflow falla con un
mensaje claro en lugar de republicar. El `versionCode` se deriva de la versión con
`major×1.000.000 + minor×1.000 + patch`, sin colisiones entre versiones distintas.

> **La primera APK firmada no se instala encima de la de depuración.** La app que has estado
> probando está firmada con el keystore de depuración de Android y la release usa tu keystore
> propio; Android rechaza el cambio de certificado. Desinstala la app una vez y a partir de
> ahí las actualizaciones firmadas se instalan sin problemas.
