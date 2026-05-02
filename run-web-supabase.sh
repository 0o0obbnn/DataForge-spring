#!/bin/bash
set -euo pipefail

if [ ! -f ".env" ]; then
  echo "Missing .env. Copy .env.example -> .env and fill DATASOURCE_PASSWORD and JWT_SECRET."
  exit 1
fi

# Load .env (simple KEY=VALUE lines)
set -a
source .env
set +a

cd data-forge-web
mvn spring-boot:run

