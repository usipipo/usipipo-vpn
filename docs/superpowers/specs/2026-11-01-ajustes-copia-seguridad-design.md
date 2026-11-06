# Spec de diseño — Ajustes: copia de seguridad

**Fecha:** 2026-11-01
**Estado:** aprobado por el usuario (2026-11-01)
**Implementación:** fase 6 · completada · rama `feat/ajustes-copia-seguridad` · PR #21 · merge `2a3deb5`
**Proyecto:** uSipipo VPN
**Alcance:** Sección «Copia de seguridad» del hub de Ajustes (`ui/settings/backup/`): exportar todo (todos los perfiles cifrados con contraseña + ajustes) en un .zip, exportar un solo perfil como .conf en claro con aviso de claves privadas, importar con detección de duplicados, y el nuevo paquete criptográfico `backup/` de nivel top. Exportación e importación vía Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`), sin permisos de escritura externa.

---

## 1. Objetivo y tabla antes / después

| Comportamiento | Antes | Después |
|---|---|---|
| Copia de seguridad | No existe | Exportar todo en .zip cifrado con contraseña + `settings.json` |
| Exportar un perfil | No existe | Exportar un .conf en claro con aviso explícito de claves privadas |
| Importar | Importar un `.conf` individual (QR o archivo) | Importar .zip de copia o .conf suelto, con detección de duplicados |
| Dónde se elige la ubicación | — | El usuario elige la ubicación vía SAF; la app no pide permisos de escritura externa |
| Cripto de copias | — | Nuevo paquete `backup/` con `BackupArchive`, `BackupCrypto`, `BackupWriter`, `BackupReader` (AES-GCM + PBKDF2-HMAC-SHA256) |
| Ajustes en la copia | No | `settings.json` con tema, paleta, seguridad, apps excluidas, prefs del widget |
| Copia automática / nube | No existe | Fuera de alcance |

---

## 2. Decisiones de la sección

| Tema | Decisión | Por qué |
|---|---|---|
| Formato | `.zip` con cada `.conf` cifrado + `settings.json` | Un solo archivo portable, todo o nada |
| Cripto de perfiles | AES-GCM + PBKDF2-HMAC-SHA256, sal y IV aleatorios por archivo | Cifrado autenticado; derivación de clave desde contraseña del usuario; portátil entre dispositivos |
| Portabilidad | Descifrar con contraseña y re-cifrar con Keystore del dispositivo destino | El destino nunca necesita la contraseña original para seguir usando los perfiles |
| Exportar un solo perfil | `.conf` en claro con aviso explícito de que contiene claves privadas | Caso de uso de compartir un perfil con confianza; advertencia obligatoria |
| Ajustes en la copia | `settings.json` con todos los valores de `AppSettings` | Restaurar preferencias junto con los perfiles |
| Exportación / importación | SAF `ACTION_CREATE_DOCUMENT` y `ACTION_OPEN_DOCUMENT` | No requiere `WRITE_EXTERNAL_STORAGE`; el usuario elige la ubicación |
| Detección de duplicados | Comparar perfiles existentes por clave pública / endpoint / nombre | Evita perfiles repetidos al importar |
| UI de duplicados | Lista de duplicados con acción por ítem: sobrescribir / mantener ambos / omitir | El usuario decide caso por caso |
| Programación / nube | Fuera de alcance | Decisión del usuario; se deja para una fase posterior |

---

## 3. Requisitos funcionales

- **RF-1** — La sección «Copia de seguridad» del hub de Ajustes tiene «Exportar todo»: genera un .zip que contiene todos los perfiles cifrados con la contraseña del usuario más `settings.json` con los ajustes de la app.
- **RF-2** — Cada `.conf` dentro del zip va cifrado individualmente con AES-GCM + PBKDF2-HMAC-SHA256, con sal e IV aleatorios por archivo.
- **RF-3** — La copia es portable: se descifra con la contraseña y, en el dispositivo destino, se re-cifra con el Android Keystore del dispositivo.
- **RF-4** — «Exportar perfil»: exporta un perfil individual como `.conf` en claro, mostrando un aviso explícito de que el archivo contiene claves privadas antes de escribirlo.
- **RF-5** — Los ajustes de la app se incluyen en la copia (`settings.json`): tema, paleta, seguridad, apps excluidas, prefs del widget.
- **RF-6** — «Importar»: abre .zip de copia o `.conf` suelto vía SAF; para .zip pide la contraseña.
- **RF-7** — Al importar, detectar duplicados comparando perfiles existentes por clave pública, endpoint y nombre; mostrar lista de duplicados y, para cada uno, preguntar al usuario: sobrescribir / mantener ambos / omitir.
- **RF-8** — Exportación e importación vía SAF (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`, tipo `application/zip` o `*/*`); no requiere permisos de escritura externa.
- **RF-9** — Nuevo paquete de nivel top `com.usipipo.vpn.backup` con `BackupArchive`, `BackupCrypto`, `BackupWriter`, `BackupReader`.
- **RF-10** — Tras exportar o importar con éxito, se escribe `backup_last_at` en `AppSettings` y se muestra en la pantalla.
- **RF-11** — Contraseña perdida = datos irrecuperables: la pantalla lo advierte antes de exportar (no hay mecanismo de recuperación).

