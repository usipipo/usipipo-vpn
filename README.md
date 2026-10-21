# uSipipo VPN (Android) — Fases 1 a 3: estructura, estilo, datos reales, país y ping

App Android nativa en Kotlin que importa el `.conf` de WireGuard de tu VPS y conecta con un botón.
Este paquete incluye la **fase 1: la interfaz del mockup llevada a código real** — barra inferior de 4
pestañas (Inicio, Configuraciones, Importar, Diagnóstico), tema azul noche (oscuro) y azul pastel
(claro), pantalla de Configuraciones con "Mis perfiles" y "Favoritos", Importar con dos tarjetas
(QR y archivo), y «Acerca de» integrado dentro de Diagnóstico — y la **fase 2: datos reales del
túnel y pantalla Detalles** — descarga/subida con gráfica, tiempo activo, protocolo, endpoint e
IPv6; pantalla Detalles con botón Desconectar y lápiz para renombrar el perfil; tarjetas de
estadísticas en Inicio cuando el túnel está conectado — y la **fase 3: país y ping** — bandera del
servidor por geolocalización del endpoint (ipapi.co, con caché de 24 h por perfil) y ping real
dentro del túnel cada 5 s, visibles en la pantalla Detalles y con la bandera en el perfil de Inicio.
Se parte de la fase 0 (arquitectura, almacén cifrado, túnel WireGuard, diagnóstico, actualizaciones).

Diseños aprobados:
- [`docs/plans/2026-10-09-usipipo-vpn-android-design.md`](docs/plans/2026-10-09-usipipo-vpn-android-design.md)
- [`docs/plans/2026-10-11-usipipo-vpn-diagnostico-design.md`](docs/plans/2026-10-11-usipipo-vpn-diagnostico-design.md) (Diagnóstico y ajustes avanzados)
- [`docs/plans/2026-10-18-usipipo-vpn-mockup-design.md`](docs/plans/2026-10-18-usipipo-vpn-mockup-design.md) (Mockup: de la pantalla a la app real)

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
├── domain/                          SavedConfig, TunnelState, CountryInfo, PingResult
├── data/
│   ├── Crypto.kt                    AES-256-GCM con clave del Android Keystore
│   ├── ConfigStore.kt               Contrato del almacén
│   ├── EncryptedFileConfigStore.kt  Un archivo cifrado por configuración + índice sin claves
│   ├── JsonExt.kt                   Lectura segura de strings del JSON (ausentes o null)
│   ├── ConfigImporter.kt            Valida el .conf con el parser oficial de WireGuard
│   └── geo/                         País y ping
│       ├── CountryRepository.kt     Geolocalización con caché de 24 h por perfil y cooldown ante 429
│       ├── IpApiClient.kt           Cliente HTTPS de ipapi.co (timeout 3 s, sin reintentos)
│       └── PingRepository.kt        Ping nativo con fallback a InetAddress.isReachable
├── diagnostics/                     Registro propio: AppLog, LogRedactor (tapa claves), FileLogWriter
│                                    (archivo rotativo), CrashHandler, informe y envío por correo
├── tunnel/
│   ├── TunnelManager.kt             Contrato: state, connect, disconnect
│   └── WireGuardTunnelManager.kt    GoBackend; "Conectado" solo tras el primer handshake
└── ui/
    ├── UsipipoRoot.kt               Navegación por pestañas (4 destinos, sin librería de navegación)
    ├── nav/MainTab.kt               Enum de pestañas (Home, Configs, Import, Diagnostics) + backTarget
    ├── theme/Theme.kt               Paleta azul noche (oscuro) y azul pastel (claro), tipografía Manrope
    ├── components/                  Componentes base del mockup
    │   ├── ConnectButton.kt         Botón circular con halo (respeta "reducir movimiento")
    │   ├── UsipipoCard.kt           Tarjeta con borde fino y esquinas de 20 dp
    │   ├── IconTile.kt              Icono en cuadro redondeado (44 dp)
    │   ├── AppLogo.kt               Logo "u" sobre azul de marca
    │   ├── AppHeader.kt             Cabecera con logo y nombre de la app
    │   ├── UsipipoBottomBar.kt      Barra inferior Material 3 con 4 pestañas
    │   └── UiSymbols.kt              Símbolos compartidos: globo y bandera emoji
    ├── home/                        Pantalla Inicio: botón conectar, estado, tarjeta del perfil activo, tarjetas de estadísticas (descarga, subida, tiempo, "Ver detalles")
    ├── configs/                     Pantalla Configuraciones: "Mis perfiles" y "Favoritos" con estrella
    ├── importer/                    Archivo .conf o QR (escáner ZXing offline, dos tarjetas)
    ├── diagnostics/                 Diagnóstico con «Acerca de» y comprobación de actualizaciones integrados
    └── details/                     Pantalla Detalles (sub-pantalla de Inicio): estadísticas con gráfica, tiempo activo, protocolo, endpoint, IPv6, país, ping, desconectar, renombrar
