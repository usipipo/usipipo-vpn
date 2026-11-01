# Spec de diseño — Ajustes: Apariencia (tema, animaciones, paleta)

**Fecha:** 2026-11-01
**Estado:** pendiente de revisión del usuario
**Proyecto:** uSipipo VPN
**Alcance:** Subpantalla **Apariencia** del hub de Ajustes: selección de tema (Sistema / Claro / Oscuro), interruptor de animaciones y selector de paleta de color (3 variantes). Introduce `AppSettings` (fachada tipada sobre `SharedPreferences`) y los modelos `ThemeMode` y `PaletteVariant`. Es la primera sección de Ajustes; abre el camino a las demás.

---

## 1. Objetivo

Que el usuario elija el tema visual, pueda apagar las animaciones aunque el sistema no esté reducido y seleccione entre 3 paletas de color, todo con efecto inmediato y persistencia.

| Comportamiento | Antes | Después |
|---|---|---|
| Tema | Siempre sigue al sistema (`isSystemInDarkTheme()` en `UsipipoTheme`) | El usuario elige Sistema / Claro / Oscuro; se aplica de inmediato y persiste (clave `theme_mode`) |
| Animaciones | Solo se respetan las reducidas del sistema: `rememberReduceMotion()` lee `Settings.Global.ANIMATOR_DURATION_SCALE` | El usuario puede apagar las animaciones aunque el sistema no esté en modo reducido (clave `animations_enabled`) |
| Paleta de color | Una sola paleta (Azul noche) hardcodeada en `Theme.kt` / `Atmosphere.kt` | 3 variantes de paleta (Azul noche, Verde bosque, Ámbar) que definen **todos** los tokens (clave `palette`) |
| Almacén de ajustes | No existe (`DiagnosticsSettings` usa prefs propios "diagnostics") | `AppSettings` sobre `SharedPreferences` (archivo `usipipo_settings`), observable vía `Flow<String>` |

---

## 2. Decisiones de la sección

| Tema | Decisión | Por qué |
|---|---|---|
| Almacén de ajustes | `AppSettings` sobre `SharedPreferences` (archivo `usipipo_settings`), paquete top-level `com.usipipo.vpn.settings` | Patrón ya usado por `DiagnosticsSettings` (prefs "diagnostics"); simple y testable en JVM con `SharedPreferences` falso |
| Ubicación del código | `settings/AppSettings.kt`, `settings/ThemeMode.kt`, `settings/PaletteVariant.kt` (top-level, junto a `update/`) | Coherente con `update/`, `diagnostics/` como paquetes top-level (`AGENTS.md`) |
| Observabilidad | `SharedPreferences.OnSharedPreferenceChangeListener` expuesto como `Flow<String>` (clave modificada) | La UI reacciona en vivo (tema, animaciones) sin recargar la app (`contract base` §3.6) |
| Tema forzado | `UsipipoTheme(darkTheme = <resuelto desde theme_mode>)`, efecto inmediato | `UsipipoTheme` ya acepta `darkTheme` como parámetro; solo hay que dejar de leer siempre el sistema |
| Animaciones | `reduceMotion efectivo = !animations_enabled || rememberReduceMotion()` | El ajuste del usuario puede apagar animaciones incluso si el sistema no las reduce |
| Paletas | 3 variantes; **cada una define TODOS los tokens**: `colorScheme` (primary, onPrimary, primaryContainer, background, surface, surfaceVariant, outline, secondary, error), `StatusColors`, `PingColors`, `BottomBarColors`, `AtmosphereColors` (calm/drifting/flowing/alert). Si falta un token, se deriva del modo claro/oscuro por defecto | Evita tokens nulos o colores inconsistentes al cambiar de paleta |
| Fuera de alcance | Tamaño de texto y densidad de interfaz | Decisión del usuario |

---

## 3. Requisitos funcionales

