# Draw the boundary before choosing the control

Job 81 belongs to Alice. Its record says to export document C from Cedar. Yet the archive contains document B from Birch, and every permission check returned allow.

Here is how that can happen in a constructed implementation of our export service:

| Step | Facts used | Result |
|---|---|---|
| API accepts Alice's request | Alice may export C | Creates job 81 for Alice and C |
| Worker receives a message | `{job: 81, requester: bob, document: B}` | Checks Bob's permission for B; generates B into archive 81 |
| Alice downloads job 81 | Stored job says Alice owns it and may export C | Releases archive 81, containing B |

The worker evaluated a real permission. The download endpoint evaluated another real permission. They disagreed about what the job meant.

Essay 2 already required a server-created job record protected against untrusted modification. This implementation violates that requirement by giving a queue message authority to replace parts of the record. Our next task is to examine which components and credentials could make that replacement possible, then choose controls and evidence that address it.

That is a useful entry into threat modeling: follow a specific failure through a design until you can explain both its opportunity and its consequence.

## Two accounts of the same job

Assume a compromised retry tool can publish to the export queue. It cannot change the API's stored job record. This is the attacker's starting capability, not a claim that sharing a broker automatically grants access to every queue.

The relevant paths in the flawed design are:

```text
API ──writes──> job record: 81 / Alice / C
API ──publishes──> export queue ──delivers──> worker
Retry tool ──can also publish──> export queue

Worker uses message fields ──> checks Bob / B
Worker writes B ──> archive associated with job 81
Download API uses job record ──> checks Alice / C
```

This is a deliberately narrow drawing. It exposes the two sources of authority that explain the disclosure. We will add permission lookups and the repaired delivery path as we choose them. A diagram should make its omissions visible; boxes for every infrastructure service would not establish who can write the message.

A **trust boundary** marks a change in the authority or assurances a component may rely on. At the browser boundary, authenticated identity does not make the requested document identifier authoritative. At the queue boundary, permission to publish does not necessarily include permission to redefine a job. At storage, the worker's broad read credential does not establish what Alice may receive.

Ask who can read, write, execute and administer each part. Two components on the same machine can have different authority. Two services on separate machines may be under the same administrator. Placement helps describe the system; credentials and enforced permissions explain what it permits.

Keep two identities visible while following the work:

| Operation | Identity performing the operation | User whose product permission matters | Source of that user identity |
|---|---|---|---|
| Request an export | Authenticated Alice | Alice | API authentication result |
| Publish a notification | API service, or the compromised retry tool | Not established by publishing alone | Message fields require a trusted origin or comparison with a protected record |
| Generate the archive | Worker service | Original requester | In the repair: protected job record |
| Read source bytes from storage | Worker's storage identity | Original requester still governs generation | Worker's application-level permission check |
| Download through the API | Authenticated downloader | Downloader, who must also own the job | API authentication plus protected job and permission records |

The worker does not become Alice. It performs an operation on her behalf. Likewise, a queue does not inherently destroy identity provenance. The flaw is accepting a publisher's fields as authoritative when that publisher is not trusted to choose them.

## Give each fact one authoritative source

For this example, make the queue message carry only a job identifier. The worker loads the originating requester and exact document reference from the protected record, checks that requester's current permission, and associates the resulting archive with that same record.

```text
Queue ──job ID──> worker
Job record ──requester + document──> worker
Permission records ──current decision inputs──> worker
Document store ──authorized source bytes──> worker
Worker ──result for that job──> archive store
```

Now the retry tool can ask the worker to inspect job 81, but cannot make that job refer to Bob or B. Unknown jobs produce no export. Extra identity or document fields must be rejected or ignored; they must never override the record. Alice's legitimate export of C should still complete.

Define the write permissions more precisely than “the table is protected.” The API may create the job's requester, owner and document reference. The worker may read those fields and update execution status and the result reference, but must not rewrite the authorization context. Those permissions might be enforced through separate tables, constrained database operations or a service interface. The mechanism remains to be implemented and tested.

Restrict queue publishing to the intended producers as well. That reduces unwanted submissions, but it does not replace the worker's handling of job identity. A future second producer should not require trusting a second copy of the requester and document.

“Enable TLS” addresses another part of the path. Correctly configured transport protection helps prevent interception and alteration in transit. It cannot make false content from an authorized publisher true.

There are now two distinct evidence tasks. Infrastructure tests should exercise permitted and forbidden operations with the actual service accounts, including attempts to update protected fields. Application tests should deliver a forged message for job 81 and establish that no Birch bytes become associated with Alice's job. Include a valid message that still produces C. Testing only that one account cannot publish leaves the worker's interpretation untested.

This also exposes a limit: a database administrator may be able to change the protected record. Do not silently assume that a metadata administrator is already entitled to every document. Record whether this is an accepted trust assumption or requires further separation of duties. The diagram has located a decision; it has not made it for the product owner.

## Use STRIDE to widen the investigation

We found one failure by tracing two inconsistent accounts of a job. STRIDE supplies six prompts for looking beyond it:

| Category | Question for this export service |
|---|---|
| Spoofing | Can a caller act under Alice's identity? |
| Tampering | Can someone replace the requester, source or result of a job? |
| Repudiation | What evidence remains if someone disputes an export? |
| Information disclosure | Can document bytes reach someone without permission? |
| Denial of service | Can one tenant's exports prevent others from completing? |
| Elevation of privilege | Can a caller obtain an effect available only to a more privileged identity? |

