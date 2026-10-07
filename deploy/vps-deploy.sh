#!/bin/bash
set -euo pipefail
umask 077

repo=/opt/jeep-club-repo
env_file=/opt/jeep-club-compose/.env
state_dir=/var/lib/jeep-club-deploy
mkdir -p "$state_dir"
exec 9>"$state_dir/deploy.lock"
flock -w 1200 9
cd "$repo"

if [[ "${SSH_ORIGINAL_COMMAND:-}" == status ]]; then
    docker compose --env-file "$env_file" ps
    exit 0
fi

if [[ ! "${SSH_ORIGINAL_COMMAND:-}" =~ ^deploy\ ([a-f0-9]{40})$ ]]; then
    echo 'Only deploy <commit SHA> and status are supported.' >&2
    exit 1
fi
sha=${BASH_REMATCH[1]}
git fetch --prune origin master
git cat-file -e "$sha^{commit}"
git merge-base --is-ancestor "$sha" origin/master

previous_commit=$(git rev-parse HEAD)
previous_image=$(docker inspect --format '{{.Config.Image}}' jeep-club-backend-1)
compose() { docker compose --env-file "$env_file" "$@"; }
git switch --detach "$sha"
export BACKEND_IMAGE="jeep-club-backend:$sha"
compose config --quiet
compose build backend
/usr/local/sbin/jeep-club-backup

if ! compose up -d --no-build --wait --wait-timeout 240; then
    echo 'Deployment failed; restoring previous application image.' >&2
    git switch --detach "$previous_commit"
    export BACKEND_IMAGE="$previous_image"
    compose up -d --no-build --wait --wait-timeout 240 || true
    echo 'The database backup is available in /var/backups/jeep-club; schema changes are not automatically reverted.' >&2
    exit 1
fi

printf '%s\n' "$sha" > "$state_dir/current-commit"
printf '%s\n' "$BACKEND_IMAGE" > "$state_dir/current-image"
tmp_env=$(mktemp "${env_file}.XXXXXX")
awk -v image="$BACKEND_IMAGE" '!/^BACKEND_IMAGE=/ { print } END { print "BACKEND_IMAGE=" image }' "$env_file" > "$tmp_env"
chmod 600 "$tmp_env"
mv "$tmp_env" "$env_file"
compose ps
echo "Deployed commit $sha"
