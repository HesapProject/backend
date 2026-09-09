#!/usr/bin/env bash
# Payme `ac.companyId` xatosi davrida yaratilgan (hech qachon to'lana olmaydigan)
# PENDING paket buyurtmalarini CANCELLED qiladi. Tuzatish deploy'i 2026-08-29
# 10:24:39Z da tugagan — faqat undan OLDINGI qatorlar tegiladi, keyingilari
# (yangi `ac.id` linki bilan) o'z holicha qoladi.
# [apply] marker bo'lsa yozadi; aks holda faqat PREVIEW.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

docker run --rm -i --network hesap-postgres -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 -v apply="$APPLY" <<'PSQL'
\set CUTOFF '2026-08-29 10:24:39'

\echo '=== HOZIRGI holat ==='
SELECT status, count(*) FROM integration.payment_order GROUP BY 1 ORDER BY 1;

\echo '=== Bekor qilinadigan (PENDING, deploy oldidan) ==='
SELECT count(*) FROM integration.payment_order
WHERE status='PENDING' AND created_date < CAST(:'CUTOFF' AS timestamp);

\echo '=== Tegilmaydi (PENDING, deploy dan keyin) ==='
SELECT count(*) FROM integration.payment_order
WHERE status='PENDING' AND created_date >= CAST(:'CUTOFF' AS timestamp);

\if :apply
\echo '>>> APPLY: CANCELLED'
UPDATE integration.payment_order
SET status='CANCELLED'
WHERE status='PENDING' AND created_date < CAST(:'CUTOFF' AS timestamp);

\echo '=== YAKUNIY holat ==='
SELECT status, count(*) FROM integration.payment_order GROUP BY 1 ORDER BY 1;
\else
\echo 'PREVIEW — yozilmadi.'
\endif
PSQL
