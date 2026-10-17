#!/bin/bash
# Invoked only by the root-owned wrapper after validating the exact master SHA.
set -euo pipefail
umask 077
sha=${1:?Validated SHA required}
release=${2:?Immutable release directory required}
state=/var/lib/jeep-club-deploy
env_file=/opt/jeep-club-compose/.env
[[ $sha =~ ^[a-f0-9]{40}$ && $release == "$state/release-$sha-"* ]]
[[ /proc/$$/fd/9 -ef $state/deploy.lock ]] || { echo 'Missing wrapper deployment lock.' >&2; exit 1; }
cd "$release"
mkdir -p "$state/logs"
log="$state/logs/$sha-$(date -u +%Y%m%dT%H%M%SZ).log"
# Private raw logs stay on VPS. Actions receives stage/error identifiers only.
exec 3>&1 4>&2
exec >>"$log" 2>&1
stage=preflight
cutover=false
compose() { docker compose --project-name jeep-club --project-directory "$release" -f "$release/compose.yaml" --env-file "$env_file" "$@"; }
fail() {
    rc=$?
    trap - EXIT
    if (( rc != 0 )); then
        if [[ $cutover == true ]]; then
            compose stop backend || docker stop jeep-club-backend-1 || true
        fi
        docker logs --tail 300 jeep-club-backend-1 > "$log.backend" 2>&1 || true
        printf 'FAILED sha=%s stage=%s log=%s; no automatic image/schema rollback.\n' "$sha" "$stage" "$log" >&4
        printf 'Inspect private backend log and Flyway history; follow docs/database/06-vps-runbook.md.\n' >&4
        printf '%s %s %s\n' "$sha" "$stage" "$log" > "$state/last-failure"
    fi
    exit "$rc"
}
trap fail EXIT
progress() { stage=$1; printf 'Deploy %s: %s\n' "$sha" "$stage" >&3; }
export BACKEND_IMAGE="jeep-club-backend:$sha"
compose config --quiet
history=$(docker exec jeep-club-mysql-1 sh -c 'export MYSQL_PWD="$MYSQL_PASSWORD"; mysql --batch --skip-column-names -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=\"flyway_schema_history\""')
[[ $history == 1 ]] || { echo 'Explicit verified legacy adoption required.' >&4; exit 1; }
progress build
compose build backend
progress backup
cutover=true
bash "$release/deploy/vps-backup.sh" --deployment
progress startup-flyway-hibernate-health
compose up -d --no-build --wait --wait-timeout 300
progress proxy-reload
# Nginx may retain the address of a replaced backend until its configuration reloads.
compose exec -T proxy nginx -t
compose exec -T proxy nginx -s reload
progress persist-success
# State only changes after migrations, Hibernate validation and health succeed.
tmp_env=$(mktemp "${env_file}.XXXXXX")
awk -v image="$BACKEND_IMAGE" '!/^BACKEND_IMAGE=/ { print } END { print "BACKEND_IMAGE=" image }' "$env_file" > "$tmp_env"
chmod 600 "$tmp_env"
mv "$tmp_env" "$env_file"
printf '%s\n' "$sha" > "$state/current-commit.tmp"
printf '%s\n' "$BACKEND_IMAGE" > "$state/current-image.tmp"
mv "$state/current-image.tmp" "$state/current-image"
mv "$state/current-commit.tmp" "$state/current-commit"
printf '%s\n' "$release" > "$state/current-release"
compose ps
printf 'Deployed commit %s; migrations and health checks succeeded. Private log: %s\n' "$sha" "$log" >&3
