# Review the security property, including in generated code

The proposed patch adds the missing tenant condition. Its two tests pass: Alice can read her document, and Bob cannot read Alice’s document.

The review still needs one more request.

Let Alice read the document first. Then repeat Bob’s denied request against the same running service. This time the service returns Alice’s private summary to Bob. The repository query has not become less restrictive. It has stopped being called.

This constructed case is about reviewing a plausible repair across its actual response paths. The code and diff are authored learning fixtures, not a captured production incident or a benchmark of a coding model. Generated code deserves the same property-based investigation as handwritten code; neither its provenance nor a confident explanation settles what the program does.

## The patch says something true

The fictional document service has two authenticated users. Alice belongs to Cedar; Bob belongs to Birch. C-1001 belongs to Cedar and B-2001 belongs to Birch. The preview endpoint should disclose only the requesting tenant’s summary.

The [review packet](https://github.com/AnthonyKot/book21/tree/main/labs/11-security-property/review) contains a prepared diff that adds tenant scoping to the repository read. Its central change is:

```diff
- SELECT summary,body FROM document WHERE id=?
+ SELECT summary,body FROM document WHERE id=? AND tenant=?
```

The updated call binds the tenant as well as the identifier. The controller obtains that tenant from a fixed mapping of authenticated principal names, not from `?tenant=`. These are real improvements to the lookup. The candidate’s claim is that this makes the preview safe across tenants.

Now read the caller. It asks an application cache for the document and supplies the scoped query as the cache-loading function. With the candidate configuration, the cache key identifies only the document:

```java
return cache.computeIfAbsent(key,
    ignored -> store.read(id, tenant));
```

The important word is *absent*. Java’s `ConcurrentHashMap.computeIfAbsent` invokes the supplied function when the key has no value; it does not invoke that function for an existing mapping. A hit returns the stored value. The method’s atomicity does not add a tenant check to that return path. [Java 21 cache-operation contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

The diff fixed the path through the loader. It did not establish that every response passes through the loader.

## Expand the review boundary by one call

State the property independently of the proposed implementation:

> A preview response may contain a document’s summary only when that document belongs to the authenticated requester’s tenant, regardless of which requests populated the cache.

The final clause is what the two initial tests omit. Each starts with an empty cache, so each necessarily exercises the repository.

With empty state, Bob’s attempt to read C-1001 reaches the scoped query and receives `404`. Alice’s permitted read then puts the document under C-1001’s shared cache key. Bob’s next request finds that entry and returns its summary without executing the query that would have denied him.

```text
Bob → cache miss → scoped query → 404
Alice → cache miss → scoped query → cache entry → Cedar summary
Bob → cache hit ───────────────────────────────→ Cedar summary
```

Authentication succeeds for Bob in both attempts. His authority has not changed. Another user’s request changed the program’s path.

OWASP recommends validating permissions on every request and locating authorization where it is consistently enforced. In this example, that principle requires inspecting the cache-hit branch, not merely checking that a repository method contains `tenant=?`. [Authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)

The review question becomes concrete: where can this endpoint obtain response data, and what establishes the permission for each source? A database, in-memory cache, previously created export or fallback response can each become a separate answer. You need not inspect the entire codebase indiscriminately, but you must follow the property beyond the edited lines.

## Make the reassuring tests part of the evidence

The [lab](https://github.com/AnthonyKot/book21/tree/main/labs/11-security-property) uses Java 21, Spring Boot 4.1.1 and H2 2.4.240. From its directory, run only the two cold-cache cases first:

```sh
mvn '-Dtest=ReviewReproductionTest#ownCold+foreignCold' test
```

Both pass with the flawed shared cache. Keep that result. It explains what the candidate tests established, rather than dismissing them as useless.

Build and start the candidate configuration:

```sh
mvn -q package -DskipTests
java -jar target/security-property-1.0.jar \
  --lab.partition=false --lab.fixtures=true
```

Then run `python3 demo.py`, which executes the curl sequence and records response bodies and cache diagnostics. The [README](https://github.com/AnthonyKot/book21/blob/main/labs/11-security-property/README.md) also gives the three individual curl commands. Use only the synthetic accounts `alice` and `bob`, password `local-only`, on loopback port 8092.

Expect Bob’s C-1001 requests to change from `404` before Alice’s read to `200` afterwards. The second response contains `CEDAR-PRIVATE-SUMMARY`. The script also reverses the tenants: Bob warms B-2001 and Alice then receives Birch’s summary. No simultaneous requests or timing trick is needed.

The `bodyLoads` counter records repository body-load attempts, including denied misses. It stays unchanged on the unauthorized warm hit. This is supporting evidence for the skipped path; the decisive disclosure evidence is the other tenant’s summary in the actual HTTP response. The counter is not a count of every SQL query or a performance measurement.

An anonymous request still receives `401`, even after the cache is warm. That useful result limits the finding to authenticated cross-tenant access in this fixture. It does not make the leaked summary acceptable.

## A finding another engineer can reproduce

A good review comment names a failure and gives the recipient a way to test it. For this candidate, an actionable comment would be:

> The scoped query runs only on a cache miss. After Alice previews C-1001, Bob’s identical document request returns Cedar’s summary from the shared key without calling that query. Please partition cached documents by the trusted tenant as well as ID, retain the scoped loader, and test both cross-tenant warm-up directions while preserving owner cache hits.

That comment distinguishes the true improvement from the unsupported conclusion. It does not claim that the query is injectable, that Basic authentication is bypassed, or that every document is always exposed. Those would need different evidence.

NIST SSDF 1.1 task PW.7.2 calls for recording and triaging review findings and recommended remediation in the development workflow. Here, the useful record contains the property, request sequence, observed disclosure, affected branch, repair and retest. A catalogue label alone does not supply those details. [SSDF code-review practice](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-218.pdf)

## Repair the key without removing the useful behavior

The guided repair uses a structured key:

```java
private record Key(String tenant, String id) {}
```

In repaired mode, the trusted tenant fills the first component. The scoped loader remains unchanged. Bob cannot address Alice’s cached entry; his own tenant-and-ID key misses, and the repository denies the lookup. An exception from the loading function leaves that mapping unestablished, so the denied attempt does not create an entry that poisons Alice’s later read.

Stop the server and restart with `--lab.partition=true --lab.fixtures=true`. The partition flag defaults to true. Repeat the same demo. Both foreign attempts now receive `404`, whether the other tenant has warmed the cache or not. Repeated owner requests still return the right summary without another body load. In the demo’s repaired sequence, Alice’s load leaves `bodyLoads` at two because Bob already made a denied cold attempt. Bob’s later request uses a different tenant key, tries the scoped loader and raises the count to three while returning `404`; it does not create another cache entry.

The complete guided suite runs twenty tests, ten in each mode. It includes both warm-up directions, repeated owner reads, a denied miss followed by an owner read, a second owner document, an ignored caller-supplied tenant parameter, missing documents and anonymous access.

The reproduction class expects the flawed behavior. To verify that the repair assertions actually detect its return, run:

```sh
mvn -Dtest=ReviewRepairTest -Dtest.partition=false test
```

Exactly the two warm cross-tenant tests fail; eight pass. This negative control makes the missing coverage visible: the cold-cache checks remain green when the security property is broken.

The repair is scoped to this fixture’s stable tenant mapping and document ownership. A tenant in the key is not a universal permission model. Membership changes, ownership transfers, per-user grants and revocation create additional questions about whether a cached decision is still valid. There is no TTL, eviction policy, distributed invalidation or content-update API here. Nor is this a browser or CDN cache experiment.

## Put a model’s review through the same process

Before asking an AI tool for help, save your own property, call-path trace and proposed counterexample. Then give it the relevant callers and data access, not just the green diff. Ask it to identify an executable violating sequence, the branch that permits it, and a legitimate case the proposed fix must preserve.

Treat each returned finding as a claim. Reproduce the mechanism or inspect the relevant API contract. Record whether the claim was confirmed, rejected or remains unresolved. Run the suggested repair against both denied and allowed behavior. A plausible suggestion to “disable caching” may stop the leak while unnecessarily removing the requested cache-hit behavior; a suggestion to “use a concurrent map” does not address the missing tenant dimension at all.

The worksheet includes a review prompt and a comparison ledger. Count confirmed additions and rejected claims, and record investigation time and the context supplied. One guided example cannot establish that a model is generally better, worse or faster at security review. If it was given the answer-bearing tests or the walkthrough, disclose that assistance instead of calling the result an independent discovery.

The separate AGY review used in authoring this chapter receives the manuscript and verification evidence. It is an editorial and technical check with findings verified afterwards, not a blind contest between a person and a model. This distinction also applies to generated patches: execution evidence belongs to the particular artifact and configuration, not to the reputation of its author.

<!--mission-->

## Practice: the action changes after the preview

The second endpoint returns full document content, while preview returns only a summary. The product rule permits a tenant to preview its archived summaries, but forbids downloading full content when the download authorization check observes archived state.

The starter reuses the partitioned preview cache for download without checking that action’s current rule. Run `python3 demo.py --exercise`. Alice warms C-1001, the fixture commits its archive, and the next download still returns `CEDAR-FULL-INVOICE-CONTENT`. An empty cache does not solve this case either: the preview loader deliberately permits archived documents.

The preview cache holds a complete document record even though its HTTP response selects only the summary. Sharing that richer cached value makes the second action easy to implement and easy to authorize incorrectly. A production design should assess caching only the required fields or separating the data paths; an action-specific cache key alone would still not establish current download permission.

Repair `DownloadService.download` using the existing current-state authorization query before returning full content. Retain visible archived summaries, downloads of unaffected documents and cross-tenant denial. The [worksheet](../practice/11-review-worksheet.md) provides the required cases; the [review guide](../practice/11-review-guide.md) supplies progressive hints. Two of six starter exercise tests fail. The private reference passes all twenty-six combined cases; run `mvn '-Dtest=*Test,ArchiveExercise' test` after your repair because default Maven naming excludes the unfinished exercise.

These tests archive first and download afterwards. They do not establish cancellation of a response already authorized before a concurrent archive, revocation across multiple processes or removal of copies already delivered. State the policy’s decision point instead of claiming instant global revocation.

Plan 10–12 hours within the 10–15-hour week: two for reading and your first review, four for reproduction and repair, two for verified model comparison and evidence, and two to four for the named Academy tasks. Carry unfinished work forward. For employment, present the finding and preserved behavior as a constructive review. For consulting, name the endpoints, cache states and policy changes assessed, along with the paths you did not inspect. In both cases, let the evidence show what you can defend.
