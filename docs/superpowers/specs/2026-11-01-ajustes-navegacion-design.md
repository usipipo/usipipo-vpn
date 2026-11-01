# Spec de diseño — Base: navegación por iconos y pantalla de Ajustes

**Fecha:** 2026-11-01
**Estado:** aprobado por el usuario (2026-11-01)
**Implementación:** fase 1 (base) · rama `feat/ajustes-base` · PR #13
**Proyecto:** uSipipo VPN
**Alcance:** Rediseño de la barra inferior (sin etiquetas de texto), renombrado y re-iconografía de las pestañas, y creación del subsistema de **Ajustes** como hub con subpantallas. Este documento es el **contrato común** de los nueve diseños de sección que lo accompanyan.

---

## 1. Objetivo

Que la app tenga una navegación coherente con el contenido real de cada pestaña y un lugar único donde vivan los ajustes de la aplicación:

| Comportamiento | Antes | Después |
|---|---|---|
| Barra inferior | 4 pestañas con icono **y** texto | 4 pestañas con **solo icono** (el nombre queda como `contentDescription` para TalkBack) |
| Pestaña 2 | «Configuraciones» con icono de engranaje, pero la pantalla ya era la lista de perfiles | «**Perfiles**» con icono de perfiles/carpeta |
| Pestaña 4 | «Diagnóstico» (log, modo detallado, informe, Acerca de, actualizaciones) | «**Ajustes**»: hub con 9 secciones + apartado «Más» |
| Ajustes de la app | Repartidos: Acerca de, diagnóstico, actualizaciones, mosaico | Un sitio: Ajustes → 9 secciones → subpantallas |

### 1.1 Decisiones ya tomadas (no se re-discuten)

| Tema | Decisión | Por qué |
|---|---|---|
| Estructura de Ajustes | **Hub con subpantallas** (enfoque A): una pantalla-lista y una pantalla por sección, cada una en su paquete con su ViewModel | Es la única estructura que aguanta «todo funcional» sin que cada sección nueva sea una fight con la anterior |
| Etiquetas de la barra | **Fuera** | Lo pidió el usuario; el nombre se conserva como descripción accesible, no como texto visible |
| Contenido del Diagnóstico | Vive dentro de **Más → Reportar un problema** | Confirmado por el usuario |
| Alcance | **Todo funcional**: no hay secciones de relleno | Decisión del usuario |
| Ubicación del código | `ui/settings/<seccion>/` (co-locación, regla de `AGENTS.md`) | Cada sección tiene su ViewModel y sus componentes privados |

---

## 2. Estado actual del código (verificado)

| Fichero | Qué aporta hoy |
|---|---|
| `ui/nav/MainTab.kt` | Enum de 4 pestañas (`Home`, `Configs`, `Import`, `Diagnostics`) + `iconRes` + `labelRes` + `fromKey` |
| `ui/components/UsipipoBottomBar.kt` | `NavigationBar` con `NavigationBarItem` que pinta icono **y** `Text` de etiqueta |
| `ui/UsipipoRoot.kt` | Enrutado por pestañas con `AnimatedContent`; subpantalla de Detalles resuelta con `var showDetails` |
| `ui/configs/ConfigsScreen.kt` | Lista de perfiles importados («Mis perfiles» / «Favoritos») — **el contenido ya es de perfiles**, solo el nombre y el icono mienten |
| `ui/diagnostics/DiagnosticsScreen.kt` | Información del dispositivo, pista del mosaico, modo detallado, registro, enviar informe, borrar registro |
| `ui/diagnostics/AboutSection.kt` | Logo, versión, atribución a IPinfo, correo de soporte, botón «Buscar actualizaciones» |
| `ui/diagnostics/QuickAccessHint.kt` | Botón/pista para añadir el mosaico de Ajustes rápidos |
| `update/` | `UpdateChecker`, `UpdateStatus`, `UpdateStateStore` (posponer) y diálogo global de actualización |
| `widget/` | Widget de pantalla de inicio, mosaico de Ajustes rápidos, `UsipipoTileService.requestAdd` |
| `theme/Theme.kt` | `UsipipoTheme(darkTheme = isSystemInDarkTheme())`: forzar tema es un parámetro, no un rediseño |
| `data/EncryptedFileConfigStore.kt` | Perfiles cifrados en disco con `KeystoreCrypto` |