Apply these prompts to processes and stores as well as crossings. A deletable audit record matters even if the initial drawing omitted the log service. Write a scenario with an actor, capability and consequence before assigning its category. A category name by itself gives an engineer little to investigate.

Our forged-message scenario involves tampering that uses the worker's authority to cause an unauthorized disclosure. Choosing its single best label matters less than preserving that mechanism. Two further questions change the proposed design.

## Does the download still enforce the contract?

Suppose the archive store issues a signed link valid for 24 hours. Alice pastes it into a support ticket. An administrator then revokes her export permission. Assume the link remains valid and storage has no connection to the application's permission records. Both Alice and a ticket reader holding the link can fetch the archive.

The access credential has changed: possession of the URL now permits retrieval. The store need not identify the person holding it. Reducing its lifetime helps limit exposure, but does not establish that the downloader is Alice or still has permission.

Compare two delivery designs:

| Design | Access promise | Tradeoff |
|---|---|---|
| API checks identity, job ownership and current permission, then streams the archive | Each download is authorized under essay 2's selected policy | API carries file traffic |
| API checks permission, then issues a 60-second bearer link | Permission checked at issuance; a holder can subsequently use the link while valid | Forwarding and revocation exposure remain during validity |

For S3 specifically, expiry is checked when the request starts; a transfer begun before expiry can continue afterwards. A 60-second link is therefore not a promise that disclosure stops after 60 seconds. [S3 presigned URL behaviour](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)

Choose API streaming for our constructed service: exports are infrequent and the current-permission requirement is retained. The repaired delivery path is:

```text
Downloader ──authenticated request──> API
Job + permission records ──decision inputs──> API
Archive store ──bytes after allow──> API ──bytes──> downloader
```

No bearer storage URL is released to the downloader. Already-downloaded copies remain outside this control, and revocation during an active transfer remains unresolved, as in essay 2. If the product chooses the short-link alternative, update its access promise explicitly.

## What survives a disputed export?

Now consider a report that Cedar's document left through an export. Alice denies requesting it. A log line says only `export completed job=81`, and the worker's logging credential can delete earlier entries.

That evidence cannot distinguish several explanations: Alice requested the export, someone used her credentials, or a service manufactured the job. The absence of detail does not establish that no vulnerability was exploited.

The authorization events from essay 2 need a protected destination. Record requester, job, decision stage, outcome, reason, policy revision and time; retain the protected job's source reference. Keep credentials and document contents out of the event. Application identities should append events without being able to modify or delete earlier ones.

Verify both the content of representative events and the restrictions on changing them. Append-only access still cannot force a compromised application to emit truthful, complete events. A record attributing a request to Alice's account also does not prove Alice was at the keyboard. These limits belong beside the proposed control.

## Leave a record someone can act on

A useful review ends with decisions and missing evidence. For this fictional service, the record might begin:

| Finding | Decision and owner | Evidence still needed |
|---|---|---|
| Message can redefine a job | Backend lead: use protected job context; constrain writers | Forged-message test, valid export, account-permission tests |
| Bearer link bypasses download policy | API lead: stream after current authorization | Revocation and wrong-owner download tests |
| Export decisions can be erased | Platform lead: protected event destination | Event-content checks and forbidden update/delete tests |
| Administrator can alter job context | Product security owner: resolve trust assumption before release | Actual admin access and compensating-control review |
| Bulk exports can starve other tenants | Service owner: decide workload limits before release | Load assumptions and an isolation test plan |

These are proposed responsibilities, not evidence that anyone has accepted them. None of these controls has been executed in this design essay.

Mitigation, removing the risky feature, transferring an obligation and accepting a risk are possible responses. Acceptance needs a reason and an accountable owner. An unresolved release decision is not an accepted risk merely because it appears in a table.

Update the model when credentials, producers, flows or policy change. Periodic review can also catch changes that escaped that process. STRIDE does not rank business impact or establish completeness; the value is in the specific decisions it helps expose.

<!--mission-->

## Practice: the export nobody clicked

Use the [worksheet](../practice/03-boundary-worksheet.md). An administrator schedules a weekly folder export. A scheduler creates Monday's job, and an external email provider sends a link to an address the administrator enters. Folder contents and permissions can change between runs.

For this exercise, choose a schedule that acts on behalf of its creator and requires that person's current permissions. Keep essay 2's job-owner download rule. These are selected product policies; a tenant-owned automation account would require a different explicit contract.

Draw the path through scheduling, generation and download. Distinguish service identity from originating user at each relevant hop. Choose three threat scenarios, then state the response, owner, proposed control, verification case and remaining limit. Include one legitimate weekly run and one where the creator leaves the tenant on Sunday. Identify an unresolved decision if you find one; there is no quota of risks you must accept.

Try it before opening the [review notes](../practice/03-boundary-review.md). If stuck, identify who supplies Monday's requester. Next ask who can change that record. Finally ask whether a permitted email address also establishes permission to download.

Completion means another engineer can follow a threat to its proposed control and evidence, without guessing which facts are trusted. This is design practice, not proof of a running scheduler.

Source note: inspected on 13 September 2026. STRIDE terminology follows Microsoft's [threat categories](https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats); review structure and risk responses draw on OWASP's [Threat Modeling Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Threat_Modeling_Cheat_Sheet.html). ASVS 5.0.0 [V8](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x17-V8-Authorization.md), [V13](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x22-V13-Configuration.md) and [V16](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x25-V16-Security-Logging-and-Error-Handling.md) provide related authorization, service-communication and logging requirements. The service, attack trace and decisions are original constructed examples; the diagrams show selected flows rather than a complete deployment.
