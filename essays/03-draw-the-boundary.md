# Draw the boundary before choosing the control

Essay 2 ended with a good export contract. The API checks Alice's tenant and export permission when she asks. The worker checks her current permission again before it generates anything. The download endpoint checks once more before any bytes leave. Every rule names a principal, a resource and a decision point.

Now a reviewer asks one question: when the worker checks "Alice's current permission", how does the worker know the job belongs to Alice?

The contract cannot answer. It says *what* must be decided and *when*. It does not say where the facts used by each decision come from, or who else could have written them. A worker that reads `requester=alice` from a queue message performs the check faithfully, for whichever requester the message names. If something other than the API can put a message on that queue, the check protects exactly nothing.

That is the gap this essay closes. Before you pick a control, draw the path the request actually takes, mark where trust changes, and ask concrete questions at each crossing. The controls you choose afterwards will then answer a question someone can check.

## Follow the request, not the boxes

The export feature has six parts. Draw them in the order a request touches them:

<figure class="diagram">
<svg viewBox="0 0 720 300" width="100%" role="img" aria-label="Export path: browser to API, API writes a job record and a queue message, worker reads both and document storage, download returns through the API. A download link returns from storage to the browser. Dashed lines mark three trust boundaries.">
<g fill="none" stroke="currentColor" stroke-width="1.5">
<rect x="10" y="40" width="110" height="50" rx="6"/>
<rect x="190" y="40" width="130" height="50" rx="6"/>
<rect x="190" y="200" width="130" height="50" rx="6"/>
<rect x="400" y="40" width="120" height="50" rx="6"/>
<rect x="590" y="40" width="120" height="50" rx="6"/>
<rect x="590" y="200" width="120" height="50" rx="6"/>
<path d="M120 65 H190 M320 65 H400 M520 65 H590 M255 90 V200 M650 90 V200 M650 250 V278 H65 V90"/>
<path d="M155 15 V285 M360 15 V130 M555 15 V285" stroke-dasharray="6 5"/>
</g>
<g fill="currentColor" font-family="system-ui,sans-serif" font-size="14" text-anchor="middle">
<text x="65" y="70">Browser</text>
<text x="255" y="70">Export API</text>
<text x="255" y="230">Job table</text>
<text x="460" y="70">Queue</text>
<text x="650" y="70">Worker</text>
<text x="650" y="230">Document store</text>
<text x="155" y="12" font-size="12">B1</text>
<text x="360" y="12" font-size="12">B2</text>
<text x="555" y="12" font-size="12">B3</text>
<text x="455" y="272" font-size="12">download link</text>
</g>
</svg>
<figcaption>The export path. B1: the internet meets your API. B2: the API hands work to shared infrastructure. B3: everything to its right acts with the worker's own credential, which can read every tenant's documents. The download link crosses B3 and B1 on its way back.</figcaption>
</figure>

A **trust boundary** is a line where data or identity passes between parts that are controlled differently, so the receiving side has to decide what it will believe. The browser is controlled by whoever holds it. The queue is shared infrastructure that more than one service may be allowed to write to. The worker holds a credential that can read every tenant's documents. Each dashed line is a place where "the sender said so" stops being good enough.

Boundaries are not the same as boxes. The API and the job table sit on the same side here because only the API's database account can write jobs. If an operations script also had write access to that table, a boundary would run between them too. You find boundaries by asking who can write, not by counting deployments.

Now carry the identity along the path. For each hop, write down what identity is present, where it came from, and whether the sender could have chosen it:

| Hop | Identity present | Established by | Could the sender choose it? |
|---|---|---|---|
| Browser → API | Alice | Session validated by the API | Only by stealing a session |
| API → job table | Alice as requester, document C, job owner Alice | Written by the API after its check | No, if only the API can write |
| API → queue | Whatever the message carries | The message body | Yes, for anyone allowed to publish |
| Queue → worker | Worker's own service identity | Worker's credential | Not applicable: it is not Alice |
| Worker → store | Worker's own service identity | Storage credential | Not applicable: it is not Alice |
| Store → whoever holds the link | Nobody | Possession of the link | Yes: anyone who has it |

Two rows stand out. At the queue, identity turns from a verified fact into a claim in a message. At the link, identity disappears entirely. Those are the rows where the controls from essay 2 can quietly stop meaning anything.

## Ask STRIDE at each crossing

STRIDE is a mnemonic from Microsoft for six kinds of thing that can go wrong. Each one is the violation of a property you already care about:

