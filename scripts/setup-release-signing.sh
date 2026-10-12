#!/usr/bin/env bash
#
# setup-release-signing.sh — asistente de firma de releases de uSipipo VPN.
#
# Genera un keystore de release con keytool, lo convierte a base64 y guarda los cuatro
# secretos del repositorio con `gh secret set`. Ninguna contraseña se muestra por
# pantalla ni pasa por argv: se envían a GitHub por stdin.
#
# Requisitos: keytool (viene con el JDK 17) y gh (CLI de GitHub) con sesión iniciada.
# Uso:        bash scripts/setup-release-signing.sh   (desde la carpeta del repositorio)
#
# El keystore generado es un .jks: ya está cubierto por *.jks en .gitignore. Guarda
# copia de seguridad del fichero y de sus contraseñas: sin ellos no podrás publicar
# actualizaciones firmadas con el mismo certificado.

set -euo pipefail

readonly DNAME="CN=uSipipo VPN, O=usipipo, C=ES"
readonly DEFAULT_STORE="$HOME/usipipo-release.jks"
readonly DEFAULT_ALIAS="release"

# Único fichero temporal: el base64 del keystore. Se borra siempre al salir.
TMP_B64=""
cleanup() {
  if [ -n "$TMP_B64" ] && [ -f "$TMP_B64" ]; then
    rm -f "$TMP_B64"
  fi
}
trap cleanup EXIT

say() { printf '%s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

say "=== Asistente de firma de releases de uSipipo VPN ==="
say ""
say "Vas a generar un keystore de release y a guardar sus datos como secretos de GitHub."
say "El keystore no se sube al repositorio; solo se guarda en tu máquina y en GitHub"
say "como secreto (en base64)."
say ""

# --- 1. Comprobar herramientas ------------------------------------------------

missing=()
command -v keytool >/dev/null 2>&1 || missing+=("keytool")
command -v gh >/dev/null 2>&1 || missing+=("gh")

if [ "${#missing[@]}" -gt 0 ]; then
  say "Faltan herramientas necesarias:"
  for tool in "${missing[@]}"; do
    case "$tool" in
      keytool)
        say "  - keytool: forma parte del JDK (Temurin/Adoptium 17 o posterior). Instala un"
        say "    JDK o añade su carpeta bin al PATH."
        ;;
      gh)
        say "  - gh: CLI de GitHub (https://cli.github.com). Tras instalarlo ejecuta:"
        say "    gh auth login"
        ;;
    esac
  done
  die "Instala lo que falta y vuelve a ejecutar el asistente."
fi

if ! gh auth status >/dev/null 2>&1; then
  die "gh no tiene sesión iniciada. Ejecuta: gh auth login"
fi

REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner 2>/dev/null || true)
if [ -z "$REPO" ]; then
  die "No se pudo detectar el repositorio. Ejecuta el asistente desde la carpeta del repositorio."
fi
say "Repositorio: $REPO"
say ""

# --- 2. Preguntar ruta del keystore y alias -----------------------------------

read -rp "Ruta del keystore [$DEFAULT_STORE]: " STORE
STORE="${STORE:-$DEFAULT_STORE}"

read -rp "Alias de la clave [$DEFAULT_ALIAS]: " ALIAS
ALIAS="${ALIAS:-$DEFAULT_ALIAS}"

if [ -e "$STORE" ]; then
  say ""
  say "Ya existe un keystore en: $STORE"
  say "No se ha generado uno nuevo ni se han tocado los secretos."
  say "Si quieres empezar de cero, borra ese fichero (¡y todas sus copias!) o indica"
  say "otra ruta. Ten en cuenta que las actualizaciones deben firmarse con el mismo"
  say "certificado que la primera release publicada."
  exit 0
fi

# --- 3. Contraseñas (nunca se muestran) ---------------------------------------