Iconos actuales de la barra (vectores monolinea, trazo 1.75, esquinas redondeadas):
`ic_nav_home` (casa) · `ic_nav_configs` (engranaje) · `ic_nav_import` (documento con flecha) · `ic_nav_diagnostics` (engranaje con rayita).

---

## 3. Arquitectura

### 3.1 Pestañas

```kotlin
enum class MainTab { Home, Profiles, Import, Settings }
```

- `fromKey` (`ui/nav/MainTab.kt:22`) acepta **las claves viejas** (`Configs`, `Diagnostics`) para no romper intents o estados guardados de versiones anteriores, y las nuevas son las canónicas. La clave persistida en el `Intent` es siempre el nombre del enum.
- `labelRes` deja de usarse para pintar texto y pasa a alimentar el `contentDescription` del icono. Se conserva el recurso de string (el nombre visible en la pantalla de Perfiles y en el contenido accesible).

| Pestaña | Icono | Nota |
|---|---|---|
| Inicio | `ic_nav_home` | sin cambios |
| Perfiles | `ic_nav_profiles` (**nuevo**) | Sustituye a `ic_nav_configs`. Forma: wallet/carpeta con tarjetas, monolinea, mismo trazo |
| Importar | `ic_nav_import` | sin cambios |
| Ajustes | `ic_nav_settings` (**nuevo**) | Sustituye a `ic_nav_diagnostics`. Engranaje limpio (el de Configurations se reutiliza como base) |

### 3.2 Barra inferior sin texto

`ui/components/UsipipoBottomBar.kt`:

- `NavigationBarItem(label = {})` para no pintar la etiqueta.
- El `Icon` recibe `contentDescription = stringResource(entry.labelRes)`: **el nombre nunca desaparece del árbol de accesibilidad** (requisito de TalkBack; un icono sin nombre es inaccesible).
- El tinte se mantiene igual (icono activo en azul de marca, inactivo atenuado) y el indicador de Material 3 sigue transparente (`UsipipoBottomBar.kt:78`).
- Sin tooltip ni texto visible. El nombre de la pestaña se ve en la cabecera de cada pantalla («Perfiles», «Importar», «Ajustes»).

### 3.3 Pantalla de Ajustes (hub)

`ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt`.

Estructura: `AppHeader()` (como el resto de pantallas) + título «Ajustes» + `LazyColumn` con filas agrupadas en tarjetas (`UsipipoCard`). Cada fila: icono propio + título + resumen del estado actual a la derecha (p. ej. «Tema: Oscuro», «3 apps excluidas») + chevron.

Orden de las secciones (fijado por el usuario):

1. Apariencia
2. Seguridad
3. Pantalla inicial y panel
4. Burbuja de tráfico
5. Notificaciones
6. Actualizaciones
7. Copia de seguridad
8. Almacenamiento
9. Más

La fila «Más» abre su propia subpantalla con cuatro filas: **Reportar un problema**, **Invitar amigos**, **Donar al proyecto**, **Acerca de**.

### 3.4 Enrutado

No hay librería de navegación en el repo (`AGENTS.md`: «Navegación por barra inferior de 4 pestañas sin librería de navegación»). El enrutado de Ajustes es **estado local**, igual que la subpantalla de Detalles que ya existe (`ui/UsipipoRoot.kt:66`).

```kotlin
// ui/settings/SettingsRoute.kt
enum class SettingsRoute { Appearance, Security, HomeScreen, TrafficBubble, Notifications, Updates, Backup, Storage, More }
```

- `UsipipoRoot` mantiene `var settingsRoute by rememberSaveable { mutableStateOf<SettingsRoute?>(null) }`.
- Con `settingsRoute != null` se pinta la subpantalla **y se oculta la barra inferior** (patrón de Ajustes del sistema). Al volver, la lista de Ajustes reaparece.
- `BackHandler`: subpantalla → lista de Ajustes → Inicio (`backTarget` de `ui/nav/MainTab.kt:28` se extiende con la ruta de Ajustes).
- Cada subpantalla lleva cabecera propia con flecha de vuelta (no hay `TopAppBar` en el repo todavía: se compone con `Row` + `IconButton` + `ic_arrow_back`, reutilizando `AppHeader` cuando aporte).

### 3.5 Disposición de paquetes

