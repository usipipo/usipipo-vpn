# Plan — Seguridad: separar la actividad de lanzamiento

**Fecha:** 2026-11-08
**Rama:** `fix/seguridad-actividad-de-lanzamiento`
**PR:** #28
**Origen:** hallazgo 5 de `docs/security/masvs-audit.md`, con el enfoque A aprobado por el usuario el 2026-11-08.

## Contexto

`MainActivity` está `exported="true"` con filtro `MAIN/LAUNCHER` y lee dos extras del intent
(`EXTRA_INITIAL_TAB`, `EXTRA_SHOW_DETAILS`). Cualquier proceso del dispositivo puede lanzarla
nombrando el componente y forzar una pestaña o el panel de Detalles. No hay escalada de
privilegios ni fuga de datos, pero es superficie innecesaria.

El enfoque aprobado (A) reparte el trabajo en dos actividades: una de lanzamiento expuesta que
desecha el intent recibido, y `MainActivity` cerrada que conserva sus extras para las llamadas
internas (widget, burbuja de tráfico), que la nombran por componente dentro del mismo UID.

**Estado al abrir el plan:** los tests ya están escritos y el rojo ya está observado en la CI
(run `42547882706` sobre `313e3b6`, 4 fallos de 484 tests). Este plan solo cubre el verde y su
documentación.

## Restricciones globales

1. **No se toca la lógica de conexión del túnel** (`AGENTS.md`, paso 6).
2. **`MainActivity` sigue leyendo los dos extras exactamente igual.** Sus constantes, sus nombres
   y su comportamiento no cambian: las llamadas internas tienen que seguir funcionando.
3. **La actividad nueva puede leer como máximo cero extras del intent.** Si lee uno, el hallazgo
   sigue abierto. Hay un test que lo fija.
4. **Ningún componente más cambia de `exported`.** El receptor del widget y el mosaico siguen
   exportados a propósito (el sistema los necesita).
5. **No se inventa ningún símbolo.** Verificar cada API en el propio repo (precedente:
   `VpnPermissionActivity` es `ComponentActivity`) antes de escribirla.
6. **La app es solo española**, pero este cambio no da formato a nada: no hay decisión de locale.

## Tareas

### Task 1 · La actividad de lanzamiento (el verde)

Crear `app/src/main/java/com/usipipo/vpn/LauncherActivity.kt` y editar
`app/src/main/AndroidManifest.xml` hasta que los cuatro tests rojos pasen.

- `LauncherActivity` es `ComponentActivity` (precedente: `widget/VpnPermissionActivity.kt`),
  declarada `exported="true"` con el único filtro `MAIN`/`LAUNCHER`. Su `onCreate` abre
  `MainActivity` con un `Intent` nuevo y vacío, y llama a `finish()`. No lee ningún extra.
- El filtro `MAIN`/`LAUNCHER` sale de `.MainActivity`. `MainActivity` queda
  `android:exported="false"`, sin `intent-filter` y conservando `windowSoftInputMode`.
- Nada más cambia en el manifiesto ni en `MainActivity.kt`.

Verde = los cuatro tests de `SystemIntegrationResourceTest` pasan y los otros 480 no se rompen.
La CI es quien lo confirma (esta máquina no tiene SDK).

### Task 2 · Cerrar el hallazgo en la auditoría

Actualizar `docs/security/masvs-audit.md`:

- La fila 5 de la tabla de resumen pasa a **corregido**, con la rama y el merge o PR.
- La sección «## 5» describe el arreglo real: el reparto entre las dos actividades, por qué se
  optó por una actividad real y no por un `activity-alias` (el alias seguiría exportado y los
  extras de un proceso externo llegarían igual a `MainActivity`), y qué pruebas lo fijan.
- Se añade la fila correspondiente a la tabla de regresión si la sección la tiene.

No inventar el SHA del merge: si aún no está fusionado, se cita la PR y se deja el hueco del SHA.

## Fuera de alcance

- Cambiar `EXTRA_INITIAL_TAB` o `EXTRA_SHOW_DETAILS` de sitio, o validar su contenido.
- Endurecer `UiLocale`/formatters (ya cerrado en otra rama).
- Mover el filtro a un `activity-alias` (descartado arriba, con razón).

## Criterio de aceptación

1. Los cuatro tests rojos pasan en CI y ningún test que pasaba se rompe.
2. `LauncherActivity.kt` no contiene ninguna lectura de extras.
3. `MainActivity` queda `exported="false"` sin `intent-filter`, con sus dos constantes intactas.
4. El hallazgo 5 de la auditoría queda documentado como corregido con su evidencia.
