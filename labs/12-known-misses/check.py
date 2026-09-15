"""Run the rule over the application and compare findings with a truth ledger.

The guided ledger covers the search cases in SearchCases and was established by HTTP execution
and code review. With --ledger, you supply your own ledger for the reporting module; the guided
cases are still checked, so a rule change cannot silently lose their findings or add false alarms.
Expected misses remain vulnerabilities.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile

GUIDED_VULNERABLE = {"direct", "alias", "wrapped"}
GUIDED_EXPECTED = {"direct": True, "alias": True, "wrapped": False,
                   "bound": False, "repaired": False, "constant": False}

parser = argparse.ArgumentParser()
parser.add_argument("--semgrep", default="semgrep")
parser.add_argument("--rule", default="rules/request-sql.yaml")
parser.add_argument("--ledger", help="JSON: {case: {\"vulnerable\": bool, \"expectReported\": bool}} for report-* cases")
parser.add_argument("--negative", action="store_true", help="replace every source pattern with one that matches nothing")
parser.add_argument("--output", default="results.json")
args = parser.parse_args()

root = Path(__file__).resolve().parent
source = root / "src/main/java"
locations = {}
for file in source.rglob("*.java"):
    for line, text in enumerate(file.read_text().splitlines(), 1):
        if "// case: " in text:
            locations[(str(file.relative_to(root)), line + 1)] = text.split("// case: ")[1].strip()
cases = set(locations.values())
if not set(GUIDED_EXPECTED) <= cases:
    raise SystemExit("Guided fixture markers changed: review the matrix")
report_cases = sorted(c for c in cases if c.startswith("report-"))

ledger = {}
if args.ledger:
    ledger = json.loads(Path(args.ledger).read_text())
    unknown = set(ledger) - set(report_cases)
    missing = set(report_cases) - set(ledger)
    if unknown or missing:
        raise SystemExit(f"Ledger must cover exactly the report cases. Unknown: {sorted(unknown)} Missing: {sorted(missing)}")

rule = Path(args.rule).resolve()
with tempfile.TemporaryDirectory(prefix="book21-rule-") as tmp:
    if args.negative:
        text = rule.read_text()
        block = re.search(r"(pattern-sources:\n)((?:[ \t]+.*\n)+?)(?=[ \t]*pattern-(?:sinks|sanitizers|propagators):)", text)
        if not block:
            raise SystemExit("Negative mutation could not find the pattern-sources block")
        indent = re.match(r"[ \t]*", block.group(2)).group(0)
        altered = text.replace(block.group(0), block.group(1) + f"{indent}- pattern: book21NoSuchSource(...)\n")
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
print("Guided search cases:")
for name, wanted in GUIDED_EXPECTED.items():
    found = name in hits
    vulnerable = name in GUIDED_VULNERABLE
    if args.ledger:
        # A rule change may legitimately start reporting a known miss; it may not lose a finding
        # or report SQL-safe code.
        passed = found if wanted else (not found or vulnerable)
    else:
        passed = found == wanted
    failures += not passed
    truth = "vulnerable" if vulnerable else "SQL-safe"
    print(f"  {'PASS' if passed else 'FAIL'} {name}: {truth}; reported={found}; expected={wanted}")

if args.ledger:
    print("Reporting module (your ledger):")
    for name in report_cases:
        entry = ledger[name]
        found = name in hits
        passed = found == entry["expectReported"]
        failures += not passed
        truth = "vulnerable" if entry["vulnerable"] else "SQL-safe"
        note = ""
        if entry["vulnerable"] and not found:
            note = "  <- known miss: needs a stated review obligation"
        elif not entry["vulnerable"] and found:
            note = "  <- false positive: needs a stated triage decision"
        print(f"  {'PASS' if passed else 'FAIL'} {name}: {truth} (your ledger); reported={found}; expected={entry['expectReported']}{note}")

print(f"Matrix: {failures} fail. Expected misses remain vulnerabilities.")
raise SystemExit(1 if failures else 0)
