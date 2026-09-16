# Essay 3 — review notes for the scheduled export design

Open these only after saving your worksheet. Sections 1 and 2 help without giving answers. Sections 3 and 4 contain the answers; read them after you have finished or decided to stop.

## 1. Judging any review

Different drawings and wording can express the same judgment. Assess an answer, including your own, by these questions:

- **Decisions are complete.** Does the table include every point where the design chooses what to generate, what to send and to whom, and what to release? Missing a decision usually means missing its writers.
- **Writers include time and people.** Are writers named as identities (services, administrators, the broker) and events (edits, redelivery), not only as tables?
- **Verdicts are traceable.** Does each verdict name the note sentence, the fact it concerns and why the writer is or is not trusted under the policy?
- **Sound parts are recognised.** A review that flags every item has not distinguished a control at the right crossing from one at the wrong crossing.
- **Findings have consequences under the stated policy.** "Could be insecure" is not a finding. "Administrator D receives titles of documents D cannot read" is.
- **Legitimate behaviour survives.** Each proposed control keeps a named run working, such as the creator receiving and downloading an ordinary weekly archive.
- **Open questions stay open.** Where the policy is silent, the answer names who decides and before what event rather than quietly choosing.
- **Chains may be located either way.** Some consequences need two note items together. A review may put the failing verdict on either item, or on both, if the finding cites both sentences and names the writer. Giving *no change needed* to an item that your own finding depends on is inconsistent.

## 2. Hints, in order

Record each hint you open and when.

**Hint 1 — start from decisions.** Write three headings: *what goes into the archive*, *who is told what*, *who may download*. Under each, list every field, message or record the design reads to make that choice.

**Hint 2 — start from outputs instead.** List everything that leaves the system during a run: bytes, messages and text. For each, write who receives it and whether the policy says that person may see it.

**Hint 3 — writers change over time.** For each fact a run reads, ask who could have set it and when: at creation, at a later edit, at 02:00, or at a redelivery two days later.

**Hint 4 — one sound check does not cover another output.** Compare what item 8 protects with what has already happened by the time anyone clicks the link.

**Stop here if you are still attempting.** Everything below this line is the answer. Return to your worksheet and record which hint you used.

## 3. What the note contains

### Decisions and their writers

| Decision | Facts read | Who or what can write them |
|---|---|---|
| Which documents go into the archive (item 4) | `allowed_document_ids` in the message | Any queue publisher: `scheduler-svc` and `api-svc`. A redelivery replays a value computed up to 72 hours earlier |
| Which folder and creator a run uses (item 3) | `schedules.folder_id`, `schedules.creator_id` | Creator at creation; **any tenant administrator** can change the folder; `creator_id` is set from the session and never changed |
| Who receives the email and what it says (item 6) | `schedules.recipient`, folder name, manifest file names | Recipient: creator or **any administrator**. File names follow whatever the worker included |
| Who may download (item 8) | Signed-in user, `jobs.creator_id`, manifest, current permissions | Session; `scheduler-svc` copies `creator_id` from the schedule; `worker-svc` writes the manifest; permission service is current |

### Verdicts

| Item | Verdict | Why |
|---|---|---|
| 1 Creation | No change needed | `creator_id` comes from the authenticated session; the creation check is not what runs rely on |
| 2 Editing | Does not satisfy the policy (with item 6); needs a product decision | Another administrator writes the folder and recipient that a run uses with the creator's authority. A folder edit alone still exports only what the creator may export; the breach appears when item 6 sends that folder's metadata to the editor's address. Whether anyone but the creator may change what runs on the creator's behalf is not settled by the policy |
| 3 Starting a run | Protects a different crossing | The permission check happens when the job is created, and its result becomes a message fact that any publisher can write and redelivery can replay. The generation decision needs it at generation |
| 4 Generating | Does not satisfy the policy | The worker trusts the message list: no check against the job's folder or the creator's current permission at generation |
| 5 Queue access | Helps, but insufficient | Fewer writers, but two legitimate producers remain and redelivery staleness is untouched. It is the essay's publisher-restriction point in a new place |
| 6 Notifying | Does not satisfy the policy | The subject carries the folder name and the body carries document titles. Both reach an address chosen by the creator or any administrator, whose owner need not have read permission |
| 7 Email protection | TLS protects a different crossing; domain rule helps but is insufficient | TLS protects the connection to the provider, not the choice of recipient. A verified domain does not show read permission |
| 8 Downloading | No change needed | Current checks at every download against the manifest; no bearer URL. A reviewer who reports a link problem here is recalling the essay, not reading the note |
| 9 Audit | No change needed for its purpose | Edits are attributable afterwards. It detects item 2 misuse but does not prevent it |

