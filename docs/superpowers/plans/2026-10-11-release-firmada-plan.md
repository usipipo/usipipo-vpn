# Plan — Subsistema «release firmada» (agente 1 de 2)

- Fecha: 2026-10-11
- Rama: `feat/release-firmada`
- Especificación: `docs/superpowers/specs/2026-10-11-release-firmado-y-actualizacion-design.md`
  (autoridad: §2.1, §3.1 requisitos R1–R6 y §8 de migración).
- Alcance: **solo** la release firmada. R7 (pasar el repositorio a público) lo hace el
  controlador, no este plan. La parte de «actualización desde la app» (R8–R15) es de otro
  agente: no se toca `app/src/**` ni `ui/**`.

## Contexto

- Sin build local: todo se verifica por GitHub Actions (workflow `release.yml` nuevo +
  `android-ci.yml` existente por disparo manual).
- El repositorio hoy es privado y aún **no** tiene los secretos de firma, así que el camino
  «con etiqueta y secretos» no se puede ejecutar end-to-end; se comprueba el camino sin
  etiqueta (release sin firmar + artefacto) y la lógica de versión/firma por unitarios locales.
- Prohibido tocar: `app/src/**`, `strings.xml`, `ui/**`, `.github/workflows/android-ci.yml`,
  `gradle/**`, `settings.gradle.kts`, `docs/superpowers/specs/**`, `.gitignore`.

## Tareas

1. **`docs/superpowers/plans/2026-10-11-release-firmada-plan.md`** (este documento):
   plan escrito antes de implementar y quedado como registro.

2. **`.github/workflows/release.yml`** (nuevo) — R1, R2, R5:
   - Disparo: `push` de etiquetas `v*` y `workflow_dispatch`.
   - Pasos: checkout → JDK 17 (temurin) → `android-actions/setup-android@v4` →
     `sdkmanager "platforms;android-37.0" "build-tools;36.0.0"` → build → publicar.
   - Sin etiqueta (disparo manual): `assembleRelease` **sin** firma; solo se sube artefacto
     (`actions/upload-artifact@v4`); no se crea release.
   - Con etiqueta `vX.Y.Z`: paso de shell que calcula `VERSION_NAME` (quitando la `v`) y
     `VERSION_CODE` (`major*1_000_000 + minor*1_000 + patch`, con `10#` y rechazo de
     componentes >= 1000 — ver «Enmienda» al final); si faltan los 4 secretos
     (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
     `ANDROID_KEY_PASSWORD`) el job **falla** con `::error::` claro; si están, decodifica el
     keystore a `$RUNNER_TEMP/release.jks`, compila con las variables de entorno de firma y
     publica con `gh` (`release view` → `upload --clobber` si ya existe, `create` si no).
   - La etiqueta debe validar el formato `vX.Y.Z`; si no, error claro.

3. **`app/build.gradle.kts`** (modificar, solo lo imprescindible) — R3, R4:
   - `signingConfigs { release { ... } }` leyendo de entorno (`ANDROID_KEYSTORE_FILE`,
     `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`); la config
     solo se crea si las cuatro variables existen; si no, release sin firmar y el build no falla.
   - `versionCode`/`versionName` desde `-PVERSION_CODE` / `-PVERSION_NAME` si se pasan;
     si no, valores actuales (`1` / `"0.1.0"`).
   - Nada más cambia; el build debug queda igual.

4. **`scripts/setup-release-signing.sh`** (nuevo, wizard interactivo) — R6:
   - Comprueba `keytool` y `gh` (y sesión de `gh`); si faltan, explica qué instalar y sale.
   - Pide ruta del keystore (defecto `~/usipipo-release.jks`) y alias (defecto `release`);
     contraseñas con `read -rs` dos veces verificando que coincen; nunca hace eco.
   - Genera el keystore con `keytool -genkeypair` (RSA 2048, validez 10000 días,
     `CN=uSipipo VPN, O=usipipo, C=ES`). Si el keystore ya existe, avisa y sale (re-ejecutable).
   - Base64 (`base64 -w0` en Linux; `base64 | tr -d '\n'` donde no exista `-w0`) a fichero
     temporal borrado con `trap`, y sube los 4 secretos con `gh secret set` **por stdin**
     (nada en el historial del shell ni en argv).
   - Termina indicando `git tag v0.2.0 && git push origin v0.2.0` y el aviso de copia de
     seguridad del keystore.

5. **`README.md`** (modificar, solo la zona de release):
   - Sección «Release firmada»: cómo se ejecuta el asistente, cómo se crea/sube una etiqueta
     `vX.Y.Z`, que la APK firmada aparece en Releases, el disparo manual (release sin firmar
     solo como artefacto) y la advertencia de que la primera APK firmada **no** se instala
     sobre una de depuración (hay que desinstalarla una vez, por el certificado). Tono y
     español actuales; no se tocan otras secciones.

## Verificación

1. `bash -n scripts/setup-release-signing.sh` + ejecución controlada con `keytool`/`gh`
   falsos en el PATH (sin keystore real, sin secretos reales) y comprobación de que las
   contraseñas no aparecen en la salida.
2. Lógica de versión ejecutada en bash con `v0.2.3`, `v1.0.0`, `v10.20.30`.
3. Commit (conventional commits, en español), `git push -u origin feat/release-firmada`.
4. `gh workflow run release.yml --ref feat/release-firmada` → a la espera de `success`
   (camino sin secretos: release sin firmar + artefacto, sin release creada).
5. `gh workflow run android-ci.yml --ref feat/release-firmada` → a la espera de `success`
   (el CI debug sigue verde: los cambios en `app/build.gradle.kts` no rompen nada).
6. Informe completo en `/home/mowgli/usipipo-vpn/.superpowers/sdd/release-report.md`.

## Riesgos y límites conocidos

- El camino «etiqueta + secretos» no se puede probar sin secretos reales: se prueba por
  inspección del workflow y por el camino sin etiqueta (mismo build de release).
- Nombre de la APK release sin firmar: AGP la llama `app-release-unsigned.apk`; por eso el
  artefacto se sube con glob `app/build/outputs/apk/release/*.apk` y solo la release
  publicada usa la ruta exacta `app-release.apk` (firmada).
- `versionCode` grande sigue holgado: el máximo de Android es 2.100.000.000 y, con la
  fórmula `major*1_000_000 + minor*1_000 + patch` y el guardián de componentes < 1000, el
  valor más alto posible es 999.999.999 (`v999.999.999`).

## Enmienda (ronda de revisión 1, 2026-10-12)

La revisión del subsistema corrigió, con el visto bueno del controlador (ruling R1), la
fórmula de `versionCode` documentada aquí originalmente (`major*10000 + minor*100 + patch`,
`v10.20.30` → 104.030): esa fórmula producía colisiones (`v0.0.100` == `v0.1.0`) y la
aritmética bash sin `10#` rompía con ceros iniciales (`v1.08.3`). Queda sustituida por
`major*1_000_000 + minor*1_000 + patch` con `10#` y rechazo de componentes >= 1000
(`v0.2.3` → 2003, `v10.20.30` → 10.020.030). El spec R4 se enmendó en el mismo commit.
La revisión añadió además: puerta de pruebas unitarias antes de publicar, verificación de
la firma con `apksigner`, publicación idempotente (`gh release view` → `upload --clobber`),
recuperación del asistente con `--solo-secretos` y `keytool` leyendo contraseñas por
variables de entorno (`-storepass:env`).
