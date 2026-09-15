# Stored-file consumer worksheet

Use with [essay 9](../essays/09-file-consumer.md) and the [lab](https://github.com/AnthonyKot/book21/tree/main/labs/09-file-consumer). Save your attempt before opening the [review guide](09-file-review.md) or the lab's `ReleaseReviewCheck` (leave it closed in your IDE's project tree too; it skips itself unless `-DreviewCheck=true` is set). Work only with the lab's synthetic targets.

## Plan the week

These are authoring estimates derived from the steps below, not measured learner times. Record your actual time.

| Part | What it involves | Provisional estimate |
|---|---|---|
| Setup | Java 21, Maven, Python 3 and curl, already used in essays 4–8; first dependency download | 0.5–1 hour |
| Guided run | Read the essay, run both preview modes, run the negative control, answer part A | 2–3 hours |
| Independent release review | Part B: inventory, reproduction, design, your own tests, handover note | 4–7 hours |
| Delayed check | The final evidence-record question, about a week later | 0.5 hour |
| Optional transfer | Part C: two hosted Academy labs; schedule for a later week by default | 1–3 hours |

The required parts total about 7–11.5 hours, before troubleshooting. In a 10-hour week, the upper end leaves no room. Keep the week bounded:

- If setup or an unrelated tool failure takes more than an hour, record the command and error and move on to what you can do.
- If part B has not produced a reproduction after about 3 hours, stop and record what you tried. Open only the next hint in the review guide, marked as assistance, or carry the review into next week.
- Leave part C for a later week unless the required parts finished early.

Start of attempt:

- Date, repository revision, Java and Maven versions:
- Assistance you expect to use or have already seen (hints, AI, earlier notes):

## A. Guided run: preview

Before running `python3 demo.py`, predict the response and counter changes for each document in both modes. Then record what you observed.

| Document | Permissive: status, body, resolutions, HTTP hits | Hardened: status, resolutions, HTTP hits |
|---|---|---|
| Ordinary invoice | | |
| File entity | | |
| HTTP entity | | |
| External DTD | | |

1. What does `201` establish about an uploaded file, and what does it not?
2. Why can the invoice shape check not prevent the file read, even though it rejects unexpected content?
3. Why does the test for the HTTP entity check the fixture counter as well as the response body?
4. Run the negative control from the README. Which cases failed, and why do the intake and shape cases still pass?

## B. Independent release review: partner statements

### The brief

The release adds two operations on stored uploads (see the lab README):

- `POST /api/uploads/{id}/statement` totals a partner statement.
- `GET /api/uploads/{id}/receipt` returns the SHA-256 and size of the stored bytes.

You are reviewing the release before it ships. You have not been told whether it is safe.

**Policy.** Apply these to every operation on stored uploads:

1. Invoices and partner statements do not use a `DOCTYPE`. Any operation that parses stored bytes as XML must reject a document containing a `DOCTYPE` with `422`, including one whose declarations are never used.
2. No operation may request resolution of, read or contact any location named in an upload. This holds even when the operation would reject the document afterwards.
3. The policy applies to every encoding the XML parser accepts, not only UTF-8.

**Behaviour to preserve.**

- An ordinary statement still totals correctly, including predefined entities such as `&amp;` and CDATA text. `samples/statement.xml` uses both and returns `2 lines, 1500 cents: Toner & paper; Delivery`.
- An invalid statement still returns `422`.
- An ordinary invoice still previews, and the guided preview repair and its tests remain intact.
- The receipt still returns the hash and size for every stored upload, including documents that XML processing rejects.

**Constraints.** Run with `lab.hardened=true`. Keep `Fixture.confine` in place: it is lab safety, not the control under review. Do not change the formats, disable an operation, or reject every statement. Any design is acceptable if it meets the policy and preserves the behaviour above.

### Deliverable 1: consumer inventory

List every path from stored bytes to a component that reads them. Base each verdict on observation or inspected configuration, not on what the code looks like it should do.

| Operation | Code location | Interprets the bytes as XML? With which API? | Settings relevant to DTDs and external resources | Evidence for your verdict | Meets the policy? |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

### Deliverable 2: reproduction

For each violation you find, give the smallest document that shows it, then the observed status, body, resolution count and HTTP-hit count. For each operation you judge compliant, give the observation that supports that verdict.

### Deliverable 3: verdict and enforcement decision

A release can pass review. If you conclude that no change is needed, give the evidence for that verdict and name a change to the code that would break it; then skip to the tests.

- Verdict (change needed or not), with the evidence:
- What you changed, and where the policy is now enforced:
- Why that location. Would the next consumer of stored bytes also receive it?
- What happens if a required setting is unsupported:
- At least one alternative you considered or tried, and the evidence that made you reject it:
- A document that your design still accepts, and why the policy permits it:

### Deliverable 4: your tests

Write tests under `src/test/java/lab/` (extending `UploadHttp` is convenient). Include:

- Every refusal case your inventory and policy require. Each must assert that no resolution or fixture contact happened, as well as the status.
- Legitimate cases showing the behaviour to preserve still works.
- A negative control that shows your tests can fail for the right reason. If you changed code, run them against the original release; if you did not, run them against a deliberately broken copy you describe (for example, one setting removed). Record which fail. A compilation or startup error does not count.

| Test | What it proves | Result on the original or broken code | Result on the final code |
|---|---|---|---|
| | | | |

Final command and summary line:

### Limits

Name one thing your tests do not establish, and one other consumer or format that would need its own review.

## C. Optional transfer: hosted labs

Use only the authorized training environments. Record hints and walkthroughs as assistance. If a lab is unavailable, mark it pending; repeating a local case is not completion.

- [Exploiting XXE using external entities to retrieve files](https://portswigger.net/web-security/xxe/lab-exploiting-xxe-to-retrieve-files). Which response shows external resolution, and how does the attacker's control differ from the local upload?
- [Exploiting XInclude to retrieve files](https://portswigger.net/web-security/xxe/lab-xinclude-attack). What does the attacker control when they cannot supply the whole document? Why does the local lab's inactive-XInclude test not show that every XML consumer disables it?

## Handover

Write a short release-review note stating the policy, inventory, finding, change, evidence and limits. For an internal team, address the engineer who owns the release. For a consulting client, add the assessed scope, the retest you would run and the exclusions. Label the lab as synthetic.

## Evidence record

- Actual time per part:
- Assistance used, including hints, AI suggestions and any look at the review check:
- What you could now explain without notes:
- Delayed check, a week later: without looking at your patch, pick a different XML-reading component (from any codebase you may inspect) and state how you would establish its effective policy.
