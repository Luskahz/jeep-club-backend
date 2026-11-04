"""Summarize focal PIT XML and retain individual unresolved mutations for review."""
import argparse
from collections import Counter
import json
from pathlib import Path
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('xml', type=Path)
parser.add_argument('out', type=Path)
args = parser.parse_args()
mutants = []
for mutation in ET.parse(args.xml).getroot().findall('mutation'):
    mutants.append(dict(mutation.attrib, **{n.tag: [child.text for child in n] if len(n) else n.text for n in mutation}))
counts = Counter(m['status'] for m in mutants)
detected = sum(m.get('detected') == 'true' for m in mutants)
statuses = ('KILLED','SURVIVED','NO_COVERAGE','TIMED_OUT','NON_VIABLE','RUN_ERROR','MEMORY_ERROR','NOT_STARTED')
result = {'total': len(mutants), 'counts': {s: counts[s] for s in dict.fromkeys((*statuses,*counts))},
          'mutation_score_detected': round(100 * detected / len(mutants), 4) if mutants else None,
          'killed_percent': round(100 * counts['KILLED'] / len(mutants), 4) if mutants else None,
          'classes': sorted({m['mutatedClass'] for m in mutants}),
          'unresolved': [m for m in mutants if m['status'] in ('SURVIVED', 'NO_COVERAGE')],
          'mutants': mutants}
args.out.parent.mkdir(parents=True, exist_ok=True)
args.out.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print({k: v for k, v in result.items() if k not in ('mutants', 'unresolved')})