---

## 4. Interfaz

**Pantalla de Ajustes «Copia de seguridad» (`ui/settings/backup/BackupScreen.kt`):**
- Cabecera con flecha de vuelta y título «Copia de seguridad».
- Fila «Última copia»: resumen con fecha/hora de `backup_last_at` o «Sin copias».
- Botón «Exportar todo»: abre SAF `ACTION_CREATE_DOCUMENT` (tipo `application/zip`); pide contraseña (dos campos: contraseña, confirmar); advierte que sin contraseña no hay recuperación; escribe el .zip en la ubicación elegida.
- Botón «Exportar perfil»: lista de perfiles; al elegir uno, aviso explícito «Este archivo contiene claves privadas del túnel. No lo compartas con nadie de confianza»; escribe el `.conf` en claro en ubicación SAF elegida.
- Botón «Importar»: abre SAF `ACTION_OPEN_DOCUMENT`; si es .zip pide contraseña; tras descifrar, muestra lista de duplicados (si los hay) con acción por ítem (sobrescribir / mantener ambos / omitir); si es `.conf` suelto, lo importa directamente (con comprobación de nombre duplicado).
- Estados de resultado: «Copia exportada», «Copia importada», «Error: contraseña incorrecta», «Error: archivo corrupto».

**Estados:**
- Vacío: no hay perfiles → «Exportar perfil» deshabilitado; «Exportar todo» exporta solo `settings.json` (o avisa que no hay perfiles).
- Error: contraseña incorrecta, archivo corrupto, zip con entradas no válidas (zip slip), sin espacio → mensaje claro, sin escribir nada parcial.
- Permiso denegado / cancelado: SAF no requiere permiso de la app; si el usuario cancela el selector, no pasa nada y la pantalla queda igual.

---

## 5. Datos y ajustes

**Clave en `AppSettings` (`settings/AppSettings.kt`):**

| Clave | Tipo | Valor por defecto | Quién escribe |
|---|---|---|---|
| `backup_last_at` | Long (ms) | `0` | `BackupViewModel` tras export/import exitoso |

**Modelos de dominio (nuevos, paquete `backup/`):**
- `BackupEntry(id: String, name: String, encrypted: Boolean, size: Long)` — entrada del zip.
- `DuplicateConflict(existing: SavedConfig, incoming: SavedConfig)` — par existente/nuevo para la UI de duplicados.
- `BackupAction` enum: `OVERWRITE`, `KEEP_BOTH`, `SKIP`.

**Contenido del zip:**
- `settings.json` — JSON con todos los valores de `AppSettings` (tema, paleta, seguridad, apps excluidas, prefs del widget), cifrado con la contraseña del usuario.
- `<id>.conf.enc` por cada perfil — `.conf` cifrado individualmente (AES-GCM), cada uno con su propia sal e IV aleatorias.

