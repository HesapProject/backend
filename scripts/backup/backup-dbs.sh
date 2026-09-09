#!/usr/bin/env bash
# Prod bazalarni zaxiralash — [backup-db] commit gate orqali ishga tushadi.
# Ikkala baza: yangi `hesap` (postgres:5435, hesap-postgres tarmog'i) va eski `trust` (pg_prod, app_net).
# Dumplar serverda $OUT_SRV da saqlanadi; kichiklari CI artifact sifatida ham beriladi.
set -uo pipefail

OUT_SRV="${BACKUP_DIR:-$HOME/backups/hesap}"   # runner yoza oladigan katalog
OUT_ART="$CI_PROJECT_DIR/backup"
# GitLab instansiyasi artifact limiti past (210 MB da ham 413 Payload Too Large).
# 0 = artifact umuman qilinmaydi, dumplar faqat serverda qoladi (scp bilan olinadi).
ART_LIMIT_MB=${ART_LIMIT_MB:-0}
MIN_FREE_GB=30            # dump uchun zarur minimal bo'sh joy
TS="$(date +%Y%m%d-%H%M)"

mkdir -p "$OUT_SRV" "$OUT_ART" || { echo "ERROR: katalog yaratilmadi"; exit 1; }

echo "=== disk holati ==="
df -h "$OUT_SRV" | tail -1
FREE_GB="$(df -BG --output=avail "$OUT_SRV" 2>/dev/null | tail -1 | tr -dc '0-9')"
if [ "${FREE_GB:-0}" -lt "$MIN_FREE_GB" ]; then
  echo "ERROR: bo'sh joy ${FREE_GB}GB < ${MIN_FREE_GB}GB — dump to'xtatildi"
  exit 1
fi

# Server versiyasiga mos pg_dump image tanlaymiz (eski tool yangi serverdan dump ololmaydi).
pick_image() { # $1=network $2=host $3=port $4=user $5=db  -> "postgres:<major>-alpine"
  local v
  v="$(docker run --rm --network "$1" -e PGPASSWORD="$PGPASSWORD" postgres:17-alpine \
        psql -h "$2" -p "$3" -U "$4" -d "$5" -tAc 'SHOW server_version_num' 2>/dev/null | tr -dc '0-9')"
  if [ -n "$v" ]; then echo "postgres:$((v / 10000))-alpine"; else echo "postgres:17-alpine"; fi
}

# ---------- 1) yangi baza: hesap ----------
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
export PGPASSWORD="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"
IMG_NEW="$(pick_image hesap-postgres postgres 5435 "$NU" hesap)"
echo "=== hesap dump ($IMG_NEW) ==="
docker run --rm --network hesap-postgres -e PGPASSWORD="$PGPASSWORD" -v "$OUT_SRV":/backup "$IMG_NEW" \
  pg_dump -h postgres -p 5435 -U "$NU" -d hesap -Fc -Z6 -f "/backup/hesap-$TS.dump" \
  || echo "XATO: hesap dump muvaffaqiyatsiz"

# ---------- 2) eski baza: trust ----------
export PGPASSWORD="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
if [ -z "$PGPASSWORD" ]; then
  echo "SKIP: pg_prod konteyner topilmadi — trust dump o'tkazib yuborildi"
else
  IMG_OLD="$(pick_image app_net pg_prod 5432 postgres trust)"
  echo "=== trust dump — pasport rasmlarisiz ($IMG_OLD) ==="
  # passport jadvali 17 GB (base64 rasm) — struktura saqlanadi, ma'lumoti tashlanadi.
  docker run --rm --network app_net -e PGPASSWORD="$PGPASSWORD" -v "$OUT_SRV":/backup "$IMG_OLD" \
    pg_dump -h pg_prod -U postgres -d trust -Fc -Z6 \
    --exclude-table-data=public.passport \
    -f "/backup/trust-nopassport-$TS.dump" \
    || echo "XATO: trust (nopassport) dump muvaffaqiyatsiz"

  echo "=== trust dump — TO'LIQ ($IMG_OLD) ==="
  docker run --rm --network app_net -e PGPASSWORD="$PGPASSWORD" -v "$OUT_SRV":/backup "$IMG_OLD" \
    pg_dump -h pg_prod -U postgres -d trust -Fc -Z6 -f "/backup/trust-full-$TS.dump" \
    || echo "XATO: trust (to'liq) dump muvaffaqiyatsiz"
fi
unset PGPASSWORD

echo "=== natija ($OUT_SRV) ==="
ls -lh "$OUT_SRV" | tail -20

# ---------- 3) artifactga nusxalash (limitdan kichiklari) ----------
for f in "$OUT_SRV"/*-"$TS".dump; do
  [ -f "$f" ] || continue
  [ "$ART_LIMIT_MB" -eq 0 ] && { echo "artifact o'chirilgan: $(basename "$f") serverda"; continue; }
  SZ_MB=$(( $(stat -c%s "$f") / 1024 / 1024 ))
  if [ "$SZ_MB" -le "$ART_LIMIT_MB" ]; then
    cp "$f" "$OUT_ART"/ && echo "artifact: $(basename "$f") (${SZ_MB} MB)"
  else
    echo "artifactga QO'YILMADI (${SZ_MB} MB > ${ART_LIMIT_MB} MB): $f — serverda qoldi"
  fi
done

echo "=== yakuniy disk ==="
df -h "$OUT_SRV" | tail -1
