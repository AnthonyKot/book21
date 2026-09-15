# Security review worksheet

Use with [essay 11](../essays/11-security-property.md). Start from the [review brief and diff](https://github.com/AnthonyKot/book21/tree/main/labs/11-security-property/review) before opening the walkthrough or [review guide](11-review-guide.md), if you want to preserve an unassisted first pass.

- Date, revision, Java/framework/database versions:
- Time available this week (10–15 hours total):
- Material already seen, hints and AI assistance:

## First review

Write the security property without naming the proposed fix. Follow every preview return path from authenticated principal to response data. Record what the changed query establishes and which paths call it.

| Sequence | Prediction | Observed status/body | Branch and evidence |
|---|---|---|---|
| Bob requests C-1001 cold | | | |
| Alice then Bob request C-1001 | | | |
| Bob then Alice request B-2001 | | | |
| Alice repeats own preview | | | |
| Anonymous request after warm-up | | | |

Save your first finding before model assistance. Run the two cold tests, the full suite and the negative control from the lab README. Record actual failures versus setup errors. Explain how a passing assertion can be correct while the test collection misses the security property.

## Compare a model-assisted pass

Use only this public synthetic code or other material you are authorized to share. Record tool/model/date, prompt, exact files, whether answer-bearing tests were included, output and elapsed investigation time. An optional reusable prompt:

```text
Review this candidate against this property: every preview response must belong to the authenticated tenant, regardless of prior cache population. Trace all response paths through the supplied callers, cache and repository. For each suspected violation give the input sequence, failing branch and evidence required to confirm it. Identify one legitimate behavior the fix must preserve. Separate suspected and verified claims. Do not edit or execute anything.
```

| Claim | Already in my first pass? | Verification performed | Confirmed/rejected/unresolved | Time |
|---|---|---|---|---|
| | | | | |

Validate every accepted finding and patch. Do not score a claim as correct merely because it sounds familiar. If no model is available, leave the comparison pending; the code investigation can still be completed. One small assisted task is not a productivity benchmark or proof of independent mastery.

## Changed task: archive and download

Policy: own archived summaries stay visible; full downloads require an own-tenant, non-archived document at the current authorization check. The archive completes before the tested download begins. The starter’s tenant-partitioned cache remains populated after archive.

Run `python3 demo.py --exercise`, then `mvn -Dtest=ArchiveExercise test`. Repair `DownloadService.download` using the existing fresh policy query. Required results:

- Active own document downloads its full content.
- Warm cache followed by archive denies full download without another body load.
- Archive before cache fill denies download without loading the body.
- Own archived summary remains visible.
- Foreign download is denied even after owner warm-up.
- An unaffected own document still downloads.

Run `mvn '-Dtest=*Test,ArchiveExercise' test` for all 26 cases. Default `mvn test` excludes the unfinished exercise. Reintroduce the missing action check in a separate copy and demonstrate the failed regression. Record the original flaw, your change, permitted behavior and remaining limitations.

## Independent transfer

Use only the authorized PortSwigger lab environments:

- [User ID controlled by request parameter](https://portswigger.net/web-security/access-control/lab-user-id-controlled-by-request-parameter). Derive the object-authorization property from the observed endpoint; record the violating request and needed server decision.
- [User ID controlled by request parameter with password disclosure](https://portswigger.net/web-security/access-control/lab-user-id-controlled-by-request-parameter-with-password-disclosure). Inspect the actual response fields when assessing impact rather than relying only on what the page visibly displays.

These are black-box transfer tasks, not replicas of the cache. If you completed one earlier, record that and use it as delayed revalidation rather than claiming a new independent discovery. Record hints; unavailable tasks remain pending.

## Handover

Write a concise engineering review with property, reproducer, branch, fix, retest and limits. For consulting, add assessed endpoints and exclusions. Record elapsed time, assistance, next gap and a delayed task: trace a changed response path a week later without reading your previous finding.
