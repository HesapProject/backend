#!/usr/bin/env bash
# № 2927 (yoki FIX_NUM) buzuq to'lov jadvalini tuzatish.
# Commit sarlavhasida [apply] bo'lsa YOZADI, aks holda PREVIEW.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
NUM="${FIX_NUM:-2927}"
# Commit sarlavhasida "num=<raqam>" bo'lsa — o'sha shartnoma (per-commit, xavfsiz).
if echo "${CI_COMMIT_TITLE:-}" | grep -qE 'num=[0-9]+'; then
  NUM="$(echo "${CI_COMMIT_TITLE}" | grep -oE 'num=[0-9]+' | head -1 | cut -d= -f2)"
fi
echo "APPLY=$APPLY  NUM=$NUM"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"
docker run --rm -i --network hesap-postgres -v "$CI_PROJECT_DIR/scripts/migration":/sql \
  -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 -v apply="$APPLY" -v num="$NUM" \
  -f /sql/13-fix-contract-2927.sql
