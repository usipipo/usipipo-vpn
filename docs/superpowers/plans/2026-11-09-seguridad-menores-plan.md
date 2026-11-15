# uSipipo VPN — Menores de seguridad del hallazgo 5 — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cerrar los menores detectados en la revisión final de la rama `fix/seguridad-actividad-de-lanzamiento`: la guarda anti-extras de `LauncherActivity` es demasiado estrecha, `LauncherActivity` no pinta edge-to-edge, y dos frases de la auditoría quedaron desactualizadas.

**Architecture:** Tres tareas, una por fichero de superficie: el test de contrato (`SystemIntegrationResourceTest.kt`), la actividad nueva (`LauncherActivity.kt`) y la auditoría (`masvs-audit.md`). Ninguna toca la lógica del túnel ni `MainActivity.kt`. Todas las verificaciones son tests de JVM que leen ficheros del repo: los ejecuta la CI, no esta máquina.

**Tech Stack:** Kotlin 2.3.21, JUnit 4, `androidx.activity:activity-compose` 1.10.1 (ya presente; `enableEdgeToEdge()` lo usa `MainActivity.kt:18`, que es la prueba de que existe en este repo).

**Spec:** `docs/plans/2026-11-08-seguridad-actividad-de-lanzamiento-plan.md`

## Global Constraints

1. **No se toca la lógica de conexión del túnel** (`AGENTS.md`, paso 6).
2. **`MainActivity` sigue leyendo los dos extras exactamente igual.** Sus constantes, sus nombres y su comportamiento no cambian.
3. **`LauncherActivity` lee como máximo cero extras del intent.** Si lee uno, el hallazgo 5 vuelve a estar abierto.
4. **Ningún componente cambia de `exported`.** El receptor del widget y el mosaico siguen `exported="true"` a propósito.
5. **No se inventa ningún símbolo** (`AGENTS.md`, paso 7): cada API se verifica en el propio repo antes de escribirla. Precedente de `enableEdgeToEdge()`: `MainActivity.kt:5` (import) y `MainActivity.kt:18` (llamada).
6. **Esta máquina no tiene JDK ni Android SDK.** El test se escribe primero, el rojo se deja documentado, y la CI es quien lo confirma en verde.
7. La app es solo española.

## Review Focus

- **Un test que afirma pero no muerde:** la guarda anti-extras es la única defensa de la restricción global 3, y hoy solo cubre dos getters. Si alguien añade `intent.extras` o `getIntExtra` a `LauncherActivity`, el test pasa en verde y la restricción se rompe en silencio. Se amplía la lista de cadenas prohibidas en la Task 1.
- **Un mayor de API sin cobertura:** `enableEdgeToEdge()` en una actividad que vive ~100 ms no se puede verificar en JVM. La Task 2 la fija con un test que lee el fuente y con la compilación de CI (`assembleDebug`) como red de seguridad de que la API existe en la versión del dependency.
- **Documentación que envejece:** `masvs-audit.md` cita runs y SHAs de un estado de entrega que cambia al fusionar. La Task 3 distingue la cita del commit de la cita del merge y no inventa ningún SHA.
- **Un `exported` que ya no existe en el texto:** la frase «Servicios y actividades auxiliares `exported="false"`» describe ahora menos de lo que es verdad, porque `MainActivity` también quedó cerrada.
- **Un renombrado que deja citas huérfanas:** si se renombra un test, hay que buscar el nombre viejo en todo el repo, no solo en el fichero.

---

### Task 1: La guarda anti-extras cubre todas las APIs de lectura

**Files:**
- Modify: `app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt` (test `la actividad expuesta no lee datos del intent`)
- Test: el mismo (es un test de contrato sobre `app/src/main/java/com/usipipo/vpn/LauncherActivity.kt`)

**Interfaces:**
- Consumes: `LAUNCHER_ACTIVITY_PATH` (constante privada de la clase de test, ya usada por este test).
- Produces: una guarda que falla si `LauncherActivity.kt` contiene cualquier lectura de extras.

- [ ] **Step 1: Ampliar la lista de cadenas prohibidas**

En el test `la actividad expuesta no lee datos del intent`, sustituir la condición
`codigo.contains("getStringExtra") || codigo.contains("getBooleanExtra")` por una
comprobación sobre una lista de prohibidos. La lista, exacta:

```kotlin
val prohibidos = listOf(
    "getStringExtra", "getBooleanExtra", "getIntExtra", "getLongExtra",
    "getParcelableExtra", "getStringArrayListExtra", "extras", "getIntent", "intent.",
)
```

Cada prohibido genera su propio `assertFalse` con un mensaje que nombre la cadena, para que
un fallo futuro diga cuál apareció. Mantener `assertFalse` como aserción (ya está importado
desde `313e3b6`; verificar que el import sigue en el fichero antes de usarlo).

- [ ] **Step 2: Ejecutar el test y dejar el rojo documentado**

Run (en CI, no aquí): `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.SystemIntegrationResourceTest"`

Expected: **PASS** — `LauncherActivity.kt` no lee extras, así que la guarda ampliada ya se
satisface. **No hay rojo legítimo**: esto es un endurecimiento de una guarda existente, no
comportamiento nuevo. El rojo se demuestra por mutación, no por estado inicial.

- [ ] **Step 3: Probar que la guarda muerde (mutación)**

Añadir temporalmente a `LauncherActivity.kt` una línea que lea un extra de una API de la
lista que el test viejo no cubría, por ejemplo `val x = intent.extras`. Ejecutar el test:

Expected: **FAIL** con el mensaje que nombra `extras`. Revertir la línea.