```

## Qué incluye la fase 1 y la fase 2

- Build completo (Gradle Kotlin DSL, catálogo de versiones, wrapper) — verificado en GitHub Actions.
- Las tres capas (UI → ViewModel → datos/túnel) con contratos por interfaz.
- **Navegación por pestañas:** barra inferior Material 3 con 4 destinos (Inicio, Configuraciones, Importar, Diagnóstico),
  sin librería de navegación; estado con `rememberSaveable`; Atrás desde cualquier pestaña vuelve a Inicio.
- **Tema:** oscuro azul noche (igual al mockup) y claro con azules pastel aprobados; comprobación de contraste ≥ 4.5:1.
- **Pantalla Inicio:** botón de conectar con Atmósfera, estado del túnel, tarjeta del perfil activo (nombre y protocolo),
  toque lleva a Configuraciones; subtítulo «Tu privacidad está protegida» al conectar. Cuando el túnel está conectado
  aparecen tarjetas de descarga y subida, tiempo activo y «Ver detalles».
- **Pantalla Configuraciones:** pestañas «Mis perfiles» y «Favoritos»; reutiliza tarjetas actuales (chip «Activo»,
  deslizar para borrar) y añade estrella de favorito; botón «+» y «Añadir configuración» llevan a Importar.
- **Pantalla Importar:** dos tarjetas grandes (QR y archivo `.conf`), paso «Nombre» con Cancelar, Atrás cancela el paso.
- **Pantalla Diagnóstico:** la pantalla actual de diagnóstico más la sección «Acerca de» (logo, versión, soporte,
  comprobación de actualizaciones).
- **Pantalla Detalles (sub-pantalla de Inicio):** chip «Activo», estadísticas con gráfica Canvas (últimos ~60 muestras),
  tiempo activo, protocolo, endpoint, IPv6, botón rojo «Desconectar» y lápiz para renombrar el perfil.
- **Datos:** campo `favorite` en `SavedConfig` y en el índice del almacén cifrado; perfiles existentes cargan como no favoritos.
- **Renombrar:** `ConfigStore.rename(id, name)` con validación de duplicados (sin distinguir mayúsculas), nombre vacío,
  e id desconocido.
- **Estadísticas del túnel:** `TunnelManager.counters()` (una muestra por segundo), `SpeedCalculator` (ventana de 3
  muestras, delta negativo como 0), `TunnelState.Connected(sinceMs)`, velocidad en bytes por segundo suavizada.
- **Componentes base:** `UsipipoCard`, `IconTile`, `AppLogo`, `AppHeader`, `UsipipoBottomBar`, `StatTile`,
  `ThroughputChart`, `UptimeText`; iconos de `material-icons-core` y vectores propios en `res/drawable`
  (incluido el logo «u» convertido a icono adaptable).
- **Pruebas unitarias:** `HomeViewModel` (incluyendo favoritos y renombrar), `ConfigsFilter`, `EncryptedFileConfigStore`
  (favoritos, migración de índice antiguo y renombrar), `SpeedCalculator`, `ConfSummary`, `Formatters`, `MainTab`,
  `PaletteContrastTest`; el registro (16 pruebas).
- **Diagnóstico y ajustes avanzados:** registro en archivo con rotación, claves tapadas automáticamente,
  captura de cierres inesperados, modo detallado (con logcat), visor, borrado y envío del informe por
  correo a usipipo@gmail.com.

## Qué NO incluye (según el diseño)

Conexión automática, acceso a "VPN siempre activa", pegar texto,
split tunneling, ícono definitivo, minificación y todo el **modo administrador**
(fase 4, que requiere su propio diseño previo).

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
corregir lo que salga. La fase 3 está completa; el siguiente paso es la **fase 4** (**modo
administrador**: listar, crear y revocar usuarios del WireGuard desde la app), que necesita su
propio diseño antes de implementarse.

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

Hay dos caminos para crear el keystore de release y sus cuatro secretos. El recomendado
**no instala nada en tu máquina**: el keystore se genera en un runner de GitHub Actions y
todo se hace desde la web. El asistente de siempre, con un script local, queda como
alternativa para quien ya tenga un JDK instalado (al final de esta sección).

> **No hace falta poner el repositorio en privado.** Las contraseñas nunca se teclean en
> el formulario del workflow ni aparecen en los logs: se guardan antes como secretos de
> repositorio de solo escritura. Por eso todo el proceso funciona con el repositorio
> público.

#### Camino recomendado: desde la web, sin instalar nada

**Paso 1. Crea un token temporal (PAT) y guárdalo como secreto `GH_PAT`.**

El `GITHUB_TOKEN` automático de los workflows no puede crear secretos de repositorio, así
que hace falta un token personal que se usa **una sola vez** y se revoca al final.

1. En GitHub, arriba a la derecha, tu foto de perfil → **Settings**.
2. En el menú lateral, baja hasta el final → **Developer settings**.
3. **Personal access tokens** → **Fine-grained tokens** → **Generate new token**.
4. *Repository access*: **Only select repositories** → marca **usipipo-vpn** (solo ese).
5. *Repository permissions*: busca **Secrets** y ponlo en **Read and write**.
6. Pulsa **Generate token** y **copia el token** (empieza por `github_pat_`). Solo se
   muestra una vez; tenlo a mano un momento.

Ahora guárdalo como secreto del repositorio:

1. Entra en el repositorio → **Settings** → **Secrets and variables** → **Actions**.
2. **New repository secret**.
3. *Name*: `GH_PAT`. *Secret*: pega el token que acabas de copiar.
4. **Add secret**.

**Paso 2. Crea el secreto con la contraseña del keystore.**

El generador necesita una sola contraseña —la del almacén— y **no** se pide en el
formulario: la guardas tú antes, como un secreto de repositorio de solo escritura, con el
valor que tú elijas. La clave de dentro hereda esa misma contraseña, así que solo hay una
que recordar y guardar:

1. **Settings** → **Secrets and variables** → **Actions** → **New repository secret**.
2. *Name*: `TMP_STORE_PASSWORD`. *Secret*: la contraseña del keystore (mínimo 6 caracteres,
   que es lo que exige `keytool`). **Add secret**.

Elige una contraseña fuerte y **guárdala en tu gestor de contraseñas**: GitHub no vuelve a
mostrar el valor de un secreto después de guardarlo, así que nadie podrá leerla por la web.
El workflow la usa al ejecutarse y **borra el secreto temporal cuando termina**, para que
no se quede en el repositorio.

**Paso 3. Lanza el workflow «Generar keystore de release».**

1. Pestaña **Actions** → **Generar keystore de release** → **Run workflow**.
2. Rellena el formulario con solo tres campos:

   | Campo | Qué poner |
   |---|---|
   | `key_alias` | `release` (ya viene puesto) |
   | `validity_days` | `10000` (unos 27 años de validez) |
   | `confirm` | `GENERAR`, exactamente así y en mayúsculas |

3. Pulsa **Run workflow**.

El workflow (`.github/workflows/generate-keystore.yml`) lee la contraseña del secreto
temporal, genera el keystore en el runner, lo borra de ahí, crea los cuatro secretos
finales, borra el temporal y verifica al final que todo existe. La contraseña nunca se
imprime en los logs ni viaja por la línea de comandos. Si algo falla, no crea nada y puedes
volver a lanzarlo: espera a que la ejecución se ponga **verde** antes de seguir.

**Paso 4. Comprueba que están los cuatro secretos.**

Vuelve a **Settings** → **Secrets and variables** → **Actions**. Deben aparecer
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y
`ANDROID_KEY_PASSWORD`. Con esos cuatro ya se puede publicar una release firmada
(sección 2).

**Paso 5. Revoca el token y borra el secreto `GH_PAT`.**

Ya no hacen falta. Vuelve a **Settings** → **Developer settings** →
**Personal access tokens** → **Fine-grained tokens**, busca el que acabas de crear y
bórralo. Después, en **Settings** → **Secrets and variables** → **Actions**, borra también
el secreto `GH_PAT`. Si alguna vez hay que regenerar los secretos, repites los pasos 1 y 2
con un token nuevo.

El secreto `TMP_STORE_PASSWORD` ya lo borra el propio workflow al terminar. Si el registro
de la ejecución avisó de que no pudo borrarlo, bórralo a mano en **Settings** →
**Secrets and variables** → **Actions**.

**Guarda una copia de seguridad (no te la saltes).** Copia el contenido del secreto
`ANDROID_KEYSTORE_BASE64` (el keystore convertido a base64) y la contraseña (la que
elegiste en el paso 2) en tu gestor de contraseñas. Si se pierden, **no podrás publicar
actualizaciones que se instalen encima de las anteriores**: Android exige que todas las
versiones de una app vengan firmadas con el mismo certificado. El fichero del keystore
solo existió en el runner, que se borra al terminar: **su única copia es ese secreto**.

#### Alternativa: el asistente local (si tienes JDK)

Si en tu máquina tienes `keytool` (viene con el JDK 17) y la CLI de GitHub `gh` con la
sesión iniciada (`gh auth login`), dentro de la carpeta del repositorio ejecuta el
asistente:

```bash
scripts/setup-release-signing.sh
```

Te pregunta la ruta del keystore (por defecto `~/usipipo-release.jks`), el alias de la
clave (`release`) y las contraseñas; ninguna se muestra por pantalla, se guarda en el
historial del shell ni viaja por la línea de comandos (`keytool` las lee de variables de
entorno con `-storepass:env` y `gh` las recibe por stdin). Después genera el keystore, lo
convierte a base64 y guarda los cuatro secretos del repositorio. Aquí sí te queda una
copia del fichero en tu disco: guárdala también a buen recaudo.

Si algo queda a medias (por ejemplo, falla uno de los `gh secret set`), vuelve a ejecutar
el asistente con `--solo-secretos`: reutiliza el keystore que ya tienes y vuelve a subir
los cuatro secretos sin generar uno nuevo:

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
