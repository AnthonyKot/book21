"""Post-attempt comparison for the reporting-module task. Opening this file reveals the answers.

Save your truth ledger, rule change, annotated examples and coverage statement first. This script
compares them with the authored truth for the reporting module, which was established by HTTP
execution and code review. It fails on a wrong truth entry in your ledger, a lost guided finding, a
false alarm on guided SQL-safe code, or a rule that does not do what your ledger expects. Misses and false
positives in the reporting module are listed as decisions your coverage statement must justify.
"""
import argparse
import json
from pathlib import Path
import subprocess
import sys

AUTHORED_VULNERABLE = {"report-by-owner", "report-rows"}
AUTHORED_SAFE = {"report-sorted", "report-page", "report-owner-list"}

parser = argparse.ArgumentParser()
parser.add_argument("--semgrep", default="semgrep")
parser.add_argument("--rule", default="rules/request-sql.yaml")
parser.add_argument("--ledger", required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parent

ledger = json.loads(Path(args.ledger).read_text())
failures = 0
print("Truth ledger:")
for name in sorted(AUTHORED_VULNERABLE | AUTHORED_SAFE):
    truth = name in AUTHORED_VULNERABLE
    yours = ledger.get(name, {}).get("vulnerable")
    ok = yours == truth
    failures += not ok
    print(f"  {'PASS' if ok else 'FAIL'} {name}: authored {'vulnerable' if truth else 'SQL-safe'}; yours {yours}")

run = subprocess.run([sys.executable, "check.py", "--semgrep", args.semgrep, "--rule", args.rule,
                      "--ledger", args.ledger, "--output", "results-review.json"],
                     cwd=root, capture_output=True, text=True)
print(run.stdout)
guided_part, _, report_part = run.stdout.partition("Reporting module")
guided_failed = any(line.strip().startswith("FAIL") for line in guided_part.splitlines())
failures += guided_failed
mismatched = [line.split()[1].rstrip(":") for line in report_part.splitlines() if line.strip().startswith("FAIL report-")]
if mismatched:
    print("Your rule does not do what your ledger's expectReported says for: " + ", ".join(mismatched))
failures += len(mismatched)
if run.returncode and not guided_failed and "Matrix:" not in run.stdout:
    print(run.stderr)
    raise SystemExit("check.py did not complete")

reported = set()
for line in run.stdout.splitlines():
    if line.strip().startswith(("PASS report-", "FAIL report-")) and "reported=True" in line:
        reported.add(line.split()[1].rstrip(":"))
print("Decisions your coverage statement must justify:")
for name in sorted(AUTHORED_VULNERABLE - reported):
    print(f"  known miss: {name} is vulnerable and unreported")
for name in sorted(AUTHORED_SAFE & reported):
    print(f"  false positive: {name} is SQL-safe and reported")
if not (AUTHORED_VULNERABLE - reported) and not (AUTHORED_SAFE & reported):
    print("  none in the reporting module")
print(f"Review check: {failures} failure(s): truth entries, guided regressions or rule/ledger mismatches.")
raise SystemExit(1 if failures else 0)
