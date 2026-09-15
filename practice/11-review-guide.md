# Security review guide

Compare after your [worksheet](11-review-worksheet.md) attempt.

## Hint 1: the query is not every return path

On a cache hit, `computeIfAbsent` does not call the repository loader. Adding tenant filtering only there leaves the candidate’s shared-key return path unaffected. A denied cold request therefore cannot establish denial after another principal warms the cache.

## Hint 2: preserve the trusted dimensions

The guided key contains trusted tenant and document ID. Caller input must not choose the tenant. Retain the tenant-scoped repository lookup and owner cache hits. A concurrent collection coordinates map operations; it does not infer which caller may receive a value.

## Hint 3: download is a separate action

`DownloadService` has access to the store’s `requireDownload` policy check. Use the current tenant, identifier and archive state before returning full content from either a cache hit or a miss. The preview deliberately permits archived summaries, so changing its loader to reject all archived data would break the legitimate preview requirement. This lab also caches full content during preview loading. Assess whether that extra cached data is needed; splitting cache data by action does not remove the need for a current download decision.

## Expected evidence

The two cold candidate tests pass. The 20 guided tests also pass because the reproduction class expects the flaw. Restoring the shared key under repair expectations produces 2 failures and 8 passes. The exercise starter fails warm-after-archive and cold-archived downloads, while four other cases pass. The private reference passes all 26 cases. The body-load counter excludes the fresh policy query; do not claim the repair makes zero database calls.

The model ledger is a record of your investigation, not a leaderboard. Findings that are technically possible but outside supplied evidence remain unresolved until checked. Context matters: a model shown the walkthrough has not independently discovered its answer. A suggested patch earns acceptance through its denied and permitted behavior.

No cache TTL, distributed invalidation, membership/ownership change, concurrent revocation guarantee, production benchmark or human security audit is demonstrated. The exercise tests an authorization decision after a completed archive. A response authorized before a concurrent archive is outside that contract.
