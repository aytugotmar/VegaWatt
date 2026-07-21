#!/usr/bin/env bash
# Starts the full local VegaWatt stack: backend infra + apps in Docker, frontend
# as a live-reloading Vite dev server on the host.
#
# Usage: ./dev.sh
set -euo pipefail

cd "$(dirname "$0")"

echo "==> Starting backend infrastructure and services (Postgres, Kafka, Ignite, Mailpit, Core, Sensors)..."
docker compose up -d --build postgres kafka ignite mailpit vegawatt-core vegawatt-telemetry-sensors

echo "==> Waiting for vegawatt-core to respond..."
for _ in $(seq 1 30); do
  # /api/v1/homes/live now requires auth (401 without a token) — any HTTP
  # response at all means the server is up, so don't use curl -f here.
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/homes/live 2>/dev/null || echo "000")
  if [ "$status" != "000" ]; then
    echo "    backend is up."
    break
  fi
  sleep 2
done

echo "==> Starting frontend dev server (vegawatt-web)..."
cd vegawatt-web

export NVM_DIR="$HOME/.nvm"
if [ -s "$NVM_DIR/nvm.sh" ]; then
  # nvm.sh relies on internal commands returning non-zero as normal control
  # flow, which trips `set -e` — relax it just for sourcing/using nvm.
  set +e
  # shellcheck disable=SC1091
  source "$NVM_DIR/nvm.sh"
  nvm use
  set -e
fi

if [ ! -d node_modules ]; then
  npm install
fi

echo ""
echo "Backend:      http://localhost:8080"
echo "Swagger UI:   http://localhost:8080/swagger-ui.html"
echo "Mailpit:      http://localhost:8025"
echo "Frontend:     http://localhost:5173"
echo ""
echo "(Note: this runs the frontend as a live-reloading dev server, not the"
echo " Dockerized nginx build — the 'vegawatt-web' Docker service is intentionally"
echo " left out to avoid a port 5173 conflict. Run"
echo " 'docker compose up -d --build vegawatt-web' separately if you want the"
echo " production-style container instead, after stopping this dev server.)"
echo ""

npm run dev
