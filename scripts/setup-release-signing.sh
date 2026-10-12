#!/usr/bin/env bash
#
# setup-release-signing.sh — asistente de firma de releases de uSipipo VPN.
#
# Genera un keystore de release con keytool, lo convierte a base64 y guarda los cuatro
# secretos del repositorio con `gh secret set`. Ninguna contraseña se muestra por
# pantalla, se guarda en el historial del shell ni viaja por argv: keytool las lee de
# variables de entorno (-storepass:env / -keypass:env) y gh las recibe por stdin.
#
# Uso:
#   scripts/setup-release-signing.sh                 generar keystore + subir secretos
#   scripts/setup-release-signing.sh --solo-secretos re-subir los secretos desde un
#                                                   keystore ya existente (recuperación
#                                                   si algo falló a mitad, p. ej. un
#                                                   gh secret set que no llegó a guardarse)
#
# Requisitos: keytool (viene con el JDK 17) y gh (CLI de GitHub) con sesión iniciada.
# Se ejecuta desde la carpeta del repositorio.
#
# El keystore generado es un .jks: ya está cubierto por *.jks en .gitignore. Guarda
# copia de seguridad del fichero y de sus contraseñas: sin ellos no podrás publicar
# actualizaciones firmadas con el mismo certificado.

set -euo pipefail

readonly DNAME="CN=uSipipo VPN, O=usipipo, C=ES"
readonly DEFAULT_STORE="$HOME/usipipo-release.jks"
readonly DEFAULT_ALIAS="release"
# Nombres de las variables de entorno que leerá keytool con :env (nunca por argv).
readonly ENV_STORE_PASS="USIPIPO_STORE_PASS"
readonly ENV_KEY_PASS="USIPIPO_KEY_PASS"

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

usage() {
  say "Uso: $0 [--solo-secretos]"
  say ""
  say "  --solo-secretos   No genera keystore: reutiliza uno existente y vuelve a subir"
  say "                   los cuatro secretos a GitHub. Sirve para recuperarse de un"
  say "                   fallo parcial (por ejemplo, un gh secret set que falló a mitad"
  say "                   dejaba el asistente bloqueado en el «ya existe el keystore»)."
  say "  -h, --help        Muestra esta ayuda."
}

# --- 0. Argumentos -------------------------------------------------------------

ONLY_SECRETS=0
for arg in "$@"; do
  case "$arg" in
    --solo-secretos) ONLY_SECRETS=1 ;;
    -h|--help) usage; exit 0 ;;
    "")
      ;;
    *)
      printf 'ERROR: argumento no reconocido: %s\n' "$arg" >&2
      usage >&2
      exit 2
      ;;
  esac
done

say "=== Asistente de firma de releases de uSipipo VPN ==="
say ""
if [ "$ONLY_SECRETS" -eq 1 ]; then
  say "Modo --solo-secretos: se reutiliza un keystore existente y se vuelven a subir los"
  say "secretos a GitHub. No se genera ningún keystore nuevo."
else
  say "Vas a generar un keystore de release y a guardar sus datos como secretos de GitHub."
  say "El keystore no se sube al repositorio; solo se guarda en tu máquina y en GitHub"
  say "como secreto (en base64)."
fi
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

if [ "$ONLY_SECRETS" -eq 1 ]; then
  if [ ! -f "$STORE" ]; then
    die "No existe un keystore en '$STORE'. Ejecuta el asistente sin --solo-secretos para generarlo."
  fi
else
  if [ -e "$STORE" ]; then
    say ""
    say "Ya existe un keystore en: $STORE"
    say "No se ha generado uno nuevo ni se han tocado los secretos."
    say "Si quieres empezar de cero, borra ese fichero (¡y todas sus copias!) o indica"
    say "otra ruta. Ten en cuenta que las actualizaciones deben firmarse con el mismo"
    say "certificado que la primera release publicada."
    say "Si solo quieres volver a subir los secretos de ese keystore, usa --solo-secretos."
    exit 0
  fi
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

# --- 4. Generar el keystore (salvo en modo --solo-secretos) --------------------

# Las contraseñas se pasan a keytool por variables de entorno (-storepass:env /
# -keypass:env), nunca por argv, para que no sean visibles en ps.
export "$ENV_STORE_PASS=$STORE_PASS"
export "$ENV_KEY_PASS=$KEY_PASS"

if [ "$ONLY_SECRETS" -eq 0 ]; then
  say ""
  say "Generando el keystore (RSA 2048, válido 10000 días) en:"
  say "  $STORE"
  say "Certificado: $DNAME"

  mkdir -p "$(dirname "$STORE")"
  keytool -genkeypair -v \
    -keystore "$STORE" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass:env "$ENV_STORE_PASS" \
    -keypass:env "$ENV_KEY_PASS" \
    -dname "$DNAME" >/dev/null || die "keytool no pudo generar el keystore. No se ha tocado nada."
else
  say ""
  say "Usando el keystore existente en: $STORE"
fi

keytool -list -v -keystore "$STORE" -alias "$ALIAS" -storepass:env "$ENV_STORE_PASS" >/dev/null \
  || die "No se puede leer el keystore con esa contraseña. No se ha tocado nada."

say "Keystore verificado."

# Las contraseñas ya no hacen falta como variables de entorno.
unset "$ENV_STORE_PASS" "$ENV_KEY_PASS"

# --- 5. Base64 y secretos -------------------------------------------------------

say ""
say "Convirtiendo el keystore a base64 y guardando los secretos en GitHub..."

TMP_B64=$(mktemp "${TMPDIR:-/tmp}/usipipo-release-jks.XXXXXX")
if base64 -w0 </dev/null >/dev/null 2>&1; then
  base64 -w0 "$STORE" > "$TMP_B64"
else
  # macOS/BSD: base64 no acepta -w0; se quitan los saltos de línea.
  base64 "$STORE" | tr -d '\n' > "$TMP_B64"
fi
[ -s "$TMP_B64" ] || die "La conversión a base64 falló."

set_secret() {
  local name="$1"
  if ! gh secret set "$name" --repo "$REPO"; then
    die "No se pudo guardar el secreto $name. Vuelve a ejecutar el asistente con --solo-secretos."
  fi
}

# Los secretos se envían por stdin: la contraseña no queda en el historial del shell.
gh secret set ANDROID_KEYSTORE_BASE64 --repo "$REPO" < "$TMP_B64" \
  || die "No se pudo guardar el secreto ANDROID_KEYSTORE_BASE64. Vuelve a ejecutar el asistente con --solo-secretos."
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
say "     GitHub Actions compilará la APK release firmada, verificará la firma con"
say "     apksigner y la publicará en Releases (https://github.com/$REPO/releases)"
say "     con las notas generadas automáticamente."