**Quién escribe qué:**
- `BackupWriter` — crea el zip: cifra cada `.conf` y `settings.json`, escribe entradas.
- `BackupReader` — lee el zip: valida contraseña, descifra entradas, devuelve perfiles y ajustes.
- `BackupCrypto` — derivación PBKDF2-HMAC-SHA256 de la contraseña + sal; cifrado/descifrado AES-GCM con IV aleatorio por archivo.
- `BackupViewModel` — coordina `ConfigStore` (leer `readText(id)` para exportar; `add(name, configText)`/`remove`/`rename` para importar), escribe `backup_last_at` en `AppSettings`, gestiona la UI de duplicados.

---

## 6. Permisos

| Permiso | Tipo | Cuándo se pide | Qué pasa si se deniega |
|---|---|---|---|
| Ninguno especial | — | — | SAF no requiere permisos de escritura externa; el usuario elige la ubicación en el selector del sistema. Si cancela, no hay efecto. |

Nota: `EncryptedFileConfigStore` lee/escribe en `context.filesDir` (área privada de la app), accesible sin permisos adicionales.

---

## 7. Integración con el código existente

**Ficheros tocados:**
- `ui/settings/backup/BackupScreen.kt` (nuevo) — pantalla de la sección en Ajustes.
- `ui/settings/backup/BackupViewModel.kt` (nuevo) — lógica de exportación/importación, duplicados, `backup_last_at`.
- `com.usipipo.vpn.backup.BackupArchive.kt` (nuevo, paquete `com.usipipo.vpn.backup` de nivel top junto a `update/` y `tunnel/`) — modelo de entrada del zip y estructura del archivo.
- `com.usipipo.vpn.backup.BackupCrypto.kt` (nuevo) — PBKDF2-HMAC-SHA256 + AES-GCM, sal e IV aleatorios por archivo.
- `com.usipipo.vpn.backup.BackupWriter.kt` (nuevo) — escritura del zip cifrado.
- `com.usipipo.vpn.backup.BackupReader.kt` (nuevo) — lectura y descifrado del zip, validación de contraseña.
- `data/EncryptedFileConfigStore.kt` (modificado, opcional) — si se necesita un método para listar todos los textos de perfil (`readText(id)` ya existe por id; `configs` expide la lista). No se toca la lógica de cifrado de perfiles (`KeystoreCrypto`).
- `settings/AppSettings.kt` (nuevo, introducido por Apariencia) — clave `backup_last_at`.
- `ui/UsipipoRoot.kt` (modificado) — añade `SettingsRoute.Backup` y pinta `BackupScreen` como subpantalla de Ajustes.
- `AndroidManifest.xml` — sin cambios (SAF no requiere permisos declarados).

**APIs del SDK:**
- `android.content.Intent.ACTION_CREATE_DOCUMENT` con `CATEGORY_OPENABLE` y tipo `application/zip`.
- `android.content.Intent.ACTION_OPEN_DOCUMENT` con `CATEGORY_OPENABLE`.
- `android.content.ContentResolver.openFileDescriptor(uri, "w")` / `"r"` → `FileOutputStream` / `FileInputStream`.
- `java.util.zip.ZipOutputStream` / `ZipInputStream` / `ZipEntry` (validar nombres de entrada: sin `..` ni rutas absolutas para evitar zip slip).
- `javax.crypto.SecretKeyFactory` con `PBKDF2WithHmacSHA256`, `PBEKeySpec`, `SecretKeySpec` ("AES").
- `javax.crypto.Cipher` con `AES/GCM/NoPadding`, `GCMParameterSpec(128, iv)`.
- `java.security.SecureRandom` para sal (16 bytes) e IV (12 bytes).
- `com.usipipo.vpn.data.ConfigStore.configs` y `readText(id)` (exportar), `add(name, configText)` (importar).
- `com.usipipo.vpn.domain.SavedConfig`.
- `settings.AppSettings`.

---

## 8. Criterios de aceptación

