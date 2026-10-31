"""Physical JaCoCo counters and unique Surefire/Failsafe case inventory (stdlib only).

python docs/testing/final-hardening/measure.py --coverage target/site/jacoco/jacoco.xml --reports target/surefire-reports --out output.json
Run on preserved artifacts. Never average class percentages or sum repeated runs.
"""
import argparse
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

MODULES = {"Identity": "iam/identity", "Authorization": "iam/authorization",
           "Publications": "publications", "Platform": "platform", "Shared": "shared"}
KINDS = ("INSTRUCTION", "LINE", "BRANCH", "METHOD", "CLASS")


def mechanisms(source, sources):
    """Inspect runner/context, including inherited scenarios; names never determine integration."""
    parent = re.search(r'class\s+\w+\s+extends\s+(\w+)', source)
    if parent and parent[1] in sources:
        source += '\n' + sources[parent[1]]
    tags = []
    if '@Testcontainers' in source or 'MySQLContainer' in source:
        tags.append('mysql-testcontainers')
    if 'MockMvc' in source or 'HttpClient' in source or 'TestRestTemplate' in source:
        tags.append('http-contract')
    if 'springSecurity()' in source or '@AutoConfigureMockMvc' in source:
        tags.append('security-filter-chain')
    if '@DataJpaTest' in source:
        tags.append('persistence')
    if '@SpringBootTest' in source or '@WebMvcTest' in source or '@SpringJUnitConfig' in source:
        tags.append('spring-context')
    if 'CountDownLatch' in source or 'CyclicBarrier' in source:
        tags.append('concurrency')
    if not tags:
        tags.append('unit-with-mocks' if 'mock(' in source or '@Mock' in source else 'pure-unit')
    return tags


def counters(node):
    return {c.attrib["type"]: {k: int(c.attrib[k]) for k in ("missed", "covered")}
            for c in node.findall("counter") if c.attrib["type"] in KINDS}


def aggregate(packages):
    result = {k: {"covered": 0, "missed": 0} for k in KINDS}
    for package in packages:
        for kind, values in counters(package).items():
            for key, value in values.items():
                result[kind][key] += value
    for value in result.values():
        total = value["covered"] + value["missed"]
        value["percent"] = round(100 * value["covered"] / total, 4) if total else None
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--coverage", type=Path)
    parser.add_argument("--reports", type=Path)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--sources", type=Path, default=Path('src/test/java'))
    args = parser.parse_args()
    result = {}
    if args.coverage:
        root = ET.parse(args.coverage).getroot()
        result["modules"] = {}
        for module, suffix in MODULES.items():
            prefix = "com/jeepclub/backend/" + suffix
            packages = [p for p in root.findall("package") if p.attrib["name"] == prefix or p.attrib["name"].startswith(prefix + "/")]
            result["modules"][module] = {"counters": aggregate(packages),
                "packages": {p.attrib["name"]: counters(p) for p in packages},
                "partial_methods": [{"class": c.attrib["name"], "method": m.attrib["name"],
                                     "descriptor": m.attrib["desc"], "counters": counters(m)}
                                    for p in packages for c in p.findall("class") for m in c.findall("method")
                                    if any(x["missed"] for x in counters(m).values())]}
        combined = [p for p in root.findall("package") if p.attrib["name"].startswith(("com/jeepclub/backend/platform/", "com/jeepclub/backend/shared/"))]
        result["platform_shared"] = aggregate(combined)
    if args.reports:
        cases = {}
        source_files = list(args.sources.rglob('*.java'))
        sources = {p.stem: p.read_text(encoding='utf-8-sig') for p in source_files}
        for path in sorted(args.reports.glob("TEST-*.xml")):
            for case in ET.parse(path).getroot().iter("testcase"):
                key = case.attrib["classname"] + "#" + case.attrib["name"]
                cases[key] = "failure" if case.find("failure") is not None else "error" if case.find("error") is not None else "skipped" if case.find("skipped") is not None else "passed"
        result["tests"] = {"discovered": len(cases), "counts": {s: sum(v == s for v in cases.values()) for s in ("passed", "failure", "error", "skipped")}, "cases": cases}
        result['test_mechanisms'] = {name: mechanisms(sources.get(name.rsplit('.', 1)[-1], ''), sources)
                                     for name in sorted({k.split('#')[0] for k in cases})}
        # Primary categories are mutually exclusive; tags above retain overlapping mechanisms.
        priority = ('mysql-testcontainers', 'http-contract', 'persistence', 'spring-context', 'unit-with-mocks', 'pure-unit')
        result['test_categories'] = {tag: 0 for tag in priority}
        for key in cases:
            tags = result['test_mechanisms'][key.split('#')[0]]
            result['test_categories'][next(tag for tag in priority if tag in tags)] += 1
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    if "modules" in result:
        for module, data in result["modules"].items():
            print(module, data["counters"])
    if "tests" in result:
        print({k: v for k, v in result["tests"].items() if k != "cases"})


if __name__ == "__main__":
    main()
