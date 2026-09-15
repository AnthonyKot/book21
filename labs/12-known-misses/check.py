"""Measure exact fixture locations; do not treat expected misses as safe code."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument("--semgrep", default="semgrep")
parser.add_argument("--rule", default="rules/request-sql.yaml")
parser.add_argument("--exercise", action="store_true")
parser.add_argument("--negative", action="store_true")
parser.add_argument("--output", default="results.json")
args = parser.parse_args()
root = Path(__file__).resolve().parent
source = root / "src/main/java"
expected = {"direct": True, "alias": True, "wrapped": False, "bound": False,
            "repaired": False, "constant": False, "header-unsafe": args.exercise,
            "header-bound": False, "header-wrapped": False}
# This independent truth ledger is established by HTTP execution and code review.
vulnerable = {"direct", "alias", "wrapped", "header-unsafe", "header-wrapped"}
locations = {}
for file in source.rglob("*.java"):
    for line, text in enumerate(file.read_text().splitlines(), 1):
        if "// case: " in text:
            locations[(str(file.relative_to(root)), line + 1)] = text.split("// case: ")[1]
if set(locations.values()) != set(expected):
    raise SystemExit("Fixture marker set changed: review the matrix")
rule = Path(args.rule).resolve()
with tempfile.TemporaryDirectory(prefix="book21-rule-") as tmp:
    if args.negative:
        altered = (rule.read_text().replace("getParameter", "getBook21MissingParameter")
                   .replace("getHeader", "getBook21MissingHeader"))
        if altered == rule.read_text():
            raise SystemExit("Negative mutation did not change the rule")
        rule = Path(tmp) / "request-sql.yaml"
        rule.write_text(altered)
    result = subprocess.run([args.semgrep, "scan", "--metrics=off", "--disable-version-check",
                             "--no-git-ignore", "--config", str(rule), "--json", "src/main/java"],
                            cwd=root, capture_output=True, text=True)
print(result.stderr, file=sys.stderr)
Path(args.output).write_text(result.stdout)
if result.returncode:
    raise SystemExit(f"Scanner failed: exit {result.returncode}")
data = json.loads(result.stdout)
if data.get("version") != "1.177.0" or data.get("engine_requested") != "OSS":
    raise SystemExit("Expected pinned Semgrep 1.177.0 OSS engine; inspect changed results")
if data["errors"]:
    raise SystemExit(f"Scanner errors: {data['errors']}")
scanned = set(data["paths"]["scanned"])
required = {str(p.relative_to(root)) for p in source.rglob("*.java")}
if scanned != required:
    raise SystemExit(f"Incomplete scan: expected {required}, got {scanned}")
hits = set()
for finding in data["results"]:
    loc = (finding["path"], finding["start"]["line"])
    if loc not in locations:
        raise SystemExit(f"Unexpected finding location: {loc}")
    hits.add(locations[loc])
failures = 0
for name, wanted in expected.items():
    found = name in hits
    passed = found == wanted
    failures += not passed
    truth = "vulnerable" if name in vulnerable else "SQL-safe fixture"
    print(f"{'PASS' if passed else 'FAIL'} {name}: {truth}; reported={found}; expected={wanted}")
print(f"Measured {len(hits & vulnerable)} of {len(vulnerable)} vulnerable fixture cases reported; "
      f"{len(hits - vulnerable)} of {len(expected) - len(vulnerable)} SQL-safe cases reported.")
print(f"Matrix: {len(expected)-failures} pass, {failures} fail. Expected misses remain vulnerabilities.")
raise SystemExit(1 if failures else 0)