| Letter | Threat | Property it breaks | Question at a crossing |
|---|---|---|---|
| S | Spoofing | Authentication | Can someone claim to be another principal here? |
| T | Tampering | Integrity | Can data change between being checked and being used? |
| R | Repudiation | Accountability | Could someone deny an action, and could you show otherwise? |
| I | Information disclosure | Confidentiality | Can data reach someone not permitted to see it? |
| D | Denial of service | Availability | Can someone stop legitimate users getting their exports? |
| E | Elevation of privilege | Authorization | Can someone gain the effect of a permission they lack? |

STRIDE does not find threats for you. It is a prompt you run against a specific drawing. Asked of the whole system at once, the six letters give six generic answers. Asked at B2, they give questions with names in them: can a service other than the API publish an export message? Does the worker believe the requester field in that message? Can someone fill the queue with ten thousand export jobs?

Run all six at each boundary and you get more candidate threats than you will fix this sprint. That is intended. The threat model's job is to put them on the table so that choosing among them is a visible decision, not an omission.

For the export path, the pass produces, among others: a forged queue message (T, E at B2), a session stolen from the browser (S at B1), a forwarded download link (I at B3), a user denying they exported a document (R), bulk job submission starving other tenants (D at B2), and a worker credential leaked from its host (E at B3). The rest of this essay works three of them through to a decision.

## Threat 1: the queue message names a different requester

**Scenario.** Job 81 is Alice's legitimate export of document C. A message arrives saying `{job: 81, requester: bob, document: B}`. The worker checks Bob's current permission for B, finds it, and writes Birch's document into job 81's archive. The download endpoint then checks what its own record says, that Alice owns job 81 and may export C, and hands her the archive. Who wrote those messages? Anything with publish rights on the queue: the API, but perhaps also a retry tool, a second service sharing the broker, or a developer's laptop with a leftover credential.

This is tampering at B2 that becomes elevation of privilege at the download. The worker's check was never skipped. It was applied to facts that an untrusted writer chose.

**The attractive control.** "Enable TLS on the queue connection." It is worth having, and it answers a different question. TLS stops a party on the network path from reading or altering a message in transit. It does nothing about a sender that is allowed to connect and simply writes false content. A control must match the crossing where the threat actually occurs.

**The control that fits.** Make the message carry only the job identifier. The worker loads requester, document and owner from the job table, which only the API's database account can write, and checks the originating requester's current permission from there, never its own. Restrict publishing on the export queue to the API's service identity, authenticated with its own credential.

Notice that this moves the boundary rather than removing it. The question "who can publish?" becomes "who can write the job table?" That is progress because the table has one intended writer and ordinary database permissions to enforce it, while the queue had several plausible publishers and no record of intent.

**Policy or control?** "Only the API creates export jobs" is a sentence in a design document. It becomes a control when the broker's access list denies publishing to every other principal and the database grants insert on the job table to the API account alone. It becomes *verified* when a test publishes as the worker's identity and observes rejection, and a second test forges a job row through a non-API account and observes the database refuse. Record which of the three states each control is in. A threat model full of policies reads as protection while enforcing none of it.

**Residual risk.** A database administrator can still write a job row. Accept that, with the reason written down: administrators are already trusted with every document, and changes to the table are audited.

## Threat 2: the download link outlives the permission

**Scenario.** On completion the service gives Alice a pre-signed storage URL valid for 24 hours. Anyone who has the URL can fetch the archive until then. Alice pastes it into a support ticket. Separately, an administrator revokes her export permission an hour later. Both the ticket reader and Alice can still download. The essay 2 contract said downloads require *current* permission, and nothing in this delivery path can evaluate that.

This is information disclosure at B3, and the boundary table predicted it: identity disappears at the link.

**Two controls, two costs.**

| Option | What it enforces | What it costs |
|---|---|---|
| Download through the API: check identity, job owner and current permission, then stream | The contract as written, on every download | The API carries file traffic |
| Check in the API, then issue a link valid for 60 seconds | Current permission at the moment the link is issued | A 60-second window in which the link is a bearer credential |

Neither is wrong. The second changes the promise from "current permission at download" to "current permission within a minute of download". If you choose it, change the contract to say so. Quietly keeping the old wording while shipping the short link is exactly the gap between policy and control that this essay is about.

Choose the first for this example: exports are infrequent and documents are sensitive. **Residual risk:** a file Alice has already downloaded stays with her. Essay 2 recorded that limit; the threat model repeats it here so a reviewer does not assume otherwise.