### Findings a strong review reaches

The wording and number of rows can differ. These are the consequences the design actually permits under the policy.

| Actor and capability | Consequence | Example response | Must be denied | Must still succeed |
|---|---|---|---|---|
| Administrator D, with no read permission on Finance, edits creator Alice's schedule | Alice's authority exports Finance; the email sends the Finance folder name and document titles to D's address | Product owner: folder or recipient edits re-authorise the run as the editor, or require an explicit ownership transfer accepted by the new owner | D's edit to Finance plus D's address sends D no Finance metadata | Alice's unedited schedule; a holiday change of weekday |
| Creator removed on Tuesday; Monday's job fails and is redelivered on Wednesday | The archive includes documents under Monday's permissions; the email lists their titles | Backend lead: worker loads the job, derives documents from the job's folder and checks the creator's current permission at generation | Wednesday redelivery produces no archive and no title list | Monday's first delivery for a current member |
| Bug or compromise in `api-svc`, an authorised publisher | Any job can be filled with any document IDs; titles go out by email | Backend lead: messages carry only `job_id`, as in the essay | Forged list for an existing job adds nothing the job's folder and creator do not allow | On-demand exports through `api-svc` |
| Recipient without read permission, chosen at creation or later | The folder name and titles reach them even though download is denied | Product owner and API lead: email carries no titles; the application shows them after sign-in | Email to someone without read permission contains no titles or folder name | Creator signs in and sees the list |

An open decision a strong review records: under the policy, a recipient other than the creator receives a link they can never use. Either delivery is limited to the creator's own address, or a separately authorised sharing feature is designed. The product owner decides before release; weakening the download check to make the email useful is not an answer.

Also sound, though not required:

- A database or infrastructure administrator can write `schedules` and `jobs` directly. This is the essay's administrator decision in a new place: record the owner and the event before which it must be decided.
- The note does not say what the scheduler or worker does when the permission service cannot be reached at 02:00 or during a redelivery. Recording that as an open decision is good; the policy's generation rule means a run must not proceed on a guessed answer.
- The scheduler could disable a schedule when its creator is no longer a member, rather than sending empty or failing runs.

Findings about shared capacity, such as redeliveries delaying other tenants' exports, are real concerns that this exercise's policy places out of scope. Naming them as out of scope is a good answer; they do not replace the findings above.

Reassessment triggers that fit this design include schedules owned by a service account, sharing archives with other users, attaching files to the email, adding a second producer, or letting the worker use a permission cache.

## 4. Answers that look finished, and what they miss

- **A STRIDE row for every component.** Six categories per box produce plausible threats that never mention `allowed_document_ids` or who edits a schedule. The table has not identified a decision's facts.
- **"Add TLS and restrict recipients to the tenant domain."** The note already does both. Neither decides whether this recipient may see these titles.
- **"Replace the email link with a signed link" or "the link is a bearer token."** Item 8 already streams after current checks. Changing it would weaken a sound control.
- **"Worker re-checks the creator's permission."** Necessary, but if the document list still comes from the message, a publisher can still choose documents outside the scheduled folder that the creator happens to be allowed to export.
- **"Remove the file list from the email" alone.** It closes the title disclosure. The archive itself can still contain documents chosen by another administrator or included after the creator lost permission, which violates the generation rule even if download is denied.
- **"Only the creator may edit schedules" with no product decision.** The note wanted schedules to survive holidays and staff changes. A good answer keeps that requirement visible and names an owner for reassignment instead of silently dropping it.
- **"Audit events mitigate unauthorised edits."** They support investigation after the email has already gone.

## 5. Evidence behind this guide

This is a design review. No scheduler, broker or email provider was built or run, and none of the proposed controls has been implemented or tested. The verdicts follow from the policy and note as written. Another sound review may divide findings differently or propose different controls with equivalent consequences. A blind model attempt (not a learner) was run on these pages before publication; its feedback broadened this guide to accept verdicts placed on either item of a chain and to include the administrator and permission-service observations above.

## 6. Limits

The exercise is near transfer: the essay teaches the decision-facts-writers method with a queue, a link and a log, and this task applies it to a new design with new writers. It does not assess discovering an unfamiliar vulnerability class, estimating likelihood or impact, or reviewing a real deployment's permissions. The policy is supplied, so the task exercises judging controls against a policy, not choosing that policy (essay 2 does that). Several policy rules correspond closely to particular note items, and Part A rehearses the essay's message-versus-record pattern, so part of the task can be approached by matching. The findings about who else writes schedule fields and where the document list must come from are the parts that matching alone does not reach.
