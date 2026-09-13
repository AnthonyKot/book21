# A valid user can still read the wrong invoice

In our local fixture, Alice belongs to Cedar Dental. She requests invoice C-1001 and receives Cedar's bill. Now suppose she obtains B-2001 from a forwarded email and requests that identifier instead:

```
$ curl -sS -w '\n[HTTP %{http_code}]\n' -u alice:alice-pass localhost:8081/invoices/B-2001
{"id":"B-2001","tenantId":"birch","customer":"Birch Legal","amountCents":990000}
[HTTP 200]
```

That is another company's invoice. Nothing about the request was unusual. Alice's HTTP Basic credentials were valid, and the identifier was well formed. Spring Security did exactly what it was configured to do: the same request without credentials gets `401`. The service established *who* was asking and never asked *whether this invoice is theirs*.

This essay reproduces that failure in a small Spring Boot service, repairs it, and follows the same question into two further places where the policy must hold: the export worker and the download of a finished export. The lab is in [labs/04-wrong-invoice](https://github.com/AnthonyKot/book21/tree/main/labs/04-wrong-invoice). It runs locally with synthetic tenants and an in-memory database.

The starting policy is intentionally smaller than essay 2's: membership permits reading and exporting any invoice in the same tenant; a finished export also belongs to its requester. The worked repair implements that policy. The exercise adds a separate export permission and revocation cases. This lets us inspect tenant isolation before combining it with another rule.

To follow along, use Java 21 or later and Maven; run `mvn test` in the lab directory. Its [README](https://github.com/AnthonyKot/book21/blob/main/labs/04-wrong-invoice/README.md) gives the exact commands for starting the vulnerable and repaired versions. The examples below use synthetic data only.

## Authentication is not ownership

In essay 3 the browser-to-API crossing was the first trust boundary. Two different things cross it together. One is the authentication evidence: HTTP Basic credentials in this lab, or a session in another design. The API validates it to establish a principal. The other is the identifier in the path, which is only the caller's choice of object. Authentication does not turn that choice into a right.

The failure has a name, insecure direct object reference or IDOR: the application uses input from the user to fetch an object directly, without checking that the user may have it. PortSwigger classes it as horizontal privilege escalation, reaching resources of another user of the same kind rather than functions reserved for a higher role.

Here is the code that answered Alice:

```java
public Optional<Invoice> invoiceFor(String username, String invoiceId) {
    return invoices.findById(invoiceId);
}
```

The method receives the username and ignores it. A happy-path test using Alice and C-1001 would not expose the omission: both a scoped and an unscoped lookup return the expected invoice. The discriminating input is a real invoice belonging to someone else. A random nonexistent identifier tests absence, not this boundary.

## Why the list was already right

The same fixture has a list endpoint that already filters by tenant. Its response is abbreviated here:

```
$ curl -u alice:alice-pass localhost:8081/invoices
[{"id":"C-1001",...},{"id":"C-1002",...}]
```

Its query is `findByTenantId(tenantOf(user))`. An unfiltered list can expose the mismatch immediately by including both companies. A lookup by identifier returns one plausible row either way, so the missing condition remains invisible when you test only with your own data. The two endpoints call different repository methods. Evidence about one does not establish the behaviour of the other.

Making identifiers unguessable does not change the question either. Replace `B-2001` with a random UUID and the flaw is still there; you have only made the identifier harder to find. Identifiers leak through emails, exports, shared screens, logs and other endpoints. PortSwigger's Academy has a lab built around exactly this: user accounts identified by GUIDs that turn up elsewhere in the application.

## Put the tenant condition in the lookup

There are three common places to add the missing check:

| Where | How | What must hold |
|---|---|---|
| Service code after loading | Load by ID, compare the row's tenant with the user's tenant | Deny before releasing data or performing an unauthorized effect; callers use this service |
| Method security | Use `@PostAuthorize` to check a returned object | Method security is enabled and the call is intercepted; the method has not already performed an unauthorized effect |
| In the query | Look up by ID and trusted tenant | Callers use the scoped lookup and supply a tenant established by trusted code |

Each approach can centralize a rule, and each can be bypassed by a caller using a different path. The lab uses the third because tenant equality fits directly in the query and the lookup does not retrieve a foreign row:

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

Where the tenant comes from matters as much as the condition. `users.tenantOf(username)` reads the service's own user table, keyed by the name Spring Security authenticated. It does not read a `tenant` parameter or an `X-Tenant-Id` header, both of which the caller controls. Accepting an attacker-selected tenant without validating membership would defeat the condition. A product where users can switch among several tenants needs an explicit check on that selection; this fixture gives each user one tenant.

The other two options are not wrong. A team that already uses method security consistently may prefer the annotation. The property is that a user receives an invoice only when the tenant condition holds. The choice of syntax does not establish that every caller enforces it.

## Refuse the same way as a missing invoice

With the repair, Alice's request for B-2001 returns `404`, with the same empty body as a request for an invoice that does not exist. That is a choice. Returning `403` for an existing foreign invoice and `404` for a missing one would distinguish existence. A `403` by itself does not necessarily reveal that distinction. For identifiers from another tenant, the lab chooses not to say. A test pins the choice so nobody changes it by accident:

```java
var foreign = call("alice", "GET", "/invoices/B-2001");
var missing = call("alice", "GET", "/invoices/X-9999");
assertThat(foreign.statusCode()).isEqualTo(404);
assertThat(missing.statusCode()).isEqualTo(404);
assertThat(foreign.body()).isEqualTo(missing.body());
```

This makes the status and body indistinguishable. It does not make timing indistinguishable, and the lab does not measure that.

## The worker has no session

Exports run later, in a worker. It has no interactive user request or session. It still runs with the application's database access; it knows who asked only from the job row the API wrote, which is why essay 3 made that row the thing to protect.

The vulnerable version fails here in two ways. First, the API's own check used the unscoped lookup, so Alice's `POST /exports?invoiceId=B-2001` was accepted and the worker produced Birch's invoice. Second, and more instructive, the worker also used the unscoped lookup. The lab's reproduction test inserts a queued row naming Alice and B-2001 directly, then runs the worker. This isolates the worker's behaviour if inconsistent job data reaches it; it does not demonstrate an attacker obtaining database write access:

```
worker: queued row alice/B-2001 -> DONE      (unscoped)
worker: queued row alice/B-2001 -> DENIED    (scoped)
```

The repaired worker calls the same `invoiceFor(job.requester(), job.invoiceId())` as the controller. It checks the original requester's tenant as it is *now*, not a flag saying the API once approved the job. The selected policy requires a current check at generation. A historical acceptance decision would not establish that permission still holds.

## A second identifier: the export job

A finished export is fetched with `GET /exports/{jobId}`. That is another object reached by an identifier from the path, and job numbers are sequential:

```
bob   GET  /exports/2   -> 200 {"requester":"alice","invoiceId":"C-1001",...,"content":"C-1001,Cedar Dental,120000"}
```

The repair looks up the job by ID *and* requester, then re-applies the invoice lookup, so a job whose source invoice the user can no longer see is not downloadable either. After the repair Bob gets `404`. The invoice identifier and job identifier therefore require related but different checks: tenant membership for the source, and requester ownership for the result.

## Prove the repair, and prove the proof

The lab has two test classes exercising the vulnerable and repaired versions. Endpoint cases use real HTTP; worker cases invoke the worker directly so the test controls when it runs:

| Request | Unscoped (vulnerable) | Scoped (repaired) |
|---|---|---|
| Alice reads C-1001 | 200 | 200 |
| Alice reads B-2001 | 200, Birch Legal | 404, same body as a missing invoice |
| Alice exports B-2001 | 202, job DONE with Birch's data | 404, no job row created |
| Bob downloads Alice's job | 200, Cedar's data | 404 |
| Worker runs queued alice/B-2001 | DONE | DENIED |
| Alice exports C-1001 and downloads it | Works | Works |

Two rows keep the repair honest. The legitimate reads and exports must keep working, or "deny everything" would pass. We also need evidence that the security assertions distinguish the repair from the known flaw. Running `ScopedRepairTest` with the unscoped lookup gives four failures, exactly the four security tests, while the two legitimate-path tests still pass. That negative control supports a specific claim: these assertions detect these defects. It does not establish coverage of every authorization path.

## What this repair does not cover

The tests support the repaired behaviour for the cases shown. The access component does not stop the next developer from adding an endpoint that calls `invoices.findById` directly. PortSwigger's prevention advice points the same way: use one application-wide mechanism, deny by default, and test access controls. For this lab that means routing single-invoice and job access through `InvoiceAccess`; the list retains its separate tenant-scoped query and needs its own regression coverage. Essay 12 will investigate how a static-analysis rule can detect bypasses, including its misses.

The demonstration also does not cover lookups by other keys (invoice number, search, bulk endpoints), native SQL, caches keyed only by invoice ID, or support tools that act across tenants on purpose. Each needs the same question asked of its own path.

The deployment boundary is also simplified. Controller and worker share one process and database access. This lab tests application authorization; it does not implement essay 3's separate service credentials or restrictions on job-field updates. Likewise, re-reading permission does not make the check and its later effect atomic. Concurrency remains a separate problem.

<!--mission-->

## Practice: permission changes while the export waits

Use the [lab worksheet](../practice/04-lab-worksheet.md). This is an executable exercise: you will change the Spring Boot lab and run its tests.

Add a tenant-wide export permission, separate from membership. This first extension is coarser than essay 2's per-document permission; record that difference. A new Cedar user, Carol, can read Cedar invoices but has no export permission. Alice has it. Retain essay 2's revocation policy: revocation before generation prevents generation, and revocation after completion prevents a new download. Choose the refusal responses and job states that express those outcomes. Write the tests first, watch them fail, then make them pass without breaking `ScopedRepairTest`.

Then solve two PortSwigger Academy labs without the published solution, and note which hints you used: [User ID controlled by request parameter, with unpredictable user IDs](https://portswigger.net/web-security/access-control/lab-user-id-controlled-by-request-parameter-with-unpredictable-user-ids) and [Insecure direct object references](https://portswigger.net/web-security/access-control/lab-insecure-direct-object-references). Both are Apprentice level. For each, write one sentence naming the missing check and where it belongs.

Try the worksheet before opening the [review notes](../practice/04-lab-review.md).

If stuck, first decide the policy in a sentence, before touching code. Next, list every place that currently calls `invoiceFor`; each is a candidate for the new check. Finally, distinguish the worker's database credentials from the originating user's product permission. The latter must be read when the worker runs.

Completion means your tests show Carol can still read but cannot export, a denied job after revocation, a blocked download after revocation, and Alice's normal export still working, and you can explain why each check sits where it does.

Source note: primary sources inspected on 13 September 2026. Definitions and prevention guidance follow PortSwigger's [access control](https://portswigger.net/web-security/access-control) and [IDOR](https://portswigger.net/web-security/access-control/idor) pages. ASVS 5.0.0 requirement [8.2.2](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x17-V8-Authorization.md) asks for data-specific access restricted to consumers with explicit permission, and 8.3.1 for enforcement at a trusted service layer. The lab, its data and its outputs are original; test runs, curl traces and SQL logs were executed on Java 21.0.12 with Spring Boot 4.1.1 and are recorded in the authoring evidence. The repository contains the executable tests and run instructions. The method-security comparison follows Spring Security's [method authorization reference](https://docs.spring.io/spring-security/reference/7.1/servlet/authorization/method-security.html); the lab itself uses explicit service code.
