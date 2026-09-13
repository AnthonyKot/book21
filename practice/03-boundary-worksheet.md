# Essay 3 — scheduled export boundary worksheet

Status: blank practice artifact. Fill this in before reading the review notes. Completing the essay's own example is not recorded as your own attempt.

## Feature and selected policy

An administrator schedules a weekly folder export. A scheduler creates Monday's job, and an external email provider sends a download link to an address entered by the administrator. Folder contents and permissions can change between runs.

For this exercise, the schedule acts on behalf of its creator. Each run requires that person's current tenant membership and per-document export permission. Download requires the authenticated job owner and current source-document permissions, as in essay 2. The recipient address does not grant download rights. Keep the job owner as the creator; sharing exports would require a separate policy.

## Drawing

Sketch the path from the scheduling request to the recipient opening the link. Paper or a text diagram is fine.

- Components and data flows, including job, schedule and permission records:
- Trust boundaries, with labels you can refer to below:
- Who can read, create, update and administer the relevant records:
- What your drawing leaves out:

## Identity along the path

| Operation | Service or user performing it | User whose product permission matters | Trusted source of that identity |
|---|---|---|---|
| Create schedule | | | |
| Create Monday's job | | | |
| Generate archive | | | |
| Read source bytes from storage | | | |
| Send email | | | |
| Authorize download | | | |

- What happens if the creator leaves the tenant on Sunday:
- What happens if one new folder document is not exportable by the creator:
- What can a recipient other than the creator do with the email link:

## Three threats and decisions

For each, describe an actor, starting capability and consequence. A control proposed on paper is not implemented or verified.

| Boundary or component | Threat scenario and STRIDE category | Response and owner | Proposed control | Verification case | Remaining limit |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

- Legitimate weekly run that must still succeed:
- An unresolved decision, if any, and who must resolve it before which event:
- Any risk explicitly accepted, with reason and owner (none is a valid answer):

## Your evidence

- Date and time spent:
- Assistance/hints used:
- The identity row you found hardest, and what you decided:
- Next unanswered question:
