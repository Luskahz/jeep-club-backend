#!/bin/bash
# Stable root-owned SSH entrypoint. Install once; normal release logic lives in Git.
set -euo pipefail
umask 077
export PATH=/usr/sbin:/usr/bin:/sbin:/bin
unset BASH_ENV ENV CDPATH GIT_DIR GIT_WORK_TREE GIT_CONFIG GIT_CONFIG_COUNT
repo=/opt/jeep-club-repo
state=/var/lib/jeep-club-deploy
origin=https://github.com/Luskahz/jeep-club-backend.git
mkdir -p "$state"
exec 9>"$state/deploy.lock"
flock -w 1200 9
if [[ ${SSH_ORIGINAL_COMMAND:-} == status ]]; then
    cat "$state/current-commit" "$state/current-image"
    docker ps --filter label=com.docker.compose.project=jeep-club --format '{{.Names}} {{.Image}} {{.Status}}'
    exit 0
fi
[[ ${SSH_ORIGINAL_COMMAND:-} =~ ^deploy\ ([a-f0-9]{40})$ ]] || { echo 'Only status or deploy <40 lowercase hex SHA> is allowed.' >&2; exit 1; }
sha=${BASH_REMATCH[1]}
[[ $(stat -c %u "$repo") == 0 && $(stat -c %u "$state") == 0 ]]
[[ $(git -C "$repo" remote get-url origin) == "$origin" ]] || { echo 'Unexpected repository origin.' >&2; exit 1; }
git -C "$repo" -c core.hooksPath=/dev/null fetch --no-tags "$origin" +refs/heads/master:refs/remotes/origin/master
git -C "$repo" cat-file -e "$sha^{commit}"
git -C "$repo" merge-base --is-ancestor "$sha" refs/remotes/origin/master
# Reject stale queued runs and older schema/image releases.
[[ $(git -C "$repo" rev-parse refs/remotes/origin/master) == "$sha" ]] || { echo 'Requested SHA is no longer master HEAD; deploy the latest tested master.' >&2; exit 1; }
if [[ -f $state/current-commit ]]; then
    current=$(cat "$state/current-commit")
    [[ $current =~ ^[a-f0-9]{40}$ ]]
    git -C "$repo" merge-base --is-ancestor "$current" "$sha"
fi
# Refuse legacy commits without migration-aware deployment logic.
for path in deploy/vps-deploy.sh deploy/vps-backup.sh src/main/resources/db/migration/V1__initial_deploy_schema.sql; do
    git -C "$repo" cat-file -e "$sha:$path"
done
release=$(mktemp -d "$state/release-$sha-XXXXXX")
git -C "$repo" archive "$sha" | tar -xf - -C "$release"
# No caller-supplied shell, path, options or environment reaches versioned code.
exec env -i PATH="$PATH" HOME=/root LANG=C.UTF-8 /bin/bash "$release/deploy/vps-deploy.sh" "$sha" "$release"
