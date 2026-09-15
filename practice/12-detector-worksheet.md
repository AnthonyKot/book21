# Measure the rule you can explain

Use this worksheet with [essay 12](../essays/12-known-misses.md) and the [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/12-known-misses). Open the [review guide](12-detector-review.md) after saving an attempt. The supplied fixtures are deliberately small and vulnerable; keep the application on loopback.

## Before scanning

Write the security property in one sentence. Name the source expression, SQL argument and method boundary in each of `SearchCases.direct`, `alias`, `wrapped` and `bound`. Predict the attack response separately from the expected scanner result. Explain what an apostrophe in an ordinary title tests.

Record the repository revision, Java/Maven/Python/Semgrep versions, engine and commands. Follow the README to execute `mvn test`, the annotated rule tests, and `python3 check.py`. Inspect `results.json` for scanned paths and errors, not just a summary banner.

| Case | Predicted HTTP result | Predicted finding | Actual result | Explanation |
|---|---|---|---|---|
| Direct | | | | |
| Alias | | | | |
| Wrapped | | | | |
| Bound | | | | |

The table focuses on the four primary search cases. The full matrix also checks the repaired endpoint, the constant query and three header routes; add predictions for these before measuring the exercise.

Run both negative controls. Record the exact failed cases and whether they failed for the intended reason. Restore the ordinary configuration before using the demo.

## Independent task: a header becomes SQL

Inspect `HeaderCases` without changing the application. A caller now controls `X-Title` instead of the title parameter. Extend the rule so `python3 check.py --exercise` reports the local unsafe header case. Preserve the original direct and alias findings and all four safe non-findings. Keep both wrapper misses documented.

Add at least one positive and one negative annotated header example in `rules/request-sql.java`. Choose a further variation yourself: a different local variable name, a different header name, or a value transformed before concatenation. Predict its behavior before running the scanner. Explain the observed result; do not claim all transformations work from a single example.

After the extension, also run `python3 check.py --exercise --negative`: all three required findings should disappear and fail their matrix checks. Keep using `--exercise` to measure your extended rule.

Save the patch, rule-test result and complete matrix JSON. A green matrix with an unchanged starter rule is an error to investigate. Do not remove fixture files, move case markers or change expected vulnerability labels to get a pass.

## Investigation decision

Draw the missing wrapper flow in text: source → SQL construction → helper parameter → JDBC SQL argument. Read the linked CodeQL Java guide's global taint section. Write down the sources, sinks and call connection your proposed query would need, plus one reason a missing result might be inconclusive.

Choose a next action for this helper: model it explicitly in a rule, investigate interprocedural analysis, or retain a named manual review check. Explain the cost and maintenance obligation. No CodeQL installation is needed to complete this design investigation; label it unexecuted unless you actually create and analyze a database.

## Handover and time

Write a short coverage statement with files, source revision, rule version, engine, findings verified, safe fixtures checked and unresolved misses. Prepare either a team-maintenance note for employment or a bounded assessment/retest note for consulting. Keep both career routes available.

Plan 10–12 hours inside the 10–15-hour weekly budget: roughly 2 reading/setup, 3 guided execution, 3 independent rule work, and 2–4 triage, writing and delayed review. These are allowances to revise against actual time. Carry unfinished work forward; optional CodeQL setup should not silently expand the week.

Record elapsed time, assistance and the next unresolved question. After a delay, explain why the passing wrapper expectation is evidence of a known limitation. Reading, following a hint and independently solving the changed task are distinct progress states.