say ""
say "Contraseñas: no se mostrarán mientras las escribes."
while true; do
  read -rsp "Contraseña del keystore: " STORE_PASS
  printf '\n'
  if [ -z "$STORE_PASS" ]; then
    say "La contraseña no puede estar vacía."
    continue
  fi
  read -rsp "Repite la contraseña del keystore: " STORE_PASS2
  printf '\n'
  if [ "$STORE_PASS" = "$STORE_PASS2" ]; then
    break
  fi
  say "Las contraseñas no coinciden. Inténtalo de nuevo."
done

say ""
say "Pulsa Enter para usar la misma contraseña para la clave."
while true; do
  read -rsp "Contraseña de la clave: " KEY_PASS
  printf '\n'
  if [ -z "$KEY_PASS" ]; then
    KEY_PASS="$STORE_PASS"
    break
  fi
  read -rsp "Repite la contraseña de la clave: " KEY_PASS2
  printf '\n'
  if [ "$KEY_PASS" = "$KEY_PASS2" ]; then
    break
  fi
  say "Las contraseñas no coinciden. Inténtalo de nuevo."
done
unset STORE_PASS2 KEY_PASS2

# --- 4. Generar el keystore ----------------------------------------------------

say ""
say "Generando el keystore (RSA 2048, válido 10000 días) en:"
say "  $STORE"
say "Certificado: $DNAME"

mkdir -p "$(dirname "$STORE")"
keytool -genkeypair -v \
  -keystore "$STORE" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
  -dname "$DNAME" >/dev/null || die "keytool no pudo generar el keystore. No se ha tocado nada."

keytool -list -v -keystore "$STORE" -storepass "$STORE_PASS" -alias "$ALIAS" >/dev/null \
  || die "El keystore se generó pero no se puede leer con esa contraseña. No se ha tocado nada."

say "Keystore generado y verificado."

# --- 5. Base64 y secretos -------------------------------------------------------

say ""
say "Convirtiendo el keystore a base64 y guardando los secretos en GitHub..."

TMP_B64=$(mktemp "${TMPDIR:-/tmp}/usipipo-release-jks.XXXXXX")
if base64 -w0 </dev/null >/dev/null 2>&1; then
  base64 -w0 "$STORE" > "$TMP_B64"
else
  # macOS/BSD: base64 no acepta -w0; se quitan los salt de línea.
  base64 "$STORE" | tr -d '\n' > "$TMP_B64"
fi
[ -s "$TMP_B64" ] || die "La conversión a base64 falló."

set_secret() {
  local name="$1"
  if ! gh secret set "$name" --repo "$REPO"; then
    die "No se pudo guardar el secreto $name. Vuelve a ejecutar el asistente."
  fi
}

# Los secretos se envían por stdin: la contraseña no queda en el historial del shell.
gh secret set ANDROID_KEYSTORE_BASE64 --repo "$REPO" < "$TMP_B64" \
  || die "No se pudo guardar el secreto ANDROID_KEYSTORE_BASE64. Vuelve a ejecutar el asistente."
printf '%s' "$STORE_PASS" | set_secret ANDROID_KEYSTORE_PASSWORD
printf '%s' "$ALIAS" | set_secret ANDROID_KEY_ALIAS
printf '%s' "$KEY_PASS" | set_secret ANDROID_KEY_PASSWORD

say "  - ANDROID_KEYSTORE_BASE64"
say "  - ANDROID_KEYSTORE_PASSWORD"
say "  - ANDROID_KEY_ALIAS"
say "  - ANDROID_KEY_PASSWORD"

# --- 6. Despedida y siguientes pasos -------------------------------------------

say ""
say "Listo. Pasos siguientes:"
say ""
say "  1. Guarda una copia de seguridad del keystore ($STORE) y de sus contraseñas"
say "     en un lugar seguro (gestor de contraseñas, copia cifrada...). Si se pierden,"
say "     no podrás publicar actualizaciones instalables encima de las anteriores."
say ""
say "  2. Crea y sube la etiqueta de la versión, por ejemplo:"
say ""
say "       git tag v0.2.0 && git push origin v0.2.0"
say ""
say "     GitHub Actions compilará la APK release firmada y la publicará en Releases"
say "     (https://github.com/$REPO/releases) con las notas generadas automáticamente."
