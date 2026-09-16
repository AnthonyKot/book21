# Essay 5 — token acceptance worksheet

Status: blank practice artifact. Record your attempt separately from the worked example. Start from [lab 5](https://github.com/AnthonyKot/book21/tree/main/labs/05-token-boundary) with `lab.audience-check=true`. Save your attempt before opening the review notes, which hold the hints.

## Time plan

Provisional; no learner has reported actual times.

| Part | Work | Estimate |
|---|---|---|
| Setup | Guided lab already built; reread the essay | 0.25–0.5 h |
| A — API profile | Predict, write your own failing tests, decide and implement the rule | 1.5–3 h |
| B — recovery review | Design review of the reset fragment | 1.5–2.5 h |
| Delayed check | Two to four days later, without notes | 0.5 h |
| Required total | | 3.75–6.5 h of a 10-hour week |
| C — optional | Two named Academy labs | 2–3 h |

Stop rules: if the lab fails to build for more than an hour, record the blocker and move on. Your new tests should fail against the unrepaired API; if after about two hours of Part A they do not (a setup problem), or your repair still does not make them pass, open Hint 1 in the review notes and record it as assistance.

## Predict before changing code

Both `reset` and `no-purpose` have a valid signature, the expected issuer, the document audience, valid timestamps and Alice's subject. Only the purpose differs from `valid`.

- Predicted response for each token at `/api/invoices/C-1001`:
- Observed response for each:
- Which existing check, if any, examines purpose:

## Implement the API profile

Create `PurposeRepairTest` extending `TokenHttp`, with the same `@SpringBootTest` configuration as `AudienceRepairTest` (`lab.audience-check=true`). Put the two new rejection tests there. Existing valid and foreign-invoice checks already run in the original suites.

Decide the rule the API's acceptance profile needs so that a credential issued for one operation cannot be presented as a credential for another. Inspect the issued fixtures at `/lab/tokens/<kind>` to see what distinguishes them. State which values your rule accepts, what an absent value means, and why the obvious alternative is weaker. Preserve issuer, signature, time, required-claim and audience checks. Do not change the existing reproduction or repair assertions to make the new tests pass.

| Test | Expected result | Observed before repair | Observed after repair |
|---|---|---|---|
| Reset token at invoice API | 401 | | |
| Missing-purpose token at invoice API | 401 | | |
| Valid token, Cedar invoice | 200, Cedar data | | |
| Valid token, Birch invoice | 404, no invoice data | | |
| All existing tests | Still pass | | |

- Which test failure demonstrates acceptance of the wrong token, rather than a setup problem:
- The rule you chose, the alternative you rejected, and what an absent value means under each:
- Where the rule is enforced, and why there rather than in the controller:
- How the issuer's matching token contract is documented:

## Review the separate recovery path

No recovery endpoint exists in this lab. Produce a design review of this illustrative fragment:

```text
validateResetToken(request.token)
changePassword(request.username, request.newPassword)
```

Choose a recovery credential that identifies its authorized account and permitted effect, expires, and can be consumed only once. Describe whether the binding is in protected server state or authenticated token claims plus a consumption record. Passwords and recovery credentials must not appear in your evidence log.

| Trace | Required outcome | Enforcement point and evidence you would request |
|---|---|---|
| Alice's valid recovery credential, Alice's password change | One authorized change succeeds | |
| Alice's credential, Bob named in form | Bob's account unchanged; reject the mismatch or remove the unrelated account selector | |
| Credential reused | No second change | |
| Two simultaneous uses | At most one succeeds | |
| Recovery credential at ordinary invoice API | Refused as an API credential | |
| Expired recovery credential | No password change | |

- Trusted source of the account being changed:
- Atomic operation that consumes the credential and authorizes the change:
- Failure/retry behaviour if the password write fails:
- Chosen policy for existing sessions and access tokens after reset:
- Which guarantees are only specified here, not demonstrated by the lab:

## Independent transfer

| Academy lab | Completed? | Assistance used | Missing check and where it belongs |
|---|---|---|---|
| JWT authentication bypass via unverified signature | | | |
| Password reset broken logic | | | |

## Evidence and next review

- Date and time spent:
- Test commands, failures before repair and final result:
- Assistance used, including AI-generated code:
- One token behaviour the test suite does not cover:
- Next unanswered question:
