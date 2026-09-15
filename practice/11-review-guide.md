# Security review guide

Open this after saving your [worksheet](11-review-worksheet.md) attempt. Everything below counts as assistance once you have read it. If you are stuck partway, read only the next hint in section 2 and record that you did.

## 1. Judge the attempt before comparing answers

A sound release review shows the following, whatever fix it chose.

**Inventory.** Each operation that returns a summary or full content is listed with the source its data can come from on a cache hit and on a cache miss, and with what enforces each property on each source. A verdict rests on an observed response or on code that was then exercised, not on the change summary.

**Reproduction.** A violation is shown by a short request sequence with the observed response body, and the cache state is stated. A correct operation is supported by tests in both cache states where the cache is involved.

**Review comment.** It acknowledges what the candidate does correctly, identifies the claim in the summary that the code does not support, gives a reproducible sequence and names the behaviour a fix must keep. It does not overstate: no claim of cross-tenant exposure unless one was observed.

**Verdict.** Approving the candidate is a legitimate conclusion when every operation's evidence supports it and the negative control uses a deliberately broken copy. For release 2 that verdict does not survive the archive-after-export sequence in section 3.

**Decision.** If a change was made, the write-up says where the check now sits and why that placement suits future operations that return content. It records an alternative that was considered or tried and rejected with evidence.

**Tests.** Violations are tested warm and cold where the cache is involved. Preserved behaviour includes the owner's cache reuse. The negative control fails against the candidate, or a described broken copy, on assertions, not on startup or compilation.

**Limits.** At least one honest gap, such as a response already authorized before a concurrent archive, other caches or replicas, or the fixed tenant mapping.

## 2. Progressive hints, if you are stuck

1. List every controller method that returns a summary or content. For each, follow the call to the point where the returned value is produced. Which of those values can come out of `PreviewService`'s cache, and which queries run before that happens?
2. The change summary says each new operation "authorizes the document against its current state". Find each point where that happens and ask which request it runs in. Then test the property's second rule with the request order it describes: an action first, the archive afterwards.
3. There is more than one way to place the missing decision: repeat the current-state check in the operation, or route the operation through a path you have already verified. Test either against a warm and a cold cache, and check that the owner's archived summary still comes from the cache.

## 3. What release 2 contains

**Download is correct.** `DownloadService.download` now calls `requireDownload` before reading from the cache. An archived document returns 404 whether its record is cached or not, and an active document still downloads. The candidate's first claim holds.

**Preview is unchanged and holds.** The tenant-partitioned key from the guided repair keeps other tenants out. Archived summaries remain visible to the owner by design.

**Export creation checks current state; export delivery does not.** `create` calls `requireDownload`, so an archived document cannot be exported. `fetch` checks that the caller's tenant created the export, then returns `previews.preview(...).content()`, which runs no archive check on a cache hit or on a miss. Create an export, archive the document and fetch the link: the full content is still delivered, in both cache states. Another tenant still gets 404, so the summary's tenant claim holds.

Two sentences in the summary go further than the code. "Authorizes the document against its current state" is true only of the request that creates the link. "Both new operations reuse ... the existing current-state authorization query" is false for the fetch. The seven candidate tests pass because none archives a document after an export exists.

## 4. Designs that look finished

Each row was run as a variant of the private reference against the review check and the reference's own tests.

| Change to `ExportService.fetch` | What still happens |
|---|---|
| Nothing (the candidate) | Archive after export: content delivered warm and cold. |
| Evict the document's cache entry when it is archived | The next fetch reloads through the same loader, which has no archive condition, so content is still delivered warm and cold. The owner's archived preview now costs a second body load, breaking preserved behaviour. The cache was never the missing decision. |
| Check an `archived` flag carried in the cached record | Correct on a cold cache. With a warm entry cached before the archive, the flag is stale and content is delivered. A cached copy of authorization data is not a current decision. |
| Call `requireDownload` before reading from the cache | Meets every tested case. |
| Return `DownloadService.download(...)` after the export ownership check | Meets every tested case, and reuses the check the candidate already fixed. |

The last two are both sound. Repeating the check is explicit at the call site; delegating keeps one place responsible for full-content authorization, so a future change to that rule reaches both operations. Either way, the next operation that returns content still has to be reviewed.

A third incomplete design, reading directly from the repository instead of the cache, was also run. It fails the two archive-after-export cases for the same reason as eviction, because the fresh read has no archive condition, but it keeps the owner's cache reuse. It is not listed separately because it exposes the same misunderstanding.

## 5. Reference evidence

Recorded while authoring. None of it is evidence of your attempt.

- Guided suites: 20 pass. Negative control with the shared key: 2 failures (both warm cross-tenant directions), 8 passes.
- Default `mvn test` with the candidate: 27 pass (20 guided, 7 candidate tests).
- `ReleaseReviewCheck` against the candidate: 2 failures (archive after export, warm and cold), 8 passes.
- The private reference's learner-style tests against the candidate: 2 failures, 4 passes.
- Private reference with the check at fetch: 43 pass (20 guided, 7 candidate, 6 of its own, 10 review checks). Delegating to download: 43 pass.
- Variants against the review check: eviction 3 failures (archive after export warm and cold, owner's archived preview reloads); cached flag 1 failure (warm only); direct repository read 2 failures.

To compare after your attempt:

```sh
mvn -DreviewCheck=true '-Dtest=*Test,ReleaseReviewCheck' test
```

The check covers the stated property and preserved behaviour through HTTP. It cannot judge your inventory, your comment or tests you did not write. Passing it after reading this guide is assisted evidence.

## 6. Limits of the task

The property is checked at the moment a request is authorized. The lab does not test cancelling a response already authorized before a concurrent archive, revocation across processes or copies already delivered. Tenant membership and ownership are fixed. The cache is one in-process map, so HTTP intermediaries, CDNs, replicas and distributed caches, each with its own key and freshness rules, are outside it. The candidate was produced for teaching; one generated patch says nothing general about how often AI-assisted code contains such gaps.
