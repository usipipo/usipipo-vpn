#!/usr/bin/env bash
#
# next-version.sh — calcula la siguiente versión de uSipipo VPN a partir del historial
# de git. La versión NUNCA se escribe a mano: se deriva de las etiquetas vX.Y.Z y de
# los commits convencionales que las siguen.
#
# REGLAS
#
# 1. Versión inicial. Si el repositorio no tiene ninguna etiqueta vX.Y.Z, la versión
#    es INITIAL_VERSION (0.1.0).
#
# 2. Salto. Con etiqueta base —la más alta por semver, NO por fecha— se analizan los
#    commits desde ella (git log <etiqueta>..HEAD):
#      - `tipo!:` en el asunto (p. ej. `feat!:`) o pie `BREAKING CHANGE:` /
#        `BREAKING-CHANGE:` → salto MAYOR. Excepción (regla semver para 0.x): si el
#        mayor actual es 0, un cambio incompatible salta el MENOR, porque en 0.x la
#        API todavía no es estable y un salto a 1.0.0 debe ser una decisión tomada a
#        mano (creando la etiqueta), no una consecuencia automática de un commit.
#      - si no hay breaking, cualquier `feat:` → salto MENOR.
#      - si no, cualquier `fix:` → salto de PARCHE.
#      - si solo hay `docs:`, `chore:`, `ci:`, `style:`, `refactor:`, `test:` (o
#        commits no convencionales) → salto de PARCHE.
#    El breaking gana al feat y el feat al fix: se evalúa el CONJUTO de commits desde
#    la etiqueta, no el último. Una etiqueta apuntando a HEAD (ningún commit nuevo)
#    cuenta como parche.
#
# 3. Componentes. Se imprimen sin ceros a la izquierda y cada uno debe ser < 1000
#    (MAX_COMPONENT). Si no lo es, el script falla con un mensaje claro por stderr y
#    código distinto de 0: nunca imprime una versión ambigua. El límite de 1000 es el
#    mismo que usa versionCode (major×1.000.000 + minor×1.000 + patch, sin colisiones
#    entre versiones distintas).
#
# SALIDA
#
# Por stdout sale ÚNICAMENTE la versión, para poder encadenarla:
#
#     VERSION=$(scripts/next-version.sh)
#
# Todo lo demás (avisos, errores) va por stderr.
#
# USO
#
#     scripts/next-version.sh            imprime la siguiente versionName (X.Y.Z)
#     scripts/next-version.sh --code     imprime el versionCode de esa misma versión
#
# REQUISITOS
#
# git y bash; nada más. El script se ejecuta desde la carpeta del repositorio.
#
# Clones shallow (GitHub Actions con fetch-depth: 1): las etiquetas pueden no estar
# descargadas. El workflow de release ejecuta `git fetch --tags --force` antes de
# llamar a este script, y el script también lo intenta una vez por su cuenta si no ve
# ninguna etiqueta vX.Y.Z. Sin las etiquetas no se puede calcular el salto: si algo
# impide leer el historial, el script falla en lugar de inventar una versión.
#
# Solo cuentan las etiquetas con el formato exacto vX.Y.Z (se admiten ceros a la
# izquierda: v1.08.3 se normaliza a 1.8.3). Etiquetas de otro tipo (p. ej. `latest`)
# y versiones con prerelease (v1.2.3-rc1) se ignoran. La etiqueta base se elige por
# semver aunque su commit no sea ancestro de HEAD.

set -euo pipefail

readonly INITIAL_VERSION="0.1.0"   # primera versión de la app; se usa sin etiquetas
readonly MAX_COMPONENT=1000        # cada componente debe ser menor (límite de versionCode)

readonly TAG_RE='^v([0-9]+)\.([0-9]+)\.([0-9]+)$'
readonly SUBJECT_RE='^[a-z]+(\([^)]*\))?!:'      # asunto convencional con !: → breaking
readonly FEAT_RE='^feat(\([^)]*\))?!?:'          # asunto convencional de feature
readonly BREAKING_FOOTER_RE='^BREAKING[ -]CHANGE:'  # pie de commit → breaking

# --- Utilidades -----------------------------------------------------------------

err() { printf '%s\n' "$*" >&2; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

usage() {
  err "Uso: $(basename "$0") [--code]"
  err "  (sin argumentos)  imprime la siguiente versionName, p. ej. 0.2.0"
  err "  --code            imprime el versionCode de esa misma versión"
}

mode="version"
case "${1:-}" in
  "") ;;
  --code) mode="code" ;;
  -h|--help) usage; exit 0 ;;
  *) printf 'ERROR: argumento no reconocido: %s\n' "$1" >&2; usage; exit 2 ;;
esac

# validate_component <dígitos> <contexto>: valor normalizado (sin ceros a la
# izquierda) por stdout, o muere si es >= MAX_COMPONENT o no es un número válido.
validate_component() {
  local digits="$1" where="$2"
  if [ "${#digits}" -gt 18 ] || [ "$(( 10#$digits ))" -ge "$MAX_COMPONENT" ]; then
    die "El componente \"$digits\" de $where debe ser un número menor que $MAX_COMPONENT (límite del versionCode: major*1000000 + minor*1000 + patch)."
  fi
  printf '%s' "$(( 10#$digits ))"
}

# --- 1. Etiqueta base: la más alta por semver (no por fecha) ---------------------

tags_output="$(git tag -l 2>/dev/null || true)"

# Clone shallow: si no se ve ninguna etiqueta vX.Y.Z se reintenta una vez tras
# `git fetch --tags --force` (best effort: puede no haber remoto).
if ! printf '%s\n' "$tags_output" | grep -Eq "$TAG_RE"; then
  git fetch --tags --force >/dev/null 2>&1 || true
  tags_output="$(git tag -l 2>/dev/null || true)"
fi

base_tag=""
base_major=0
base_minor=0
base_patch=0
best_key=-1

while IFS= read -r tag; do
  [ -n "$tag" ] || continue
  [[ "$tag" =~ $TAG_RE ]] || continue
  major="$(validate_component "${BASH_REMATCH[1]}" "la etiqueta \"$tag\"")"
  minor="$(validate_component "${BASH_REMATCH[2]}" "la etiqueta \"$tag\"")"
  patch="$(validate_component "${BASH_REMATCH[3]}" "la etiqueta \"$tag\"")"
  # Con cada componente < 1000 esta clave ordena por semver sin colisiones.
  key=$(( major * 1000000 + minor * 1000 + patch ))
  if [ "$key" -gt "$best_key" ]; then
    best_key="$key"
    base_tag="$tag"
    base_major="$major"
    base_minor="$minor"
    base_patch="$patch"
  fi
done <<< "$tags_output"

# Sin ninguna etiqueta vX.Y.Z: la versión sigue siendo la inicial (0.1.0).
if [ -z "$base_tag" ]; then
  if [ "$mode" = "code" ]; then
    initial_major="${INITIAL_VERSION%%.*}"
    initial_rest="${INITIAL_VERSION#*.}"
    initial_minor="${initial_rest%%.*}"
    initial_patch="${initial_rest#*.}"
    printf '%s\n' "$(( initial_major * 1000000 + initial_minor * 1000 + initial_patch ))"
  else
    printf '%s\n' "$INITIAL_VERSION"
  fi
  exit 0
fi

# --- 2. Salto según los commits convencionales desde la etiqueta base ------------

# Un registro por commit separado por 0x1e; 0x1f no se usa (%B puede contener de todo).
if ! log_output="$(git log --format='%B%x1e' "$base_tag..HEAD" 2>/dev/null)"; then
  die "No se pudo leer el historial desde la etiqueta \"$base_tag\" (git log $base_tag..HEAD). ¿Clon shallow sin las etiquetas? Ejecuta: git fetch --tags --force"
fi

breaking=0
has_feat=0
while IFS= read -r -d $'\x1e' msg; do
  if printf '%s\n' "$msg" | grep -Eq "$SUBJECT_RE"; then
    breaking=1
  fi
  if printf '%s\n' "$msg" | grep -Eq "$BREAKING_FOOTER_RE"; then
    breaking=1
  fi
  if printf '%s\n' "$msg" | grep -Eq "$FEAT_RE"; then
    has_feat=1
  fi
done <<< "$log_output"

if [ "$breaking" -eq 1 ]; then
  if [ "$base_major" -eq 0 ]; then
    bump="minor"   # 0.x: un breaking no salta a 1.0.0 por sí solo
  else
    bump="major"
  fi
elif [ "$has_feat" -eq 1 ]; then
  bump="minor"
else
  bump="patch"
fi

next_major="$base_major"
next_minor="$base_minor"
next_patch="$base_patch"
case "$bump" in
  major) next_major=$(( base_major + 1 )); next_minor=0; next_patch=0 ;;
  minor) next_minor=$(( base_minor + 1 )); next_patch=0 ;;
  patch) next_patch=$(( base_patch + 1 )) ;;
esac

for component in "$next_major" "$next_minor" "$next_patch"; do
  if [ "$component" -ge "$MAX_COMPONENT" ]; then
    die "La versión resultante ($next_major.$next_minor.$next_patch) tiene el componente \"$component\", que debe ser menor que $MAX_COMPONENT: el versionCode dejaría de ser único."
  fi
done

if [ "$mode" = "code" ]; then
  printf '%s\n' "$(( next_major * 1000000 + next_minor * 1000 + next_patch ))"
else
  printf '%s.%s.%s\n' "$next_major" "$next_minor" "$next_patch"
fi
