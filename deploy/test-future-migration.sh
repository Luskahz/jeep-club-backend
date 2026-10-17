#!/usr/bin/env bash
# Prove that an additional legitimate version does not require editing fixed V3 assertions.
# This runs in a separate Git archive, never in the release build or production SQL directory.
set -euo pipefail
root=$(git rev-parse --show-toplevel)
probe=$(mktemp -d)
trap 'rm -rf -- "$probe"' EXIT
git -C "$root" archive HEAD | tar -xf - -C "$probe"
python3 - "$probe" <<'PY'
from pathlib import Path
import sys
folder=Path(sys.argv[1])/'src/main/resources/db/migration'
major=max(int(p.name[1:].split('__')[0].replace('_','.').split('.')[0]) for p in folder.glob('V*__*.sql'))
(folder/f'V{major+1}__ci_forward_compatibility_probe.sql').write_text('-- Ephemeral CI archive only. Never part of a release.\nSELECT 1;\n')
PY
cd "$probe"
chmod +x mvnw
result=0
./mvnw --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true verify || result=$?
mkdir -p "$root/target/forward-migration-evidence"
cp -R target/failsafe-reports "$root/target/forward-migration-evidence/" || true
exit "$result"
