# Known misses: a local detection lab

Companion to [essay 12](https://anthonykot.github.io/book21/essays/12-known-misses.html). Authored synthetic fixtures, including intentionally vulnerable routes. Run locally; the application binds `127.0.0.1:8093`. No authentication or tenant authorization is modeled. Do not deploy this lab as a service.

Requires Java 21, Maven 3.9+, Python 3.12 with venv support, and curl. Verified on Linux with Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1 / Framework 7.0.9, H2 2.4.240 and Semgrep 1.177.0 Community Edition. The Maven parent and scanner version are pinned. Python transitive dependencies are resolved at installation, not fully locked. Initial Maven/pip downloads need network access; these scans use the local rule with metrics and version checks disabled, without a Semgrep login or Pro engine.

Commands below run from `labs/12-known-misses` in a clone of the book repository. If tools live elsewhere, put Maven on PATH and activate the venv, or supply `--semgrep /absolute/path/to/semgrep` to `check.py`.

## Install and reproduce

```bash
python3 -m venv .venv
. .venv/bin/activate
python3 -m pip install -r requirements.txt
mvn test
semgrep scan --metrics=off --disable-version-check --test rules/
python3 check.py
```

Expected: 19 HTTP tests pass; one annotated rule test group passes; nine matrix checks pass. The matrix is about expected scanner behavior: only 2 of 5 deliberately vulnerable cases are reported, with 0 of 4 SQL-safe fixture cases reported. Neither the matrix nor the `todoruleid` annotation means the missed code is safe. Rules examples are scanner fixtures outside the Maven source tree; HTTP tests exercise the separately scanned, compiled application methods.

`check.py` records raw JSON in `results.json`. It checks the pinned version, OSS engine, absence of scanner errors, every expected Java input file, and exact finding lines. `--no-git-ignore` is deliberate: the target is explicitly `src/main/java`, including when a learner's copy sits in an ignored directory. The script measures the default `lab.bind=true` application; it does not infer runtime configuration. Exit 0 from a bare scan is insufficient evidence of no findings; the harness interprets the JSON.

## Negative controls

```bash
python3 check.py --negative --output results-negative.json
mvn -Dtest=RepairTest -Dlab.bind=false test
```

Both must exit nonzero. The rule mutation removes the recognized parameter source in a temporary copy: 2 matrix failures, 7 passes. The HTTP control restores real concatenation on `/search/repaired`: 2 failures, 7 passes. One failure is the injection response, the other is a legitimate apostrophe title returning an error. An unrelated startup/parse error does not count as a successful negative control. Default tests are run without `-Dlab.bind=false`.

## Observe the actual application

```bash
mvn -DskipTests package
java -jar target/known-misses-1.0.jar
```

In another terminal, from the same lab directory:

```bash
curl -G --data-urlencode "title=' OR 1=1 -- " \
  http://127.0.0.1:8093/search/wrapped
curl -G --data-urlencode "title=' OR 1=1 -- " \
  http://127.0.0.1:8093/search/repaired
curl -G --data-urlencode "title=O'Brien" \
  http://127.0.0.1:8093/search/repaired
python3 demo.py
```

Expected responses: `["Budget","Payroll","O'Brien"]`, `[]`, `["O'Brien"]`. The demo uses curl and checks nine routes with the attack supplied as both parameter and header. Stop the server with Ctrl-C. State is an in-memory database; no external target is contacted by the fixture.

The `direct`, `alias`, `wrapped`, `header-unsafe`, and `header-wrapped` routes intentionally remain vulnerable. `bound`, default `repaired`, and `header-bound` bind the supplied value. `constant` ignores the request and returns Budget. This chapter's repair is the bound endpoint; it is not a claim that the whole demonstration application was secured.

## Changed exercise

Start with the [worksheet](https://anthonykot.github.io/book21/practice/12-detector-worksheet.html). Modify the rule and its annotated examples, preserving application fixtures and truth labels:

```bash
python3 check.py --exercise --output results-exercise.json
```

The public starter has 1 failure / 8 passes: it misses locally concatenated header input. Extend detection to this input source without flagging bound header values or losing the two existing findings. After extending the rule, use `--exercise` for its matrix: the base matrix intentionally expects the starter behavior. Run `python3 check.py --exercise --negative --output results-exercise-negative.json` as well; it removes both recognized source names and must produce 3 failures / 6 passes, including the newly required header finding. Add your own positive and negative annotated header examples, run `--test rules/` again, and explain the two wrapper misses. An unpublished reference was executed during authoring: the rule tests and 9/9 exercise matrix pass, reporting 3 of 5 vulnerable cases and 0 of 4 SQL-safe cases.

## Limits and evidence

This is one custom source/sink rule, not Semgrep's maintained SQL ruleset or a product benchmark. Its source name is not receiver-type-constrained; sink type/API, same-method scope and input selection limit what it sees. Different overloads, APIs, frameworks, transformations and file exclusions need their own measurements. The SQL-safe label is about these inspected fixtures and tested inputs, not complete application security. No performance, production precision/recall, tenant-isolation or CodeQL execution claim is made.

The essay's CodeQL section investigates how global taint tracking might connect the wrapper call. A CodeQL database, query, extraction configuration and result would be required before claiming detection. This follow-up can wait for another study week.
