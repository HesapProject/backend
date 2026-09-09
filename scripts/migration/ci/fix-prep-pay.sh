#!/usr/bin/env bash
# Eski tizim bug'i: shartnoma prepare'dan yaratilganda to'lov jadvali
# payment.contract_id = PREPARE_ID bilan yozilgan (contract.id emas).
# Natijada migratsiyada shartnomalar bo'sh yoki BEGONA (id qayta ishlatilgan
# eski) jadval bilan ko'chgan. Bu skript:
#   1) trust'dan haqiqiy (prepare_id ostidagi, shartnoma bilan bir vaqtda
#      yaratilgan) to'lovlarni va begona (shartnomadan >1 soat oldin yaratilgan)
#      to'lovlarni eksport qiladi;
#   2) hesap'da 10-fix-prepare-payments.sql bilan PREVIEW/APPLY.
# [apply] marker bo'lsa yozadi; aks holda faqat PREVIEW. Idempotent
# (deterministik uuid + ON CONFLICT).
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

MIG=/tmp/fix-prep-pay
rm -rf "$MIG"; mkdir -p "$MIG"; chmod 777 "$MIG"

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

oldpsql() {
  docker run --rm -i --network app_net -v "$MIG":/mig -e PGPASSWORD="$OP" postgres:16-alpine \
    psql -h pg_prod -U postgres -d trust "$@"
}
newpsql() {
  docker run --rm -i --network hesap-postgres -v "$MIG":/mig -v "$CI_PROJECT_DIR/scripts/migration":/sql \
    -w /mig -e PGPASSWORD="$NP" postgres:16-alpine \
    psql -h postgres -p 5435 -U "$NU" -d hesap "$@"
}

echo "=== EXPORT (eski trust) ==="
oldpsql -v ON_ERROR_STOP=1 <<'PSQL'
\copy (SELECT c.id AS old_contract_id, p.id AS old_payment_id, p.arrangement_at, p.is_paid, p.paid_at, p.price, p.paid, p.status, p.currency_id, p.created_at, EXISTS(SELECT 1 FROM public.payment po WHERE po.contract_id=c.id AND po.created_at BETWEEN c.created_at - interval '2 minutes' AND c.created_at + interval '15 minutes') AS has_own FROM public.contract c JOIN public.payment p ON p.contract_id = c.prepare_id WHERE c.prepare_id IS NOT NULL AND p.created_at BETWEEN c.created_at - interval '2 minutes' AND c.created_at + interval '15 minutes') TO '/mig/fix_real.csv' CSV HEADER
\copy (SELECT p.id AS old_payment_id, c.id AS old_contract_id FROM public.contract c JOIN public.payment p ON p.contract_id = c.id WHERE c.prepare_id IS NOT NULL AND EXISTS (SELECT 1 FROM public.payment pp WHERE pp.contract_id = c.prepare_id AND pp.created_at BETWEEN c.created_at - interval '2 minutes' AND c.created_at + interval '15 minutes') AND p.created_at < c.created_at - interval '1 hour') TO '/mig/fix_foreign.csv' CSV HEADER
PSQL
wc -l "$MIG"/fix_real.csv "$MIG"/fix_foreign.csv

echo "=== HESAP (preview/apply) ==="
newpsql -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/10-fix-prepare-payments.sql