- **RF-1** — La pantalla Apariencia muestra el selector de tema con tres opciones: Sistema, Claro, Oscuro; la activa se destaca.
- **RF-2** — Al cambiar el tema, `UsipipoTheme` se reconstruye con el nuevo `darkTheme` de inmediato (no hace falta reiniciar la app).
- **RF-3** — La elección de tema persiste en `theme_mode` y se respeta al reiniciar la app.
- **RF-4** — La pantalla muestra un interruptor "Animaciones" (activado por defecto); desactivarlo apaga animaciones y transiciones aunque el sistema no esté en modo reducido.
- **RF-5** — El interruptor de animaciones persiste en `animations_enabled`.
- **RF-6** — La pantalla muestra un selector de paleta con 3 variantes: Azul noche (actual), Verde bosque, Ámbar.
- **RF-7** — Al cambiar la paleta, todos los tokens de color (esquema Material, estado del botón, ping, barra inferior, atmósfera) se actualizan de inmediato; clave `palette`.
- **RF-8** — Si una paleta define un token, se usa; si falta, se deriva del modo claro/oscuro por defecto (no hay huecos de color).
- **RF-9** — Cada fila del hub de Ajustes muestra el resumen del valor actual ("Tema: Oscuro", "Animaciones: Activado", "Paleta: Azul noche").

---

## 4. Interfaz

**Qué se ve:** `AppearanceScreen` (subpantalla de `SettingsScreen`) con cabecera y flecha de vuelta. Tres grupos de filas dentro de `UsipipoCard`:

1. **Tema:** 3 opciones (Sistema, Claro, Oscuro); la activa se destaca con el color de marca.
2. **Animaciones:** switch `Material` con título "Animaciones" y resumen "Desactiva transiciones y efectos de movimiento".
3. **Paleta:** 3 chips con muestra del color primario de cada variante (Azul noche, Verde bosque, Ámbar); la activa se destaca.

**Acciones:** tocar opción de tema → escribir `theme_mode` → aplicar a `UsipipoTheme` de inmediato; toggle animaciones → escribir `animations_enabled` → aplicar; tocar paleta → escribir `palette` → aplicar a todos los `CompositionLocal` de color.

**Estados:**
- Vacío: N/A (siempre hay valores por defecto).
- Error: N/A (prefs locales, sin red).
- Permiso denegado: N/A (no requiere permisos).

**Previews:** `@Preview` para cada tema y paleta, en modo claro y oscuro (`uiMode = Configuration.UI_MODE_NIGHT_NO` / `UI_MODE_NIGHT_YES`).

---

## 5. Datos y ajustes

**Fichero:** `app/src/main/java/com/usipipo/vpn/settings/AppSettings.kt` (paquete `com.usipipo.vpn.settings`).

Fachada tipada sobre `SharedPreferences` (archivo `usipipo_settings`, `MODE_PRIVATE`). Cada getter tiene **valor por defecto explícito**. Expone `val changedKeys: Flow<String>` (`OnSharedPreferenceChangeListener`) para que la UI reaccione en vivo.

| Clave | Tipo | Valor por defecto | Quién escribe |
|---|---|---|---|
| `theme_mode` | `String` ("system" \| "light" \| "dark") | `"system"` | `AppearanceViewModel` |
| `palette` | `String` ("azul_noche" \| "verde_bosque" \| "ambar") | `"azul_noche"` | `AppearanceViewModel` |
| `animations_enabled` | `Boolean` | `true` | `AppearanceViewModel` |

**Modelo:**
- `settings/ThemeMode.kt`: `enum class ThemeMode { System, Light, Dark }` + `ThemeMode.darkTheme(isSystemDark: Boolean): Boolean`.
- `settings/PaletteVariant.kt`: `enum class PaletteVariant { AzulNoche, VerdeBosque, Ambar }` + `data class PaletteTokens` con todos los tokens de ambas variantes (claro/oscuro).

**Paletas (tokens exactos):**

