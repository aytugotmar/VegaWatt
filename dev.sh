#!/usr/bin/env bash
# Starts the full VegaWatt stack via Docker Compose
set -euo pipefail

cd "$(dirname "$0")"
echo "==> Starting VegaWatt Stack with Docker..."
docker compose up -d --build
echo "==> Stack is running!"
echo "Frontend:   http://localhost:5173"
echo "Backend:    http://localhost:8080"
echo "Swagger:    http://localhost:8080/swagger-ui.html"
echo "Mailpit:    http://localhost:8025"
