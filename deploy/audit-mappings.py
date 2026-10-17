#!/usr/bin/env python3
"""Rebuild the PR #100 mapping evidence from immutable Git references (read only)."""
import hashlib
import io
import json
import pathlib
import re
import subprocess
import tarfile

REFS = {"initial_reference": "e4aa00e4a492900b26cec78839dcfae480712b0b",
        "master": "6865bc4f600462c5fcc6cf1212c9660591eff4db",
        "develop": "9d3fba331fba5a9284c85a0e5ab718efb58f93b5"}

def sources(ref):
    archive = subprocess.check_output(["git", "-c", "core.autocrlf=false", "archive", ref, "src/main"])
    with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
        return {item.name: tar.extractfile(item).read() for item in tar
                if item.isfile() and item.name.endswith((".java", ".xml", ".sql", ".properties"))}

def blob(content):
    if content is None:
        return None
    return hashlib.sha1(b"blob " + str(len(content)).encode() + b"\0" + content).hexdigest()

if __name__ == "__main__":
    states = {key: sources(ref) for key, ref in REFS.items()}
    mappings, selected = [], []
    for path, content in sorted(states["develop"].items()):
        text = content.decode("utf-8")
        if re.search(r"@Entity\b|@Embeddable\b|@MappedSuperclass\b|@Converter\b", text):
            table = re.search(r'@Table\(\s*name\s*=\s*"([^"]+)"', text)
            row = {"entity": pathlib.Path(path).stem, "table": table.group(1) if table else "(inheritance/embeddable)", "path": path}
            row.update({key+"_blob": blob(state.get(path)) for key, state in states.items()})
            mappings.append(row)
        if re.search(r"\benum\s+\w+|AttributeConverter|@Converter|@Entity\b|@Embeddable\b|@MappedSuperclass\b", text) or path.endswith((".sql", ".xml", ".properties")):
            selected.append({"path": path, "master_develop_identical": states["master"].get(path)==content,
                             "initial_master_identical": states["initial_reference"].get(path)==states["master"].get(path)})
    result = {"refs": REFS, "mappings": mappings, "indirect_inputs": selected,
              "initial_master_changed_inputs": [r["path"] for r in selected if not r["initial_master_identical"]],
              "master_develop_changed_inputs": [r["path"] for r in selected if not r["master_develop_identical"]]}
    print(json.dumps(result, ensure_ascii=False, indent=2))
