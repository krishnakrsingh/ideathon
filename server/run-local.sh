#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

if [[ ! -x .venv/bin/uvicorn ]]; then
  echo "Install the local API first: python3 -m venv .venv && . .venv/bin/activate && python -m pip install -e ." >&2
  exit 1
fi

if [[ ! -f .env.local ]]; then
  umask 077
  secret="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
  printf 'SWASTHAI_ENV=local\nBETTER_AUTH_URL=http://127.0.0.1:8766\nBETTER_AUTH_SECRET=%s\nSWASTHAI_AUTH_MODE=oidc\nSWASTHAI_AUTH_JWKS_URL=http://127.0.0.1:8766/api/auth/jwks\nSWASTHAI_AUTH_ISSUER=http://127.0.0.1:8766\nSWASTHAI_AUTH_AUDIENCE=http://127.0.0.1:8766\n' "$secret" > .env.local
elif ! grep -q '^BETTER_AUTH_SECRET=' .env.local; then
  umask 077
  secret="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
  printf '\nBETTER_AUTH_URL=http://127.0.0.1:8766\nBETTER_AUTH_SECRET=%s\nSWASTHAI_AUTH_MODE=oidc\nSWASTHAI_AUTH_JWKS_URL=http://127.0.0.1:8766/api/auth/jwks\nSWASTHAI_AUTH_ISSUER=http://127.0.0.1:8766\nSWASTHAI_AUTH_AUDIENCE=http://127.0.0.1:8766\n' "$secret" >> .env.local
fi

set -a
. ./.env.local
set +a

export SWASTHAI_AUTH_MODE="${SWASTHAI_AUTH_MODE:-oidc}"
export SWASTHAI_AUTH_JWKS_URL="${SWASTHAI_AUTH_JWKS_URL:-${BETTER_AUTH_URL%/}/api/auth/jwks}"
export SWASTHAI_AUTH_ISSUER="${SWASTHAI_AUTH_ISSUER:-$BETTER_AUTH_URL}"
export SWASTHAI_AUTH_AUDIENCE="${SWASTHAI_AUTH_AUDIENCE:-$BETTER_AUTH_URL}"

exec .venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8765 --no-access-log
