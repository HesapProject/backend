\echo '=== Oxirgi 2 soat: 10 daqiqalik ERROR oqimi ==='
SELECT date_trunc('hour', created_date) + interval '10 min' * floor(extract(minute from created_date)/10) AS oraliq,
       count(*)
FROM log.payme_log WHERE status='ERROR' AND created_date > now()-interval '2 hours'
GROUP BY 1 ORDER BY 1;
\echo '=== Jami ==='
SELECT count(*) FILTER (WHERE status='ERROR') AS xato, count(*) AS jami,
       pg_size_pretty(pg_total_relation_size('log.payme_log')) AS hajm FROM log.payme_log;
