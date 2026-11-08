# Spec de diseño — Ajustes: Almacenamiento

**Fecha:** 2026-11-01
**Estado:** aprobado por el usuario (2026-11-01)
**Implementación:** fase 7 · implementada · rama `feat/ajustes-almacenamiento` · PR #27
**Enmiendas:** §2, §3 (RF-1), §4, §5, §7 y §9 — el 2026-11-08, al comprobar que dos de las
cinco filas del resumen medían almacenamiento que no existe (ver la tabla de §2), y el
mismo día para fijar el idioma del formato de los tamaños (ver §7).
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
| Filas del resumen (enmienda 2026-11-08) | Solo tres: perfiles, registro y temporales | Las otras dos medían almacenamiento que no existe. Ver la tabla de abajo |
| Borrado de perfiles (enmienda 2026-11-08) | Uno a uno por `ConfigStore.remove`, no borrando el directorio | Borrar el directorio dejaría el estado en memoria del almacén contando perfiles que ya no están en disco |
| Palabra clave de confirmación | "BORRAR" escrita por el usuario en un campo de texto | Evita borrados accidentales; doble confirmación explícita |
| Caché de geolocalización | Se borra el campo país de cada perfil en `EncryptedFileConfigStore` (no hay caché separada; el país vive en el perfil) | `CountryRepository` no tiene caché propia; la caché son los campos `countryCode`/`countryName`/`countryIp`/`countryFetchedAt` del perfil |
| Archivos temporales de importación | Se borra el directorio de caché de la app (`cacheDir`) | `ConfigImporter` y `ReportSender` escriben en `cacheDir`; es seguro vaciarlo |
| Idioma del formato (enmienda 2026-11-08) | Español fijo, no `Locale.getDefault()` | La app solo está en español (no hay `values-en` ni `localeConfig`): los números tienen que acompañar a las etiquetas. Ver §7 |

### Lo que el spec daba por hecho y no existe (enmienda 2026-11-08)

El spec aprobado describía un resumen de cinco categorías. Dos de ellas medían
almacenamiento inexistente, así que se han quitado. No se ha inventado nada para
cuadrar con el texto:

| Fila del spec | Por qué no puede existir |
|---|---|
| «Resultados de ping (X KB)» | `PingRepository` es stateless: `ping(target)` devuelve el resultado y se pierde. No hay fichero que medir ni nada que borrar. Los resultados viven solo en memoria del `CountryPingViewModel` mientras la pantalla está abierta |
| «Caché de geolocalización (X KB)» | La caché de país son los campos `countryCode`/`countryName`/`countryIp`/`countryFetchedAt` **dentro** de cada perfil cifrado. `CountryRepository` no tiene fichero propio. Ya están contados en «Perfiles cifrados» |

Consecuencia en RF-3: «Limpiar caché» borra el país de los perfiles y vacía `cacheDir`.
No hay resultados de ping que borrar porque no se guardan.

---

## 3. Requisitos funcionales

- **RF-1** — La pantalla muestra el espacio usado por: perfiles cifrados, registro de diagnóstico, archivos temporales de importación, y el total.
- **RF-2** — "Borrar registro" muestra un diálogo de confirmación y, al aceptar, llama a `AppLog.clear()` y refresca el resumen.
- **RF-3** — "Limpiar caché" borra: campos de país de todos los perfiles en `EncryptedFileConfigStore`, y archivos temporales en `cacheDir`.
- **RF-4** — "Borrar todos los datos" requiere escribir la palabra clave "BORRAR" en un campo de texto antes de habilitar el botón de confirmación.
- **RF-5** — Al borrar todos los datos, `AppContainer.wipeAll()` borra el directorio de configs cifrados y todos los `SharedPreferences` de ajustes, dejando la app en estado de recién instalada (directorios vacíos recreados).
- **RF-6** — Tras cualquier acción de borrado, el resumen de espacio se vuelve a calcular y se muestra el estado vacío si no hay datos.

---

## 4. Interfaz

### Qué se ve

- Cabecera con flecha de vuelta y título "Almacenamiento".
- Tarjeta "Espacio usado" con filas: Perfiles cifrados (X KB), Registro de diagnóstico (X KB), Temporales de importación (X KB), Total (X KB).
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
| `app/src/main/java/com/usipipo/vpn/ui/UiLocale.kt` | **Nuevo** (enmienda 2026-11-08): la locale con la que la app da formato a sus textos |
| `app/src/main/java/com/usipipo/vpn/ui/settings/storage/StorageFormat.kt` | **Nuevo**: `readable(bytes)` y `total(...)`. Sin parámetro de locale: el idioma es único |

### Idioma del formato (enmienda 2026-11-08)

El spec no decía nada de locale. La primera versión usaba `Locale.getDefault()`, y la
CI lo cazó dos veces: el runner no está en español, así que «2,5 MB» salía «2.5 MB»
junto a etiquetas españolas.

La guía de Android (*Be wary of the default locale*) recomienda la locale del usuario
para texto que ve una persona. Aquí esa premisa no se cumple: la app no tiene segundo
idioma, así que la locale del usuario no corresponde a ningún texto de la UI. Se fija
`Locale.forLanguageTag("es-ES")` en `UiLocale.kt` y los tests de `StorageFormatTest`
corren con `Locale.setDefault(Locale.US)` para que el pinado se note si alguien lo
quita.

**Coste:** desvío de la recomendación genérica, asumido a propósito. Si algún día se
añade `values-en`, se sustituye por la locale con la que Android resuelve los recursos
y los tests dejan de fijarla. Se aplica también a `formatRate`
(`ui/details/Formatters.kt`), que leía la locale del dispositivo por su cuenta;
`formatUptime` no necesita nada porque su patrón no lleva separador decimal.

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
| `StorageFormat` | Tamaños legibles y total como suma de las tres categorías | JVM (JUnit 4) |
| `StorageFormat` | El separador decimal es español aunque el dispositivo esté en otro idioma | JVM con `Locale.setDefault` en `@Before` |
| `StorageSizes` | Suma los ficheros de cada directorio, con subdirectorios y con directorios ausentes | JVM con carpetas temporales |
| `StorageViewModel` | Cálculo de `totalBytes` como suma de categorías | JVM (JUnit 4) |
| `StorageViewModel` | La palabra "BORRAR" habilita el borrado y nada más lo hace | JVM |
| `StorageViewModel` | `clearLog()` llama al limpiador y refresca el resumen | JVM con el limpiador inyectado |
| `StorageViewModel` | `clearCache()` vacía los campos país y refresca el resumen | JVM |
| `StorageViewModel` | `wipeAll()` con éxito deja el resumen a cero y olvida la palabra clave | JVM |
| `StorageWipe` | Vacía `cacheDir` dejándolo creado y limpia cada fichero de preferencias | JVM con `SharedPreferences` en memoria |
| `StorageWipe` | Si las preferencias fallan, el borrado entero falla y no toca la caché | JVM |
| Error | Si el borrado falla, se avisa y no se marca como completado | JVM (el mensaje sale del ViewModel) |

`AppContainer.wipeAll()` no se prueba en la JVM: construir un `Context` haría falta
Robolectric, que no está en el proyecto. La parte que borra de verdad (`StorageWipe`)
se prueba con preferencias en memoria, y el orquestador queda como pegamento fino
sobre Android.

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
