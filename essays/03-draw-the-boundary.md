# Draw the boundary before choosing the control

Job 81 belongs to Alice. Its record says to export document C from Cedar. Yet the archive contains document B from Birch, and every permission check returned allow.

Here is how that can happen in a constructed implementation of our export service:

| Step | Facts used | Result |
|---|---|---|
| API accepts Alice's request | Alice may export C | Creates job 81 for Alice and C |
| Worker receives a message | `{job: 81, requester: bob, document: B}` | Checks Bob's permission for B; generates B into archive 81 |
| Alice downloads job 81 | Stored job says Alice owns it and may export C | Releases archive 81, containing B |

The worker evaluated a real permission. The download endpoint evaluated another real permission. Each check was correct about the facts in front of it; they disagreed because different parties had written those facts.

This essay follows one question through the export service from essay 2: for each decision, which facts does it read, and who is able to write them? A trust boundary sits wherever that answer changes. A control is worth choosing only when it constrains a writer that the decision cannot afford to trust.

## Who wrote the fact the worker read?

Assume a compromised retry tool can publish to the export queue but cannot change the API's stored job record. That is the attacker's starting capability.

```text
API ──writes──> job record: 81 / Alice / C
API ──publishes──> export queue ──delivers──> worker
Retry tool ──can also publish──> export queue

Worker reads message fields ──> checks Bob / B
Worker writes B ──> archive for job 81
Download API reads job record ──> checks Alice / C
```

The drawing leaves out the broker, databases and network. It keeps what explains the disclosure: two decisions reading two sources. Writing those sources down makes the flaw visible without the attack story:

| Decision | Facts it reads | Who can write those facts |
|---|---|---|
| Generate the archive | Requester and document in the message | Any identity allowed to publish to the queue |
| Release the archive | Owner and document in the job record | The API, and whoever administers that table |

Whoever can publish chooses whose permission the worker checks. The worker is entitled to use its own service identity to read storage, because it performs work on a user's behalf. The error is taking *which user* from a writer that was never trusted to decide it.

A **trust boundary** marks a change in the authority or assurances a component may rely on. Placement is a weak guide. Two processes on one host can hold different credentials, and services on separate hosts can share an administrator. Ask who can read, write, execute and administer each thing a decision uses.

## Move the fact, then constrain its writers

Make the queue message carry only a job identifier. The worker loads the requester and exact document reference from the job record, checks that requester's current permission, and associates the result with the same record. Identity or document fields in a message are rejected or ignored; they never override the record.

```text
Queue ──job ID──> worker
Job record ──requester + document──> worker
Permission records ──current decision inputs──> worker
Worker ──result for that job──> archive store
```

The retry tool can still ask the worker to process job 81. It can no longer make job 81 mean Bob and B, and Alice's export of C still completes.

Generation now depends on the job record, so the record's writers matter. The API may create the requester, owner and document reference. The worker may update execution status and the result reference but must not rewrite that authorization context. Separate tables, constrained database operations or a service interface can enforce the split.

Listing writers also changes how other plausible controls look. Restricting queue publishers reduces who can submit work, but a second legitimate producer would reopen the flaw if the worker still trusted message fields. Enabling TLS stops network parties from altering messages in transit. The retry tool was an authorized publisher, so its message arrives intact and wrong.

The list exposes a writer nobody has decided about: a database administrator can change the job record. Do not assume that a metadata administrator is entitled to every document. Whether that is an accepted trust assumption or needs separation of duties belongs to a product security owner, and the drawing should show it as open until they decide.

Evidence follows the same split. Infrastructure tests exercise the actual service accounts, including attempts to update protected fields. Application tests deliver a forged message for job 81, check that no Birch bytes become associated with Alice's job, and check that a valid message still produces C. Proving that one account cannot publish leaves the worker's interpretation untested.

## Use STRIDE to find the other decisions

Tracing one failure explains it; it does not show what else the design gets wrong. STRIDE supplies six prompts. Spoofing: can someone act as Alice? Tampering: can someone change a fact a decision reads? Repudiation: what evidence survives a dispute? Information disclosure: can document bytes reach someone without permission? Denial of service: can one tenant's work block others? Elevation of privilege: can a caller obtain an effect reserved for a more privileged identity?

Apply the prompts to every decision and store in the drawing, including ones not yet under suspicion. Write the actor, capability and consequence before choosing a label. The forged message is tampering that causes disclosure, and that mechanism matters more than the category. In this service, the disclosure prompt applied to the download, and the repudiation prompt applied to the logs, each find a decision whose facts have an unexpected writer.

## A link moves the download decision

