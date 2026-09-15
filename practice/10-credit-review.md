# Credit invariant review

Use after saving your own tests, initial results and decision in the [worksheet](10-credit-worksheet.md). Open hints progressively; record which you used, when and why. The findings below disclose the case.

## Judge the evidence before the patch

A convincing answer connects a business obligation to a test that could falsify it:

- Inspect both the balance and the consumed-credit state after calls complete. A response code or final flag alone cannot distinguish the relevant effects.
- Establish ordinary application, duplicate rejection, controlled overlap, distinct credits and foreign/unknown rejection. Preserve HTTP authentication and CSRF; the guided suite covers those controls.
- Inject each documented failure and inspect committed state outside the call, then retry. Test the production invocation through Spring or HTTP, without a transaction supplied by the test.
- Explain the actual enforcement boundary and why it covers every write that must succeed or fail together. Choose a repair from that explanation, not by matching an annotation.
- Show reader-written tests detect a removed protection or a deliberately broken copy. Distinguish assertion failures from tool errors and gate timeouts.

An evidence-backed no-change verdict is allowed by the task. It does not survive the failure observations in this starter. A tiny patch can be justified; copying it without establishing those observations misses the exercise.

## Hint 1: separate outcomes

For a failed call, what can each final state tell you? Balance zero with one consumed credit differs from balance 2000 with one consumed credit. Either trace the writes from the worker entry point or start with an experiment that distinguishes those states. Which contract obligation would each result contradict?

## Hint 2: compare invocation paths

Follow the guided service and the worker into their shared operation. The same statements can run under different commit boundaries. Alternatively, log state after each documented failure and ask which earlier write survived. Identify who begins and ends the unit of work before choosing a change.

## Hint 3: make the boundary real

In this starter, `CreditWorker.apply` calls the conditional operation without an enclosing transaction. A transaction on the externally invoked, Spring-managed worker method is one repair. The reference uses `@Transactional(isolation = Isolation.READ_COMMITTED)` and lets the unchecked failure escape. Programmatic transaction management can also establish a boundary, but no second implementation is needed to explain this case.

With Spring's default proxy approach, an annotation only takes effect when the call crosses the configured proxy; calling an annotated method from inside the same object does not create that interception. This fixture does not contain a self-invocation trap. Check the actual call path rather than adding machinery to solve a different problem. [Spring invocation rules](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

## What the starter does

The conditional claim already prevents duplicate worker applications in the tested overlap. Ordinary application, sequential replay, distinct credits and rejected IDs behave correctly. The absent enclosing transaction allows writes to commit individually:

| Experiment | Starter observation | Required observation |
|---|---|---|
| Exception after claim | Balance 0; applied 1 | Balance 0; applied 0 |
| Exception after balance | Balance 1000; applied 1 | Balance 0; applied 0 |
| Retry after failed claim | Conflict; no balance effect | Success; balance 1000; applied 1 |

The reference places both writes inside the worker transaction while preserving conditional claiming. The guided service stays as it was. The point is to distinguish two protections and prove each through the caller that needs them.

## A repair that looks relevant but misses the result

Serializing the worker with Java `synchronized` may seem natural after reading about races. Executed against the three failure/retry checks, with the transaction still absent, it fails all three: a lock does not undo committed writes. This control tests partial application only. The existing after-read gate requires two callers to arrive and cannot assess a lock held around the entire call; a gate timeout would be an instrumentation mismatch, not proof of a credit violation.

Removing the conditional predicate from the guided transactional service exposes the complementary mistake: overlap assertions fail while rollback assertions pass. Together these controls show why “add a lock” and “use a transaction” are incomplete explanations without identifying the property and boundary.

## Executed comparison evidence

Pinned lab: Java 21.0.12, Spring Boot 4.1.1, Spring Framework 7.0.9 and H2 2.4.240. Authoring runs establish fixture behavior, not human learning.

| Run | Result |
|---|---|
| Guided default suite | 24 pass |
| Guided repair assertions without conditional predicate | 3 overlap failures; 9 pass |
| Post-attempt check without opt-in flag | 9 skipped |
| Post-attempt check against starter | 3 failures; 6 pass |
| Private reference: guided + own evidence tests + comparison | 41 pass (24 + 8 + 9) |
| Private reader-style evidence tests against starter | 2 failure-point cases fail; 6 pass |
| Serializing-only worker, failure/retry subset | 3 failures |

The private tests exercise failure plus retry at both points, HTTP overlap in both orders, overlapping distinct credits, ordinary/replay and rejected IDs. The supplied check uses direct worker calls for overlap (B first) and failures, plus an ordinary HTTP call; it does not cover every worker schedule. Use your tests to explain this difference in coverage.

After your saved attempt, run from the lab directory:

```sh
mvn -DreviewCheck=true '-Dtest=*Test,WorkerContractCheck' test
```

Your total depends on the tests you wrote. The supplied cases alone total 33 after a repair; the private reference's additional eight tests are not in the public lab. A passing count is not the assignment's verdict.

## Where the evidence stops

This is near transfer of a taught mechanism. The check is visible in the repository; independence depends on honoring its post-attempt label. Study estimates and learning effect remain unmeasured.

The lab uses one process and one local H2 database. There is no queue, external payment, broken-connection recovery, response-replay protocol or exhaustive schedule analysis. The state endpoint uses two queries and is read only after calls finish. If a payment succeeds elsewhere and local work then fails, rolling back the credit rows cannot undo that payment. A blind retry needs a separate design and evidence. That is the changed condition to explain at delayed review.
