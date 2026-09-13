# Essay 3 — review after attempting the worksheet

There is no single correct drawing. The expected evidence is a path whose boundaries follow who can write, an identity column that has an answer for every hop, and threats tied to specific crossings with controls whose state is honest.

## The hard row: who is the requester on Monday?

No user session exists when the scheduler runs. The scheduler's service identity is not the administrator, and using its permissions would repeat the worker-credential mistake from essay 3 one component earlier. A sound answer stores the administrator who created or last changed the schedule as the originating requester, and evaluates that person's *current* tenant membership and export permission at every run. A schedule is a request that repeats; it is not a standing grant.

Then decide what removal means. If the administrator leaves the tenant on Sunday, Monday's run must not generate. Stronger answers also disable or reassign the schedule when the membership ends, so the failure is visible rather than silent every week.

## Boundaries that are new

- **The scheduler is a second writer of jobs.** Essay 3 made the job table API-write-only. The scheduler now needs to create jobs too, so the boundary moves again. A good answer limits the scheduler to creating jobs from stored schedule records, and has the worker check the schedule's originating requester, not a requester the scheduler supplies.
- **The email provider is a third party.** It receives the recipient address and whatever the email contains. Putting a bearer download link in the email means the provider, the mailbox and anyone the email is forwarded to can hold a credential.
- **The recipient address is an output channel chosen by a user.** An administrator can type any address, including one outside the tenant.

## Threats a strong answer usually includes

| Boundary | STRIDE | Threat | A fitting control |
|---|---|---|---|
| Scheduler → job | E | Removed or demoted administrator's schedule keeps exporting | Evaluate the stored requester's current permission at each run; disable the schedule on removal |
| Service → email → recipient | I | Tenant documents sent to an arbitrary external address | Recipient must be a tenant user who signs in to download, or addresses restricted to verified tenant domains |
| Email → recipient | I | Link in email is a bearer credential stored at the provider and forwardable | Link opens a sign-in-required download that applies the essay 2 current-permission check |
| Worker → store | E | Folder gains a document the administrator may not export | Check permission per document at generation, not per folder at scheduling |
| Browser → API | R | Nobody can show who changed the recipient address | Append-only event for schedule creation and every change |

Choosing to *transfer* the email-provider risk is acceptable if you say what you transfer: the provider's handling of message contents under its contract. You cannot transfer the decision to put a bearer link in the message; that stays yours.

## Look for these incomplete answers

- The scheduler "runs as the administrator" without saying whose permissions are checked, or when.
- A threat listed without the boundary where it occurs.
- "Validate the email address" offered for a threat that is about *which* address is permitted, not whether it is well formed.
- Controls written as sentences with no state, so a policy reads as enforcement.
- No threat left unmitigated, which usually means none were weighed, not that all were fixed.
- TLS to the email provider offered as the control for sending documents to the wrong recipient.

The worksheet is ready for discussion when another engineer can point at any line on your drawing and find the threats that apply there. It is not evidence that a running scheduler behaves this way.
