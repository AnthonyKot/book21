# Detection maintenance worksheet

Use with [essay 12](../essays/12-known-misses.md) and the [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/12-known-misses). Save your part B ledger, rule, examples and coverage statement before opening the [review guide](12-detector-review.md) or running `review_check.py`, whose source reveals the answers. The fixtures are deliberately vulnerable; keep the application on loopback.

## Plan the week

These are authoring estimates derived from the steps below, not measured learner times. Record your actual time.

| Part | What it involves | Provisional estimate |
|---|---|---|
| Setup | Python venv and pinned Semgrep; Java and Maven from earlier essays | 0.5–1 hour |
| Guided | Read the essay, run the tests, rule tests, matrix and both negative controls, part A | 2–2.5 hours |
| Independent | Part B: truth ledger by HTTP, rule decisions, annotated examples, measured matrix, coverage statement | 3.5–5.5 hours |
| Delayed check | The final evidence-record question, about a week later | 0.5 hour |
| Optional | Part C, the CodeQL investigation plan; running CodeQL is a separate week | 1–2 hours |

The required parts total about 6.5–9.5 hours. Keep the week bounded:

- If the venv or scanner install blocks you for more than an hour, record the command and error, and do the HTTP truth ledger first; it needs only Java.
- If part B has not produced a complete ledger after about 2.5 hours, save it as it stands, open only the next hint in the review guide, marked as assistance, or carry the work into next week.
- Leave part C for a later week unless the required parts finished early.

Start of attempt:

- Date, repository revision, Java, Maven, Python and Semgrep versions:
- Material already seen (hints, AI assistance, the review script):

## A. Guided: search cases

Before scanning, write the security property in one sentence. For `direct`, `alias`, `wrapped` and `bound`, name the source expression, the SQL argument and the method boundary, and predict the HTTP result separately from the scanner result.

| Case | Predicted HTTP result | Predicted finding | Actual | Explanation |
|---|---|---|---|---|
| Direct | | | | |
| Alias | | | | |
| Wrapped | | | | |
| Bound | | | | |

1. What does the passing expectation for `wrapped` establish, and what does it not?
2. Why does the sink focus on the SQL argument rather than the whole call?
3. Run both negative controls. What does each show that the ordinary passing run cannot?

## B. Independent: the reporting module

### The brief

`ReportCases` was added after the rule was written. Your job is to maintain the detector: establish what is actually vulnerable, measure what the rule reports, and decide what to change.

**Requirements.**

1. **Truth first.** For each of the five `report-*` cases, decide whether request input can change the SQL grammar. Back each verdict with at least one HTTP observation (an attack input and an ordinary input) and the code path from input to query. Do this before relying on scanner output.
2. **Decide each difference.** Where the rule's result differs from your truth, choose one: change the rule (source, sink or sanitizer), or keep the difference as a documented known miss or false positive with a reason and a review obligation. Changing everything and accepting everything both need justification.
3. **Preserve guided behavior.** Your rule must still report `direct` and `alias` and must not report `bound`, `repaired` or `constant`. Reporting the vulnerable `wrapped` helper is optional. `check.py --ledger` enforces this.
4. **Pin your choices.** Add annotated examples in `rules/request-sql.java` for each behavior you changed or chose to keep: `ruleid` for required findings, `ok` for required non-findings, `todoruleid` for a miss you accept.
5. Do not edit application code, case markers or `check.py` to make a result pass.

### Deliverable 1: truth ledger

| Case | Input used | Attack request and observed response | Ordinary request and observed response | Code path | Vulnerable? |
|---|---|---|---|---|---|
| report-by-owner | | | | | |
| report-rows | | | | | |
| report-sorted | | | | | |
| report-page | | | | | |
| report-owner-list | | | | | |

Save it as a ledger file for `check.py` (format in the lab README).

### Deliverable 2: rule decisions

| Case | Rule result before | Your decision (change rule / keep miss / keep false positive / already correct) | Reason | Risk of the decision |
|---|---|---|---|---|
| | | | | |

- For any sanitizer you add: why is it safe to trust for SQL grammar, and what similar-looking call would it wrongly bless?
- For any sink you add: which argument is SQL text, and which legitimately carries request data?

### Deliverable 3: measured evidence

- `semgrep --test rules/` output after your changes:
- `python3 check.py --ledger your-ledger.json` output:
- A negative control of your own: remove one change you made, show which annotated example or matrix row fails, and restore it.

### Deliverable 4: coverage statement

Write five to eight sentences: the rule version and engine, the files and cases measured, what the rule now reports, each accepted miss or false positive with its reason, who reviews what the rule cannot see (including the `wrapped` helper), and one condition that would make you re-measure.

## C. Optional: deeper analysis

Draw the `wrapped` flow in text: source → SQL construction → helper parameter → JDBC SQL argument. Read the linked CodeQL Java global taint guidance. Write the sources, sinks and call connection a query would need, and one reason a missing result could be inconclusive. Label it unexecuted unless you actually create and analyze a database.

## Handover

For an internal team, write a maintenance note: the rule change, its annotated examples, the coverage statement and the review obligation for known misses. For a consulting client, add the source revision, files and rules assessed, and the unresolved cases. Label the fixtures as synthetic.

## Evidence record

- Actual time per part:
- Assistance used, including hints, AI suggestions and any look at `review_check.py`:
- What you could now explain without notes:
- Delayed check, a week later: without reopening your rule, explain why a clean scan of a new module is not evidence that it is safe, using one of your own ledger entries.
