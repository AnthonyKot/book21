# A valid user can still read the wrong invoice

Alice works for Cedar Dental. She signs in to the document service, opens invoice C-1001 and sees her company's bill. Then she changes the last part of the address to B-2001, an identifier she saw in a forwarded email:

```
$ curl -u alice:alice-pass localhost:8081/invoices/B-2001
{"id":"B-2001","tenantId":"birch","customer":"Birch Legal","amountCents":990000}  [HTTP 200]
```

That is another company's invoice. Nothing about the request was unusual. Alice's password was correct, her session was valid, and the identifier was well formed. Spring Security did exactly what it was configured to do: the same request without credentials gets `401`. The service established *who* was asking and never asked *whether this invoice is theirs*.

This essay reproduces that failure in a small Spring Boot service, repairs it, and follows the same question into the two places developers usually forget: the export worker and the download of a finished export. The lab is in [labs/04-wrong-invoice](https://github.com/AnthonyKot/book21/tree/main/labs/04-wrong-invoice). It runs locally with synthetic tenants and an in-memory database.

## Authentication is not ownership

In essay 3 the browser-to-API crossing was the first trust boundary. Two different things cross it together. One is the session, which the API validates and can believe. The other is the identifier in the path, which is only the caller's choice of object. A valid session does not turn that choice into a right.

The failure has a name, insecure direct object reference or IDOR: the application uses input from the user to fetch an object directly, without checking that the user may have it. PortSwigger classes it as horizontal privilege escalation, reaching resources of another user of the same kind rather than functions reserved for a higher role.

Here is the code that answered Alice:

```java
public Optional<Invoice> invoiceFor(String username, String invoiceId) {
    return invoices.findById(invoiceId);
}
```

The method receives the username and ignores it. Nobody writes that deliberately. It arrives by a shorter route: a controller that calls `repository.findById(id)` because every Spring Data repository already has that method, and the endpoint's test only ever used an invoice that belonged to the test user.

## Why the list was already right

The same service has a list endpoint, and it never leaked:

```
$ curl -u alice:alice-pass localhost:8081/invoices
[{"id":"C-1001",...},{"id":"C-1002",...}]
```

Its query is `findByTenantId(tenantOf(user))`. A list *needs* a filter; without one it returns every row, which somebody notices in the first demo. A lookup by identifier returns one plausible row either way, so the missing condition is invisible when you test with your own data. That asymmetry is why "the list is scoped" is weak evidence that the detail endpoint is.

Making identifiers unguessable does not change the question either. Replace `B-2001` with a random UUID and the flaw is still there; you have only made the identifier harder to find. Identifiers leak through emails, exports, shared screens, logs and other endpoints. PortSwigger's Academy has a lab built around exactly this: user accounts identified by GUIDs that turn up elsewhere in the application.

## Put the owner in the query

There are three common places to add the missing check:

| Where | How | What goes wrong |
|---|---|---|
| After loading | Load by ID, then compare `invoice.tenantId()` with the user's tenant | Works, but every caller must remember the comparison, and the foreign row is already in memory |
| Framework annotation | `@PostAuthorize` on the method, checking the returned object | Same load-then-check shape; the rule lives far from the query |
| In the query | Look up by ID *and* tenant | The foreign row is never loaded, so there is nothing to forget to discard |

The lab uses the third:

```java
public Optional<Invoice> invoiceFor(String username, String invoiceId) {
    return invoices.findByIdAndTenantId(invoiceId, users.tenantOf(username));
}
```

`findByIdAndTenantId` is a derived query: Spring Data builds the SQL from the method name. With SQL logging on, the two versions differ by one condition:

```
unscoped: SELECT ... FROM "INVOICE" WHERE "INVOICE"."ID" = ?
scoped:   SELECT ... FROM "INVOICE" WHERE "INVOICE"."ID" = ? AND ("INVOICE"."TENANT_ID" = ?)
```

Where the tenant comes from matters as much as the condition. `users.tenantOf(username)` reads the service's own user table, keyed by the name Spring Security authenticated. It does not read a `tenant` parameter or an `X-Tenant-Id` header, both of which the caller controls. A scoped query fed a tenant the attacker chose is the original flaw with extra steps.

The other two options are not wrong. A team that already uses method security consistently may prefer the annotation. What matters is the property, not the syntax: every path that loads an invoice for a user applies the user's tenant.

## Refuse the same way as a missing invoice

With the repair, Alice's request for B-2001 returns `404`, with the same empty body as a request for an invoice that does not exist. That is a choice. A `403` would be honest about the refusal, but it would also confirm to Alice that B-2001 exists. For identifiers from another tenant, the lab chooses not to say. A test pins the choice so nobody changes it by accident:

```java
var foreign = call("alice", "GET", "/invoices/B-2001");
var missing = call("alice", "GET", "/invoices/X-9999");
assertThat(foreign.statusCode()).isEqualTo(404);
assertThat(foreign.body()).isEqualTo(missing.body());
```

This makes the status and body indistinguishable. It does not make timing indistinguishable, and the lab does not measure that.

## The worker has no session

Exports run later, in a worker. The worker has no HTTP request, no session and no user. It knows who asked only from the job row the API wrote, which is why essay 3 made that row the thing to protect.

The vulnerable version fails here in two ways. First, the API's own check used the unscoped lookup, so Alice's `POST /exports?invoiceId=B-2001` was accepted and the worker produced Birch's invoice. Second, and more instructive, the worker also used the unscoped lookup. The lab's reproduction test inserts a queued row naming Alice and B-2001 directly, the way a retry tool, a migration or a second producer could, and runs the worker:

```
worker: queued row alice/B-2001 -> DONE      (unscoped)
worker: queued row alice/B-2001 -> DENIED    (scoped)
```

The repaired worker calls the same `invoiceFor(job.requester(), job.invoiceId())` as the controller. It checks the original requester's tenant as it is *now*, not a flag saying the API once approved the job. A check at the front door does not travel with the work; it has to be made again where the work happens.

## A second identifier: the export job

A finished export is fetched with `GET /exports/{jobId}`. That is another object reached by an identifier from the path, and job numbers are sequential:

```
bob   GET  /exports/2   -> 200 {"requester":"alice","invoiceId":"C-1001",...,"content":"C-1001,Cedar Dental,120000"}
```

The repair looks up the job by ID *and* requester, then re-applies the invoice lookup, so a job whose source invoice the user can no longer see is not downloadable either. After the repair Bob gets `404`. Every new identifier the service hands out is a new place to ask the same question.

## Prove the repair, and prove the proof

The lab has two test classes that send the same real HTTP requests to a running instance:

| Request | Unscoped (vulnerable) | Scoped (repaired) |
|---|---|---|
| Alice reads C-1001 | 200 | 200 |
| Alice reads B-2001 | 200, Birch Legal | 404, same body as a missing invoice |
| Alice exports B-2001 | 202, job DONE with Birch's data | 404, no job row created |
| Bob downloads Alice's job | 200, Cedar's data | 404 |
| Worker runs queued alice/B-2001 | DONE | DENIED |
| Alice exports C-1001 and downloads it | Works | Works |

Two rows keep the repair honest. The legitimate reads and exports must keep working, or "deny everything" would pass. And the repair tests must fail against the vulnerable code, or they prove nothing. Running `ScopedRepairTest` with the unscoped lookup gives four failures, exactly the four security tests, while the two legitimate-path tests still pass. A regression test you have never seen fail is a guess.

## What this repair does not cover

The repair makes one component correct. It does not stop the next developer from adding an endpoint that calls `invoices.findById` directly. PortSwigger's prevention advice points the same way: use one application-wide mechanism, deny by default, and test access controls. For this lab that means keeping repository access behind `InvoiceAccess`. Enforcing that with a static-analysis rule is a job for essay 12.

It also does not cover lookups by other keys (invoice number, search, bulk endpoints), native SQL, caches keyed only by invoice ID, or support tools that act across tenants on purpose. Each needs the same question asked of its own path.

And tenant membership is not the whole policy. In essay 2, exporting needed an export permission as well as membership. The lab does not model it yet. That is your task.

<!--mission-->

## Practice: permission changes while the export waits

Use the [lab worksheet](../practice/04-lab-worksheet.md). This is an executable exercise: you will change the Spring Boot lab and run its tests.

Add an export permission, separate from membership. A new Cedar user, Carol, can read Cedar invoices but has no export permission. Alice has it. Decide and enforce what happens when Alice's permission is revoked after her export is queued but before the worker runs, and after it completes but before she downloads it. Write the tests first, watch them fail, then make them pass without breaking `ScopedRepairTest`.

Then solve two PortSwigger Academy labs without the published solution, and note which hints you used: [User ID controlled by request parameter, with unpredictable user IDs](https://portswigger.net/web-security/access-control/lab-user-id-controlled-by-request-parameter-with-unpredictable-user-ids) and [Insecure direct object references](https://portswigger.net/web-security/access-control/lab-insecure-direct-object-references). Both are Apprentice level. For each, write one sentence naming the missing check and where it belongs.

Try the worksheet before opening the [review notes](../practice/04-lab-review.md).

If stuck, first decide the policy in a sentence, before touching code. Next, list every place that currently calls `invoiceFor`; each is a candidate for the new check. Finally, remember the worker runs as nobody: the permission to check is the job's requester's, read at the moment the worker runs.

Completion means your tests show a refused export for Carol, a denied job after revocation, a blocked download after revocation, and Alice's normal export still working, and you can explain why each check sits where it does.

Source note: primary sources inspected on 13 September 2026. Definitions and prevention guidance follow PortSwigger's [access control](https://portswigger.net/web-security/access-control) and [IDOR](https://portswigger.net/web-security/access-control/idor) pages. ASVS 5.0.0 requirement [8.2.2](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x17-V8-Authorization.md) asks for data-specific access restricted to consumers with explicit permission, and 8.3.1 for enforcement at a trusted service layer. The lab, its data and its outputs are original; test runs, curl traces and SQL logs were executed on Java 21.0.12 with Spring Boot 4.1.1 and are recorded with the lab.
