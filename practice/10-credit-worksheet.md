# Credit invariant worksheet

Use after [essay 10](../essays/10-valid-requests.md). Keep the [review guide](10-credit-review.md) and `WorkerContractCheck.java` closed until you have saved your own tests, initial results and decision. Opening either early is assistance; record it and continue learning.

## Plan a bounded attempt

Prerequisites: the guided example, Java/Spring test experience, Java 21, Maven 3.9+, Python 3 and curl. Reuse the existing lab setup. These are provisional estimates, not measured learner times.

| Work | Allowance | Stop rule |
|---|---|---|
| Setup | 0.5–1 h | If tooling still blocks execution at one hour, save the error and next step; resume after resolving it. |
| Part A: reading, prediction and guided runs | 1.5–2 h | Save the two guarantees and one trace before moving on. |
| Part B: own tests, decision and evidence | 2–4 h | After roughly three hours without a useful reproduction or justified verdict, save the attempt; open only the next hint and record assistance, or carry over. |
| Delayed check | 0.25–0.5 h next week | Explain from memory before reopening the code. |
| Part C: optional hosted transfer | 1–3 h initially | Choose one lab; carry unfinished work forward. |

Required total: **4.25–7.5 hours**, including delayed review. That leaves room in a ten-hour week for interruptions. Keep the established **10–15-hour weekly budget**; optional work can move to another week.

- Date, repository revision and Java/database versions:
- Available time; actual setup / guided / independent / delayed time:
- Assistance: hint or tool, when, why, what it supplied:

## Part A: predict, then run

For one 1000-cent credit, predict responses, final balance and applied-credit count.

| Schedule | Unconditional claim | Conditional claim |
|---|---|---|
| A finishes before B starts | | |
| Both read unused; A commits first | | |
| Both read unused; B commits first | | |
| Both read unused; gates released together | | |
| Two distinct credits | | |

Follow the [README](https://github.com/AnthonyKot/book21/blob/main/labs/10-valid-requests/README.md) for guided tests, both ordered curl schedules in both modes and the negative control. Save commands and results. Never reset during active requests.

Explain why both transactions can be atomic while the one-use rule fails. Which observation becomes stale? Why does one final applied flag miss the duplicate effect? Which guarantee survives the negative control?

## Part B: choose evidence for the worker

The worker is a second caller of the credit operation. Assess its behavior against this contract:

- An ordinary Cedar credit adds its fixed amount once. A consumed credit conflicts without another increment; two distinct Cedar credits may both succeed.
- Overlapping applications of the same credit produce one success and one conflict, with one balance effect. Use controlled overlap rather than sleeps.
- An injected failure after either write leaves neither write committed. A later retry may apply the credit once.
- Foreign and unknown credit IDs remain rejected, without changing either tenant's balance. Authentication and CSRF protection remain intact on HTTP paths.

The task does not prescribe a change. Find evidence for your conclusion. Retain the guided service behavior and the fixture's control guards. No new database, queue, idempotency protocol or framework is needed.

Produce **your tests and a short decision note**:

1. Before editing production code, choose cases that distinguish the contract's obligations. Write and run your own tests through the real Spring-managed worker or its HTTP adapter; include controlled overlap, both failure points with retry, distinct credits and rejected IDs. Inspect committed state outside the call. You may reuse the existing test helpers; do not wrap a test in a transaction that supplies a missing application guarantee. Save predictions and initial results before opening the check or hints.
2. Decide whether a change is needed and explain the evidence. If so, implement it and rerun your tests. If not, explain why the required observations support approval. In a separate copy, remove the claimed protection (or deliberately break the approved design) and show your tests detect the resulting violation. An assertion failure caused by the intended behavior is evidence; a compile error or gate timeout is not.
3. In about 150–250 words, explain the enforcement boundary, one plausible alternative you rejected, and what would need fresh evidence if the balance effect became an external payment. Link the tests and negative-control result. For employment, use this as an engineering explanation; for consulting, add the assessed database, schedules, failure points and exclusions. Label the fixture synthetic.

After saving these artifacts, use the README's opt-in comparison commands and then the review guide. Record newly discovered gaps separately from what you established yourself. A small patch can be enough; the evidence and explanation are the work.

## Part C: optional transfer

Choose one authorized Academy lab; the second can wait:

- [Limit overrun race conditions](https://portswigger.net/web-security/race-conditions/lab-race-conditions-limit-overrun): identify the checked limit and observed effect; contrast exploratory timing with a deterministic regression seam.
- [Multi-endpoint race conditions](https://portswigger.net/web-security/race-conditions/lab-race-conditions-multi-endpoint): draw the observed interleaving across endpoints without inventing hidden implementation details.

Record hints and results. If unavailable or unfinished, leave it pending. Local completion does not establish hosted completion.

## Delayed check and evidence record

Next week, without the code, explain why a transaction did not prevent the guided duplicate and what your worker tests establish separately. Then change a condition: the balance update becomes an external payment. Sketch a failure after that payment but before local completion, and state why the current evidence cannot justify automatic retry.

- Initial verdict and supporting observations:
- Final decision; changed files; tests and negative-control commands/results:
- Comparison-check gaps and hint access:
- Remaining uncertainty; next step if interrupted:
- Delayed explanation and changed-condition reasoning:

This is near transfer of the essay's mechanism. Passing supplied tests, a model solution or a quick patch does not establish independent mastery.
