# Two valid requests can violate one business rule

Request A belongs to Alice. It names her tenant’s unused credit. The service checks the credit, marks it applied and adds 1,000 cents to the tenant balance.

Request B belongs to Alice too. It names the same credit and passes the same checks. After both requests finish, the credit is marked applied once, but the balance has increased by 2,000 cents.

There is no forged identity, foreign object or injected expression in this constructed case. The problem is the interval between a correct observation and the write that relies on it. A fact that was true when each request checked it is not permission for both requests to perform the effect.

## Write the rule about the result

The fictional document service now applies a one-use credit to a tenant account. Credit `C-1001` belongs to Cedar and is worth 1,000 cents. Its rule is:

> A committed application of this credit may increase Cedar’s balance by 1,000 cents at most once. If the application fails before committing, neither the claim nor its balance effect should remain.

These are two related obligations. One prevents duplicate application. The other prevents partial application. Treating both as “use a transaction” hides the distinction we need to test.

The [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/10-valid-requests) stores an `applied` flag in a credit row and a balance in an account row. All amounts are integer cents. Alice is the only synthetic account, and Cedar is selected server-side. A second Cedar credit tests that legitimate independent applications remain possible; a Birch credit tests rejection of a foreign identifier. This remains a small fixture, not a general authorization system or a payment service.

The initial implementation looks reasonable when read as one request:

```text
BEGIN
read credit where id = supplied id and tenant = Cedar
reject if missing or already applied
mark credit applied
increment Cedar balance by the credit amount
COMMIT
```

It uses bound SQL parameters. It checks the tenant. It even wraps the operation in a Spring transaction at `READ_COMMITTED`. All of that is retained in the vulnerable version.

## Keep both requests alive

A sequential test passes. Apply C-1001 once and receive `200`; apply it again after the first call finishes and receive `409`. The second call sees the committed `applied=true` flag and stops.

An overlapping schedule asks a different question:

| Step | Request A | Request B |
|---|---|---|
| 1 | Reads `applied=false` | |
| 2 | Pauses before writing | Reads `applied=false` |
| 3 | Marks applied, adds 1,000, commits | Still holds its earlier observation |
| 4 | | Marks applied, adds 1,000, commits |

The second unconditional update is allowed to set an already-true flag to true again. Its balance update still runs. Looking only at the final flag would miss the duplicate effect; inspecting the final balance exposes it.