```
settings/                       # ajustes globales de la app (nuevo top-level, junto a update/)
  AppSettings.kt                # fachada tipada sobre SharedPreferences
  ThemeMode.kt
  PaletteVariant.kt
ui/settings/
  SettingsScreen.kt             # hub
  SettingsViewModel.kt
  SettingsRoute.kt
  components/                   # componentes compartidos entre secciones
    SettingsRow.kt
    SettingsSectionCard.kt
    SettingsSwitchRow.kt
  appearance/AppearanceScreen.kt + AppearanceViewModel.kt
  security/SecurityScreen.kt + SecurityViewModel.kt
  homescreen/HomeScreenSettingsScreen.kt + HomeScreenSettingsViewModel.kt
  bubble/TrafficBubbleScreen.kt + TrafficBubbleViewModel.kt
  notifications/NotificationsScreen.kt + NotificationsViewModel.kt
  updates/UpdatesScreen.kt + UpdatesViewModel.kt
  backup/BackupScreen.kt + BackupViewModel.kt
  storage/StorageScreen.kt + StorageViewModel.kt
  more/MoreScreen.kt + MoreViewModel.kt
```

Cada sección tiene su propio diseño (ver §8). Los nombres de paquete son en inglés para no romper la convención existente (`configs`, `importer`, `diagnostics`), con las cadenas visibles en español.

### 3.6 Almacén de ajustes compartido

Nuevo paquete top-level `settings/` con `AppSettings`: fachada tipada sobre `SharedPreferences` (archivo `usipipo_settings`), sin flujo de datos salvo el que haga falta.

- Lectura y escritura por clave, con **valor por defecto explícito** en cada getter.
- Las claves se prefijan por sección para no colisionar: `theme_mode`, `palette`, `animations_enabled`, `notifications_enabled`, `notification_traffic`, `notification_disconnect`, `notification_updates`, `bubble_enabled`, `bubble_x`, `bubble_y`, `biometric_enabled`, `excluded_apps`, `widget_show_speed`, `widget_show_country`, `update_auto_check`, `backup_last_at`.
- **Cambios observables**: `SharedPreferences.OnSharedPreferenceChangeListener` expuesto como `Flow<String>` (la clave que cambió) para que la UI reaccione en vivo (tema, animaciones) sin recargar la app.
- `AppContainer` expone `val appSettings: AppSettings by lazy { AppSettings(appContext) }`.
- Los datos que **no** son ajustes (perfiles, registro, caché) siguen en sus repositorios actuales: `ConfigStore`, `diagnostics/`, `data/geo/`.

### 3.7 Reubicación de lo que ya existe

| Contenido actual | Destino nuevo |
|---|---|
| `DiagnosticsScreen` completo | **Más → Reportar un problema** (se conserva tal cual: información, modo detallado, registro, enviar informe, borrar registro) |
| `AboutSection` (logo, versión, atribución IPinfo, correo) | **Más → Acerca de** |
| Botón «Buscar actualizaciones» de `AboutSection` | **Sección Actualizaciones** |
| `QuickAccessHint` (añadir mosaico) | **Sección Pantalla inicial y panel** |
| `MainTab.Diagnostics` | `MainTab.Settings` |

El paquete `ui/diagnostics/` conserva su nombre y su contenido: la pantalla de diagnóstico **se mueve de sitio**, no se rehace. Solo se le cambia el título visible («Reportar un problema») y se le da cabecera con flecha de vuelta.

### 3.8 Matriz de permisos

| Permiso | Tipo | Para qué | Dónde se pide |
|---|---|---|---|
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Avisos propios: estado con tráfico, desconexión, actualización | Al activar el primer aviso en «Notificaciones» |
| `SYSTEM_ALERT_WINDOW` | Especial | Burbuja de tráfico sobre otras apps | Al activar la burbuja («Burbuja de tráfico») |
| `QUERY_ALL_PACKAGES` | Normal (instalación) | Enumerar apps instaladas para excluirlas del túnel | Al abrir el selector de apps excluidas («Seguridad») |
| `USE_BIOMETRIC` | Normal (instalación) | Desbloqueo biométrico de la app | Al activar el desbloqueo («Seguridad») |
| `VpnService` (ya implícito) | — | Always-on VPN / kill switch | El sistema; la app solo **lee** el estado (`Backend.isAlwaysOn()`, `isLockdownEnabled()`) y abre `Settings.ACTION_VPN_SETTINGS` |

Ninguna dependencia nueva de terceros salvo la que Decida la sección de Seguridad (`androidx.biometric`), que se documenta en su propio diseño.

---

## 4. Requisitos funcionales (base)

- **RF-1** — La barra inferior muestra cuatro iconos y ningún texto; el nombre de cada pestaña sigue disponible para TalkBack.
- **RF-2** — La segunda pestaña se llama Perfiles y muestra la lista de perfiles importados (sin cambios de comportamiento respecto a la actual «Configuraciones»).
- **RF-3** — La cuarta pestaña es Ajustes y muestra las nueve secciones en el orden fijado.
- **RF-4** — Tocar una sección abre su subpantalla, oculta la barra inferior y muestra una flecha de vuelta que devuelve a la lista de Ajustes.
- **RF-5** — El botón Atrás del sistema recorre subpantalla → lista de Ajustes → Inicio.
- **RF-6** — «Más → Reportar un problema» abre la pantalla de diagnóstico completa e intacta.
- **RF-7** — «Más → Acerca de» muestra logo, versión, atribución a IPinfo y correo de soporte.
- **RF-8** — «Más → Invitar amigos» comparte un enlace de la app; «Donar al proyecto» muestra una dirección de donación cripto y la copia al portapapeles.
- **RF-9** — «Pantalla inicial y panel» muestra el estado del widget y del mosaico y permite añadir el mosaico.
- **RF-10** — Cada sección tiene un resumen en la fila del hub (valor actual) que se actualiza al volver de la subpantalla.

---

## 5. Criterios de aceptación

1. La barra inferior no contiene ningún `Text` de etiqueta; TalkBack anuncia «Inicio», «Perfiles», «Importar» y «Ajustes».
2. La pestaña que antes se llamaba «Configuraciones» abre la lista de perfiles con el título «Perfiles» y un icono que no es un engranaje.
3. La pestaña que antes se llamaba «Diagnóstico» abre Ajustes, y el contenido de diagnóstico sigue accesible desde «Más → Reportar un problema».
4. Las nueve secciones aparecen en el orden del §3.3 y cada una abre una pantalla propia que se puede volver atrás.
5. Ninguna sección muestra texto de relleno tipo «Próximamente»: todas tienen comportamiento real.
6. `./gradlew testDebugUnitTest` y `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 6. Pruebas

| Ámbito | Prueba | Tipo |
|---|---|---|
| `MainTab` | `fromKey("Profiles")` → `Profiles`; `fromKey("Settings")` → `Settings`; `fromKey("Configs")` → `Profiles` (compatibilidad); clave desconocida → `Home` | JVM (JUnit 4) |
| `MainTab` | Cada pestaña tiene icono y etiqueta distintos y no nulos | JVM |
| `AppSettings` | Valor por defecto de cada ajuste; escritura → lectura; clave desconocida → error explícito | JVM con `SharedPreferences` falso o contexto de test |
| `AppSettings` | El `Flow` de cambios emite la clave modificada | JVM con `kotlinx-coroutines-test` |
| Enrutado | `backTarget` con ruta de Ajustes devuelve la secuencia esperada | JVM |

Los tests JVM son los únicos ejecutables (`AGENTS.md`: JUnit 4 en JVM). El comportamiento visual se valida en `@Preview` (tema claro y oscuro) y en la CI.

---

## 7. Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Quitar las etiquetas rompe la discoveribilidad | Medio | Cada pantalla muestra su título en la cabecera; los iconos son reconocibles |
| El icono de Perfiles puede confundirse con Importar | Bajo | Formas distintas: wallet/carpeta frente a documento con flecha; se valida en preview |
| `QUERY_ALL_PACKAGES` es sensible para las tiendas | Medio | La app se distribuye por release de GitHub; además la exclusión de apps es opcional y solo afecta al túnel |
| La app crece mucho en una sola entrega | Alto | Cada sección es un diseño (y una fase) independiente, mergeable por separado |

---

## 8. Diseños de sección (documentos independientes)

Cada sección tiene su propio documento, con su propio alcance, datos, permisos, criterios de aceptación, pruebas y riesgos. Se pueden implementar y mergear por separado:

| Sección | Documento |
|---|---|
| Apariencia | `2026-11-01-ajustes-apariencia-design.md` |
| Seguridad | `2026-11-01-ajustes-seguridad-design.md` |
| Pantalla inicial y panel | `2026-11-01-ajustes-pantalla-inicial-panel-design.md` |
| Burbuja de tráfico | `2026-11-01-ajustes-burbuja-trafico-design.md` |
| Notificaciones | `2026-11-01-ajustes-notificaciones-design.md` |
| Actualizaciones | `2026-11-01-ajustes-actualizaciones-design.md` |
| Copia de seguridad | `2026-11-01-ajustes-copia-seguridad-design.md` |
| Almacenamiento | `2026-11-01-ajustes-almacenamiento-design.md` |
| Más | `2026-11-01-ajustes-mas-design.md` |

### 8.1 Plantilla obligatoria de cada diseño de sección

1. Título, fecha, estado, proyecto, alcance.
2. **Objetivo** y tabla «antes / después».
3. **Decisiones** de la sección (tabla tema / decisión / por qué).
4. **Requisitos funcionales** numerados (`RF-n`).
5. **Interfaz**: qué se ve, qué acciones hay, estados vacío / error / permiso denegado.
6. **Datos y ajustes**: claves exactas en `AppSettings`, modelo, quién escribe.
7. **Permisos**: tipo, cuándo se pide, qué pasa si se deniega.
8. **Integración con el código existente**: ficheros tocados, APIs del SDK usadas.
9. **Criterios de aceptación** numerados.
10. **Pruebas**: qué se puede probar en JVM y qué necesita dispositivo.
11. **Riesgos** y **fuera de alcance**.
12. **Dependencias**: de qué otras secciones depende.

### 8.2 Orden de implementación recomendado

1. **Base** (este documento): rutas, hub, barra sin etiquetas, iconos, reubicación de Diagnóstico/About/QuickAccessHint.
2. **Apariencia** — introduce `AppSettings` y el primer ajuste observable en vivo; abre el camino a los demás.
3. **Seguridad**.
4. **Notificaciones**.
5. **Burbuja de tráfico** — depende del servicio en primer plano del túnel.
6. **Copia de seguridad** — depende del almacén cifrado.
7. **Almacenamiento**.
8. **Pantalla inicial y panel**.
9. **Actualizaciones**.
10. **Más**.

### 8.3 Estado de implementación

Una fase se marca como completada cuando su PR está en verde y fusionado en `main`.

| Fase | Sección | Rama | PR | Estado |
|------|---------|------|----|--------|
| 1 | Base: navegación + hub | `feat/ajustes-base` | [#13](https://github.com/usipipo/usipipo-vpn/pull/13) | En curso (CI del commit de corrección) |
| 2 | Apariencia | `feat/ajustes-apariencia` | - | No iniciada |
| 3 | Seguridad | `feat/ajustes-seguridad` | - | No iniciada |
| 4 | Notificaciones | `feat/ajustes-notificaciones` | - | No iniciada |
| 5 | Burbuja de tráfico | `feat/ajustes-burbuja-trafico` | - | No iniciada |
| 6 | Copia de seguridad | `feat/ajustes-copia-seguridad` | - | No iniciada |
| 7 | Almacenamiento | `feat/ajustes-almacenamiento` | - | No iniciada |
| 8 | Pantalla inicial y panel | `feat/ajustes-pantalla-inicial-panel` | - | No iniciada |
| 9 | Actualizaciones | `feat/ajustes-actualizaciones` | - | No iniciada |
| 10 | Más | `feat/ajustes-mas` | - | No iniciada |

Notas de la fase 1:

- El hub ya muestra las nueve secciones con su icono y su resumen; los resúmenes
  se completan en cada fase posterior.
- El enrutado de cada sección se activa en la fase que la implementa: hasta
  entonces, tocar la fila no navega (el hub es navegable pero sus destinos aún
  no existen).
- Diagnóstico deja de ser una pestaña y pasa a vivir dentro de «Más».

---

## 9. Fuera de alcance

- Cambiar la lógica de conexión del túnel (`CODING_STANDARDS.md` §7), salvo lo que la sección de Seguridad autorice explícitamente.
- Rediseñar la pantalla de Inicio, Importar o Detalles.
- Publicar en Google Play o añadir pasarelas de pago (la donación es un enlace/QR a una dirección cripto).
- Perfiles por defecto o sincronización en la nube.
