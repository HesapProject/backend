#!/usr/bin/env bash
# Eski Hesap DB (trust) dan userlarni CSV ga export qiladi.
# Eski jadvalda qaysi ustun bo'lmasa, NULL bo'lib chiqadi (versiyalar farqiga chidamli).
#
# Ishlatish:
#   OLD_DB_URL="postgresql://admin:CHANGE_ME@localhost:15432/trust" ./01-export-old-users.sh [out_dir]
set -euo pipefail

OLD_DB_URL="${OLD_DB_URL:-postgresql://admin@localhost:15432/trust}"
OUT_DIR="${1:-./old-data}"
mkdir -p "$OUT_DIR"

# users jadvalidan kerakli ustunlar; mavjud bo'lmaganlari NULL AS <nom> bo'ladi
USERS_COLS=$(psql "$OLD_DB_URL" -tA <<'SQL'
WITH want(col, ord) AS (VALUES
  ('id', 1), ('username', 2), ('firstname', 3), ('lastname', 4), ('middlename', 5),
  ('is_man', 6), ('bio', 7), ('birthday', 8), ('phone', 9), ('image', 10),
  ('pnfl', 11), ('passport_number', 12), ('type', 13), ('tin', 14), ('inn', 15),
  ('address', 16), ('verified', 17), ('verified_at', 18), ('is_deleted', 19),
  ('created_at', 20), ('test_user', 21)
)
SELECT string_agg(
  CASE WHEN c.column_name IS NOT NULL THEN quote_ident(w.col)
       ELSE 'NULL AS ' || w.col END,
  ', ' ORDER BY w.ord)
FROM want w
LEFT JOIN information_schema.columns c
  ON c.table_schema = 'public' AND c.table_name = 'users' AND c.column_name = w.col;
SQL
)

echo "users ustunlari: $USERS_COLS"
psql "$OLD_DB_URL" -c "\copy (SELECT $USERS_COLS FROM users ORDER BY id) TO '$OUT_DIR/old_users.csv' WITH (FORMAT csv, HEADER true)"

# socials jadvali (bo'lmasa bo'sh CSV)
HAS_SOCIALS=$(psql "$OLD_DB_URL" -tA -c "SELECT to_regclass('public.socials') IS NOT NULL")
if [ "$HAS_SOCIALS" = "t" ]; then
  psql "$OLD_DB_URL" -c "\copy (SELECT user_id, phone2, telegram, instagram, facebook FROM socials ORDER BY user_id) TO '$OUT_DIR/old_socials.csv' WITH (FORMAT csv, HEADER true)"
else
  echo "user_id,phone2,telegram,instagram,facebook" > "$OUT_DIR/old_socials.csv"
  echo "socials jadvali topilmadi — bo'sh CSV yozildi"
fi

echo "Export tugadi:"
wc -l "$OUT_DIR/old_users.csv" "$OUT_DIR/old_socials.csv"
