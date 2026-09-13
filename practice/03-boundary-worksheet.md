# Essay 3 — scheduled export boundary worksheet

Status: blank practice artifact. Fill this in before reading the review notes. Completing the essay's own example is not recorded as your own attempt.

## Feature

An administrator schedules a weekly export of a folder, delivered by email to an address they enter. A scheduler service creates the job every Monday. An external email provider sends a download link. Folder contents and the administrator's permissions can change between runs.

## Drawing

Sketch the path from the scheduling request to the recipient opening the link. Paper is fine; photograph it or describe it in text.

- Components, in the order a weekly run touches them:
- Trust boundaries (label B1, B2, …), and who can write on each side:
- Components that did not exist in essay 3's export path:

## Identity along the path

| Hop | Identity present | Established by | Could the sender choose it? |
|---|---|---|---|
| Administrator's browser → API (creating the schedule) | | | |
| Scheduler → job table / queue (Monday) | | | |
| Queue → worker | | | |
| Worker → document store | | | |
| Service → email provider | | | |
| Email → recipient | | | |

- Whose permissions does Monday's job use, and when are they evaluated:
- What happens to the schedule if the administrator leaves the tenant:

## Three threats

| Boundary | STRIDE letter | Threat scenario | Response (mitigate, eliminate, transfer, accept) | Control | State: policy, implemented or verified | Residual risk |
|---|---|---|---|---|---|---|
| | | | | | | |
| | | | | | | |
| | | | | | | |

## One threat you do not mitigate now

- Threat and boundary:
- Reason it is not mitigated yet:
- Who owns revisiting it, and when:

## Your evidence

- Date and time spent:
- Assistance/hints used:
- The row of the identity table you found hardest, and what you decided:
- Next unanswered question:
