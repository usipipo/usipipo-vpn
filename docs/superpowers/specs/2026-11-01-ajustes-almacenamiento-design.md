# Spec de diseño — Ajustes: Almacenamiento

**Fecha:** 2026-11-01
**Estado:** aprobado por el usuario (2026-11-01)
**Implementación:** fase 7 · no iniciada · rama `feat/ajustes-almacenamiento` · -
**Proyecto:** uSipipo VPN
**Alcance:** Pantalla de Almacenamiento dentro del hub de Ajustes: resumen del espacio usado, borrado de registro, limpieza de caché y borrado completo de todos los datos de la app.

---

## 1. Objetivo

Que el usuario pueda ver cuánto espacio ocupa la app y liberarlo de forma controlada: borrar el registro de diagnóstico, limpiar la caché (geolocalización, ping, archivos temporales de importación) y borrar todos los datos con doble confirmación.

| Comportamiento | Antes | Después |
|---|---|---|
| Espacio usado | No visible | Resumen en pantalla: perfiles cifrados, registro, caché (geo, ping), temporales de importación, total |
| Borrar registro | `DiagnosticsViewModel.clearLog()` en DiagnosticsScreen | Fila en Almacenamiento con confirmación; acción movida de `diagnostics/` a `settings/storage/` |
| Limpiar caché | No existe | Fila que borra caché de geolocalización (30 días), resultados de ping y temporales de importación |
| Borrar todo | No existe | `AppContainer.wipeAll()` con doble confirmación (palabra clave "BORRAR"), deja la app como recién instalada |

---

## 2. Decisiones

| Tema | Decisión | Por qué |
|---|---|---|
| Ubicación del código | `ui/settings/storage/StorageScreen.kt` + `StorageViewModel.kt` | Co-locación según `AGENTS.md`; cada sección tiene su paquete, ViewModel y componentes privados |
| Borrado de registro | `AppLog.clear()` invocado desde `StorageViewModel` (Almacenamiento) y también desde `DiagnosticsViewModel` (Diagnósticos) | El registro es el mismo desde ambos puntos de entrada; el diagnóstico se conserva intacto dentro de "Más → Reportar un problema" |
| Borrado total | `AppContainer.wipeAll()` borra `File(appContext.filesDir, "configs")` y los `SharedPreferences` de ajustes | `AppContainer` ya conoce todas las rutas (configs cifrados, prefs de actualizaciones, prefs de diagnóstico); es el único sitio con visibilidad global |
| Palabra clave de confirmación | "BORRAR" escrita por el usuario en un campo de texto | Evita borrados accidentales; doble confirmación explícita |
| Caché de geolocalización | Se borra el campo país de cada perfil en `EncryptedFileConfigStore` (no hay caché separada; el país vive en el perfil) | `CountryRepository` no tiene caché propia; la caché son los campos `countryCode`/`countryName`/`countryIp`/`countryFetchedAt` del perfil |
| Archivos temporales de importación | Se borra el directorio de caché de la app (`cacheDir`) | `ConfigImporter` y `ReportSender` escriben en `cacheDir`; es seguro vaciarlo |

---

## 3. Requisitos funcionales

- **RF-1** — La pantalla muestra el espacio usado por: perfiles cifrados, registro de diagnóstico, caché (geolocalización, ping), archivos temporales de importación, y el total.
- **RF-2** — "Borrar registro" muestra un diálogo de confirmación y, al aceptar, llama a `AppLog.clear()` y refresca el resumen.
- **RF-3** — "Limpiar caché" borra: campos de país de todos los perfiles en `EncryptedFileConfigStore`, resultados de ping en `PingRepository`, y archivos temporales en `cacheDir`.
- **RF-4** — "Borrar todos los datos" requiere escribir la palabra clave "BORRAR" en un campo de texto antes de habilitar el botón de confirmación.
- **RF-5** — Al borrar todos los datos, `AppContainer.wipeAll()` borra el directorio de configs cifrados y todos los `SharedPreferences` de ajustes, dejando la app en estado de recién instalada (directorios vacíos recreados).
- **RF-6** — Tras cualquier acción de borrado, el resumen de espacio se vuelve a calcular y se muestra el estado vacío si no hay datos.

