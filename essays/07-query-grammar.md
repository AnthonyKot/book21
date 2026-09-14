# Input becomes dangerous when it changes the grammar

The query already has a tenant condition. The tenant comes from the authenticated account, not a request parameter. The repository even uses a JDBC operation that creates a prepared statement.

Alice can still make it return Birch's invoice.

The mistake is in the other condition: the application builds the title comparison by joining SQL text and a search string. A title is supposed to narrow the result. One particular title instead adds an alternative way for every row to qualify.

This constructed case follows Cedar's invoice service into a database. Essay 4 established which rows Alice should be allowed to read. Essay 6 followed text becoming executable browser content. Here the interpretation changes in a SQL parser: data supplied for a comparison becomes part of the instructions that define the comparison.

The [local Java/Spring lab](https://github.com/AnthonyKot/book21/tree/main/labs/07-query-grammar) uses H2 and four synthetic rows. Its purpose is small enough to inspect: preserve exact-title search inside Alice's tenant, regardless of the characters in the requested title.

## Read the assembled query

The database contains:

| ID | Tenant | Title | Amount |
|---|---|---|---:|
| C-1001 | cedar | Office | 300 |
| C-1002 | cedar | O'Brien | 100 |
| C-1003 | cedar | 100% sample | 200 |
| B-2001 | birch | Private | 900 |

Amounts are invented integer fixture values. The search endpoint takes a title, while the server maps authenticated `alice` to `cedar`. For an ordinary request, the vulnerable repository constructs:

```sql
SELECT id, tenant_id, title, amount FROM invoice
WHERE tenant_id = ? AND title = 'Office' ORDER BY id
```

It supplies `cedar` separately for the first parameter. The title, however, is already inside the SQL string:

```java
String sql = COLUMNS
    + " WHERE tenant_id = ? AND title = '" + title + "' ORDER BY id";
return jdbc.query(sql, ROW, tenant);
```

`COLUMNS` is a fixed select list and table name. `ROW` maps a returned row into an invoice record. The interesting operation is the concatenation before `jdbc.query` runs. Only the tenant occupies a parameter slot. The title has been allowed to contribute characters to the SQL program itself.

Now submit this title, including the opening apostrophe:

```text
' OR 1=1 --
```

The resulting SQL is:

```sql
SELECT id, tenant_id, title, amount FROM invoice
WHERE tenant_id = ? AND title = '' OR 1=1 -- ' ORDER BY id
```

The first apostrophe closes the string literal the application opened. `OR 1=1` introduces another condition. The two hyphens begin a line comment, so the application's closing quote and ordering clause no longer contribute syntax.

For these non-null fixture values, the condition behaves as:

```text
(tenant matches AND title is empty) OR true
```

The tenant condition is still present. It simply no longer constrains the whole result. All four rows qualify, including B-2001. Since the comment also removes `ORDER BY`, the reproduction must not depend on their returned order. H2's grammar defines the Boolean operators and line comments used here; the executed lab pins H2 to 2.4.240. [H2 SQL grammar](https://h2database.com/html/grammar.html)

This is SQL injection. Authentication did not fail, and Alice did not choose another tenant through the API. The application gave a title the ability to change how the database combined its conditions.

## Make the observation yourself

The [lab README](https://github.com/AnthonyKot/book21/blob/main/labs/07-query-grammar/README.md) contains the complete setup. With Java 21 and Maven available, run from the lab directory:

```sh
mvn test
mvn -q package -DskipTests
java -jar target/query-grammar-1.0.jar --lab.bind-title=false
```

The application binds to loopback port 8085. It uses one synthetic Basic-auth account and an in-memory database; it does not require a database service or a browser session. The username and password are only fixture credentials. This HTTP setup is not a production authentication recommendation.

In another terminal:

```sh
curl -sS -u alice:local-only --get \
  --data-urlencode "title=' OR 1=1 -- " \
  -w '\n[HTTP %{http_code}]\n' \
  http://127.0.0.1:8085/api/search
```

The vulnerable mode returns `200` with four invoice records. `--data-urlencode` transports the title in the query string. URL encoding does not repair the SQL boundary: the web layer decodes the request before handing the title to the repository.

Compare an ordinary title containing an apostrophe:

```sh
curl -sS -u alice:local-only --get \
  --data-urlencode "title=O'Brien" \
  -w '\n[HTTP %{http_code}]\n' \
  http://127.0.0.1:8085/api/search
```

That request produces `500` with a generic error body. The application assembled a malformed string literal. A real title and an attack therefore reveal the same architectural problem: the application has not kept value characters separate from SQL syntax. Rejecting all apostrophes would exclude a legitimate invoice without addressing every possible query-building mistake.

The error handler hides database details from the response. That is useful error handling, but the earlier attack succeeded without needing an error message.

## Bind the value before it becomes syntax

Stop the application and restart it with `--lab.bind-title=true`, which is also the default. The repaired branch reads:

```java
return jdbc.query(
    COLUMNS + " WHERE tenant_id = ? AND title = ? ORDER BY id",
    ROW, tenant, title);
```

Both values now occupy parameter slots. The program defines the comparison and its Boolean structure; the caller supplies a title value. In this position, apostrophes, comment markers and `OR` are characters to compare, not instructions to parse as an additional predicate.

The same attack now returns `200` with `[]`: no Cedar invoice has that exact title. O'Brien returns C-1002. Office still returns C-1001. The successful repair preserves the search operation instead of treating every unusual string as an invalid request.

Spring Framework 7.0.9 documents this separation between query text and bound arguments. Java's `PreparedStatement` API exposes parameter setters for the same purpose. The lab uses Spring's argument-binding overload rather than manually opening a connection and setting each parameter. [Spring JDBC queries](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html), [Java 21 PreparedStatement](https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/PreparedStatement.html)

This explains why the vulnerable branch could use a prepared statement and remain injectable. Preparation receives the SQL text it is given. If that text already contains the caller's new predicate, preparing it does not recover the application's intended query. A review must trace which input goes into an argument slot and which input becomes SQL text; searching for the class name alone cannot establish safety.

Nor should a fix put quotation marks around a placeholder. In the repaired query, `title = ?` asks for a parameter. Writing `title = '?'` asks for comparison with a literal question mark. Parameter binding is an API contract, not string replacement performed wherever a question mark happens to appear.

## Verify the behavior that matters

The two HTTP test classes each exercise eleven cases:

| Case | Title concatenated | Title bound |
|---|---|---|
| Office | C-1001 | C-1001 |
| Unknown title | Empty result | Empty result |
| Birch's ordinary title | Empty result | Empty result |
| O'Brien | 500 | C-1002 |
| Exact `100% sample`, then `%` alone | C-1003, then empty | C-1003, then empty |
| Always-true injected predicate | All four rows | Empty result |
| Injected predicate targeting B-2001 | B-2001 | Empty result |
| No authentication | 401 | 401 |
| Scoped lookup of C-1001 | C-1001 | C-1001 |
| Scoped lookup of B-2001 | 404 | 404 |
| Deliberately unscoped lookup of B-2001 | B-2001 | B-2001 |

The normal suite passes in both configurations because the reproduction class intentionally asserts the flaw. Run the repair assertions against the vulnerable branch:

```sh
mvn -Dtest=QueryRepairTest -Dtest.bind-title=false test
```

Exactly three tests fail: the two injections still return records, and the apostrophe still produces an error. Eight pass. That negative control establishes that the repair assertions detect this faulty branch rather than merely exercising a server that starts successfully.

The percent test also records a feature decision. This endpoint uses equality, so `%` has no wildcard role. If the feature later changes to `LIKE`, a bound pattern can still contain pattern-language metacharacters. The team must decide whether users are submitting a pattern or literal text and implement that contract. Binding protects the SQL structure; it does not choose every operator's intended semantics.

The final table row is deliberately uncomfortable. `/api/unscoped/B-2001` uses a bound ID but omits the tenant predicate:

```sql
SELECT id, tenant_id, title, amount FROM invoice WHERE id = ?
```

Nothing has changed the grammar. The query faithfully performs an insufficiently restricted lookup. Parameterization cannot invent the missing authorization condition. The adjacent scoped endpoint returns `404` for the same foreign ID. Keep this distinction when writing a finding: an injection repair and an access-control repair establish different properties.

## The caller sometimes chooses structure

A search value fits a parameter slot. A request to sort by amount is different: it chooses a column or expression used by the query. The lab's `/api/sorted` endpoint binds the tenant but concatenates the `sort` parameter into `ORDER BY`. This is the changed practice boundary.

Do not assume that `ORDER BY ?` with the value `"amount"` means “order by the amount column.” A bound value does not become an identifier. The safe design is to expose a small vocabulary and map each accepted key to a fixed fragment owned by the program. Unsupported keys should fail as invalid requests rather than becoming SQL.

That is a suitable use of an allow-list. The title can contain ordinary punctuation; the sort key has a deliberately finite vocabulary. They need different input contracts. OWASP's SQL guidance distinguishes parameterized values from structural choices such as identifiers and sort directions. [SQL injection prevention](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html)

The broader lesson is about an interpreter boundary. Last essay's HTML sink and this essay's SQL concatenation both let supplied text change a program's meaning. Their repairs are specific to their consumers. HTML text insertion is not SQL parameterization; SQL parameterization is not a shell or template defense. In another interpreter, first identify what is data, what is executable structure, and which API actually preserves that separation.

This lab does not establish safety for every query or database. It tests H2, one driver configuration and a small read-only HTTP surface. Its bootstrap database account is not a least-privilege production role. Restricting database privileges can reduce consequences, but even a read-only account can disclose records it may select. The unscoped endpoint and unfinished sorting path remain deliberate flaws after the guided title repair.

<!--mission-->

## Practice: give the caller three sort choices

Repair `/api/sorted` so it accepts exactly `id`, `title` and `amount`, ascending, with ID as the final tie-breaker. Missing or empty input defaults to `id`; unknown keys and expressions such as `amount DESC` must return `400`. Retain tenant binding and verify the actual order of legitimate results.

Use the [worksheet](../practice/07-query-worksheet.md), then the [review guide](../practice/07-query-review.md). The public starter includes explicit exercise checks; the privately executed reference passes them without publishing the solution as your completed work.

Allow 10–12 hours within the 10–15-hour study week: two for reading and tracing the query, four for reproduction and repair, two for evidence, and two to four for independent transfer. Carry unfinished work forward. The worksheet names two authorized Academy labs; record hints, supplied tests and AI assistance separately from your own discoveries.

For employment, explain why the original prepared-statement call was insufficient and show the regression evidence in a code review. For consulting, state the endpoints and query paths assessed, the retest results and the remaining authorization gap. Both routes require a precise account of what the fix establishes.
