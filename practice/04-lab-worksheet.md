# Essay 4 — export permission lab worksheet

Status: blank practice artifact. Fill it in as you work. The lab source is [labs/04-wrong-invoice](https://github.com/AnthonyKot/book21/tree/main/labs/04-wrong-invoice); start from `lab.lookup=scoped`, which is where essay 4 leaves it.

## The change

- Add an export permission that is separate from tenant membership.
- Add Carol, a Cedar member without export permission. Alice and Bob keep export permission in their own tenants.
- Enforce the permission wherever an export is requested, generated or downloaded.
- Keep every test in `ScopedRepairTest` passing.

## Decide the policy before the code

- Where the permission is stored, and who can change it:
- Carol requests an export of C-1001. Response status and why that status (she can already read the invoice):
- Alice's permission is revoked after her job is queued, before the worker runs. Job outcome:
- Alice's permission is revoked after the job is DONE, before she downloads. Download outcome:
- The permission check happens against whose identity, in the worker:
- What your design deliberately does not handle (for example, revocation during a download in progress):

## Tests to write first

Write each test, run it, and record that it failed before your change.

| Case | Expected observable result | Failed before change? | Passes after? |
|---|---|---|---|
| Carol reads C-1001 | | | |
| Carol requests export of C-1001 | | | |
| Alice's valid export completes and downloads | | | |
| Revoked before worker runs | | | |
| Revoked after DONE, before download | | | |
| All of `ScopedRepairTest` | Still passes | — | |

## Where each check sits

| Path | File and method you changed | Identity whose permission is checked | Why here |
|---|---|---|---|
| Export request | | | |
| Worker | | | |
| Download | | | |

## PortSwigger Academy transfer

| Lab | Solved? | Hints or solution used | Missing check, in one sentence | Where it belongs |
|---|---|---|---|---|
| User ID controlled by request parameter, with unpredictable user IDs | | | | |
| Insecure direct object references | | | | |

## Your evidence

- Date and time spent (lab, and each Academy lab):
- Command and final test summary line:
- Assistance used (hints, AI, review notes):
- One path your change still does not protect:
- Next unanswered question:
