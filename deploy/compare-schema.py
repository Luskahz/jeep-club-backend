#!/usr/bin/env python3
"""Strict structural comparison of two audit-schema.sh output directories; never writes DBs."""
import difflib
import pathlib
import sys
FILES = ("tables.tsv", "columns.tsv", "indexes.tsv", "constraints.tsv", "keys.tsv",
         "foreign-keys.tsv", "checks.tsv", "views.tsv", "triggers.tsv", "routines.tsv", "events.tsv")

def compare(reference, actual):
    changed = False
    for name in FILES:
        left = (reference / name).read_text(encoding="utf-8").splitlines()
        right = (actual / name).read_text(encoding="utf-8").splitlines()
        diff = list(difflib.unified_diff(left, right, fromfile="reference/"+name, tofile="actual/"+name, lineterm=""))
        if diff:
            changed = True
            print("\n".join(diff))
    return changed

if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: compare-schema.py REFERENCE_DIRECTORY ACTUAL_DIRECTORY")
    try:
        changed = compare(pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]))
    except (OSError, UnicodeError) as error:
        sys.exit(str(error))
    if changed:
        sys.exit(1)
    print("Structural metadata matches exactly. This does not authorize baseline or prove data/restore compatibility.")
