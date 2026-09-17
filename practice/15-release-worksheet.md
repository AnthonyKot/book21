# Release trust worksheet

Use with [essay 15](../essays/15-build-credential.md) and the [companion lab](https://github.com/AnthonyKot/book21/tree/main/labs/15-build-credential). Save your part B verifier, tests and note before opening `consumer_check.py` or the [review guide](15-release-review.md). The GitHub and SLSA pages linked from the essay are the only outside reading you need.

## Plan the week

These are authoring estimates derived from the steps below, not measured learner times. Record your actual time.

| Part | What it involves | Provisional estimate |
|---|---|---|
| Setup | Python 3.12, PyYAML, openssl, curl; no network, Docker or account | 0.25–0.5 hour |
| Guided | Read the essay and the four documentation pages, run the trace matrix and the guided tests, part A | 1.5–2.5 hours |
| Independent | Part B: policy, verifier, own tests with a constructed rejection, note on limits, then the post-attempt check | 3–5 hours |
| Delayed check | The final evidence-record question, about a week later | 0.5 hour |
| Optional | Part C, reading a real workflow you are permitted to inspect | 1–2 hours, later week |

The required parts total about 5.25–8.5 hours. Keep the week bounded:

- If setup or a tool fails for more than an hour, record what failed and answer part A from the transcript quoted in the essay and the documentation pages; the guided questions do not need a working runner.
- If you have spent about three hours on part B without a verifier that accepts the lab's own release, save what you have, open only the first hint in the review guide, mark it as assistance, or carry the work into next week.
- Leave part C for a later week unless the required parts finished early.

Start of attempt:

- Date and repository revision:
- Material already seen (hints, AI assistance, the check file):

## A. Guided: trace the change

Run the vulnerable and repaired workflows against the lab's events as the README shows, then answer from the transcripts and the documentation, not from the essay's summary.

1. For the telemetry pull request under `release-v1.yml`, list each credential that reached the contributor's script and the line of the workflow that made it available. Which of those lines would a code reviewer of the *pull request* ever see?
2. The same pull request under `ci.yml` still runs the contributor's script. Name the two documented rules that make that acceptable, and one thing the script can still do.
3. Version 2 checks out with `persist-credentials: false` in the build job. What does that remove, and why is it not sufficient on its own?
4. Run the negative control (the repair tests against `release-v1.yml`). Which tests fail, and does each failure correspond to a real difference in authority or only to a difference in workflow layout?

## B. Independent: the trusted consumer

### The brief

A deployment system you operate receives, from a producer you do not control, an artifact and a provenance statement signed by the build platform. You must decide whether to deploy it.

**Trust facts.** These are the deployment's policy inputs; they are stated here so that the task is designing the check, not guessing the expectations.

- The only builder you trust is the lab platform, identified by the public key `platform/builder.pub` and the builder id `https://lab.local/platform/hosted-runner`.
- A real release is one the release workflow `release-v2.yml` of the canonical repository `doc-approval/service` built from a commit on `refs/heads/main` in response to a `push`.
- The artifact you deploy must be the bytes that build produced.

**Requirements.**

1. Implement `consumer/verify.py` under the command-line contract in the starter file: exit 0 to accept, 1 to reject, first output line `ACCEPT: …` or `REJECT: …`. The signature helper in `attest.py` is available; the policy is yours.
2. Decide, and write down before coding, what a delivery must establish and from which source each expected value comes. A value the provenance asserts about itself is not an expectation.
3. Accept every genuine release. Rejecting a good delivery is a defect, not caution.
4. Write your own tests. At least one must be a delivery you construct and expect to be rejected, produced with the lab's runner or by editing a genuine delivery, with the reason your verifier gives.
5. Write a note on what an accepted delivery still does not establish.

### Deliverable 1: policy

- What the verifier checks, in order, and what each check rules out:
- Where each expected value lives, and who can change it:
- What the verifier does with a field it does not recognise:

### Deliverable 2: verifier and tests

Save `consumer/verify.py`, your test file, and the output of your tests including the constructed rejection.

### Deliverable 3: the post-attempt check

After deliverables 1 and 2 are saved, run `python3 consumer_check.py`. Record the result. For each mismatch, say whether the check exposed a missing check, an over-strict one, or a disagreement with the stated trust facts that you can defend.

### Limits

- One thing a correctly signed, fully verified release does not tell you:
- One thing your verifier would need that this lab's provenance does not carry:

## C. Optional: a real workflow

Read a workflow you are permitted to inspect. For each job, write which event starts it, which tree it checks out, which secrets and token scopes it holds, and whether it executes code from the checked-out tree. Do not change the workflow or run anything against a repository you do not own. Record only the authority table.

## Handover

Write a short note for the team that owns the deployment: what the consumer now verifies, what it rejects, what it still trusts without evidence, and what would have to change if the producer moved to a different platform or repository. For a consulting client, add what you could and could not verify about their pipeline from the outside, and the retest that would confirm the boundary holds.

## Evidence record

- Actual time per part:
- Assistance used, including hints, AI suggestions and any look at the check or the review guide before your attempt was saved:
- What you could now explain without notes:
- Delayed check, a week later: without reopening your code, describe one delivery your verifier would wrongly accept if the platform's signing key were stolen, and one it would still reject.
