#!/usr/bin/env bash
set -uo pipefail
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"
q(){ docker run --rm --network hesap-postgres -e PGPASSWORD="$NP" postgres:16-alpine psql -h postgres -p 5435 -U "$NU" -d hesap -Atc "$1" 2>&1; }
echo "=== 1) eski lock (18 kunlik) bo'shatish ==="
q "UPDATE \"user\".databasechangeloglock SET locked=false, lockgranted=NULL, lockedby=NULL WHERE id=1"
q "SELECT id, locked FROM \"user\".databasechangeloglock"
echo "=== 2) main-service restart (Liquibase kutilayotgan migratsiyalarni qo'llaydi) ==="
docker restart main-service >/dev/null 2>&1 && echo "restarted"
echo "=== 3) boot + migratsiyalarni kutish (200s) ==="
sleep 200
echo "=== 4) hesap_score jadvali ==="
q "SELECT to_regclass('\"user\".hesap_score')"
echo "=== 5) oxirgi qo'llanilgan changeset'lar ==="
q "SELECT id FROM \"user\".databasechangelog ORDER BY orderexecuted DESC LIMIT 4"
echo "=== 6) main Started bo'ldimi? ==="
docker logs main-service --tail 40 2>&1 | grep -iE 'Started MainApplication|APPLICATION FAILED|ERROR .*Liquibase|Exception' | tail -6 || echo "(hali boot)"
echo "=== 7) recompute (butun oqim) ==="
docker run --rm --network container:main-service curlimages/curl -s -m 30 -X POST "http://localhost:8001/main/v1/local/hesap-score/recompute" -H "Content-Type: application/json" -d '{"userIn":"33008860100043"}' 2>&1 || echo "(curl xato)"
