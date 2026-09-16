# Query grammar worksheet

Start with the [essay](../essays/07-query-grammar.md) and [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/07-query-grammar). Work only on synthetic fixtures or your assigned Academy instances. Save your attempt before opening the review guide, which holds the hints.

## Time plan

Provisional; no learner has reported actual times.

| Part | Work | Estimate |
|---|---|---|
| Setup | Guided lab already built; reread the essay | 0.25–0.5 h |
| A — guided | Assembled-SQL trace, recorded results, four explanations | 1–2 h |
| B — independent | The sort contract below, with your own regression | 2–3.5 h |
| Delayed check | Two to four days later, without notes | 0.5 h |
| Required total | | 3.75–6.5 h of a 10-hour week |
| C — optional | Two named Academy labs | 2–3 h |

Stop rules: if the lab fails to build for more than an hour, record the blocker and move on. If after about two hours of Part B the expression case still reaches the database, open Hint 1 in the review guide and record it as assistance.

Before running the attack, write the complete SQL after the title is concatenated. Mark the opening and closing string quotes, the tenant parameter, the new Boolean operator and the comment. Explain whether the tenant check was removed or whether its effect was changed.

Record the returned IDs and HTTP status for ordinary Office, O'Brien and both attack titles, before and after binding. The actual SQL result matters more than seeing a database error. Preserve the failing negative-control summary as well as the passing suite. The test harness prints only synthetic paths and response bodies.

Explain these distinctions in your own words:

- Which argument is bound in the vulnerable branch, and which becomes SQL text?
- Why does the fixed branch return an empty result for the attack rather than 400?
- Why is a bound ID insufficient in `/api/unscoped/B-2001`?
- Why does `%` alone return no records, and what would need reconsideration if search became `LIKE`?

## Changed task: a bounded sort vocabulary

`InvoiceRepository.sorted` currently puts caller-supplied text after `ORDER BY`. Implement this contract:

| Request | Expected result |
|---|---|
| No sort parameter, or `sort=` | C-1001, C-1002, C-1003 |
| `sort=id` | C-1001, C-1002, C-1003 |
| `sort=title` | C-1003, C-1002, C-1001 |
| `sort=amount` | C-1002, C-1003, C-1001 |
| Unknown key | 400 |
| `sort=amount DESC` | 400 |

Spring's `@RequestParam(defaultValue="id")` applies to absent and empty values, so `sort=` uses the default rather than becoming a rejected key. [Spring RequestParam contract](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestParam.html)

All results must remain Cedar-only. Valid choices are ascending and use ID as the final tie-breaker. The supplied data has distinct titles and amounts, so the existing cases do not verify ties.

Before changing code, call `/api/sorted` with an unknown key and with an expression such as `amount DESC`, and record what the service and the database each do now. Then decide how the contract is enforced and where an invalid request becomes a client error, and make the guided suite still pass.

Deliverables: your repair; a one-paragraph explanation of where the rejection happens and why there; one independent regression for something the contract promises but the supplied data cannot show, or an input the contract forbids that nobody has tried yet — state its expected behaviour first and show its meaningful failure against the original or a deliberately regressed implementation; and a line on what the fixture cannot verify. If you add rows, isolate that fixture so the other tests' expectations do not change.

A post-attempt check, `SortContractCheck`, encodes the contract's observable outcomes and is excluded from the default `mvn test`. Leave it closed until your attempt and regression are saved, then run:

```sh
mvn -DreviewCheck=true -Dtest=SortContractCheck test
```

It accepts any design that meets the contract; it does not grade your explanation or your regression.

## Independent transfer (optional)

Attempt the following PortSwigger Academy labs without opening their solutions first:

- [SQL injection vulnerability in WHERE clause allowing retrieval of hidden data](https://portswigger.net/web-security/sql-injection/lab-retrieve-hidden-data).
- [SQL injection vulnerability allowing login bypass](https://portswigger.net/web-security/sql-injection/lab-login-bypass).

Explain the condition whose meaning changed in each. Use only the assigned training instance. These assignments were inspected while authoring; no completion by the reader is claimed. Record hints, walkthroughs and AI use as assistance; retry a changed case later when necessary.

## Your evidence

- Date and time spent on A, B and C:
- Hints opened (which, when and why):
- Where the rejection happens and the alternative you did not choose:
- Your regression, and what it failed against:
- Delayed check (two to four days later, without notes): from memory, say why a bound value cannot select a column; compare with your saved explanation.
- Next unanswered question:

Compare with the [review guide](07-query-review.md) after your attempt.