## Threat 3: nobody can show who exported what

**Scenario.** A Cedar customer says a confidential document left the company through an export. Alice says she never requested one. The service logs contain `export completed job=81` from the worker, with no requester, no document and no record of the permission decisions. And the log store accepts deletes from the same service account that writes to it.

This is repudiation. Nothing was stolen through a flaw here; the failure is that you cannot establish what happened.

**The control.** The authorization events designed in essay 2 now get a location and a protection. The API and the worker each write an event for every export decision: who, which job and document, which stage, the outcome and the time. Denied attempts are recorded as well as successful ones. Events go to a store where those services can append but cannot modify or delete.

**Residual risk.** The log shows that Alice's session requested the export. It does not show that Alice was at the keyboard. If her session was stolen, that is a spoofing threat at B1, handled by session controls, and the log is evidence for the investigation rather than proof of intent.

## Write the decisions down

The three threats fit one record. Keep it next to the contract from essay 2:

| Boundary | STRIDE | Threat | Response | Control and its state | Residual risk |
|---|---|---|---|---|---|
| B2 | T, E | Forged queue message selects requester or document | Mitigate | Message carries job ID only; job table API-write-only; queue publish API-only. Policy written, access lists pending, tests pending | DBA can write jobs: accepted, audited |
| B3 | I | Link usable after revocation or by others | Mitigate | Download streamed through API with current check. Designed, not built | Already-downloaded copies: accepted |
| — | R | Export cannot be attributed | Mitigate | Append-only decision events from API and worker. Designed | Stolen session: separate S threat at B1 |
| B2 | D | Bulk jobs starve other tenants | Not chosen now | — | Revisit before launch |

A threat has four possible responses: mitigate it, eliminate the feature that causes it, transfer responsibility to someone else, or accept it. Accepting is a legitimate answer when it is written down with a reason and an owner. An unlisted threat is not an accepted one; it is an unexamined one. The last row matters as much as the first three: it shows the pass considered availability and chose not to act yet.

The record also shows where evidence is missing. Every "pending" in the control column is a test someone can write.

## What the drawing does not tell you

A diagram is only as true as the system it describes. If the job table later gains a second writer, the B2 control silently weakens and the diagram still looks fine. Revisit the model when a component, credential or data flow changes, not on a calendar.

STRIDE also has no scoring. It tells you what kind of harm is possible, not how likely or how costly. Choosing which three threats to work first used judgement about this product: sensitive documents, a shared queue, infrequent exports. Another product would choose differently from the same six letters.

<!--mission-->

## Practice: the export nobody clicked

Use the [worksheet](../practice/03-boundary-worksheet.md). This is a design exercise; no software execution is claimed or required.

Change the feature: an administrator can schedule a weekly export of a folder, delivered by email to an address they enter. A scheduler service creates the job every Monday. An external email provider sends a download link. The folder's contents change between runs, and so can the administrator's permissions.

Draw the path from the scheduling request to the recipient opening the email. Mark every trust boundary and say who can write on each side. Fill in the identity column for each hop; the hard row is "who is the requester when the scheduler creates Monday's job?" Then choose three threats, each with a STRIDE letter, the boundary where it occurs, a response, a control stated as policy, implemented or verified, and its residual risk. Include one threat you deliberately do not mitigate, with the reason.

Try it before opening the [review notes](../practice/03-boundary-review.md).

If stuck, start with the identity column. Next, ask which component would still act if the administrator were removed from the tenant on Sunday. Finally, look at the email address field and ask who it lets the administrator send tenant documents to.

Completion means another engineer can point at a line on your drawing and find the threats that apply there, and can tell from your record which controls exist and which are still sentences.

Source note: primary sources inspected on 13 September 2026. STRIDE categories and the properties they violate follow Microsoft's [Threat Modeling Tool threat page](https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats) and OWASP's [Threat Modeling Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Threat_Modeling_Cheat_Sheet.html), which also gives the four threat responses. ASVS 5.0.0 requirements [8.3.3](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x17-V8-Authorization.md) (originating subject's permissions), [13.2.1–13.2.2](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x22-V13-Configuration.md) (authenticated, least-privilege backend communication) and [16.2.1, 16.3.2, 16.4.2](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x25-V16-Security-Logging-and-Error-Handling.md) (log metadata, logged authorization decisions, protected logs) correspond to the three controls. The export service, boundaries, traces and threat record are original constructed examples.
