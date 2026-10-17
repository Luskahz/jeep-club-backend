#!/bin/bash
# Full backup with quiesced application writes. Never deletes old backups/volumes.
set -euo pipefail
umask 077
state=/var/lib/jeep-club-deploy
mode=${1:-standalone}
if [[ $mode == --deployment ]]; then
    [[ /proc/$$/fd/9 -ef $state/deploy.lock ]] || { echo 'Deployment lock must be inherited.' >&2; exit 1; }
else
    [[ $mode == standalone ]]
    exec 9>"$state/deploy.lock"
    flock -w 1200 9
fi
backup=$(mktemp -d /var/backups/jeep-club/release-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX)
running=$(docker inspect -f '{{.State.Running}}' jeep-club-backend-1)
restart() { if [[ $mode == standalone && $running == true ]]; then docker start jeep-club-backend-1 >/dev/null; fi; }
trap restart EXIT
if [[ $running == true ]]; then docker stop jeep-club-backend-1 >/dev/null; fi
docker exec jeep-club-mysql-1 sh -c 'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump -uroot --single-transaction --routines --events --triggers --hex-blob --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"' | gzip > "$backup/database.sql.gz"
image=$(docker inspect -f '{{.Config.Image}}' jeep-club-backend-1)
docker run --rm --network none --volumes-from jeep-club-backend-1:ro --entrypoint tar "$image" -czf - -C /app/storage . > "$backup/uploads.tar.gz"
# Keep credentials and recovery metadata private; never forward these archives to Actions.
tar czf "$backup/config.tar.gz" /opt/jeep-club-compose /etc/jeep-club /etc/letsencrypt /usr/local/sbin/jeep-club-deploy /usr/local/sbin/jeep-club-backup "$state/current-commit" "$state/current-image" 2> "$backup/config.log"
docker inspect -f '{{.Config.Image}} {{.Image}}' jeep-club-backend-1 jeep-club-mysql-1 > "$backup/images.txt"
gzip -t "$backup/database.sql.gz"
tar tzf "$backup/uploads.tar.gz" > /dev/null
(cd "$backup"; sha256sum *.gz images.txt > SHA256SUMS)
printf '%s\n' "$backup" > "$state/latest-backup.tmp"
mv "$state/latest-backup.tmp" "$state/latest-backup"
echo "Backup complete: $backup (restore must be verified separately)"