Suppose the archive store issues a signed link valid for 24 hours. Alice pastes it into a support ticket, then an administrator revokes her export permission. The store has no connection to the application's permission records, so Alice and anyone reading the ticket can both fetch the archive.

A download decision still happens, but its input has changed. The store checks that the request carries a valid signature. Possession of the URL is now the fact, and anyone who receives the URL can supply it. A shorter lifetime limits how long that stays true; it neither identifies the holder nor checks current permission.

| Design | What the download decision reads | Tradeoff |
|---|---|---|
| API checks identity, job ownership and current permission, then streams the archive | Authenticated user and current records, at every download | API carries file traffic |
| API checks permission, then issues a 60-second link | Current records at issuance; afterwards only possession of the URL | Forwarding and revocation exposure while the link is valid |

For S3, expiry is checked when a request starts, so a download that begins just before expiry can finish afterwards. A 60-second link does not promise that disclosure stops after 60 seconds. [S3 presigned URL behaviour](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)

Exports in this service are infrequent, and essay 2's policy requires current permission at download, so choose streaming. No storage URL reaches the downloader. Copies already downloaded remain outside the control, and revocation during an active transfer is still unresolved. A product that prefers the short link should change its access promise explicitly rather than inherit the old one.

## Evidence has writers too

Cedar reports that one of its documents left through an export. Alice denies requesting it. The only log line says `export completed job=81`, and the worker's logging credential can delete earlier entries.

That line cannot distinguish Alice requesting the export, someone using her credentials, or a service manufacturing the job. The component whose actions the log should account for can also erase it. Missing detail is not evidence that nothing was exploited.

Send essay 2's authorization events to a destination where application identities can append but cannot modify or delete. Record requester, job, decision stage, outcome, reason, policy revision and time, and keep credentials and document contents out. Verify representative events and the forbidden update and delete operations. Two limits stay beside the control: append-only storage cannot force a compromised application to write truthful events, and an event attributing a request to Alice's account does not prove Alice was at the keyboard.

## Leave a record someone can act on

A review ends with decisions and the evidence still missing. For this fictional service, the record might begin:

| Finding | Proposed decision and owner | Evidence still needed |
|---|---|---|
| Message fields decide whose permission is checked | Backend lead: job ID only; constrain job-record writers | Forged-message test, valid export, service-account permission tests |
| Signed link replaces the download check with possession | API lead: stream after current authorization | Revocation and wrong-owner download tests |
| Worker can erase the events that account for it | Platform lead: append-only event destination | Event-content checks; forbidden update and delete tests |
| Administrator can rewrite job context | Product security owner: decide before release | Actual administrator access; compensating-control review |
| Bulk exports may starve other tenants (not investigated here) | Service owner: set workload limits before release | Load assumptions; isolation test plan |

None of these controls has been built. Possible responses are to mitigate, remove the feature, transfer an obligation or accept a risk. Acceptance needs a reason and an accountable owner; an open row is not an accepted risk. STRIDE does not rank business impact or show that the list is complete. Redraw whenever a new producer, credential, flow or policy changes who can write a fact that a decision reads.

<!--mission-->

## Practice: review a design that chose its controls first

A colleague has written a design note for weekly scheduled exports. It describes the components and proposes controls, but has no boundary drawing. The [worksheet](../practice/03-boundary-worksheet.md) contains the note and the product policy it must satisfy.

Draw the boundaries the note implies. For each decision the design makes, list the facts it reads and who can write them. Give each proposed control a verdict with reasons: it fits the decision it claims to protect, it helps but is insufficient, or it protects a different crossing. "No change needed" is a valid verdict wherever the note is right. For each finding, state the actor and starting capability, a response and owner, a case that must be denied and a legitimate run that must still succeed. Finish with a design change that would require the review to be repeated.

Save your attempt before opening the [review notes](../practice/03-boundary-review.md), which contain the hints. Completion means another engineer can trace every verdict to a sentence in the note, a fact and its writer. It is a design review; no scheduler runs.

Source note: inspected on 13 September 2026 and rechecked on 16 September 2026. STRIDE terminology follows Microsoft's [threat categories](https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats); review structure and risk responses draw on OWASP's [Threat Modeling Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Threat_Modeling_Cheat_Sheet.html). ASVS 5.0.0 [V8](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x17-V8-Authorization.md), [V13](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x22-V13-Configuration.md) and [V16](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x25-V16-Security-Logging-and-Error-Handling.md) provide related authorization, service-communication and logging requirements. The service, attack trace, design note and decisions are original constructed examples; the diagrams show selected flows rather than a complete deployment.
