#!/usr/bin/env bash
# Bir martalik SQL diagnostika — scripts/migration/diag.sql ni yangi (hesap) DB'da,
# diag-legacy.sql bo'lsa eski (trust) DB'da o'qiydi. Ikkalasi ham READ-ONLY.
set -uo pipefail
echo "########## YANGI DB (hesap) ##########"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"
docker run --rm --network hesap-postgres \
  -v "$CI_PROJECT_DIR/scripts/migration":/sql \
  -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=0 -f /sql/diag.sql

# Eski trust DB (pg_prod konteyneri, app_net tarmog'i) — talabnoma/da'vo hali shu yerda.
if [ -f "$CI_PROJECT_DIR/scripts/migration/diag-legacy.sql" ]; then
  echo "########## ESKI DB (trust) ##########"
  OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
  if [ -z "$OP" ]; then
    echo "SKIP: pg_prod konteyner topilmadi"
  else
    docker run --rm --network app_net \
      -v "$CI_PROJECT_DIR/scripts/migration":/sql \
      -e PGPASSWORD="$OP" postgres:16-alpine \
      psql -h pg_prod -U postgres -d trust -v ON_ERROR_STOP=0 -f /sql/diag-legacy.sql
  fi
fi