| Token | Azul noche (claro) | Azul noche (oscuro) | Verde bosque (claro) | Verde bosque (oscuro) | Ámbar (claro) | Ámbar (oscuro) |
|---|---|---|---|---|---|---|
| colorScheme.primary | 0xFFA8CCFF | 0xFF4C8DFF | 0xFF2E7D5B | 0xFF5BD98A | 0xFFC8861E | 0xFFF5C451 |
| colorScheme.onPrimary | 0xFF0B2545 | 0xFF04122B | 0xFFFFFFFF | 0xFF04122B | 0xFFFFFFFF | 0xFF04122B |
| colorScheme.primaryContainer | 0xFFD6E6FB | 0xFF14386B | 0xFFD8F3E4 | 0xFF1B4332 | 0xFFFDEACB | 0xFF7A5000 |
| colorScheme.background | 0xFFF3F8FF | 0xFF061631 | 0xFFF1F8F4 | 0xFF061631 | 0xFFFFFBF0 | 0xFF061631 |
| colorScheme.surface | 0xFFFFFFFF | 0xFF0E2447 | 0xFFFFFFFF | 0xFF0E2447 | 0xFFFFFFFF | 0xFF0E2447 |
| colorScheme.surfaceVariant | 0xFFE8F1FD | 0xFF12305A | 0xFFE3F0E9 | 0xFF12305A | 0xFFFDF0D9 | 0xFF12305A |
| colorScheme.outline | 0xFFD6E6FB | 0xFF244A7C | 0xFFD8F3E4 | 0xFF244A7C | 0xFFFDEACB | 0xFF244A7C |
| colorScheme.secondary | 0xFF4A6283 | 0xFF2DD4BF | 0xFF4A6283 | 0xFF2DD4BF | 0xFF4A6283 | 0xFF2DD4BF |
| colorScheme.error | 0xFFF4A3A3 | 0xFFF4A3A3 | 0xFFF4A3A3 | 0xFFF4A3A3 | 0xFFF4A3A3 | 0xFFF4A3A3 |
| StatusColors.idle / connected / error | A8CCFF / 6FCFC0 / F4A3A3 | 8DBBFF / 4FB3A6 / F4A3A3 | 5BD98A / 2E7D5B / F4A3A3 | 4FB3A6 / 5BD98A / F4A3A3 | F5C451 / C8861E / F4A3A3 | E8A53C / F5C451 / F4A3A3 |
| PingColors good/warn/bad/unknown | 1B7F4B / 8A5A00 / B3261E / 4A6283 | 5BD98A / F5C451 / F87171 / B7C9E2 | (igual luz) | 5BD98A / F5C451 / F87171 / B7C9E2 | (igual luz) | 5BD98A / F5C451 / F87171 / B7C9E2 |
| BottomBarColors container/active/inactive | E8F1FD / 0B57C9 / 5B6E8C | 000A25 / 06D3FE / 6B87A0 | E3F0E9 / 2E7D5B / 5B6E8C | 001A1A / 5BD98A / 6B87A0 | FDF0D9 / C8861E / 5B6E8C | 1A1000 / F5C451 / 6B87A0 |
| AtmosphereColors (calm/drifting/flowing/alert) | F3F8FF→D6E6FB, EDF7FD→C6E9E0, E3F1FE→A9E5D8, FDF6F7→F7DCE2 | 04132D→0B2147, 06182E→15403C, 071D33→114C40, 231620→32202B | F1F8F4→D8F3E4, E9F4EC→BEE0CC, E8F4ED→B9E8D0, FDF6F7→F7DCE2 | (igual a Azul noche oscuro) | FBF0→DEACB, F7E8D0→ECD8A8, FDF0D9→EBD8A8, FDF6F7→F7DCE2 | (igual a Azul noche oscuro) |

**Quién lee:** `UsipipoTheme` (tema y animaciones), los `CompositionLocal` de color (paleta), `AppearanceScreen` y el hub de Ajustes (resúmenes). Único escritor: `AppearanceViewModel`.

---

## 6. Permisos

Ninguno. No requiere permisos de runtime ni especiales.

---

## 7. Integración con el código existente

**Ficheros a crear:**
- `app/src/main/java/com/usipipo/vpn/settings/AppSettings.kt`
- `app/src/main/java/com/usipipo/vpn/settings/ThemeMode.kt`
- `app/src/main/java/com/usipipo/vpn/settings/PaletteVariant.kt`
- `app/src/main/java/com/usipipo/vpn/ui/settings/appearance/AppearanceScreen.kt`
- `app/src/main/java/com/usipipo/vpn/ui/settings/appearance/AppearanceViewModel.kt`

**Ficheros a modificar:**
- `app/src/main/java/com/usipipo/vpn/ui/theme/Theme.kt`: `UsipipoTheme` recibe `darkTheme` resuelto desde `AppSettings.theme_mode` (no `isSystemInDarkTheme()`); expone `LocalReduceMotion` combinado con `animations_enabled`.
- `app/src/main/java/com/usipipo/vpn/ui/theme/Motion.kt`: `rememberReduceMotion()` sigue leyendo el sistema; añadir `rememberEffectiveReduceMotion()` que combina `!animations_enabled || rememberReduceMotion()`.
- `app/src/main/java/com/usipipo/vpn/AppContainer.kt`: añadir `val appSettings: AppSettings by lazy { AppSettings(appContext) }`.
- `app/src/main/java/com/usipipo/vpn/MainActivity.kt`: resolver `darkTheme` desde `container.appSettings.themeMode` y pasarlo a `UsipipoTheme`.
- `app/src/main/java/com/usipipo/vpn/ui/nav/UsipipoRoot.kt`: mantener el enrutado de `SettingsRoute` (Base); la subpantalla Apariencia se pinta con `settingsRoute == Appearance`.

**APIs del SDK:**
- `android.content.SharedPreferences`, `android.content.Context.getSharedPreferences`.
- `androidx.compose.runtime.CompositionLocalProvider`, `androidx.compose.material3.MaterialTheme`.
- `android.provider.Settings.Global.ANIMATOR_DURATION_SCALE` (ya en `Motion.kt`).

**No se toca:** `tunnel/` (CODING_STANDARDS §7).

---

## 8. Criterios de aceptación

1. Tres opciones de tema visibles; la activa se destaca.
2. Cambiar tema aplica `darkTheme` de inmediato en toda la app (sin reiniciar).
3. Al reiniciar la app, el tema elegido se restaura (`theme_mode`).
4. Desactivar "Animaciones" apaga `animateColorAsState`, `AnimatedContent` y transiciones del bottom bar aunque `ANIMATOR_DURATION_SCALE != 0`.
5. `theme_mode`, `palette`, `animations_enabled` persisten en `usipipo_settings`.
6. Las 3 paletas definen todos los tokens; cambiar paleta actualiza colores sin huecos.
7. `./gradlew testDebugUnitTest` + `assembleDebug`/`assembleRelease` verdes en la CI.

---

## 9. Pruebas

**JVM (JUnit 4):**
- `AppSettingsTest`: valor por defecto de cada clave; escritura → lectura; `changedKeys` emite la clave modificada; clave desconocida devuelve el default explícito (usando `SharedPreferences` falso en memoria, sin Robolectric).
- `ThemeModeTest`: `ThemeMode.darkTheme()` para System / Light / Dark con sistema claro y oscuro.
- `PaletteVariantTest`: cada variante devuelve todos los tokens no nulos; tokens faltantes se derivan del default claro/oscuro.
- `MotionTest`: `effectiveReduceMotion = true` si `animations_enabled = false` aunque el sistema no reduzca movimiento.
- `AppearanceViewModelTest`: toggle de claves y resúmenes del hub.

**Dispositivo:**
- `@Preview` claro y oscuro para cada tema y cada paleta.
- Cambio de tema en runtime se ve sin reiniciar la app.

---

## 10. Riesgos y fuera de alcance

| Riesgo | Impacto | Mitigación |
|---|---|---|
| `UsipipoTheme` se llama en `MainActivity` y en previews; `appSettings` debe estar disponible | Medio | `AppSettings` lee de `context.applicationContext` (disponible en `Application`) |
| Cambiar tema con animaciones en curso puede parpadear | Bajo | Aplicar en recomposición, sin transiciones propias |

**Fuera de alcance:** tamaño de texto, densidad de interfaz, temas por perfil.

---

## 11. Dependencias de otras secciones

- **Base** (`2026-11-01-ajustes-navegacion-design.md`): requiere el hub `SettingsScreen` y `SettingsRoute` ya creados; `AppSettings` se introduce aquí y es requisito de Seguridad y Pantalla inicial y panel.
- No depende de Seguridad, Pantalla inicial y panel, Burbuja, Notificaciones, etc.
- **Gradle:** ninguna dependencia nueva (usa `SharedPreferences` del framework y `androidx.core` ya presente en `gradle/libs.versions.toml`).
