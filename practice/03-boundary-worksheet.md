# Essay 3 — scheduled export design review worksheet

Status: blank practice artifact. Fill this in before reading the review notes. The review notes contain the hints; open them only after saving an attempt, and record any hint you use.

## Time plan

Estimates are provisional; no learner has reported actual times. There is no software to install.

| Part | Work | Estimate |
|---|---|---|
| Setup | None | 0 h |
| A — guided | Reread the essay; answer three questions about its example | 0.75–1.25 h |
| B — independent | Review the design note below | 2.5–4 h |
| Delayed check | Two to four days later, without your notes | 0.5 h |
| Required total | | 3.75–5.75 h of a 10-hour week |
| C — optional | Apply the method to a flow you know | 1–2 h |

Stop rules: if after about two hours of Part B you have not listed the facts and writers for every decision, open Hint 1 in the review notes, record it as assistance, and continue. If Part B is unfinished after about four hours, save what you have and carry the rest over rather than reading the answers.

## Part A — guided questions about the essay

1. For the repaired generation decision, list the facts it reads and every party able to write each fact. Which writer is still undecided?
2. In the 60-second link design, what does the download decision read once the link has been issued, and who can supply it?
3. Why does restricting queue publishers not, by itself, repair job 81?

## Part B — review the design note

### Product policy the design must satisfy

These rules are fixed for this exercise. If one seems wrong for the product, record it as a product question rather than changing it.

- A schedule acts on behalf of its creator. Each run may include only documents from the scheduled folder that the creator may export, using the creator's tenant membership and permissions current when the archive is generated.
- Documents the creator may not export are skipped rather than failing the run.
- Metadata is as confidential as the content it describes: a document's title only for people who may read that document, a folder's name only for people who may read that folder.
- Tenant administrators manage users, tenant settings and schedules. Being an administrator grants no read or export permission on folders.
- Only the job owner, who for a scheduled run is the schedule's creator, may download an archive, and only while holding current export permission for every document in it.
- The email is a notification. Receiving it grants no rights.
- Out of scope: the email provider's internal security, availability and load, and the user interface.

### Design note: scheduled folder exports (draft 2)

> **Goal.** Let a tenant administrator receive a weekly archive of a folder by email without clicking "export" every Monday.
>
> **Components.** Schedules API and download endpoint (`api-svc`); `schedules` and `jobs` tables; scheduler (`scheduler-svc`); export queue; export worker (`worker-svc`); document store; archive store; notifier (`notify-svc`) using an external email provider.
>
> 1. **Creating a schedule.** An administrator signs in and submits a folder, a weekday and a recipient address. The API checks that the administrator currently has export permission in that folder, then stores `creator_id` from the authenticated session together with `folder_id`, `weekday` and `recipient`.
> 2. **Editing a schedule.** Any administrator in the tenant can edit a schedule's folder, weekday or recipient, so that schedules keep working through holidays and staff changes. Edits do not change `creator_id`, so runs keep the creator's permissions.
> 3. **Starting a run.** At 02:00 on the scheduled day, `scheduler-svc` reads due schedules and inserts a job row (`job_id`, `schedule_id`, `creator_id`, `folder_id`). To avoid one permission-service call per document in the worker, the scheduler asks the permission service which documents in the folder the creator may export and publishes `{job_id, allowed_document_ids}`.
> 4. **Generating.** `worker-svc` loads the job row, fetches exactly the documents in `allowed_document_ids` from the document store, writes the archive and a manifest of included document IDs, and marks the job complete. If generation fails, the broker redelivers the same message for up to 72 hours.
> 5. **Queue access.** Broker ACLs allow only `scheduler-svc` and `api-svc` to publish to the export queue. `api-svc` publishes on-demand exports in the same message format.
> 6. **Notifying.** When a job completes, `notify-svc` emails the schedule's recipient. The subject is "Weekly export: <folder name>"; the body lists the file names in the archive and links to `https://app.example/exports/<job_id>`.
> 7. **Email protection.** Connections to the email provider require TLS 1.3. Recipient addresses must belong to one of the tenant's verified email domains.
> 8. **Downloading.** The link opens the application. The endpoint requires sign-in, loads the job, requires the signed-in user to equal `creator_id`, checks that user's current export permission for every document in the manifest, and streams the archive from the archive store. No storage URL is issued.
> 9. **Audit.** Schedule creation, edits, runs and downloads are written as events recording the acting identity, schedule or job, changed fields, outcome and time. Application identities can append events but not modify or delete them.

### What to produce

1. **Drawing.** Components, data flows and the trust boundaries the note implies, labelled so you can refer to them. State what you left out.
2. **Decisions table.** Every decision the design makes about what to generate, send or release. For each: the facts it reads, where each fact is stored or carried, and every identity or event able to write or change it.
3. **Verdicts.** One row for each of the nine note items: *no change needed*; *helps but insufficient*; *protects a different crossing*; or *does not satisfy the policy* (change needed). An item with two controls can have a verdict for each. Give the reason and the note sentence it rests on.
4. **Findings.** As many as your evidence supports; there is no quota. For each: actor and starting capability, consequence under the policy, STRIDE label if useful, response and owner, proposed control, a case that must be denied, and a legitimate run that must still succeed.
5. **Open decisions.** Any product question the policy does not settle, with who must decide and before which event. Any accepted risk, with reason and owner. "None" is a valid answer to either.
6. **Reassessment trigger.** One change to the design or policy that would require this review to be repeated, and why.

Success criteria: another engineer can follow each verdict from a sentence in the note to a fact and its writer, and can tell which runs must still succeed. Your review may conclude that parts of the note need no change.

| Decision | Facts it reads | Stored or carried in | Who or what can write or change each fact |
|---|---|---|---|
| | | | |

| Note item | Verdict | Reason and supporting sentence |
|---|---|---|
| | | |

| Actor and capability | Consequence under the policy | Response and owner | Proposed control | Must be denied | Must still succeed |
|---|---|---|---|---|---|
| | | | | | |

- Open decisions:
- Accepted risks:
- Reassessment trigger:

## Part C — optional transfer

Pick one flow from a system you know well. Write its decisions table: decision, facts read, and who can write each. Keep employer or client details private; this is for your own review, not for publication.

## Your evidence

- Date and time spent on A, B and C:
- Hints opened (which, when and why):
- The verdict you changed your mind about, and what changed it:
- Delayed check (two to four days later, without notes): choose one note item, rebuild its decisions-table row, then compare with your saved answer. What differed?
- Next unanswered question:
