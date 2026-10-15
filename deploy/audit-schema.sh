#!/usr/bin/env bash
# Read-only database collection. Writes evidence files ONLY on the invoking host.
set -euo pipefail
umask 077
out=${1:?Usage: audit-schema.sh NEW_OUTPUT_DIRECTORY}
[[ ! -e "$out" ]] || { echo 'Output directory must not already exist.' >&2; exit 1; }
mkdir -p "$out"
mysql_container=${MYSQL_CONTAINER:-jeep-club-mysql-1}
backend_container=${BACKEND_CONTAINER:-jeep-club-backend-1}
mysql_read() {
    docker exec -i "$mysql_container" sh -c '
        export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
        exec mysql --batch --raw --skip-column-names -uroot "$MYSQL_DATABASE"
    '
}
query() { printf '%s\n' "$1" | mysql_read; }
query 'SELECT VERSION(), DATABASE(), @@character_set_database, @@collation_database, @@sql_mode, @@lower_case_table_names;' > "$out/server.tsv"
query "SELECT TABLE_NAME,ENGINE,TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE' AND TABLE_NAME<>'flyway_schema_history' ORDER BY TABLE_NAME;" > "$out/tables.tsv"
query "SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,COLLATION_NAME,GENERATION_EXPRESSION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME<>'flyway_schema_history' ORDER BY TABLE_NAME,COLUMN_NAME;" > "$out/columns.tsv"
query "SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART,INDEX_TYPE,EXPRESSION FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME<>'flyway_schema_history' ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;" > "$out/indexes.tsv"
query "SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE,ENFORCED FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME<>'flyway_schema_history' ORDER BY TABLE_NAME,CONSTRAINT_NAME;" > "$out/constraints.tsv"
query "SELECT TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,ORDINAL_POSITION,IF(REFERENCED_TABLE_SCHEMA=DATABASE(),'<current>',REFERENCED_TABLE_SCHEMA),REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME<>'flyway_schema_history' ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION;" > "$out/keys.tsv"
query "SELECT TABLE_NAME,CONSTRAINT_NAME,UNIQUE_CONSTRAINT_NAME,MATCH_OPTION,UPDATE_RULE,DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() ORDER BY TABLE_NAME,CONSTRAINT_NAME;" > "$out/foreign-keys.tsv"
query "SELECT CONSTRAINT_NAME,CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() ORDER BY CONSTRAINT_NAME;" > "$out/checks.tsv"
query "SELECT TABLE_NAME,TABLE_TYPE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE<>'BASE TABLE' ORDER BY TABLE_NAME;" > "$out/views.tsv"
query "SELECT TRIGGER_NAME,EVENT_MANIPULATION,EVENT_OBJECT_TABLE,ACTION_TIMING,ACTION_STATEMENT FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() ORDER BY TRIGGER_NAME;" > "$out/triggers.tsv"
query "SELECT ROUTINE_NAME,ROUTINE_TYPE,DTD_IDENTIFIER,SQL_DATA_ACCESS,SECURITY_TYPE,ROUTINE_DEFINITION FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() ORDER BY ROUTINE_NAME;" > "$out/routines.tsv"
query "SELECT EVENT_NAME,STATUS,EVENT_TYPE,INTERVAL_VALUE,INTERVAL_FIELD,EVENT_DEFINITION FROM information_schema.EVENTS WHERE EVENT_SCHEMA=DATABASE() ORDER BY EVENT_NAME;" > "$out/events.tsv"
# Identifiers come from server metadata and are quoted, never from user input.
query 'SELECT CONCAT("SELECT ", QUOTE(TABLE_NAME), ",COUNT(*) FROM `",REPLACE(TABLE_NAME,"`","``"),"`;") FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE="BASE TABLE" ORDER BY TABLE_NAME;' > "$out/count-queries.sql"
{ printf 'START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY;\n'; cat "$out/count-queries.sql"; printf 'ROLLBACK;\n'; } | mysql_read > "$out/counts.tsv"
history=$(query "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='flyway_schema_history';")
printf '%s\n' "$history" > "$out/flyway-history-present.txt"
if [[ "$history" == 1 ]]; then
    query 'SELECT installed_rank,version,description,type,script,checksum,success FROM flyway_schema_history ORDER BY installed_rank;' > "$out/flyway-history.tsv"
fi
# No row data, no table locks, no tablespace privileges, no GTID changes.
docker exec "$mysql_container" sh -c '
    export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
    exec mysqldump -uroot --no-data --skip-lock-tables --no-tablespaces --set-gtid-purged=OFF --skip-comments "$MYSQL_DATABASE"
' > "$out/schema.sql"
{
    date -u +%FT%TZ
    git -C /opt/jeep-club-repo rev-parse HEAD 2>/dev/null || true
    cat /var/lib/jeep-club-deploy/current-commit 2>/dev/null || true
    docker inspect --format '{{.Config.Image}} {{.Image}}' "$mysql_container"
    docker inspect --format '{{range .Mounts}}{{println .Type .Name .Destination}}{{end}}' "$mysql_container"
} > "$out/runtime.txt"
if docker inspect "$backend_container" >/dev/null 2>&1; then
    docker inspect --format '{{.Config.Image}} {{.Image}}' "$backend_container" >> "$out/runtime.txt"
    docker inspect --format '{{range .Mounts}}{{println .Type .Name .Destination}}{{end}}' "$backend_container" >> "$out/runtime.txt"
    # Whitelist only. Never dump a container's full environment/configuration.
    docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$backend_container" |
        awk -F= '$1 ~ /^(SPRING_PROFILES_ACTIVE|SPRING_JPA_HIBERNATE_DDL_AUTO|SPRING_FLYWAY_ENABLED|SPRING_FLYWAY_BASELINE_ON_MIGRATE|SPRING_FLYWAY_CLEAN_DISABLED)$/ {print}' > "$out/backend-environment.txt"
    docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$backend_container" |
        sed -n 's|^SPRING_DATASOURCE_URL=jdbc:mysql://[^/]*/\([^?;]*\).*|application_database=\1|p' >> "$out/backend-environment.txt"
fi
(cd "$out"; sha256sum -- *.tsv *.sql *.txt > SHA256SUMS)
printf 'Evidence written to %s. Review locally before sharing; no application rows were exported.\n' "$out"