Si el test pasa con la mutación puesta, la guarda no muerde: la lista está mal.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt
git commit -m "test(seguridad): la guarda anti-extras cubre todas las APIs de lectura"
```

### Task 2: `LauncherActivity` pinta edge-to-edge

**Files:**
- Modify: `app/src/main/java/com/usipipo/vpn/LauncherActivity.kt`
- Test: `app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt` (test nuevo)

**Interfaces:**
- Consumes: nada. Es una actividad que ya existe y redirige.
- Produces: `LauncherActivity` que consume la ventana completa también en API < 35.

- [ ] **Step 1: Escribir el test que falla**

Nuevo test en `SystemIntegrationResourceTest`:

```kotlin
@Test
fun `la actividad de lanzamiento pinta edge a edge`() {
    val codigo = File(LAUNCHER_ACTIVITY_PATH).readText()
    assertTrue(
        "LauncherActivity debe consumir la ventana completa: enableEdgeToEdge()",
        codigo.contains("enableEdgeToEdge()"),
    )
}
```

Run (CI): `./gradlew testDebugUnitTest --tests "com.usipipo.vpn.widget.SystemIntegrationResourceTest"`
Expected: **FAIL** — `LauncherActivity.kt` no llama a `enableEdgeToEdge()`. Ese es el rojo (hallazgo N3).

- [ ] **Step 2: Llamar a `enableEdgeToEdge()` antes de redirigir**

En `LauncherActivity.onCreate`, antes de `startActivity(...)`:

```kotlin
enableEdgeToEdge()
```

Con `import androidx.activity.enableEdgeToEdge()` (mismo import que `MainActivity.kt:5`).
No se añade nada más: ni `savedInstanceState` guard ni `noHistory` — el ruling está en el
ledger, re-crear la actividad solo reenvía otro intent vacío.

Run (CI): mismo comando.
Expected: **PASS**, y los otros tests de la clasesiguen en verde (la llamada no cambia
ninguna aserción de extras ni de manifiesto).

- [ ] **Step 3: Verificar que compila sin SDK nuevo**

Run (CI): el workflow completo (`assembleDebug`).
Expected: **SUCCESS**. Es la comprobación de que `enableEdgeToEdge()` existe en
`androidx.activity:activity-compose` 1.10.1 sin dependencia nueva.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/usipipo/vpn/LauncherActivity.kt \
        app/src/test/java/com/usipipo/vpn/widget/SystemIntegrationResourceTest.kt
git commit -m "fix(seguridad): la actividad de lanzamiento pinta edge a edge"
```

### Task 3: La auditoría describe el estado actual

**Files:**
- Modify: `docs/security/masvs-audit.md` (fila 5 del resumen y la fila «Servicios y actividades auxiliares» de «Lo que ya cumple»)

**Interfaces:**
- Consumes: nada.
- Produces: una auditoría que no promete menos de lo que es verdad.

- [ ] **Step 1: La fila «Servicios y actividades auxiliares `exported="false"`»**

En `## Lo que ya cumple`, cambiar el texto a «Servicios y actividades quedan `exported="false"`»
(o equivalente que incluya a `MainActivity`). Verificar antes en `AndroidManifest.xml` que la
afirmación es cierta para todos los componentes que enumera: hoy `MainActivity`
(`exported="false"`), `UsipipoWidgetActionReceiver` (`false`), `VpnPermissionActivity`
(`false`), `TunnelForegroundService` (`false`) y `PortraitCaptureActivity` (sin declarar,
efectivamente `false` por no tener `intent-filter`). Si alguno no cuadra, la frase se ajusta
a la verdad, no al revés.

- [ ] **Step 2: La fila 5 del resumen**

La fila 5 dice **corregido** con el commit del verde. Si la PR #28 ya está fusionada cuando
esta tarea se ejecute, añadir el SHA del merge con la palabra «merge», como las filas hermanas.
**Si no está fusionada, no inventar el SHA**: se deja la cita del commit y el hueco.

- [ ] **Step 3: Buscar el nombre viejo del test renombrado**

```bash
grep -rn "solo la actividad de lanzamiento esta expuesta al sistema" docs/ app/src/
```

Expected: **cero resultados**. Si aparece alguno (la auditoría, un comentario, otro plan),
actualizarlo al nombre nuevo.

- [ ] **Step 4: Commit**

```bash
git add docs/security/masvs-audit.md
git commit -m "docs(seguridad): la auditoria refleja el cierre del hallazgo 5"
```

## Criterio de aceptación

1. La guarda anti-extras falla con `intent.extras` puesto en `LauncherActivity`, y pasa sin él.
2. `LauncherActivity.kt` llama a `enableEdgeToEdge()` y `assembleDebug` compila sin dependencia nueva.
3. La auditoría no promete menos de lo que es verdad, y no cita el nombre viejo del test.
4. Ningún test que pasaba se rompe; la CI es quien lo confirma.

## Fuera de alcance

- El parpadeo de un frame del tema: aceptado como no-defecto (`@style/Theme.Usipipo` es el
  mismo fondo que `MainActivity` ya pintaba). Arreglarlo exige tema translúcido, que sí cambia
  comportamiento visual en el arranque en frío.
- La guarda de reentrancia (`savedInstanceState` / `noHistory`): aceptada como no-defecto;
  re-crear la actividad solo reenvía otro intent vacío.
- Los 3 iconos del mosaico y el contrato de trazo: es el plan de iconos, no este.
- Validar el *contenido* de `EXTRA_SHOW_DETAILS` / `EXTRA_INITIAL_TAB`: fuera del alcance del
  plan original.
