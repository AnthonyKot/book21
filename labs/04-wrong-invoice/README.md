# Lab 4 — a valid user can still read the wrong invoice

A small Spring Boot service with two tenants, two users and three invoices. It runs locally with an in-memory database and synthetic data. Nothing here talks to another system.

Verified on 2026-09-13 with Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1 (Spring Framework 7.0.9, Spring Security 7.1.1, Spring Data JDBC 4.1.1) and H2 2.4.240.

Requires Java 21 or later and Maven (3.9.16 was used for verification). Run all commands below from `labs/04-wrong-invoice`. The first Maven run downloads dependencies and needs internet access.

## Who is who

| User | Password | Tenant | Invoices in the tenant |
|---|---|---|---|
| alice | alice-pass | cedar | C-1001, C-1002 |
| bob | bob-pass | birch | B-2001 |

## Run the tests

```
mvn test
```

- `UnscopedReproductionTest` passes because the vulnerability is present: Alice reads and exports Birch's invoice, Bob downloads Alice's export, and the worker runs a queued job naming a foreign invoice.
- `ScopedRepairTest` passes against the repaired lookup: the same requests are refused, and the legitimate reads and exports still work.

To see that the repair tests detect the flaw, change `lab.lookup=scoped` to `lab.lookup=unscoped` in `ScopedRepairTest` and run `mvn test -Dtest=ScopedRepairTest`. Four security tests fail; the two legitimate-path tests still pass. Change it back afterwards.

## Run the service and send requests yourself

```
mvn -q package -DskipTests
java -jar target/wrong-invoice-1.0.jar --server.port=8081 --lab.lookup=unscoped
```

In a second terminal:

```
curl -u alice:alice-pass localhost:8081/invoices/C-1001
curl -u alice:alice-pass localhost:8081/invoices/B-2001
curl -u alice:alice-pass -X POST "localhost:8081/exports?invoiceId=B-2001"
curl -u alice:alice-pass localhost:8081/exports/1
```

Stop the service, start it again with `--lab.lookup=scoped` and repeat the requests.

## Where to look

- `InvoiceAccess.java` — both lookups, side by side. This is the whole repair.
- `InvoiceController.java` — the endpoints. The list endpoint was never vulnerable; ask why.
- `ExportWorker.java` — the worker has no session. It knows the requester only from the job row.

Lab simplifications, not recommendations: plain-text passwords, HTTP Basic, CSRF disabled for a curl-driven API, and a property that switches between the vulnerable and repaired code.

The lab checks tenant isolation and job ownership in one process. It does not enforce separate API/worker database permissions or model per-document export permissions. The essay worksheet adds a tenant-wide export permission as a first extension.
