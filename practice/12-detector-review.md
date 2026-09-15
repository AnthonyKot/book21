# Detection maintenance review

Open this after saving your [worksheet](12-detector-worksheet.md) ledger, rule, examples and coverage statement. Everything below counts as assistance once you have read it. If you are stuck partway, read only the next hint in section 2 and record that you did.

## 1. Judge the attempt before comparing answers

A sound attempt shows the following, whatever rule it ended with.

**Truth independent of the tool.** Each ledger verdict rests on an attack request and an ordinary request with observed responses, plus the code path. A verdict copied from the scanner's output, in either direction, is not evidence.

**Differences decided one at a time.** Each mismatch between truth and rule has a stated decision: change a source, sink or sanitizer, or keep a documented miss or false positive. A rule that reports everything or nothing in the module is not a decision.

**Changes that stay narrow.** A new source or sink does not start reporting bound values. A sanitizer is trusted only for what it actually guarantees about SQL grammar. The guided search matrix is unchanged, unless the `wrapped` helper is deliberately modeled.

**Choices pinned by examples.** Annotated examples exist for each changed or accepted behavior, and a negative control of the reader's own shows at least one of them failing when its rule change is removed.

**A coverage statement someone can challenge.** It names the engine and version, the cases measured, what is reported, each accepted miss or false positive with its reason, and who reviews what the rule cannot see.

## 2. Progressive hints, if you are stuck

1. For each report method, find the first expression that reads the request and the exact argument that becomes SQL text. Then test with an input that would change the query if it reached the grammar, and with an ordinary one. Compare the responses before you look at findings.
2. Compare each vulnerable case with the rule's source and sink patterns. Is the input read by a call the rule recognizes? Is the query executed by a call the rule treats as a sink? For each safe case the rule reports, ask what stops the input from changing the grammar, and whether the rule can express that.
3. Change one thing at a time and rerun the matrix and the rule tests after each change. Watch the guided `bound` and `repaired` rows: if they start reporting, the new pattern is broader than the SQL argument.

## 3. What the reporting module contains

Established by HTTP execution and code review; the private truth tests are not published.

| Case | Truth | Base rule | Why |
|---|---|---|---|
| `report-by-owner` | Vulnerable | Not reported | The owner comes from a request header and is concatenated into the query; the attack returns every title. The rule's only source is `getParameter`. |
| `report-rows` | Vulnerable | Not reported | The title is concatenated and executed with `JdbcTemplate.query(sql, rowMapper)`; the attack returns every title. The rule's only sink is `queryForList`. |
| `report-sorted` | SQL-safe | Not reported | The request value only selects between two fixed column names; an injection attempt falls back to the default order. |
| `report-page` | SQL-safe | **Reported** | `Integer.parseInt` either yields an integer or throws; a non-numeric attack returns 400 and never reaches the query. The rule's taint propagates through the conversion, so this is a false positive. |
| `report-owner-list` | SQL-safe | Not reported | The header value is bound as a parameter; the attack is compared as data and returns nothing. |

## 4. Designs that look finished

Each row was run through `check.py --ledger` and `review_check.py`.

| Rule change | What happens |
|---|---|
| Add the header source, plus a `query` sink focused on its SQL argument, plus a sanitizer for `Integer.parseInt(...)` | Reports both vulnerable cases, silences the numeric false positive, keeps all guided rows. No decisions left to justify. |
| The same, without the sanitizer, with the ledger accepting `report-page` as a documented false positive | Passes. Legitimate if the coverage statement explains why triage can dismiss it. A rule that over-reports a safe conversion costs review time but hides nothing. |
| Add only the header source | The `rows` case stays a known miss and `page` stays a false positive. If the ledger records both as accepted, the check passes and lists them as decisions to justify; if it expects otherwise, the check fails on the mismatch. Adding a source does not reach a sink the rule never names. |
| Add `query` as a sink without focusing the SQL argument | Starts reporting the guided `bound` and `repaired` routes and `report-owner-list`. The review check fails on the guided regression and on the ledger mismatch. This is the whole-call mistake the essay warned about. |
| A ledger that marks `report-page` vulnerable | The review check fails on the truth entry, and on the mismatch if the rule does not report it. An exception on bad input is not grammar injection. |

A sanitizer deserves suspicion even when it is right here. `Integer.parseInt` constrains the value to an integer; a sanitizer for `String.valueOf`, `trim` or a logging helper would silently bless real injections. Name what the sanitizer guarantees in the coverage statement.

## 5. Reference evidence

Recorded while authoring. None of it is evidence of your attempt.

- Guided: 12 HTTP tests; rule tests 1/1; matrix 6 pass. Negative controls: rule mutation 2 failures; HTTP bind control 2 failures of 6.
- Base rule on the reporting module: reports `report-page` only.
- Private reference: 5 HTTP truth tests pass (17 with the guided tests); extended rule with header source, `query` sink and `parseInt` sanitizer; its annotated examples pass; `check.py --ledger` 0 failures; `review_check.py` 0 failures and no decisions left.
- Variants in section 4 produced the results shown.

To compare after your attempt:

```bash
python3 review_check.py --ledger your-ledger.json
```

It checks your truth entries, guided regressions and whether your rule does what your ledger expects, then lists the misses and false positives your coverage statement must justify. It cannot judge the reasons you gave.

## 6. Limits

One custom rule on authored fixtures, measured with Semgrep 1.177.0 Community Edition. The counts are not production precision or recall. Other request sources (path variables, bodies, cookies), other JDBC and ORM APIs, cross-method flows like `wrapped`, and other interpreters need their own cases. The optional CodeQL plan is unexecuted unless you ran it.
