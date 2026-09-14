# Credit invariant review

Compare after attempting the [worksheet](10-credit-worksheet.md).

## Hint 1: separate the two obligations

The conditional write prevents a second successful claimant. The transaction keeps the claim and balance effect together. Both are needed for this operation. A failure after an autocommitted claim can consume a credit even though the balance never changed.

## Hint 2: inspect the invoked boundary

Compare `CreditService.apply` and `CreditWorker.apply`. Both call `CreditOperation`, but only one establishes a transaction in the starter. The repair should cover the whole operation. If you use Spring’s annotation, ensure the actual call passes through a managed transactional proxy; constructing a worker yourself or relying on self-invocation changes that behavior. [Spring transaction invocation rules](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

## Hint 3: test failure through the real bean

The injected unchecked exception must escape the transaction boundary. After either failure point, read committed state from outside that call and expect no claim and no balance increase. Then retry. Preserve the conditional predicate and the conflict behavior under overlap. Do not add a transaction to the test as a substitute for fixing the worker.

## Evidence to expect

The starter passes ordinary application, duplicate rejection and overlapping-worker claiming. It fails rollback after the claim, rollback after the balance and retry after failure: three failures, three passes. The private reference passes all 30 combined tests. Restoring the missing-transaction version provides the counterexample again.

For the guided negative control, only the three overlap tests fail. The transaction still rolls back synthetic failures, and sequential replay sees the committed flag. Those passing tests are useful evidence about different properties; they are not evidence that the overlap is safe.

The application uses a single local H2 database with pooled connections. No distributed queue, external payment, response-replay protocol, crash recovery or general schedule coverage is established. A final state endpoint made of two queries is not an atomic snapshot during active writes. Interpret the lab’s state only after all tested requests complete.
