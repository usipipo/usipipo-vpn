#!/usr/bin/env bash
#
# next-version.test.sh — pruebas autocontenidas de scripts/next-version.sh.
#
# Sin frameworks: cada caso crea un repositorio git temporal con commits convencionales
# y etiquetas, ejecuta el script y compara la salida. Termina con código 0 solo si
# pasan todas; si alguna falla, imprime el resumen y sale con código 1.
#
# Casos cubiertos:
#   - sin etiquetas                    → 0.1.0 (versión inicial)
#   - etiqueta con prefijo v           → la salida no lleva v
#   - solo fix                         → parche
#   - feat                             → menor
#   - feat! con mayor 0                → menor (regla semver para 0.x)
#   - feat! con mayor >= 1              → mayor
#   - BREAKING CHANGE en el pie        → mayor
#   - mezcla de feat y fix             → menor (manda el cambio más grande)
#   - solo docs/chore/no convencionales→ parche
#   - ámbito en el asunto (feat(x):)   → menor
#   - etiqueta obsoleta por semver mayor aunque sea más reciente por fecha
#   - etiqueta que no es vX.Y.Z        → se ignora (p. ej. `latest`)
#   - ceros a la izquierda             → se normalizan (v1.08.3 → 1.8.x)
#   - componente >= 1000               → el script falla con mensaje claro
#   - ningún commit desde la etiqueta  → parche
#   - --code                           → versionCode (major*1000000+minor*1000+patch)

set -uo pipefail   # sin -e: un caso que falla no debe abortar el resto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NEXT_VERSION="$SCRIPT_DIR/next-version.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

REPO="$WORK/repo"
pass=0
fail=0

say() { printf '%s\n' "$*"; }

# --- Repositorio temporal de prueba ---------------------------------------------

new_repo() {
  rm -rf "$REPO"
  mkdir -p "$REPO"
  git -C "$REPO" init -q
  git -C "$REPO" config user.name "Prueba"
  git -C "$REPO" config user.email "prueba@example.com"
  git -C "$REPO" config commit.gpgsign false
  git -C "$REPO" config tag.gpgsign false
}

# commit <mensaje>: commit vacío con ese mensaje (admite varias líneas).
commit() { git -C "$REPO" commit -q --allow-empty -m "$1"; }

# commit_dated <fecha> <mensaje>: commit con fecha fija, para el caso en el que la
# etiqueta más reciente por fecha NO es la más alta por semver.
commit_dated() {
  GIT_AUTHOR_DATE="$1" GIT_COMMITTER_DATE="$1" \
    git -C "$REPO" commit -q --allow-empty -m "$2"
}

tag() { git -C "$REPO" tag "$1"; }

# --- Comprobaciones --------------------------------------------------------------

# expect <nombre> <esperado>: el script debe terminar en 0 e imprimir <esperado>.
expect() {
  local name="$1" expected="$2" actual rc
  actual="$(cd "$REPO" && bash "$NEXT_VERSION" 2>"$WORK/stderr")"
  rc=$?
  if [ "$rc" -ne 0 ]; then
    fail=$((fail + 1))
    say "FALLO: $name — el script terminó con código $rc: $(cat "$WORK/stderr")"
    return
  fi
  if [ "$actual" = "$expected" ]; then
    pass=$((pass + 1))
    say "OK: $name → $actual"
  else
    fail=$((fail + 1))
    say "FALLO: $name — se esperaba \"$expected\" y se obtuvo \"$actual\""
  fi
}

# expect_code <nombre> <esperado>: igual, pero con --code (versionCode).
expect_code() {
  local name="$1" expected="$2" actual rc
  actual="$(cd "$REPO" && bash "$NEXT_VERSION" --code 2>"$WORK/stderr")"
  rc=$?
  if [ "$rc" -ne 0 ]; then
    fail=$((fail + 1))
    say "FALLO: $name — el script terminó con código $rc: $(cat "$WORK/stderr")"
    return
  fi
  if [ "$actual" = "$expected" ]; then
    pass=$((pass + 1))
    say "OK: $name → $actual"
  else
    fail=$((fail + 1))
    say "FALLO: $name — se esperaba \"$expected\" y se obtuvo \"$actual\""
  fi
}

# expect_fail <nombre>: el script debe fallar (código != 0), con mensaje por stderr y
# sin imprimir nada por stdout.
expect_fail() {
  local name="$1" out rc err
  out="$(cd "$REPO" && bash "$NEXT_VERSION" 2>"$WORK/stderr")"
  rc=$?
  err="$(cat "$WORK/stderr")"
  if [ "$rc" -ne 0 ] && [ -n "$err" ] && [ -z "$out" ]; then
    pass=$((pass + 1))
    say "OK: $name → falla con código $rc y mensaje claro por stderr"
  else
    fail=$((fail + 1))
    say "FALLO: $name — código=$rc stdout=\"$out\" stderr=\"$err\""
  fi
}

