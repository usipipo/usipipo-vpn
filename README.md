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
split tunneling, ícono definitivo, firma de release, minificación y todo el **modo administrador**
(fase 2: servicio en el VPS dentro del túnel + clave Ed25519 del Keystore).

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

1. Sube el proyecto a un repositorio de GitHub.
2. Abre la pestaña **Actions** y entra en la ejecución más reciente de **Android CI - Build APK**.
3. Cuando termine correctamente, baja hasta **Artifacts** y descarga `usipipo-vpn-debug-apk`.
4. Descomprime el artefacto y encontrarás la APK para instalar en un dispositivo Android.

La APK es de **depuración** y no está firmada para distribución pública. La firma de una versión
`release` debe configurarse aparte mediante secretos de GitHub y un keystore privado; no guardes
el keystore ni sus contraseñas en el repositorio.

> Nota: el proyecto declara `compileSdk = 37`. El workflow instala la plataforma Android 37;
> si esa plataforma aún no estuviera disponible en el SDK de GitHub Actions, habrá que ajustar
> `compileSdk` a una plataforma disponible o actualizar el entorno de compilación.
