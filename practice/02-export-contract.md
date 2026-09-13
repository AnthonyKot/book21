# Essay 2 — export contract worksheet

Status: blank practice artifact. Fill this in before reading the review notes. Completing the author-provided example is not recorded as your own attempt.

## Feature and policy

- Feature: export several documents in one job.
- Trusted source of requester identity:
- Source of document tenant and permission data:
- Who may download the generated archive:
- Whole-batch or partial-result policy, and why:
- Permission-check stages:
- Meaning of revocation at each stage:
- Permission service unavailable:
- Behaviour for revocation concurrent with a check/use transition:
- Deliberately unresolved limits:

## Security properties

Write at least one property for request acceptance, generation and download. Define what must not happen when a decision denies the operation. Describe the permitted audit side effect separately.

## Traces

| Case | Initial permissions | Ordered events | Expected job/result state | Bytes permitted to reach caller | Audit evidence |
|---|---|---|---|---|---|
| All requested documents permitted | | | | | |
| One document from another tenant | | | | | |
| C2 revoked before generation | | | | | |
| C2 revoked after generation, before download | | | | | |
| Permission service unavailable | | | | | |
| Another user obtains the job identifier | | | | | |

## Counterexample and revision

- Original wording:
- Trace that the wording failed to settle or prevent:
- Revised wording:
- Legitimate behaviour preserved:

## Your evidence

- Date and time spent:
- Assistance/hints used:
- What you can explain independently:
- Next unanswered question:
