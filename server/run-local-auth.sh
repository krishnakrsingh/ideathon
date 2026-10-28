#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
auth_dir="$server_dir/auth"

if [[ ! -d "$auth_dir/node_modules" ]]; then
  echo "Install Better Auth dependencies first: cd server/auth && npm install" >&2
  exit 1
fi

if [[ ! -f "$server_dir/.env.local" ]]; then
  umask 077
  secret="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
  printf 'SWASTHAI_ENV=local\nBETTER_AUTH_URL=http://127.0.0.1:8766\nBETTER_AUTH_SECRET=%s\nSWASTHAI_AUTH_MODE=oidc\nSWASTHAI_AUTH_JWKS_URL=http://127.0.0.1:8766/api/auth/jwks\nSWASTHAI_AUTH_ISSUER=http://127.0.0.1:8766\nSWASTHAI_AUTH_AUDIENCE=http://127.0.0.1:8766\n' "$secret" > "$server_dir/.env.local"
elif ! grep -q '^BETTER_AUTH_SECRET=' "$server_dir/.env.local"; then
  umask 077
  secret="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
  printf '\nBETTER_AUTH_URL=http://127.0.0.1:8766\nBETTER_AUTH_SECRET=%s\nSWASTHAI_AUTH_MODE=oidc\nSWASTHAI_AUTH_JWKS_URL=http://127.0.0.1:8766/api/auth/jwks\nSWASTHAI_AUTH_ISSUER=http://127.0.0.1:8766\nSWASTHAI_AUTH_AUDIENCE=http://127.0.0.1:8766\n' "$secret" >> "$server_dir/.env.local"
fi

set -a
. "$server_dir/.env.local"
set +a

cd "$auth_dir"
exec node src/server.mjs
