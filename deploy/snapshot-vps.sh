#!/usr/bin/env bash
# Read-only production diagnostics. Only the requested output directory is created.
set -euo pipefail
umask 077

[[ $# -ge 1 && $# -le 4 ]] || {
    echo 'Usage: snapshot-vps.sh NEW_OUTPUT_DIR [REFERENCE_AUDIT_DIR] [RESTORE_EVIDENCE_JSON] [REFERENCE_VERSION]' >&2
    exit 2
}
output=$1
reference=${2:-}
restore_evidence=${3:-}
reference_version=${4:-3}
[[ $reference_version =~ ^[1-9][0-9]*$ ]] || { echo 'Invalid reference version.' >&2; exit 2; }
[[ ! -e $output ]] || { echo 'Output directory already exists.' >&2; exit 2; }
if [[ -n $reference ]]; then [[ -d $reference ]] || { echo 'Reference audit directory missing.' >&2; exit 2; }; fi
if [[ -n $restore_evidence ]]; then [[ -f $restore_evidence ]] || { echo 'Restore evidence missing.' >&2; exit 2; }; fi

mkdir -m 700 -- "$output"
output=$(cd "$output" && pwd -P)
evidence=$(mktemp -d)
trap 'rm -rf -- "$evidence"' EXIT
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
audit_ok=false
if bash "$script_dir/audit-schema.sh" "$evidence/schema" >"$evidence/audit.out" 2>"$evidence/audit.err"; then
    audit_ok=true
fi
python3 "$script_dir/snapshot-vps.py" "$output" "$evidence/schema" "$reference" "$restore_evidence" "$audit_ok" "$script_dir" "$reference_version"
(cd "$output" && sha256sum jeep-club-vps-snapshot.json jeep-club-vps-report.md > jeep-club-vps-checksums.txt)
printf 'Sanitized snapshot created: %s\n' "$output"
