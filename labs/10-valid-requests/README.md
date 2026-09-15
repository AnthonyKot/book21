# Two requests, one credit — essay 10 lab

Java 21, Maven 3.9+, Python 3 and curl. Tested Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1, Spring Framework 7.0.9, Security 7.1.1 and H2 2.4.240. Loopback only; port 8091 must be free. Synthetic Alice (`alice` / `local-only`) belongs to Cedar. Each Cedar credit is worth 1000 cents. No money, external payment service or production data is involved.

```sh
mvn test
mvn -q package -DskipTests
java -jar target/valid-requests-1.0.jar --lab.conditional=false --lab.gates=true
```

From this directory in another terminal:

```sh
python3 demo.py --first A
python3 demo.py --first B
```

The Python script executes curl. It obtains a CSRF token and session cookie, resets only the synthetic in-memory data, starts two apply requests, waits until both observed an unused credit, then releases them in the requested order. It waits for the first HTTP response (after transaction completion) before releasing the second. Expect statuses `[200,200]`, balance 2000 and one applied credit in either order.

Stop Java with Ctrl-C. Restart with `--lab.conditional=true --lab.gates=true` and repeat. Expect `[200,409]`, balance 1000, one applied credit. The flag defaults to true; instrumentation defaults to false. Without gates enabled, ordinary application requests still work and no lane header is needed. Automated tests use random API ports; `demo.py --port NUMBER` can target an explicitly chosen loopback port.

Both modes retain `@Transactional(isolation=READ_COMMITTED)` on `CreditService.apply`. Both initially read the credit with a tenant filter and use bound SQL parameters. Vulnerable mode updates `applied=TRUE` without requiring the current value to be false. Repair mode adds `AND applied=FALSE` and requires exactly one changed row before incrementing the balance. The claim and balance update share the service transaction. The amount is fixed fixture data; there is no API to change it concurrently.

## Ordinary HTTP request

With the server running, these commands apply C-1002 (the demos consume C-1001). The first application succeeds; repetition gets 409.

```sh
curl -sS -u alice:local-only -c cookies.txt http://127.0.0.1:8091/csrf > csrf.json
TOKEN=$(python3 -c 'import json; print(json.load(open("csrf.json"))["token"])')
curl -sS -u alice:local-only -b cookies.txt -H "X-CSRF-TOKEN: $TOKEN" \
  -X POST -w '\n[HTTP %{http_code}]\n' http://127.0.0.1:8091/api/credits/C-1002/apply
curl -sS -u alice:local-only http://127.0.0.1:8091/api/state
```

CSRF remains enabled; the cookie and token belong together. Missing CSRF returns 401 in this Basic-auth setup, as does missing Basic auth with a valid CSRF token. This is the observed response for this fixture’s authentication and error-handling configuration; do not treat 401 as a universal CSRF status. A foreign or unknown credit returns 404. This one-account fixture keeps Cedar fixed server-side; it is not a general tenant-authorization implementation.

## Controlled schedule and verification

Default `mvn test` runs 24 cases: twelve per mode. Real HTTP tests include sequential replay, A-first/B-first overlap, release-both overlap, distinct credits, authorization, and rollback after synthetic failures both after claiming and after updating the balance. The release-both test does not require a particular winner. Each context has its own in-memory H2 database and requests use separate pooled connections.

The gate uses latches, not a timing sleep to create the vulnerability. Headers `X-Lab-Lane: A` and `B` pause after the initial read. Each lane accepts one request per reset and times out after 30 seconds. Polling `/lab/gates` observes arrival; POST `/lab/release/A` or `/lab/release/B` releases it. `X-Lab-Fault: claim` or `balance` injects an unchecked exception at that stage. Fault injection and tagged gate requests require `lab.gates=true`.

All lab-control endpoints require authentication and POSTs require CSRF. They are demonstration tooling, not production APIs. Run one demonstration at a time; **never reset while requests are in flight**. Reset releases existing waiters and rewrites fixture state; it is not a transactional administration operation. Ordinary requests omit these headers. The gate is a test seam, not the repair.

```sh
mvn -Dtest=CreditRepairTest -Dtest.conditional=false test
```

Expected negative control: three overlap failures and nine passes, no errors. Sequential replay and rollback still pass in vulnerable mode. This is why a green ordinary-request test and the presence of a transaction are insufficient evidence of the one-use rule.

## Independent practice: a second caller

Start with [worksheet Part B](../../practice/10-credit-worksheet.md). The worker applies the same credits through `/api/credits/{id}/worker` or an injected `CreditWorker` bean. Its HTTP adapter is synchronous; no queue or delivery system is modeled. The `lab.conditional` switch selects the guided service mode; it does not change the worker. Keep lab controls and existing guided behavior intact.

Write your own tests in a class ending in `Test`. You may reuse `CreditHttp` for authenticated requests, state assertions and gates, or build your own fixture. Its `apply()` and `send()` helpers target the guided service; use `request(id, "worker", ...)` for HTTP worker calls or invoke the injected worker bean directly. The documented lane and fault controls work with either entry point. Use the Spring-managed bean for direct calls; do not construct a replacement object or put a transaction on the test itself. Reset only after calls finish. Read the two state values only after all tested calls complete.

`WorkerContractCheck.java` is a **post-attempt comparison**: keep it closed until you save your own tests, initial results and decision. It is excluded by Maven's default naming and disabled even in IDE “run all” unless explicitly enabled. After the attempt:

```sh
mvn -DreviewCheck=true -Dtest=WorkerContractCheck test
mvn -DreviewCheck=true '-Dtest=*Test,WorkerContractCheck' test
```

The check assesses observable behavior, not a particular annotation or code diff. Interpret differences with the [review guide](../../practice/10-credit-review.md); a passing supplied check does not substitute for your tests. The guided demo covers the service only; use your own calls/tests for the worker investigation.

## Limits

The exact row and transaction behavior was executed with H2 2.4.240 at READ_COMMITTED. Porting SQL to another database is not production verification. No multi-process deployment, connection failure, deadlock-retry policy, throughput target or complete schedule enumeration is tested. State inspection uses separate queries and is only interpreted after requests finish; it is not a consistent concurrent snapshot.

The balance increment is inside the same database transaction. An email, payment or message sent to another system would not be undone by rolling it back. Response replay after a lost success response and idempotency-key protocols are separate work. This service returns 409 for a consumed credit rather than replaying a previous successful response. Database reset, latch release and fault headers exist only to make the synthetic experiment inspectable.
