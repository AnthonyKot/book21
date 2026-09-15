# Release inventory worksheet

Use with [essay 13](../essays/13-build-inventory.md) and the [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/13-build-inventory). Save your part B assessment before opening the [review guide](13-inventory-review.md) or running `review_check.py`, whose source reveals the answers. No real customer data, cloud environment or registry publication is needed.

## Plan the week

These are authoring estimates derived from the steps below, not measured learner times. Record your actual time.

| Part | What it involves | Provisional estimate |
|---|---|---|
| Setup | Docker, a checksum-verified Syft download, Java, Maven and Python; first image and dependency downloads | 1–2 hours |
| Guided | Read the essay, build and scan both images, run the verification, negative control and pairing tests, part A | 2–3 hours |
| Independent | Part B: assess five records, answer the release question, write the evidence request | 2.5–4 hours |
| Delayed check | The final evidence-record question, about a week later | 0.5 hour |
| Optional | Part C, repeating the assessment on inventory records from a build you may inspect | 1–2 hours, later week |

The required parts total about 6–9.5 hours, and setup is the least predictable. Keep the week bounded:

- If Docker or Syft setup blocks you for more than two hours, record the command and error and do part B, which needs only Python and a SHA-256 tool. Return to the guided build next week.
- If part B has not produced a verdict for every record after about two hours, save what you have, open only the next hint in the review guide, marked as assistance, or carry the work into next week.

Start of attempt:

- Date, repository revision, Docker, Syft, Java, Maven and Python versions:
- Material already seen (hints, AI assistance, the review script):

## A. Guided: what each view describes

Before running the build, read the application POM and the Dockerfile, and predict the answers below.

| Question | Prediction | Observation and where you found it |
|---|---|---|
| Is Spring Web MVC in both the Maven BOM and the image inventory? | | |
| Is the helper in the Maven BOM? | | |
| Which helper version is in A and in B? | | |
| Where do the Alpine packages come from? | | |
| Do identical application bytes imply identical images? | | |

1. Explain the difference between the 39 Maven components, the 112 native package records and the 1,336 CycloneDX components.
2. Run the negative inventory control. Why do exactly the helper and `musl` checks fail, and why do the Spring checks still pass?
3. After the tag moves, why can a record carrying the right tag still describe the wrong image?

## B. Independent: which record describes the release?

### The brief

`data/release-review/selection.json` names the image selected for the release. `records.json` lists five inventory records attached to the change, with their report files. An advisory affects `book21:export-helper` version 1.0.0 (the advisory is fictional). The release manager asks two questions: which of these records can be attached to the release as its inventory, and does the selected image contain the affected version?

**Requirements.**

1. **Check, do not accept.** For each record, verify what you can yourself: whether the recorded hash matches the report file's bytes, what the report itself names as its subject, what platform and scope it covers, and what kind of artifact its generator inventories. A record's `claimedSubject` is a claim, not evidence.
2. **One verdict per record:** `describes` (it is an inventory of the selected image), `partial` (it truthfully describes part of what the image contains, but not the image), `does-not-describe` (it describes something else), or `cannot-rely` (you cannot establish what it describes from the evidence given). If two verdicts fit, choose one and say why.
3. **Answer only from records that can carry the answer.** A record that agrees with your conclusion but describes something else does not support it.
4. **Say what you would still ask for.** Name at least one piece of evidence you would request before signing off, and why.

### Deliverable 1: record assessment

| Record | Recorded hash matches bytes? | Subject named inside the report | Platform and scope | What the generator inventories | Verdict | Reason |
|---|---|---|---|---|---|---|
| R1 | | | | | | |
| R2 | | | | | | |
| R3 | | | | | | |
| R4 | | | | | | |
| R5 | | | | | | |

### Deliverable 2: the release answer

- Does the selected image contain `export-helper` 1.0.0?
- Records that support the answer, and why each can carry it:
- Records that seem relevant but cannot carry it, and why:
- Evidence you would request before signing off:

Save your verdicts and answer as the JSON assessment described in the lab README. `review_check.py` checks only those verdicts and the records you cite; your written reasons, evidence request and limits are judged against the review guide's rubric.

### Limits

Name one thing even the best record here cannot establish about the release, and one change to the build or deployment that would make you re-inventory.

## C. Optional: a build you may inspect

In a codebase and registry you are permitted to use, find the inventory evidence attached to one release. Apply the same checks to it and write the same verdicts. Do not publish findings about a real system.

## Handover

Write a short note for the release record: which inventory is attached and why, which records were rejected and why, the advisory answer, and what remains unverified. For an internal team, address the release manager. For a consulting client, add the artifact, platform, collection method and exclusions. Label the scenario as fictional.

## Evidence record

- Actual time per part:
- Assistance used, including hints, AI suggestions and any look at the review script:
- What you could now explain without notes:
- Delayed check, a week later: without reopening the packet, explain why a correct component list attached to the right tag can still be the wrong evidence for a release.