1. La sección «Copia de seguridad» muestra «Exportar todo», «Exportar perfil», «Importar» y la fecha de última copia (`backup_last_at`).
2. Exportar todo genera un .zip con todos los perfiles cifrados (contraseña del usuario) + `settings.json`.
3. Cada `.conf` del zip está cifrado con AES-GCM + PBKDF2-HMAC-SHA256, sal e IV aleatorios por archivo.
4. Exportar un perfil escribe un `.conf` en claro tras mostrar el aviso explícito de claves privadas.
5. Importar un .zip pide contraseña; sin contraseña correcta no se importa nada.
6. Al importar se detectan duplicados por clave pública / endpoint / nombre y se muestra la lista con acciones (sobrescribir / mantener ambos / omitir).
7. Exportación e importación usan SAF (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`); no se requiere `WRITE_EXTERNAL_STORAGE`.
8. Tras exportar/importar con éxito, `backup_last_at` se actualiza y se muestra en pantalla.
9. La pantalla advierte que la contraseña perdida implica datos irrecuperables.
10. `./gradlew testDebugUnitTest` y `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

| Ámbito | Prueba | Tipo |
|---|---|---|
| `BackupCrypto` | Roundtrip cifrar/descifrar con contraseña; contraseña incorrecta falla; sal e IV distintos por llamada | JVM (JUnit 4) |
| `BackupWriter`/`BackupReader` | Estructura del zip (`settings.json` + `<id>.conf.enc`), descifrado correcto, rechazo de contraseña errónea | JVM |
| Detección de duplicados | Comparación por clave pública/endpoint/nombre; acciones sobrescribir/mantener ambos/omitir | JVM |
| `AppSettings` | `backup_last_at` valor por defecto `0`; escritura → lectura | JVM |
| Exportar todo vía SAF | .zip generado en ubicación elegida por el usuario; contiene perfiles y settings.json | Dispositivo |
| Importar .zip con contraseña | Perfiles restaurados en `ConfigStore`; duplicados resueltos según acción del usuario | Dispositivo |
| Importar contraseña incorrecta | No se importa nada; mensaje de error | Dispositivo |
| Exportar perfil en claro | `.conf` escrito con aviso de claves privadas mostrado | Dispositivo |

Los tests JVM son los únicos ejecutables localmente (`AGENTS.md`: JUnit 4 en JVM). SAF, contraseña y escritura de ficheros se validan en dispositivo / CI. Previews de `BackupScreen` en tema claro y oscuro.

---

## 10. Riesgos y fuera de alcance

**Riesgos:**
- Contraseña perdida = cifrado irrecuperable (no hay copia de seguridad de la contraseña ni recuperación); se advierte explícitamente en la UI antes de exportar.
- PBKDF2 con muchas iteraciones puede ser lento en dispositivos bajos; mitigación: iteraciones razonables (p. ej. 100 000) y operación en segundo plano.
- Zip slip (atravesamiento de rutas al extraer entradas); mitigación: validar nombres de entrada del zip (`ZipEntry.getName()` sin `..` ni rutas absolutas).
- Espacio insuficiente en la ubicación elegida; manejo de error de escritura sin dejar archivos parciales.

**Fuera de alcance:**
- Copia de seguridad automática programada.
- Sincronización en la nube.
- Exportar registro (`AppLog`) ni diagnósticos.
- Cifrar con Keystore del dispositivo de origen (la copia es portátil con contraseña; el re-cifrado con Keystore del destino ocurre al importar en el destino).

---

## 11. Dependencias de otras secciones

- **Ajustes base** (`2026-11-01-ajustes-navegacion-design.md`): hub de Ajustes, `AppSettings` (clave `backup_last_at`), `SettingsRoute`, `UsipipoRoot`.
- **`data/`**: `ConfigStore` y `EncryptedFileConfigStore` para leer/escribir perfiles (ya existen; solo se usan sus APIs públicas).
- **Seguridad** (`2026-11-01-ajustes-seguridad-design.md`): `settings.json` incluye «apps excluidas» si esa sección existe; no es bloqueante (se serializan los valores que existan en `AppSettings`).
- No depende de Notificaciones, Burbuja de tráfico ni Copia automática (fuera de alcance).