---

## 4. Interfaz

### Qué se ve

- Cabecera con flecha de vuelta y título "Almacenamiento".
- Tarjeta "Espacio usado" con filas: Perfiles cifrados (X KB), Registro de diagnóstico (X KB), Caché de geolocalización (X KB), Resultados de ping (X KB), Temporales de importación (X KB), Total (X KB).
- Fila "Borrar registro" con icono de papelera y confirmación.
- Fila "Limpiar caché" con icono de escoba y confirmación simple.
- Fila "Borrar todos los datos" con icono de advertencia, campo de texto para la palabra clave y botón deshabilitado hasta escribir "BORRAR".

### Acciones

- Tocar "Borrar registro" → `AlertDialog` de confirmación → `AppLog.clear()` → refrescar resumen.
- Tocar "Limpiar caché" → `AlertDialog` de confirmación → limpiar caché geo/ping/temporales → refrescar resumen.
- Tocar "Borrar todos los datos" → mostrar campo de texto → escribir "BORRAR" → habilitar botón → `AlertDialog` final → `AppContainer.wipeAll()`.

### Estados

- **Vacío:** si no hay perfiles ni registro, mostrar "0 B" en cada categoría y total 0 B.
- **Error:** si `wipeAll` falla al borrar un directorio, mostrar snackbar con el error y no marcar como completado.
- **Permiso denegado:** no se requieren permisos de almacenamiento (la app usa `filesDir` y `cacheDir`, ambos de acceso privado); si el sistema deniega el acceso, mostrar snackbar de error.

---

## 5. Datos y ajustes

### Claves en AppSettings

No se añaden claves nuevas a `AppSettings`. Todo lo que se borra vive fuera de `SharedPreferences` (configs en disco, registro en archivo, caché en perfiles y en `cacheDir`). Si se necesita rastrear cuándo se hizo el último borrado, se usaría `storage_last_wipe_at` (pendiente de decidir; no es requisito RF).

### Modelo

`StorageUiState` (en `StorageViewModel`):
- `profilesBytes: Long`
- `logBytes: Long`
- `geoCacheBytes: Long`
- `pingBytes: Long`
- `tempBytes: Long`
- `totalBytes: Long` (suma)
- `wipeInProgress: Boolean`

### Quién escribe

- `StorageViewModel` lee tamaños de `AppLog.sizeBytes()`, `EncryptedFileConfigStore` (tamaño del directorio `configs`), `PingRepository` (tamaño de su caché interna si la tiene), y `cacheDir.listFiles()`.
- `AppContainer.wipeAll()` escribe: borra `File(appContext.filesDir, "configs")`, llama a `appContext.getSharedPreferences("usipipo_settings", MODE_PRIVATE).edit().clear().apply()`, y borra los prefs de diagnóstico (`diagnostics`) y de actualizaciones (`updates`).

---

## 6. Permisos

| Permiso | Tipo | Cuándo se pide | Si se deniega |
|---|---|---|---|
| Ninguno (uso de `filesDir` y `cacheDir` privados) | — | N/A | N/A |

No se requieren permisos de runtime. La app solo accede a sus propios directorios privados.

---

## 7. Integración con el código existente

### Ficheros tocados

| Fichero | Cambio |
|---|---|
| `app/src/main/java/com/usipipo/vpn/ui/settings/storage/StorageScreen.kt` | **Nuevo**: pantalla de Almacenamiento |
| `app/src/main/java/com/usipipo/vpn/ui/settings/storage/StorageViewModel.kt` | **Nuevo**: ViewModel con cálculo de tamaños y acciones de borrado |
| `app/src/main/java/com/usipipo/vpn/AppContainer.kt` | Añadir `wipeAll()` |
| `app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsViewModel.kt` | **Sin cambios**: `clearLog()` sigue aquí; Almacenamiento llama a `AppLog.clear()` directamente |
| `app/src/main/java/com/usipipo/vpn/ui/diagnostics/DiagnosticsScreen.kt` | **Sin cambios**: se reutiliza intacta dentro de "Más → Reportar un problema" |
| `app/src/main/java/com/usipipo/vpn/ui/UsipipoRoot.kt` | Añadir ruta `SettingsRoute.Storage` y pantalla `StorageScreen` |
| `app/src/main/java/com/usipipo/vpn/ui/settings/SettingsRoute.kt` | Añadir `Storage` al enum |

