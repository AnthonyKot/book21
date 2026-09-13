# Essay 4 — review after attempting the lab

A reference solution was built and run while writing this essay: sixteen tests pass, the twelve original tests plus four new ones. It is deliberately not published. What follows is the behaviour a sound solution shows and the decisions it has to make, so you can check your own work without copying code.

## Behaviour a sound solution shows

| Case | Result in the reference solution |
|---|---|
| Carol reads C-1001 | 200: membership still allows reading |
| Carol requests export of C-1001 | 403: refused, no job row created |
| Alice's valid export | Job DONE, download 200 |
| Alice revoked before the worker runs | Job DENIED, no content written |
| Alice revoked after DONE, before download | Download 404 |
| `ScopedRepairTest` | All six still pass |

## Decisions that can differ and still be right

- **403 or 404 for Carol.** The reference returns 403, because Carol can already read the invoice, so a refusal reveals nothing she does not know. Returning 404 for everything is also defensible. What matters is that the choice is written down and a test pins it. A foreign tenant's invoice should still get the same 404 as a missing one.
- **Where the permission lives.** The reference uses a table of (user, tenant) export permissions read by the service. A role in Spring Security also works if it is read from the server's own records at the time of each check. A permission copied into the job row at request time does not satisfy the revocation cases.
- **Denied job versus deleted job.** The reference keeps the row with status DENIED, which leaves evidence of what happened. Deleting it hides the event.

## Where the checks belong

- **Request:** in the same access component as the tenant lookup, so the controller asks one question. A separate `if` in the controller works until someone adds a second export endpoint.
- **Worker:** against the job's requester, read when the worker runs. The worker has no user of its own; using a worker-level permission, or trusting that the API already checked, fails the revocation test.
- **Download:** again against the current permission, not only job ownership. Otherwise a revoked user keeps access to everything already generated.

## Incomplete answers to look for

- The permission is checked in the controller only. The revocation-before-worker test cannot pass.
- The worker checks a boolean like `approved=true` stored on the job.
- The download checks that the job belongs to Alice, not that Alice may still export.
- Carol's export is refused but a job row was created first.
- New tests were written after the code and never seen failing.
- `ScopedRepairTest` was changed to make it pass.

## Academy labs

For the GUID lab, the unpredictable identifier is found elsewhere in the application; the missing check is that the account page loads the requested user's data without comparing it with the signed-in user. For the IDOR lab, stored chat logs are served from static URLs with no check that the requester owns the file. In both, the repair belongs where the object is loaded for the current user, which is the same place as in the invoice lab. If you used the published solution or hints, record the lab as assisted.

This review describes behaviour, not proof that your implementation is complete. Keep a note of any path your change does not cover.
