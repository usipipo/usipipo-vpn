# Spec de diseño — Ajustes: Pantalla inicial y panel (widget, mosaico, accesos directos)

**Fecha:** 2026-11-01
**Estado:** aprobado por el usuario (2026-11-01)
**Implementación:** fase 8 · no iniciada · rama `feat/ajustes-pantalla-inicial-panel` · -
**Proyecto:** uSipipo VPN
**Alcance:** Subpantalla **Pantalla inicial y panel** del hub de Ajustes: estado del widget de pantalla de inicio, estado y añadido del mosaico de Ajustes rápidos, personalización de contenido del widget y accesos directos al sistema (lista de widgets del launcher y panel de Ajustes rápidos).

---

## 1. Objetivo

Que el usuario sepa si el widget y el mosaico están añadidos, pueda añadir el mosaico, personalizar qué muestra el widget (estado del túnel, velocidad, nombre del perfil) y abra los accesos directos del sistema para añadir el widget y el mosaico.

| Comportamiento | Antes | Después |
|---|---|---|
| Estado del widget | No se consulta desde Ajustes | Se consulta vía `AppWidgetManager.getAppWidgetIds(ComponentName)` |
| Estado del mosaico | Pista estática en Diagnóstico (`QuickAccessHint`) | Botón "Añadir mosaico" (`UsipipoTileService.requestAdd`, API 33+) + instrucciones; sin detección inventada |
| Personalizar widget | No configurable | Qué muestra: estado del túnel, velocidad, nombre del perfil (claves en prefs consumidas por `UsipipoWidgetRenderer`) |
| Accesos directos | No hay | Abre lista de widgets del launcher y panel de Ajustes rápidos del sistema (intents estándar) |

---

## 2. Decisiones de la sección

| Tema | Decisión | Por qué |
|---|---|---|
| Estado del widget | `AppWidgetManager.getAppWidgetIds(ComponentName(context, UsipipoWidgetProvider::class.java))`; lista vacía = no añadido | API pública fiable (`UsipipoWidgetRenderer.render` ya la usa) |
| Estado del mosaico | No hay API pública fiable para saber si el mosaico está añadido | Limitación de la plataforma; **no se inventa una detección que no existe** (contrato base) |
| Añadir mosaico | Botón "Añadir mosaico" → `UsipipoTileService.requestAdd` (API 33+) | Ya existe en `widget/UsipipoTileService.kt` |
| Aproximación del mosaico | Opcional: flag `tile_added` en prefs cuando el sistema llama a `TileService` (`onTileAdded` / `onStartListening`) | Señalado como **aproximación**, no como detección fiable |
| Personalizar widget | Claves `widget_show_speed`, `widget_show_country` en `AppSettings`; `UsipipoWidgetRenderer.kt` las consume | El renderizador ya existe; solo lee las prefs |
| Abrir lista de widgets del launcher | Intent estándar `AppWidgetManager.ACTION_APPWIDGET_PICK` | Abre el selector de widgets del launcher para añadir uSipipo VPN |
| Abrir panel de Ajustes rápidos | `Settings.ACTION_QUICK_SETTINGS_SETTINGS` con `resolveActivity` y fallback a `Settings.ACTION_SETTINGS` | Documentado en el contrato base; limitaciones por fabricante (Xiaomi/MIUI) documentadas |
| Fuera de alcance | Rediseñar el layout del widget más allá de los toggles de contenido | Decisión del usuario |

---

## 3. Requisitos funcionales

- **RF-1** — Pantalla "Pantalla inicial y panel" (subpantalla de `SettingsScreen`) con cabecera y flecha de vuelta.
- **RF-2** — Muestra estado del widget: "Añadido" / "No añadido" (según `getAppWidgetIds`).
- **RF-3** — Muestra estado del mosaico: "Añadido" (aprox. vía flag `tile_added`) / "No añadido" + botón "Añadir mosaico a Ajustes rápidos".
- **RF-4** — Botón "Añadir mosaico" llama `UsipipoTileService.requestAdd` (API 33+); en API < 33 indica que no está disponible.
- **RF-5** — Personalización del widget: switches "Mostrar velocidad" (`widget_show_speed`) y "Mostrar país/perfil" (`widget_show_country`).
- **RF-6** — `UsipipoWidgetRenderer.kt` consume `widget_show_speed` / `widget_show_country` al renderizar.
- **RF-7** — Botón "Abrir lista de widgets" lanza `AppWidgetManager.ACTION_APPWIDGET_PICK`.
- **RF-8** — Botón "Abrir Ajustes rápidos" lanza `Settings.ACTION_QUICK_SETTINGS_SETTINGS` (fallback `Settings.ACTION_SETTINGS` si no resuelve).
- **RF-9** — Instrucciones de texto para añadir widget y mosaico (reutilizar strings `quick_access_widget_hint`, `quick_access_tile_hint`).

---

## 4. Interfaz

**Qué se ve:** `HomeScreenSettingsScreen` con dos tarjetas (`UsipipoCard`) y una sección de accesos directos.

1. **Tarjeta "Widget de pantalla de inicio":** estado (Añadido / No añadido), switches de contenido (velocidad, país/perfil), botón "Abrir lista de widgets".
2. **Tarjeta "Mosaico de Ajustes rápidos":** estado, botón "Añadir mosaico", instrucciones (reutilizar `quick_access_tile_hint`).
3. **Sección "Accesos directos":** botón "Abrir panel de Ajustes rápidos".

**Acciones:** toggle switches → escribir prefs; botón "Añadir mosaico" → `UsipipoTileService.requestAdd`; botón "Abrir lista de widgets" → intent `ACTION_APPWIDGET_PICK`; botón "Abrir Ajustes rápidos" → intent `ACTION_QUICK_SETTINGS_SETTINGS`.

**Estados:**
- Vacío: widget no añadido → mensaje con instrucción ("Mantén pulsada la pantalla de inicio, abre Widgets y añade uSipipo VPN").
- Error: `requestAdd` devuelve `false` → "No se pudo añadir el mosaico" (reutilizar string `diag_tile_denied`).
- Permiso denegado: N/A (no requiere permisos nuevos; el widget y el mosaico ya están declarados en el manifiesto).

---

## 5. Datos y ajustes

**Fichero:** `app/src/main/java/com/usipipo/vpn/settings/AppSettings.kt` (ya creado en la sección Apariencia).

| Clave | Tipo | Valor por defecto | Quién escribe |
|---|---|---|---|
| `widget_show_speed` | `Boolean` | `true` | `HomeScreenSettingsViewModel` |
| `widget_show_country` | `Boolean` | `true` | `HomeScreenSettingsViewModel` |
| `tile_added` | `Boolean` | `false` | `UsipipoTileService` (aproximación en `onTileAdded` / `onStartListening`) |

**Modelo:** `ui/settings/homescreen/HomeScreenSettingsViewModel.kt`.

**Quién lee:** `widget/UsipipoWidgetRenderer.kt` lee `widget_show_speed` / `widget_show_country` (vía `AppSettings` o prefs directo con el `Context` que ya recibe). `tile_added` lo lee la UI para mostrar el estado aproximado del mosaico.

---

## 6. Permisos

Ninguno nuevo. `UsipipoTileService` ya declara `android.permission.BIND_QUICK_SETTINGS_TILE` en `AndroidManifest.xml`.

---

## 7. Integración con el código existente

**Ficheros a crear:**
- `app/src/main/java/com/usipipo/vpn/ui/settings/homescreen/HomeScreenSettingsScreen.kt`
- `app/src/main/java/com/usipipo/vpn/ui/settings/homescreen/HomeScreenSettingsViewModel.kt`

**Ficheros a modificar:**
- `app/src/main/java/com/usipipo/vpn/widget/UsipipoWidgetRenderer.kt`: leer `widget_show_speed` / `widget_show_country` de `AppSettings` y usarlos para mostrar/ocultar velocidad y país en el `RemoteViews` (el renderer recibe `Context`; puede instanciar `AppSettings(context)` o leer prefs directo).
- `app/src/main/java/com/usipipo/vpn/widget/UsipipoTileService.kt`: escribir `tile_added = true` en `onTileAdded()` y `onStartListening()` (aproximación del estado del mosaico).
- `app/src/main/java/com/usipipo/vpn/AppContainer.kt`: `appSettings` ya existe (creado en Apariencia).

**APIs del SDK:**
- `android.appwidget.AppWidgetManager.getAppWidgetIds(ComponentName)`.
- `android.appwidget.AppWidgetManager.ACTION_APPWIDGET_PICK` ("android.appwidget.action.APPWIDGET_PICK").
- `android.provider.Settings.ACTION_QUICK_SETTINGS_SETTINGS`.
- `android.service.quicksettings.TileService` / `android.app.StatusBarManager.requestAddTileService` (API 33+, ya en `UsipipoTileService`).

**No se toca:** `tunnel/` (CODING_STANDARDS §7).

---

## 8. Criterios de aceptación

1. Estado del widget refleja si `getAppWidgetIds` devuelve lista vacía o no.
2. Botón "Añadir mosaico" llama `requestAdd` (API 33+) y muestra el resultado (éxito / denegado).
3. Switches de widget persisten en `widget_show_speed` / `widget_show_country`.
4. `UsipipoWidgetRenderer` respeta las banderas al renderizar (velocidad y país visibles u ocultos).
5. Botón de Ajustes rápidos lanza `ACTION_QUICK_SETTINGS_SETTINGS` (o fallback a `ACTION_SETTINGS` si no resuelve).
6. Botón de lista de widgets lanza `ACTION_APPWIDGET_PICK`.
7. `./gradlew testDebugUnitTest` + `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

**JVM (JUnit 4):**
- `HomeScreenSettingsViewModelTest`: valores por defecto; escritura/lectura de `widget_show_speed`, `widget_show_country`, `tile_added`.
- `AppSettingsTest` (claves de widget).
- `AppWidgetManager` no es fácilmente mocable sin Robolectric; se testa la decisión (vacío vs no vacío) en la ViewModel con un `AppWidgetManager` falso inyectado.

**Dispositivo:**
- `getAppWidgetIds` con widget añadido y no añadido.
- `requestAdd` en dispositivo con API 33+.
- `Settings.ACTION_QUICK_SETTINGS_SETTINGS` resuelve (y comportamiento en Xiaomi/MIUI donde puede no existir una intent única).

---

## 10. Riesgos y fuera de alcance

| Riesgo | Impacto | Mitigación |
|---|---|---|
| No hay API fiable para detectar si el mosaico está añadido | Medio | Flag `tile_added` señalado como **aproximación**; no afirmar que es detección real |
| Intents de launcher / Ajustes rápidos no resuelven en todos los fabricantes (Xiaomi/MIUI) | Medio | `resolveActivity` + fallback a `ACTION_SETTINGS` + documentar la limitación |

**Fuera de alcance:** rediseñar el layout del widget (solo toggles de contenido); detección real del mosaico.

---

## 11. Dependencias de otras secciones

- **Apariencia** (`2026-11-01-ajustes-apariencia-design.md`): `AppSettings` debe existir primero; aquí se escriben las claves `widget_show_speed`, `widget_show_country`, `tile_added`.
- **Base** (`2026-11-01-ajustes-navegacion-design.md`): `UsipipoTileService.requestAdd` ya existe (`widget/UsipipoTileService.kt`); hub `SettingsScreen` y `SettingsRoute` creados.
- No depende de Seguridad, Burbuja, Notificaciones, etc.
- **Gradle:** ninguna dependencia nueva (usa APIs del framework ya presentes).
