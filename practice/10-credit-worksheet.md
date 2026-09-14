# Credit invariant worksheet

Use after [essay 10](../essays/10-valid-requests.md). Keep the [review guide](10-credit-review.md) closed for your first attempt.

- Date, repository revision and Java/database versions:
- Available time this week (10–15 hours total):
- Assistance used:

## Predict before running

For one 1000-cent credit, fill the final state and response predictions.

| Schedule | Vulnerable claim | Conditional claim |
|---|---|---|
| A finishes before B starts | | |
| Both read unused; A commits first | | |
| Both read unused; B commits first | | |
| Both read unused; both gates released | | |
| Two distinct credits | | |

Why can both transactions be atomic while the one-use rule fails? Which observation becomes stale? Why does one final applied flag fail to reveal the duplicate effect?

## Retain the guided evidence

Follow the [lab README](https://github.com/AnthonyKot/book21/blob/main/labs/10-valid-requests/README.md). Run the two ordered curl demonstrations in each mode. Save statuses and final balance/applied count. Do not reset while requests are in flight.

Run the negative control. Record which tests fail and distinguish assertion failures from setup errors. Explain why nine tests still pass after the repair is removed.

## Change the execution path

The worker uses the conditional claim but lacks the service transaction. Run `python3 demo.py --worker` and then:

```sh
mvn -Dtest=WorkerExercise test
```

Repair `CreditWorker.apply` while preserving conditional claiming. Required behavior:

| Case | Required state/effect |
|---|---|
| Ordinary credit | Balance 1000; one applied credit |
| Sequential duplicate | Conflict; no second increment |
| Two workers observe unused | One success, one conflict; balance 1000 |
| Failure immediately after claim | Balance 0; credit still unused |
| Failure after balance update | Both writes rolled back |
| Retry after rolled-back failure | Succeeds once; balance 1000 |

The tests use the Spring-managed worker; do not wrap the tests themselves in a transaction that hides a missing production boundary. Do not swallow exceptions or return success without applying a credit. After repair:

```sh
mvn '-Dtest=*Test,WorkerExercise' test
```

Default Maven naming excludes this exercise; the combined command runs 30 cases. Remove your transaction boundary temporarily in a separate copy and verify that the relevant failure tests expose partial commits again.

## Independent transfer

Attempt only the authorized hosted environments:

- [Limit overrun race conditions](https://portswigger.net/web-security/race-conditions/lab-race-conditions-limit-overrun). Identify the checked limit, the vulnerable interval and the business effect. Contrast exploratory timing with our deterministic regression seam.
- [Multi-endpoint race conditions](https://portswigger.net/web-security/race-conditions/lab-race-conditions-multi-endpoint). Explain why inspecting one endpoint in isolation misses the interleaving. Draw only the observed steps; avoid inventing hidden implementation details.

Record hints and assistance. If a lab is unavailable, leave its result pending; a local changed task may support practice but is not hosted completion.

## Handover

Write a short engineering review separating conditional claiming, rollback and response-replay semantics. For consulting, add scope, database version, schedules tested, failure points and exclusions. Label the case synthetic. Retain your patch and traces.

Record elapsed time, remaining gap and a delayed check: next week, explain why `@Transactional` alone did not repair the first implementation, and why the conditional statement alone did not repair the worker. A supplied passing solution does not establish independent mastery.
