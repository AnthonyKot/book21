# Review the security property, including in generated code

The proposed patch adds the missing tenant condition. Its two tests pass: Alice can read her document, and Bob cannot read Alice's document.

The review still needs one more request.

Let Alice read the document first. Then repeat Bob's denied request against the same running service. This time the service returns Alice's private summary to Bob. The repository query has not become less restrictive. It has stopped being called.

This constructed case is about reviewing a plausible repair across every path that can produce a response. The same discipline applies whoever wrote the change. Generated code arrives with a fluent explanation and green tests; neither settles what the program does.

## The patch says something true

The fictional document service has two authenticated users. Alice belongs to Cedar; Bob belongs to Birch. C-1001 belongs to Cedar and B-2001 belongs to Birch. The preview endpoint should disclose only the requesting tenant's summary.

The [review packet](https://github.com/AnthonyKot/book21/tree/main/labs/11-security-property/review) contains a prepared diff that adds tenant scoping to the repository read:

```diff
- "SELECT summary, body FROM document WHERE id = ?"
+ "SELECT summary, body FROM document WHERE id = ? AND tenant = ?"
```

The call binds the tenant as well as the identifier, and the controller obtains that tenant from the authenticated principal, not from `?tenant=`. These are real improvements. The candidate's claim goes further: the preview is now safe across tenants.

Now read the caller. It asks an application cache for the document and supplies the scoped query as the loading function. In the candidate configuration, the cache key identifies only the document:

```java
Key key = new Key("", id);
return cache.computeIfAbsent(key, ignored -> store.read(id, tenant));
```

The important word is *absent*. Java's `ConcurrentHashMap.computeIfAbsent` invokes the supplied function only when the key has no value. A hit returns the stored value, and the method's atomicity adds no tenant check to that return. [Java 21 cache-operation contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

The diff fixed the path through the loader. It did not establish that every response passes through the loader.

## Follow every source of the response

State the property independently of the proposed implementation:

> A preview response may contain a document's summary only when that document belongs to the authenticated requester's tenant, regardless of which requests populated the cache.

The final clause is what the two tests omit. Each starts with an empty cache, so each necessarily exercises the repository:

```text
Bob   → cache miss → scoped query → 404
Alice → cache miss → scoped query → cache entry → Cedar summary
Bob   → cache hit ──────────────────────────────→ Cedar summary
```

Authentication succeeds for Bob both times, and his authority has not changed. Another user's request changed the program's path.

The review question becomes concrete: **where can this endpoint obtain its response, and what authorizes each source?** A database query, a replica, an in-memory cache or a fallback value can each be a separate answer. OWASP's advice to check permissions on every request, where they are consistently enforced, means inspecting the cache-hit branch, not merely confirming that one repository method contains `tenant = ?`. [Authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html) You need not read the whole codebase, but you do have to follow the property beyond the edited lines.

The [lab](https://github.com/AnthonyKot/book21/tree/main/labs/11-security-property) reproduces this in Spring Boot. Run the two cold-cache tests first and they pass against the flaw; keep that result, because it shows exactly what the candidate's tests established. Then the README's script, or three curl commands, show Bob's request for C-1001 change from `404` to `200` with `CEDAR-PRIVATE-SUMMARY` after Alice's read, and the same leak in the other direction. A counter of repository body loads stays still on the leaking hit. That supports the explanation, but the decisive evidence is the other tenant's summary in an actual HTTP response.

## Turn the trace into a review method

Start with the effect the rule protects: here, returning a private summary. Write who may receive it, under which conditions, without naming a database or a proposed fix. Then work backwards from each place the response can be produced. For each path, record the source of the value, the decision that permits its release, and where that decision gets its facts. Include branches outside the diff. A caller can bypass a correct helper.

Use that record to choose a counterexample. In this case, the property is unchanged while another request changes the source from repository to cache. Predict what both requests should return before running them. Keep a legitimate request beside the denied one: blocking every response would satisfy the denial while breaking the feature.

The method fits on a small review note:

| Step | Question to answer | Evidence from this case |
|---|---|---|
| State the property | Which effect is allowed, for whom, under what conditions? | Only the authenticated tenant may receive its document's summary. |
| Trace the paths | Where can that effect be produced? | The cache hit and the repository loader can supply the response. |
| Locate authority | What permits the effect on each path, and who supplies those facts? | The loader uses the trusted tenant; the shared cache key omits it. |
| Challenge the claim | Which sequence would distinguish enforcement from an apparent fix? | Compare Bob's request before and after Alice's read; preserve Alice's repeat. |
| Bound the verdict | What was established, and what remains open? | A warm response disclosed Cedar's summary to Birch in this fixture. |

This is a way to organize an investigation, not a complete test catalogue. For another feature, the protected effect might be a payment or a queued operation; follow each place that effect can occur. If you cannot enumerate the relevant paths, record that gap before claiming coverage.

## Write the finding so someone can act on it

A useful review comment names a failure and gives the recipient a way to test it:

> The scoped query runs only on a cache miss. After Alice previews C-1001, Bob's identical request returns Cedar's summary from the shared key without calling that query. Please key cached documents by the trusted tenant as well as the ID, keep the scoped loader, and test both cross-tenant warm-up directions while preserving owner cache hits.

The comment separates the true improvement from the unsupported conclusion. It does not claim the query is injectable, that authentication is bypassed or that every document is always exposed; those would need different evidence. NIST SSDF 1.1 task PW.7.2 asks teams to record and triage review findings with recommended remediation. The useful record is the property, the request sequence, the observed disclosure, the branch, the repair and the retest. [SSDF code-review practice](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-218.pdf)

## Repair the key, keep the cache

The guided repair puts the trusted tenant into the key:

```java
private record Key(String tenant, String id) {}
```

Bob can no longer address Alice's entry. His own tenant-and-ID key misses, and the scoped loader denies him. A mapping function that throws records no entry, so his denied attempt cannot poison Alice's later read. Owner repeats still come from the cache.

Run the repair assertions against the shared key and exactly the two warm cross-tenant cases fail, while the cold denials stay green. That is the missing coverage made visible: the tests that reassured the author never exercised the path that leaked.

The repair fits this fixture's stable tenant mapping. A tenant in the key is not a permission model: membership changes, ownership transfers and per-user grants are outside this fixture and would need their own analysis.

## Decide what the evidence permits you to say

Three statements that sound reassuring carry different weight:

| Statement | What supports it | What it leaves open |
|---|---|---|
| “I reproduced a violation.” | The property, starting state, requests and observed forbidden response. | How broadly the failure occurs and whether other failures exist. |
| “These tests establish this behavior.” | Named cases on a recorded revision and configuration, with assertions that detect the failure. | Other states, paths and configurations. |
| “I have not found a violation.” | An honest account of the investigation performed. | Any unexamined path or unresolved assumption; this alone does not justify approval. |

Bob's cold denial supports a precise claim about a cache miss. It does not support a claim about a hit. Conversely, once a warm request discloses Cedar's summary, you can request changes without proving that every document leaks. A reproducible counterexample is enough to refute this property's universal claim.

When the mechanism is uncertain, choose the next observation that separates competing explanations. Suppose you suspect Bob received the summary because his identity was mapped incorrectly. Compare his cold and warm requests with the same credentials, inspect the mapping, and watch the repository counter. The identity stays fixed while the path changes. The response proves the disclosure; the other observations help locate its cause. Keep observation and explanation separate in the review comment.

Continue investigating when a path lacks an identified authorization decision, a result contradicts your trace, or a proposed fix relies on an assumption you have not checked. If you cannot resolve one of those within the review, say what evidence is missing and recommend holding the merge for that evidence. An unresolved concern is not a confirmed vulnerability, and elapsed review time does not turn it into an approval.

For this bounded fixture, a scoped approval after repair needs the path inventory, tests of the challenged states, preserved legitimate behavior and a negative control showing that the tests detect the original failure. State the remaining exclusions. In a larger service, someone must also decide whether those exclusions are acceptable for the release; a passing local suite cannot make that decision. Reopen the review when a new response source, caller or permission rule changes the reasoning.

## A fluent explanation is a claim, not evidence

Code written with an AI assistant usually arrives with a confident summary and a set of passing tests. Read both as an account of what the author checked, not of what the program does. Every sentence of the form "X is enforced" names a property; the review is to find each path by which a response could violate it and see whether that path actually carries the check. The same applies to a colleague's pull request. Provenance changes how much explanation you receive, not what counts as evidence.

A model can also assist the review. Save your own property, trace and counterexample first; then give the tool the callers and data access, not just the green diff, and ask for an executable violating sequence and a legitimate case the fix must preserve. Treat each returned finding as a claim: reproduce it or check the relevant API contract, and record it as confirmed, rejected or unresolved. "Disable caching" can stop a leak while removing behaviour the product wants; "use a concurrent map" addresses nothing about who may receive a value. One small comparison says nothing general about whether a model reviews better, worse or faster.

The skill is the same in both directions: a patch, human or generated, is shown to hold only on the paths you have followed.

<!--mission-->

## Practice: review release 2

The lab's source already contains the next candidate, produced with an AI coding assistant while this book was written. It changes the download path and adds export links, and it comes with a change summary and seven passing tests. You review it before it merges.

The [worksheet](../practice/11-review-worksheet.md) states the property it must satisfy and the behaviour to preserve. It does not say whether the candidate is correct. Produce:

1. An inventory of operations that return protected data or establish authority for a later response, with the sources and checks on each relevant branch.
2. A request sequence that violates the property, if one exists, with observed responses.
3. A defended review comment: approve within scope, request changes with evidence, or hold for named missing evidence. Test a competing explanation of your observations and name a change that would require review again.
4. If changes are needed, a fix, with your reason for putting the check where you did and an alternative you rejected.
5. Your own tests with a negative control: failing against the candidate and passing after your fix, or, if you approve, failing against a deliberately broken copy you describe.

Attempt it before opening the [review guide](../practice/11-review-guide.md), which holds the hints, the findings and a post-attempt check. An optional part compares your review with a model-assisted pass. Use the worksheet's stop rules if setup or investigation stalls; its time estimates are provisional. Record what the work actually takes.