That is a race condition in the business operation. PortSwigger calls this family a limit-overrun race: multiple requests pass a limit check before the relevant state change prevents further use. The exact schedule here is our own controlled experiment. [Race-condition mechanism](https://portswigger.net/web-security/race-conditions)

The lab uses H2 2.4.240. Under its read-committed behavior, connections see committed data and their own changes; an ordinary read does not reserve the observed business condition for that request’s later write. This experiment does not read uncommitted data. Both requests see the genuinely unused credit before either changes it. [H2 transaction isolation and MVCC](https://h2database.com/html/advanced.html#transaction_isolation)

A transaction gives the operation a commit/rollback boundary. It does not, at this isolation level and with these statements, make the earlier `SELECT` an exclusive right to apply the credit. The observed duplicate is compatible with both transactions committing all their own writes.

## Stop measuring luck

A useful regression should not depend on whether two requests happen to arrive within a few milliseconds. The fixture provides a pause immediately after the initial read. Request A and request B each signal arrival, then wait on separate latches. Once both have arrived, the driver releases the selected first request, waits for its HTTP response, and releases the second.

This does not manufacture a new database operation. It holds two real HTTP requests at a point their ordinary execution already passes through. The pause expands the vulnerable interval and makes the tested order explicit. It is demonstration instrumentation, not an estimate of exploit success in a deployed service. Java’s `CountDownLatch` supplies the wait/release mechanism. [Latch contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html)

From the lab directory, with Java 21, Maven, Python 3 and curl available:

```sh
mvn test
mvn -q package -DskipTests
java -jar target/valid-requests-1.0.jar \
  --lab.conditional=false --lab.gates=true
```

In another terminal:

```sh
python3 demo.py --first A
python3 demo.py --first B
```

The [README](https://github.com/AnthonyKot/book21/blob/main/labs/10-valid-requests/README.md) explains the fixture headers and ordinary curl request. The script executes curl against loopback port 8091, using `alice` / `local-only`, a session cookie and a CSRF token. It resets the synthetic database before each schedule. Run demonstrations sequentially; resetting while requests are active invalidates the experiment.

Both schedules produce statuses `[200,200]`, `balanceCents:2000` and `appliedCredits:1`. Neither request needs an invalid parameter. Reverse the order and the same rule fails.

The instrumentation defaults to off. Enable it only for the lab; ordinary calls omit lane and fault headers. A missing CSRF token is still rejected, and a foreign or unknown credit still receives `404`. The race exists despite those independent controls.

## Put the condition in the write

The repaired operation changes the claim statement:

```sql
UPDATE credit
SET applied = TRUE
WHERE id = ? AND tenant = ? AND applied = FALSE
```

It then checks the update count:

```java
int changed = jdbc.update(sql, id, "cedar");
if (changed != 1) {
    throw new ResponseStatusException(CONFLICT);
}
```

`JdbcTemplate.update` reports the number of affected rows. Here, the credit ID is a primary key, so a successful claim affects exactly one row. A request whose condition no longer matches must not continue to the balance increment. [JdbcTemplate update contract](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html)

The initial read is still present to obtain the fixed amount and reject obvious invalid requests. It is no longer the final authority to apply the credit. That decision is made by the conditional write against the database’s current row state. There is no API changing the fixture’s amount concurrently; a product allowing that would need to define how amount changes participate in the operation.

Stop and restart with `--lab.conditional=true --lab.gates=true`, then run both schedules again. Now the first committed claimant receives `200`; the other receives `409`. The final balance is 1,000 and only one credit is applied. The guided tests also release both waiters without choosing a winner and require one success and one conflict.

A second distinct credit still works. Applying C-1001 and C-1002 produces a 2,000-cent balance and two applied credits. The rule limits each credit, not the account’s total activity.

A lock around Java code could serialize callers sharing that lock. It would not automatically coordinate another process or a different code path. This repair places the decision where the shared row is changed. Our test uses one application process and multiple database connections; it does not claim a tested multi-instance deployment. Other designs can use database locks or uniqueness constraints, but their exact transaction behavior also needs verification.

## Keep the claim and effect together

The conditional update is only half the operation. After it succeeds, the service increments the balance:

```sql
UPDATE account
SET balance_cents = balance_cents + ?
WHERE tenant = ?
```

Both statements remain inside the service transaction. A synthetic exception after the claim must restore `applied=false` and balance zero. Another exception after the balance update must restore both values too. The guided tests execute both failure points, then retry and verify a successful single application.

Spring’s ordinary declarative rollback policy rolls back for unchecked exceptions. The fixture uses an `IllegalStateException` and lets it escape the transactional method. Checked exceptions and caught exceptions require deliberate treatment; the presence of an annotation should not replace an executed failure test. [Spring rollback rules](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)

All twenty-four guided cases pass: twelve per implementation, including the reproduction tests that expect the vulnerability. Remove the conditional predicate under the repair assertions:

```sh
mvn -Dtest=CreditRepairTest -Dtest.conditional=false test
```

The three overlap tests fail, while nine others pass. Sequential replay, tenant checks and rollback still work. That negative control explains why the original test suite could look reassuring while missing the business-rule failure.

## A successful response is another boundary

Suppose the database commits, but the caller loses the response. This service returns `409` when that caller retries the consumed credit. It preserves the one-use effect; it does not replay the original successful response. An idempotency-key protocol would need a defined scope, request matching and durable result handling. None is implemented or claimed here.

The balance is a database write in the same transaction. An email, external payment or message sent elsewhere would not be reversed by rolling back these rows. Coordinating such effects needs a separate design and failure experiment. Do not infer “exactly once everywhere” from one conditional update.

The evidence is also database-specific. These statements were run on the pinned H2 version at `READ_COMMITTED`. Porting to a different engine requires tests of its isolation, affected-row behavior, lock failures and retry policy. The lab does not enumerate every schedule, simulate a broken connection, measure throughput or verify a deadlock-recovery strategy. Its state endpoint uses separate queries, so the driver interprets state only after requests finish.

<!--mission-->

## Practice: the worker claims a credit, then fails

A later worker reuses the conditional operation but loses the enclosing transaction. Run `python3 demo.py --worker` with gates enabled. After an injected failure immediately following the claim, the starter reports balance zero and one applied credit. Its retry receives `409`. No duplicate occurred, yet the business rule still failed: the credit was consumed without its promised effect.

Repair `CreditWorker.apply` so the claim and balance change share a rollback boundary. Retain conditional claiming. Verify failures after each write, a retry after failure, ordinary application and overlapping workers. The HTTP adapter is synchronous; direct worker tests use the Spring-managed bean on separate threads, not a simulated queue.

Use the [worksheet](../practice/10-credit-worksheet.md) before the [review guide](../practice/10-credit-review.md). The starter fails three of six exercise tests; the private reference passes all thirty combined cases. Default `mvn test` excludes `WorkerExercise`. After repairing it, run `mvn '-Dtest=*Test,WorkerExercise' test`.

Plan 10–12 hours within the established 10–15-hour week: two for reading and prediction, four for controlled reproduction and repair, two for evidence, and two to four for the named Academy transfer tasks. Carry unfinished work forward and record assistance. For employment, explain which invariant each test protects. For consulting, report the schedules, database and failure points actually assessed, together with the external effects left outside scope.
