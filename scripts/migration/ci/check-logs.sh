#!/usr/bin/env bash
set -uo pipefail
ID=6a9568bd1d466c509aed2ab2
echo "=== Method bo-yicha chaqiruvlar soni (72h) ==="
docker logs --since 72h integration-service 2>&1 | grep -oE '"method":"[A-Za-z]+"' | sort | uniq -c | sort -rn
echo
echo "=== CancelTransaction / CheckTransaction satrlari (72h, oxirgi 20) ==="
docker logs --since 72h integration-service 2>&1 | grep -iE "Cancel transaction|Check transaction|CancelTransaction|CheckTransaction" | tail -20
echo
echo "=== Shu id bo-yicha jami Perform urinishlari (72h) ==="
docker logs --since 72h integration-service 2>&1 | grep -c "Perform transaction: $ID"
echo
echo "=== Boshqa id lar bilan -31008 bormi? ==="
docker logs --since 72h integration-service 2>&1 | grep -oE 'PerformTransaction","params":\{"id":"[a-f0-9]+"' | sort -u
