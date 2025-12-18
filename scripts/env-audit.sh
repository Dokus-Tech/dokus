#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 2
  fi
}

require_cmd rg
require_cmd awk
require_cmd sort
require_cmd comm

ENV_EXAMPLE="deployment/.env.example"

deployment_compose_files=(
  "deployment/docker-compose.pro.yml"
  "deployment/docker-compose.lite.yml"
  "deployment/docker-compose.cloud.yml"
)

deployment_config_files=(
  features/*/backend/src/main/resources/application-pro.conf
  features/*/backend/src/main/resources/application-lite.conf
  features/*/backend/src/main/resources/application-cloud.conf
)

shopt -s nullglob

extract_env_keys_from_env_file() {
  local file="$1"
  awk -F= '/^[A-Za-z_][A-Za-z0-9_]*=/ {print $1}' "$file" | sort -u
}

extract_required_compose_vars() {
  (rg -o --no-filename '[$][{][A-Za-z0-9_]+[}]' "${deployment_compose_files[@]}" || true) \
    | sed 's/^\${//' \
    | sed 's/}$//' \
    | sort -u
}

extract_required_hocon_vars() {
  local files=("$@")
  (rg -o --no-filename '[$][{][A-Za-z0-9_]+[}]' "${files[@]}" || true) \
    | sed 's/^\${//' \
    | sed 's/}$//' \
    | sort -u
}

extract_hocon_env_vars_all() {
  (rg -o --no-filename '[$][{][?]?[A-Za-z0-9_]+' features/**/src/main/resources/*.conf 2>/dev/null || true) \
    | sed 's/^\${//' \
    | sed 's/^?//' \
    | sort -u
}

extract_kotlin_getenv_vars() {
  (rg -o --no-filename 'System[.]getenv[(]\"[A-Za-z0-9_]+\"[)]' -S --glob='*.kt' . 2>/dev/null || true) \
    | sed -E 's/.*\"([A-Za-z0-9_]+)\".*/\1/' \
    | sort -u
}

extract_yaml_env_keys() {
  local file="$1"
  awk '
    function indent_len(line) {
      sub(/[^ ].*$/, "", line)
      return length(line)
    }
    /^[[:space:]]*environment:[[:space:]]*$/ {
      in_env=1
      env_indent=indent_len($0)
      next
    }
    in_env {
      cur_indent=indent_len($0)
      if (cur_indent <= env_indent) {
        in_env=0
        next
      }
      line=$0
      sub(/^[[:space:]]*/, "", line)
      if (line ~ /^<</) next
      if (line ~ /^[A-Z][A-Z0-9_]*[[:space:]]*:/) {
        key=line
        sub(/:.*/, "", key)
        print key
      }
    }
  ' "$file" | sort -u
}

echo "== Env Audit =="

if [ ! -f "$ENV_EXAMPLE" ]; then
  echo "Missing $ENV_EXAMPLE" >&2
  exit 2
fi

env_example_keys="$(extract_env_keys_from_env_file "$ENV_EXAMPLE")"
compose_required_vars="$(extract_required_compose_vars)"
config_required_vars="$(extract_required_hocon_vars "${deployment_config_files[@]}")"

required_vars="$(
  {
    echo "$compose_required_vars"
    echo "$config_required_vars"
  } | sort -u
)"

missing_in_env_example="$(comm -23 <(echo "$required_vars") <(echo "$env_example_keys") || true)"

echo ""
echo "Deployment required vars (compose + application-{pro,lite,cloud}.conf):"
echo "$required_vars" | sed 's/^/  - /'

if [ -n "$missing_in_env_example" ]; then
  echo ""
  echo "ERROR: These required vars are missing from $ENV_EXAMPLE:" >&2
  echo "$missing_in_env_example" | sed 's/^/  - /' >&2
  exit 1
fi

echo ""
echo "OK: $ENV_EXAMPLE contains all required deployment vars."

# Local dev drift check: only validate app-related keys (ignore infra/container-specific keys).
local_compose="docker-compose.local.yml"
if [ -f "$local_compose" ]; then
  local_env_keys="$(extract_yaml_env_keys "$local_compose")"

  local_env_keys_relevant="$(
    (
      echo "$local_env_keys" \
        | rg -N '^(DB_|JWT_|REDIS_|RABBITMQ_|MINIO_|PEPPOL_|AUTH_|SERVER_|CACHE_|CORS_|GEOIP_|EMAIL_|SMTP_|ENCRYPTION_KEY|LOG_|PORT|ENVIRONMENT)' \
        | rg -N -v '^(MINIO_ROOT_|RABBITMQ_DEFAULT_)' \
        || true
    )
  )"

  hocon_env_vars="$(extract_hocon_env_vars_all)"
  kotlin_env_vars="$(extract_kotlin_getenv_vars)"
  known_env_vars="$(
    {
      echo "$hocon_env_vars"
      echo "$kotlin_env_vars"
    } | sort -u
  )"

  unknown_local_env_vars="$(comm -23 <(echo "$local_env_keys_relevant") <(echo "$known_env_vars") || true)"

  if [ -n "$unknown_local_env_vars" ]; then
    echo ""
    echo "WARN: Local compose sets app-like env vars not referenced in config/code (possible typos):" >&2
    echo "$unknown_local_env_vars" | sed 's/^/  - /' >&2
  else
    echo ""
    echo "OK: Local compose app env keys look consistent."
  fi
fi