### APIs del SDK

- `android.content.Context.getSharedPreferences(name, mode)` — para borrar prefs
- `android.content.SharedPreferences.Editor.clear().apply()` — para limpiar prefs
- `java.io.File.deleteRecursively()` — para borrar directorio de configs
- `android.content.Context.getCacheDir()` — para borrar temporales
- `android.content.Context.getFilesDir()` — para localizar el directorio de configs
- `com.usipipo.vpn.diagnostics.AppLog.clear()` — existente, para borrar el registro
- `com.usipipo.vpn.data.EncryptedFileConfigStore` — para borrar y recrear el directorio de configs

---

## 8. Criterios de aceptación

1. La pantalla de Almacenamiento muestra el desglose de espacio por categoría y el total.
2. Tocar "Borrar registro" pide confirmación y, al aceptar, el tamaño del registro pasa a 0 B.
3. Tocar "Limpiar caché" pide confirmación y, al aceptar, los campos de país de los perfiles se vacían y el tamaño de caché baja.
4. "Borrar todos los datos" solo se habilita tras escribir exactamente "BORRAR" en el campo de texto.
5. Tras "Borrar todos los datos", la app queda sin perfiles, sin registro y sin caché; los directorios se recrean vacíos.
6. Tras cualquier borrado, el resumen se actualiza sin reiniciar la app.
7. `./gradlew testDebugUnitTest` y `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

| Ámbito | Prueba | Tipo |
|---|---|---|
| `StorageViewModel` | Cálculo de `totalBytes` como suma de categorías | JVM (JUnit 4) |
| `StorageViewModel` | `clearLog()` llama a `AppLog.clear()` y refresca el estado | JVM con `AppLog` mockeado |
| `StorageViewModel` | `clearCache()` vacía campos país de perfiles y limpia `cacheDir` | JVM |
| `AppContainer.wipeAll()` | Borra directorio de configs y prefs de ajustes | JVM con `Context` de test |
| Interfaz | El botón "Borrar todos los datos" está deshabilitado hasta escribir "BORRAR" | JVM (ComposeTest) o dispositivo |
| Error | Si `wipeAll` falla, se muestra snackbar y no se marca como completado | Dispositivo (simulado) |

Los tests JVM son los únicos ejecutables localmente. El comportamiento visual se valida en `@Preview` (tema claro y oscuro) y en la CI.

---

## 10. Riesgos y fuera de alcance

### Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Borrar todo elimina prefs que otras secciones esperan | Alto | `wipeAll` borra solo prefs de ajustes, diagnóstico y actualizaciones; cada sección lee con valor por defecto |
| `deleteRecursively` falla si un archivo está bloqueado | Medio | `runCatching` alrededor de cada borrado; snackbar de error |
| El usuario borra todo sin querer | Alto | Palabra clave "BORRAR" + dos diálogos de confirmación |

### Fuera de alcance

- Borrar selectivamente solo perfiles o solo ajustes (todo o nada).
- Sincronización en la nube o restauración de respaldo.
- Permisos de almacenamiento externo (`READ_EXTERNAL_STORAGE`, etc.).

---

## 11. Dependencias de otras secciones

- **Base (navegación):** requiere `SettingsRoute` y el patrón de subpantalla con flecha de vuelta (`ui/UsipipoRoot.kt`).
- **Diagnóstico:** `clearLog()` sigue en `DiagnosticsViewModel`; Almacenamiento llama a `AppLog.clear()` directamente, así que `DiagnosticsScreen` se reutiliza intacta dentro de "Más → Reportar un problema". El borrado de registro tiene dos puntos de entrada sobre el mismo registro.
- **AppSettings:** no depende de nuevas claves, pero `wipeAll` borra todas las prefs incluidas las de otras secciones (tema, notificaciones, actualizaciones, etc.).
