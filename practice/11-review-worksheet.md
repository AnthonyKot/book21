# Security review worksheet

Use with [essay 11](../essays/11-security-property.md) and the [lab](https://github.com/AnthonyKot/book21/tree/main/labs/11-security-property). Save your attempt before opening the [review guide](11-review-guide.md) or the lab's `ReleaseReviewCheck` (leave it closed in your IDE's project tree too; it skips itself unless `-DreviewCheck=true` is set). Work only with the lab's synthetic users and documents.

## Plan the week

These are authoring estimates derived from the steps below, not measured learner times. Record your actual time.

| Part | What it involves | Provisional estimate |
|---|---|---|
| Setup | Java 21, Maven, Python 3 and curl, already used in essays 4–10 | 0.25–0.5 hour |
| Guided review | Release 1 first pass from the brief, the essay, both demo modes, the negative control, part A | 2–3 hours |
| Independent review | Part B: read the release 2 packet, inventory, reproduction, review comment, any fix, your own tests | 3.5–6 hours |
| Delayed check | The final evidence-record question, about a week later | 0.5 hour |
| Optional | Part C: a model-assisted comparison (1–1.5 hours); a hosted lab (1–2 hours, later week by default) | — |

The required parts total about 6.25–10 hours. In a 10-hour week, the upper end leaves no room for troubleshooting, so keep the week bounded:

- If setup or a tool failure takes more than an hour, record the command and error and move on to what you can do.
- If part B has not produced either a violating sequence or evidence that each operation holds after about 3 hours, stop and record what you tried. Open only the next hint in the review guide, marked as assistance, or carry the review into next week.
- Leave part C for a later week unless the required parts finished early.

Start of attempt:

- Date, repository revision, Java and Maven versions:
- Material already seen (walkthrough, hints, AI assistance):

## A. Guided review: release 1

Before reading the essay walkthrough, read `review/release-1-BRIEF.md` and its diff. Write the property in one sentence without naming the fix, and list every branch that can return a summary. Then run the lab.

| Sequence | Prediction | Observed status and body | Branch taken, and evidence |
|---|---|---|---|
| Bob previews C-1001, cold | | | |
| Alice, then Bob, preview C-1001 | | | |
| Bob, then Alice, preview B-2001 | | | |
| Alice repeats her own preview | | | |
| Anonymous preview after warm-up | | | |

1. The two cold-cache tests pass against the flawed candidate. What exactly do they establish?
2. Why does the body-load counter support the explanation without being the decisive evidence?
3. Run the negative control from the README. Which cases fail, and why do the cold denials stay green?

## B. Independent review: release 2

### The brief

The lab's source contains release 2, produced with an AI coding assistant. Read `review/release-2/EXPLANATION.md`, `review/release-2/candidate.diff` and `ExportCandidateTest`, then the code. You are reviewing it before it merges. You have not been told whether it is correct.

**Property.** Apply these to every operation:

1. A response may contain a document's summary or full content only when the document belongs to the authenticated caller's tenant. The tenant comes from the authenticated principal, never from the request.
2. Archiving a document withdraws access to its full content from that moment, whichever operation delivers the content. The summary stays visible to the owning tenant.
3. Both rules hold whether the application cache is empty or populated.

**Behaviour to preserve.**

- An owner's repeated preview is served from the cache without another body load, including after the document is archived.
- An active document downloads its full content, and an unaffected document is not blocked by another document's archive.
- A tenant can create an export link for an active document and fetch its content through the link.
- Unknown documents and unknown export links return 404, with no private content in the body.

**Constraints.** Keep the cache and the export feature. Do not change `Tenants`, the fixture endpoints or the guided tests. Any design is acceptable if it satisfies the property and preserves the behaviour above.

### Deliverable 1: path inventory

List every operation that can return a summary or full content. Base each verdict on observation or inspected code, not on the change summary.

| Operation | Where the returned data comes from (cache, repository, other) | What enforces property 1 on that source | What enforces property 2 on that source | Evidence | Holds? |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

### Deliverable 2: reproduction

For any violation, give the shortest request sequence that shows it, with the observed status and body and the cache state (warm or cold). For each operation you judge correct, give the observation that supports that verdict, including the cache state you tested.

### Deliverable 3: review comment

Write the comment you would leave on the pull request: approve, or request changes. Separate what the candidate gets right from any unsupported claim in its summary. If you request changes, give the author a reproducible sequence and the behaviour a fix must preserve. If you approve, state the evidence that each operation satisfies the property.

### Deliverable 4: decision

If you approved the candidate unchanged, name a change to the code that would break the property and skip to the tests.

- What you changed, and where the check now sits:
- Why that location. Would a future operation that returns content receive the same check?
- At least one alternative you considered or tried, and the evidence that made you reject it:
- A request your design still allows, and why the property permits it:

### Deliverable 5: your tests

Write tests under `src/test/java/lab/` (extending `ReviewHttp` is convenient). Include:

- Every violation your inventory identified, in each cache state that matters, asserting status and the absence of private content.
- The behaviour to preserve, including the owner's cache reuse.
- A negative control that shows your tests can fail for the right reason. If you changed code, run them against the original candidate; if you approved it, run them against a deliberately broken copy you describe. Record which fail. A compilation or startup error does not count.

| Test | What it proves | Result on the candidate or broken copy | Result on the final code |
|---|---|---|---|
| | | | |

Final command and summary line:

### Limits

Name one thing your tests do not establish, and one other source of response data that would need its own review in a real service.

## C. Optional transfer

**Model-assisted comparison.** After saving part B, give a model the property, the change summary and the relevant callers, not only the diff. Record the tool, date, prompt, files supplied and whether your own findings were included. A starting prompt:

```text
Review this candidate against the property below. Trace every operation that can return a summary or full content, including cache hits and misses. For each suspected violation give an executable request sequence, the branch that permits it, and the evidence needed to confirm it. Name one legitimate behaviour a fix must preserve. Separate suspected from verified claims. Do not edit or execute anything.
```

| Claim | Already in my review? | Verification performed | Confirmed / rejected / unresolved | Time |
|---|---|---|---|---|
| | | | | |

One comparison is not a productivity benchmark. A model shown your findings or the review guide has not found anything independently.

**Hosted lab.** [Exploiting path mapping for web cache deception](https://portswigger.net/web-security/web-cache-deception/lab-wcd-exploiting-path-mapping) (Practitioner; Burp Suite Community Edition is enough). Use only the authorized training environment. Explain how the cache and the origin disagree about which response may be stored and served, and compare that with the application cache key in this lab. Record hints and walkthroughs as assistance.

## Handover

Write a short review note stating the property, inventory, finding, change, evidence and limits. For an internal team, address the author of the pull request. For a consulting client, add the assessed operations and cache states, the retest you would run and the exclusions. Label the lab as synthetic.

## Evidence record

- Actual time per part:
- Assistance used, including hints, AI suggestions and any look at the review check:
- What you could now explain without notes:
- Delayed check, a week later: without looking at your fix, pick an endpoint in a codebase you may inspect and list every source its response can come from and what authorizes each.
