# Known misses: a local detection lab

Companion to [essay 12](https://anthonykot.github.io/book21/essays/12-known-misses.html). Authored synthetic fixtures, including intentionally vulnerable routes. The application binds `127.0.0.1:8093`; no authentication or tenant authorization is modeled. Do not deploy it as a service.

Requires Java 21, Maven 3.9+, Python 3.12 with venv support, and curl. Verified on Linux with Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1 / Framework 7.0.9, H2 2.4.240 and Semgrep 1.177.0 Community Edition. The Maven parent and scanner version are pinned; Python transitive dependencies are resolved at installation, not fully locked. Initial Maven and pip downloads need network access. Scans use the local rule with metrics and version checks disabled, without a Semgrep login or Pro engine.

Run commands from `labs/12-known-misses`. If tools live elsewhere, put Maven on PATH and activate the venv, or pass `--semgrep /absolute/path/to/semgrep`.

## Setup

```bash
python3 -m venv .venv
. .venv/bin/activate
python3 -m pip install -r requirements.txt
```

## 1. Guided: search cases

```bash
mvn test
semgrep scan --metrics=off --disable-version-check --test rules/
python3 check.py
```

Expected: 12 HTTP tests pass; one annotated rule test group passes; six guided matrix checks pass. The rule reports `direct` and `alias`, misses the vulnerable `wrapped` helper, and reports none of `bound`, `repaired` or `constant`. Without a ledger, `check.py` does not look at the reporting module.

`check.py` records raw JSON in `results.json`. It checks the pinned version, the OSS engine, the absence of scanner errors, that every Java source file was scanned, and that each finding sits on a marked case line. `--no-git-ignore` is deliberate: the target is explicitly `src/main/java`, including when a copy sits in an ignored directory. Exit 0 from a bare scan is not evidence of no findings; the harness interprets the JSON.

### Negative controls

```bash
python3 check.py --negative --output results-negative.json
mvn -Dtest=RepairTest -Dlab.bind=false test
```

Both must exit nonzero. The rule mutation replaces every source pattern with one that matches nothing: `direct` and `alias` fail, four checks still pass. The HTTP control restores concatenation on `/search/repaired`: its attack and apostrophe checks fail. An unrelated startup or parse error does not count.

### Observe the application

```bash
mvn -DskipTests package
java -jar target/known-misses-1.0.jar
```

In another terminal:

```bash
curl -G --data-urlencode "title=' OR 1=1 -- " http://127.0.0.1:8093/search/wrapped
curl -G --data-urlencode "title=' OR 1=1 -- " http://127.0.0.1:8093/search/repaired
curl -G --data-urlencode "title=O'Brien" http://127.0.0.1:8093/search/repaired
python3 demo.py
```

Expected: `["Budget","Payroll","O'Brien"]`, `[]`, `["O'Brien"]`. The demo checks the six search routes with the attack. Stop the server with Ctrl-C.

## 2. Independent: the reporting module

Use the [worksheet](https://anthonykot.github.io/book21/practice/12-detector-worksheet.html). `ReportCases` was added after the rule was written; its five methods are served at `/reports/{by-owner,rows,sorted,page,owner-list}`. Read `ReportCases` and `SearchController` to see which request input each one uses. The database holds three documents: Budget and O'Brien owned by `cedar`, Payroll owned by `birch`.

Establish your own truth ledger by HTTP and code review before trusting scanner output. Record it as JSON:

```json
{
  "report-by-owner": {"vulnerable": false, "expectReported": false},
  "...": {}
}
```

`vulnerable` is your finding about the code. `expectReported` is what you decided the rule should do for that case; to see the rule's current result first, run `check.py --ledger` with draft `expectReported` values and read each row's `reported=`. It may differ from `vulnerable` when you accept a documented miss or false positive. The ledger must cover exactly the five `report-*` cases. Then measure:

```bash
python3 check.py --ledger my-ledger.json
semgrep scan --metrics=off --disable-version-check --test rules/
```

In ledger mode, guided search cases may not lose a finding or gain a false alarm on SQL-safe code; the vulnerable `wrapped` helper may start being reported if you model it. Report cases pass when the rule matches your `expectReported`. Differences between `vulnerable` and `expectReported` are printed as decisions your coverage statement must justify.

### After saving your attempt

`review_check.py` compares your ledger and rule with the authored truth ledger and prints any factual errors, regressions and decisions to justify. Opening the file reveals the answers, so run it only after saving your work:

```bash
python3 review_check.py --ledger my-ledger.json
```

## Limits

This is one custom source/sink rule, not Semgrep's maintained SQL ruleset or a product benchmark. Source method names are not receiver-type-constrained; sink API choice, same-method scope and sanitizer choices limit what it sees. Other overloads, APIs, frameworks, transformations and file exclusions need their own measurements. "SQL-safe" means these inspected fixtures and tested inputs, not complete application security. No performance, production precision/recall, tenant-isolation or CodeQL execution claim is made; the essay's CodeQL section is an investigation plan.
