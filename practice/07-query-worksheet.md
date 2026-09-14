# Query grammar worksheet

Start with the [essay](../essays/07-query-grammar.md) and [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/07-query-grammar). Work only on synthetic fixtures or your assigned Academy instances. Reserve 10–12 hours inside the 10–15-hour week; these are planning allowances, not measured learner times.

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

First run `mvn -Dtest=SortRepairExercise test`. Expect four passes and two failures. The expression currently executes; the unknown column currently becomes a database error. They must instead be rejected as invalid input.

Choose where invalid input becomes HTTP 400. You can validate at the web boundary with `ResponseStatusException(HttpStatus.BAD_REQUEST)`, or throw a specific invalid-sort exception from the repository and map it to 400 in the controller. A plain unchecked exception is not automatically a client-error response. Keep this translation explicit.

Repair the code and run `mvn '-Dtest=*Test,SortRepairExercise' test`. Do not “repair” it by always sorting by ID, removing the tenant predicate, or treating the requested column name as a bound string value. The legitimate-order assertions should catch a change that stops honoring the feature.

Add one independent regression: an unapproved expression with another shape, a whitespace/case policy check, or tied values requiring the ID tie-breaker. State its expected behavior first and show its meaningful failure on the original or deliberately regressed implementation. If adding rows, isolate that fixture so you do not silently change the other tests' expectations.

<details><summary>Hint 1 — classify the input</summary>

A title is a comparison value. A sort key chooses query structure. Trace whether the caller's actual characters need to enter the SQL text at all.

</details>
<details><summary>Hint 2 — separate the public key from SQL</summary>

Use a finite mapping or switch whose outputs are fixed column expressions written by the application. Reject the unmatched case before calling the database. Continue binding the tenant.

</details>
<details><summary>Hint 3 — inspect both outcomes</summary>

Check 400 for unsupported choices and the exact ID order for valid choices. Empty or unordered results do not preserve the feature. A SQL error is not the intended validation response.

</details>

## Independent transfer

Attempt the following PortSwigger Academy labs without opening their solutions first:

- [SQL injection vulnerability in WHERE clause allowing retrieval of hidden data](https://portswigger.net/web-security/sql-injection/lab-retrieve-hidden-data).
- [SQL injection vulnerability allowing login bypass](https://portswigger.net/web-security/sql-injection/lab-login-bypass).

Explain the condition whose meaning changed in each. Use only the assigned training instance. These assignments were inspected while authoring; no completion by the reader is claimed. Record hints, walkthroughs and AI use as assistance; retry a changed case later when necessary.

Keep the evidence pack small: versions, original query, reproduction, patch, allowed/denied behavior, negative control, time spent and remaining scope. For employment, write a focused patch explanation. For consulting, add the assessed endpoint scope and retest limits. Do not call this a complete application assessment.

Compare with the [review guide](07-query-review.md) after your attempt.
