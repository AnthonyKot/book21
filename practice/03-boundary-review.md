# Essay 3 — review after attempting the worksheet

A strong answer connects each threat to a specific capability, flow, decision and proposed verification. Different drawings can express the same design. Check whether yours contains enough information to follow a disputed export from its schedule to its recipient.

## Who is the requester on Monday?

The exercise selects a creator-owned schedule. Store the creator's identity when the API authenticates the scheduling request, protect that record, and evaluate the creator's current tenant membership and document permissions at each run. If the creator leaves on Sunday, Monday's run must not generate an archive. Disabling the schedule makes the outcome visible and avoids repeated failed work; reassignment needs an explicit authorized operation.

The scheduler still has its own service identity. Its ability to create a job does not establish the creator's permission to export its contents. The worker needs an authoritative relationship between schedule, originating requester and job, rather than trusting a requester field supplied independently in a message.

A tenant-owned automation account is another possible product design. It would need its own grants, administration and lifecycle rules. It is not the policy selected by this exercise; recognizing it as an alternative is useful, but silently changing to it would leave the Sunday-removal case unanswered.

## New components change the review

The scheduler creates jobs, so the original API-only creation rule must change. Name which fields it may create and where their authority comes from. Keep the worker's result updates separate from permission to change requester or source references. A second legitimate writer is a reason to inspect these rules, not automatic proof of a vulnerability.

The email provider receives addresses and message contents. Sending a bearer storage link also gives it a credential that could be forwarded. A sign-in-required application link avoids granting access through possession alone; the download endpoint must still enforce job ownership and current document permissions.

The recipient address is an output chosen by the administrator. A verified tenant domain does not prove that a mailbox owner may receive this export. Under the selected policy, an email sent to someone other than the creator grants that recipient no download rights. If this makes the feature awkward, record the product question: should delivery be limited to the creator's verified address, or should a separately authorized sharing feature be designed? Do not resolve it by weakening the download check unnoticed.

## Example threats and evidence

| Scenario | Proposed control | Evidence to request |
|---|---|---|
| Removed creator's schedule keeps exporting | Current creator permission checked at generation | Sunday removal prevents Monday's archive; unchanged membership still permits a valid run |
| A job producer substitutes another requester | Protected schedule/job relationship and constrained writes | Forged requester cannot redefine the job; intended producer can create valid work |
| Forwarded email exposes document bytes | Link requires authentication, ownership and current permissions | Another recipient gets no bytes; authorized creator downloads successfully |
| Folder gains an unauthorized document | Per-document checks under a declared batch policy | The archive excludes forbidden bytes and accurately reports whole-job failure or an explicitly partial result |
| Recipient changes cannot be investigated | Protected events for schedule creation and changes | Events identify actor and change; application account cannot alter earlier events |

Treat email subject lines and previews as disclosures too. A protected download does not repair sensitive contents already included in the message.

## Judge the decisions, not the number of accepted risks

It is reasonable to mitigate all three selected threats. It is also reasonable to document a remaining risk with a reason and accountable owner. A deferred decision must say who will resolve it and before what event; it is not automatically permission to launch.

A provider contract can assign obligations for handling messages. It does not make a bearer link non-forwardable or remove the product team's responsibility for choosing what it sends.

Watch for service identity substituted for user permission, a control described as verified without execution, or TLS offered as the answer to an authorized sender choosing the wrong recipient. The worksheet is ready for discussion when another engineer can explain your valid run and denied run without inventing missing policy. It does not prove a running scheduler behaves that way.
