# shellcheck disable=SC2206
list=($DB_SCHEMAS)

for i in ${list[@]};do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d "$POSTGRES_DB"  -c "CREATE SCHEMA IF NOT EXISTS $i;"
done