say "=== next-version.sh: la versión se deriva del historial ==="
say ""

# --- Casos -----------------------------------------------------------------------

# 1. Sin etiquetas: version inicial, aunque haya feat.
new_repo
commit "feat: esqueleto de la app"
commit "docs: leer el conf de WireGuard"
expect "sin etiquetas → 0.1.0" "0.1.0"
expect_code "sin etiquetas → versionCode de 0.1.0" "1000"

# 2. La etiqueta lleva v; la salida, no.
new_repo
commit "feat: pantalla de inicio"
tag v2.5.1
commit "fix: reconexion al cambiar de red"
expect "etiqueta con v (v2.5.1 + fix)" "2.5.2"

# 3. Solo fix → parche.
new_repo
commit "feat: importar conf"
tag v1.2.3
commit "fix: no conectar con DNS vacio"
expect "solo fix → parche" "1.2.4"
expect_code "versionCode de 1.2.4" "1002004"

# 4. feat → menor.
new_repo
commit "feat: pantalla de inicio"
tag v1.2.3
commit "feat: estadisticas del tunel"
expect "feat → menor" "1.3.0"

# 5. feat! con mayor 0 → menor (0.x no salta a 1.0.0 por si solo).
new_repo
commit "feat: pantalla de inicio"
tag v0.2.3
commit "feat!: cambia el formato del conf importado"
expect "feat! con mayor 0 → menor" "0.3.0"

# 6. feat! con mayor >= 1 → mayor.
new_repo
commit "feat: pantalla de inicio"
tag v1.2.3
commit "feat!: cambia el formato del conf importado"
expect "feat! con mayor 1 → mayor" "2.0.0"

# 7. BREAKING CHANGE en el pie del commit → mayor.
new_repo
commit "feat: pantalla de inicio"
tag v1.0.0
commit $'fix: ajusta el parser\n\nBREAKING CHANGE: el indice de configs cambia de forma'
expect "BREAKING CHANGE en el pie → mayor" "2.0.0"

# 8. Mezcla de feat y fix: manda el cambio mas grande (menor).
new_repo
commit "feat: pantalla de inicio"
tag v0.4.0
commit "fix: fuga en el cierre del tunel"
commit "feat: borrar una configuracion"
expect "mezcla de feat y fix → menor" "0.5.0"

# 9. Solo docs (y tipos que no tocan codigo) → parche.
new_repo
commit "feat: pantalla de inicio"
tag v0.1.0
commit "docs: explicar el conf de WireGuard"
commit "chore: ordenar imports"
expect "solo docs y chore → parche" "0.1.1"

# 10. Ambito en el asunto → menor.
new_repo
commit "feat: pantalla de inicio"
tag v0.1.0
commit "feat(tunel): reconexion automatica"
expect "feat con ambito → menor" "0.2.0"

# 11. Etiqueta mas reciente por fecha pero mas baja por semver: manda el semver.
new_repo
commit_dated "2026-01-01T10:00:00" "feat: pantalla de inicio"
tag v2.0.0
commit_dated "2026-06-01T10:00:00" "chore: limpieza"
tag v1.0.0
expect "etiqueta obsoleta por semver (v1.0.0 mas reciente)" "2.0.1"

# 12. Etiqueta que no es vX.Y.Z se ignora (como `latest` en el repositorio real).
new_repo
commit "feat: pantalla de inicio"
tag latest
expect "etiqueta no semver (latest) se ignora" "0.1.0"

# 13. Ceros a la izquierda: se normalizan.
new_repo
commit "feat: pantalla de inicio"
tag v1.08.3
commit "docs: corregir el README"
expect "etiqueta v1.08.3 sin ceros a la izquierda" "1.8.4"

# 14. Componente >= 1000: falla con mensaje claro, sin imprimir version.
new_repo
commit "feat: pantalla de inicio"
tag v0.999.999
commit "fix: ultimo parche antes del limite"
expect_fail "componente 0.999.1000 >= 1000 falla"

# 15. Ningun commit desde la etiqueta: parche.
new_repo
commit "feat: pantalla de inicio"
tag v1.2.3
expect "sin commits nuevos desde la etiqueta" "1.2.4"

# 16. Commits no convencionales: parche (el salto se aplica una sola vez, no una
#     por commit).
new_repo
commit "feat: pantalla de inicio"
tag v0.1.0
commit "merge: subsistema de release firmada"
commit "arreglo rapido sin tipo"
expect "commits no convencionales → parche" "0.1.1"

# --- Resumen ---------------------------------------------------------------------

say ""
say "=== Resumen: $pass OK, $fail FALLO (de $((pass + fail)) casos) ==="

if [ "$fail" -ne 0 ]; then
  exit 1
fi
say "Todos los casos pasan."
