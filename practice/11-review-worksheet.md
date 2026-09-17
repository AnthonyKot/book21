# Security review worksheet

Use with [essay 11](../essays/11-security-property.md) and the [lab](https://github.com/AnthonyKot/book21/tree/main/labs/11-security-property). Save your attempt before opening the [review guide](11-review-guide.md) or the lab's `ReleaseReviewCheck` (leave it closed in your IDE's project tree too; it skips itself unless `-DreviewCheck=true` is set). Use command-line `mvn test` during the attempt and avoid expanding the post-attempt class in an IDE test explorer, which may expose its scenario names. Work only with the lab's synthetic users and documents.

## Plan the week

These are authoring estimates derived from the steps below, not measured learner times. Record your actual time.

| Part | What it involves | Provisional estimate |
|---|---|---|
| Setup | Java 21, Maven, Python 3 and curl, already used in essays 4–10 | 0.25–0.5 hour |
| Guided review | Release 1 first pass from the brief, the essay, both demo modes, the negative control, part A | 2–3 hours |
| Independent review | Part B: read the release 2 packet, inventory, reproduction, defended review comment, any fix, your own tests | 3.5–6 hours |
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

Before reading the essay walkthrough, read `review/release-1-BRIEF.md` and its diff. Write the property in one sentence without naming the fix, and list every branch that can return a summary. Then run the lab. If you have already read the walkthrough, record that as assistance and use this part to rehearse the method.

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
4. Write three separate sentences: an observed violation, the behavior a named test establishes, and a remaining uncertainty. Which would justify requesting changes, and which needs more evidence?

## B. Independent review: release 2

### The brief

The lab's source contains release 2, produced with an AI coding assistant. Read `review/release-2/EXPLANATION.md`, `review/release-2/candidate.diff` and `ExportCandidateTest`, then the code. You are reviewing it before it merges. You have not been told whether it is correct.

**Property.** Apply these to every operation:

1. A response may contain a document's summary or full content only when the document belongs to the authenticated caller's tenant. The tenant comes from the authenticated principal, never from the request.
2. Archiving a document withdraws access to its full content from that moment, whichever operation delivers the content. The summary stays visible to the owning tenant.
3. Both rules hold whether the application cache is empty or populated.

Assess sequential requests, with each state change completed before the next request. Cancellation of a response already authorized before a concurrent archive is outside this task.

**Behaviour to preserve.**

- An owner's repeated preview is served from the cache without another body load, including after the document is archived.
- An active document downloads its full content, and an unaffected document is not blocked by another document's archive.
- A tenant can create an export link for an active document and fetch its content through the link.
- Unknown documents and unknown export links return 404, with no private content in the body.

**Constraints.** Keep the cache and the export feature. Do not change `Tenants`, the fixture endpoints or the guided tests. Any design is acceptable if it satisfies the property and preserves the behaviour above.

Use one review note for the five deliverables below; link to your traces and tests rather than repeating them. Before running additional tests, save your initial scope and one candidate-summary claim you intend to challenge. Predict an allowed case and a denied case from the property, with your reasons. You may revise those predictions; preserve the original.

### Deliverable 1: path inventory

List every operation that can return a summary or full content. Base each verdict on observation or inspected code, not on the change summary. Include earlier operations that establish a later response's authority even if they return no document bytes. Mark each conclusion **violation observed**, **supported within tested scope**, or **unresolved**; use **not applicable** where a rule does not govern that effect. An inspected branch and an executed case are different evidence—label which you have. For an operation using the cache, distinguish its hit and miss paths in separate rows or within the same row; add rows as needed. For an operation establishing authority, trace the record it creates and its source of decision inputs.

| Operation | Source of returned data or authority record | What enforces property 1 on that source | What enforces property 2 on that source | Evidence and untested assumptions | Conclusion |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

### Deliverable 2: reproduction

For any violation, give the shortest request sequence that shows it, with the observed status and body and the cache state (warm or cold). For each operation you judge correct, give the observation that supports that verdict, including the cache state you tested.

### Deliverable 3: review comment and defense

Write the comment you would leave on the pull request: approve within a stated scope, request changes, or hold for missing evidence. Separate what the candidate gets right from claims you have refuted or have not established. If you request changes, give the author a reproducible sequence and the behaviour a fix must preserve. If you approve, state the evidence for each relevant path and the exclusions. If you hold, name the unresolved question, the next discriminating check and what each possible result would mean. A hold is an honest intermediate result; record the task as unfinished until you resolve it or explicitly hand off the missing investigation.

Defend one consequential conclusion. Give a plausible competing explanation of your result, choose an observation that would distinguish it from yours, predict both outcomes, then run the check and record what it supports. You can use one of your reproduction or regression cases for this check; explain the two predictions rather than adding a redundant test. If you found no violation, challenge your strongest reason for approving in the same way. The competing explanation concerns why the program behaved as it did; deliverable 4 separately compares repair choices.

Finish with the specific code, policy or deployment change that would invalidate your reasoning and require another review. A reviewer should be able to tell what would change your mind.

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

| Test | Behavior established and scope | Result on the candidate or broken copy | Result on the final code |
|---|---|---|---|
| | | | |

Final command and summary line:

### Limits

Name one thing your tests do not establish, and one other source of response data that would need its own review in a real service. Explain whether your gap prevents a verdict on the stated task or belongs to an excluded deployment concern. This is near transfer within the essay's category; diagnosing an unknown vulnerability category belongs to the later independent assessment.

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
- Delayed check, a week later: without looking at your fix, pick an endpoint in a codebase you may inspect and list every source its response can come from and what authorizes each. Choose one claim, propose a counterexample, and state what you would need to observe before approving it; execution is optional and requires an authorized environment.
