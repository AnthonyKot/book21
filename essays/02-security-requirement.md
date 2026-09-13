# A security requirement must survive a hostile input

“A customer can export their documents” sounds ready for implementation. It names a user, an operation and some data. An engineer can create an endpoint, schedule a worker and write a test that downloads a file.

The sentence leaves its most consequential word undefined: *their*. Does it mean documents the customer created, documents belonging to their organization, or documents currently shared with them? Does access at the time of the request remain sufficient when the worker runs later? Can the resulting link be forwarded?

These are implementation decisions whether you discuss them or not. Leaving them unwritten lets a repository query, queue message or storage setting decide the product's security policy by accident.

This essay continues a constructed Java document-export service. The task is to turn its feature into a claim that a tester can try to disprove. There is no exploit code yet. The important artifact is a requirement precise enough to tell you when working software has done the wrong thing.

## Name the effect you must control

Start with a small fictional world. Alice belongs to tenant Cedar. Bob belongs to tenant Birch. Document C belongs to Cedar and document B belongs to Birch. For this example, tenant membership alone permits viewing document metadata; exporting contents additionally requires an export permission for that document. There is no cross-tenant sharing.

Alice has export permission for C. Bob has export permission for B. A worker can read both documents from storage because it processes jobs for both tenants.

Now consider four request cases:

| Request | Intended result | Reason |
|---|---|---|
| Alice exports C | Allow | Her tenant and document permission both match |
| Alice exports B | Deny | B belongs to another tenant |
| A Cedar member without export permission exports C | Deny | Membership does not grant this action |
| Bob exports B | Allow | The legitimate Birch path must still work |

This is already stronger than “authenticated users can export.” Authentication identifies the user. The table describes what that user may do to a particular resource. It also prevents an easy but useless repair: disabling exports for everyone.

Be precise about “deny.” An HTTP error alone is insufficient if the handler queued a job first. In this example, denial means the attempt produces no export job authorized to run, no downloadable document bytes and no successful-export notification. An internal record of the denied attempt is permitted.

The resource also changes as the feature runs. Initially you protect a source document. Later there is a generated archive, a job-status response and a download endpoint. A rule covering only the source document leaves its derived copies unexplained.

## Write a claim that has a counterexample

A useful first requirement is:

> For every accepted export request, the authenticated requester must belong to the document's tenant and hold export permission for that document when the request is authorized.

“For every” matters. One valid export demonstrates that the feature works in one case. One accepted request violating the condition disproves this requirement.

The requirement has five parts you can point to:

| Part | In this example |
|---|---|
| Principal | The authenticated requester |
| Resource | The requested document |
| Action | Request an export |
| Condition | Matching tenant and document export permission |
| Decision point | Request authorization |

A principal is the identity whose authority is being evaluated. Here it is Alice, even though a worker eventually performs storage operations. The worker's ability to read B is an implementation capability; it does not give Alice permission to export B.

The client's request supplies a document identifier. It cannot establish its own authority by also supplying `tenant=Birch` or `requester=Bob`. Those values would be claims to verify, not evidence that the requester belongs to Birch or is Bob. Your requirement should name the trusted identity and permission records used by the service.

A counterexample can now be very small: authenticate as Alice, request B, observe acceptance. No unusual encoding or malicious-looking string is needed. A syntactically valid identifier can be hostile because of the relationship it asks the application to violate.

That is why “validate the input” is unfinished advice here. B is a valid document identifier. The missing check concerns the permitted effect of using it.

## A queue turns permission into a timing question

The first requirement still leaves a gap. Follow this event sequence, with labels representing order rather than elapsed seconds:

| Event | State |
|---|---|
| T0 | Alice has permission to export C |
| T1 | The API checks permission and accepts her request |
| T2 | An administrator revokes Alice's export permission |
| T3 | The worker begins the queued job |
| T4 | Alice asks to download the result |

Should T3 proceed? Should T4 disclose the file? The request requirement says nothing about either. Both a worker that proceeds and one that refuses could satisfy its exact wording.

For this teaching example, choose the following policy: revocation applies to work that has not begun and to downloads that have not been authorized. A queued job does not preserve the requester's former permission. A completed archive is not an independent grant of access.

Now add two requirements:

- Before beginning generation, the worker must validate the original requester's current tenant membership and export permission for the job's document. If that cannot be established, the job must not generate an export.
- Before authorizing a download, the service must validate the downloader's identity, ownership of the export job, current tenant membership and current export permission for its source document. Otherwise it must release no document bytes.

Job ownership is an additional choice for this example: Bob cannot use Alice's export job even if some later policy gives him access to its source. He would request his own export. A product supporting collaborative export jobs would need a different, explicit rule.

These requirements make T3 and T4 decidable. With revocation completed at T2 and no later grant, both must deny. Merely recording that the request was once authorized cannot satisfy the selected policy.

The policy also has a boundary. It does not promise to retract a file Alice already downloaded. It does not yet require a transfer to stop halfway through when access changes. If immediate interruption is a product requirement, write it separately and investigate whether the architecture can enforce it. Do not quietly claim it from a check made before streaming.

There is a similar issue between a worker's check and its first document read. For a revocation concurrent with that transition, the design must define which event takes effect first and how that ordering is enforced. Sequential examples expose the requirement; they do not prove a race-free implementation. Record that open design question rather than hiding it behind “check again.”

## Examine the attractive repair

Suppose a proposed implementation validates Alice at request time, stores `authorized=true` in the job and lets the worker trust that flag. It is simple, avoids another permission lookup and preserves successful exports.

Compare it with the requirements. It handles Alice requesting B if the initial check is correct. It fails the T0–T3 revocation sequence because a historical boolean cannot establish current permission. The repair answers a narrower question than the product has asked.

A revised design can retain the requester's trusted identifier, the exact source-document reference and the job owner in a server-created job record. It evaluates permission again at the chosen later boundaries. The records must resist modification by an untrusted caller; otherwise the extra checks could be performed for an attacker-selected identity.

This is a design direction, not a complete implementation. You still need to decide how permission failures differ from temporary lookup failures. In this example, a permission denial cancels the job, while an unavailable permission service leaves it blocked for possible retry. Neither condition allows generation by falling back to the worker's storage access.

Keep the download rule in view. If the service hands Alice a storage link that anyone possessing it can use until expiry, that link cannot by itself enforce a fresh user-permission check on each later download. Either the delivery design must support the chosen rule or the product must explicitly choose a different access promise. Encryption of the stored file would not settle this authorization question.

## Make the decision visible after the request

For the example, require an internal event for each export authorization decision. It records the requester identifier, job identifier when one exists, stage, outcome, reason category and the policy revision used. Keep document contents and credentials out of that record. Job records supply the source reference for investigation.

That event helps answer whether an attempted download was denied and at which stage a job stopped. It does not undo disclosure. A success-shaped log line cannot compensate for a forbidden file reaching the caller.

The public response deserves its own requirement. In this example, a denied caller receives no other tenant's document title or storage path in the error or job status. Observing a denial must therefore include checking its body, not just its status. This rule addresses those explicit fields; it does not establish that timing or every other side channel reveals nothing.

Standards help you inspect the completeness of this record. ASVS 5.0.0 distinguishes documented authorization rules, data-specific permissions, trusted enforcement and the originating user's authority. Its relevant requirements include 8.1.1, 8.2.2, 8.3.1 and 8.3.3. They are useful questions to apply to the design, not substitutes for deciding the export policy. [ASVS V8](https://github.com/OWASP/ASVS/blob/v5.0.0/5.0/en/0x17-V8-Authorization.md)

NIST SSDF 1.1 calls for maintaining software security requirements in PO.1.2 and preserving requirements, risks and design decisions in PW.1.2. The practical artifact here is a versioned decision record connected to tests, including the unresolved concurrency question. [SSDF 1.1](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-218.pdf)

## Give the next reviewer an observable claim

Your evidence table should name the attempt and the relevant effects. For the revoked queued job, inspect its final state, whether generation occurred, whether any result became downloadable and the authorization event. For a permitted job, verify the intended document arrives and the restrictions do not suppress legitimate use.

Later, an AI assistant may help generate those tests. Give it the policy and ask it to propose counterexamples before accepting its implementation. If it assumes that a completed export remains downloadable after revocation, the disagreement is visible in the contract. Without that contract, two plausible implementations can simply embody different policies.

An invariant is a condition intended to hold throughout the states or transitions you specify. For this example, the central confidentiality claim is that every authorized download belongs to the identified requester and satisfies the current source-document permissions at the download decision. A counterexample is a trace that reaches a disclosure without those conditions.

You have not proved that the system is secure by writing this sentence. You have made a security failure recognizable. That gives code review, test design and architecture review a shared target.

<!--mission-->

## Practice: a permission changes while the archive is waiting

Use the [worksheet](../practice/02-export-contract.md). This is a design exercise; no software execution is claimed or required.

Change the feature: one export job may now contain several documents. Alice requests C1 and C2, both initially permitted. Before generation begins, her permission for C2 is revoked. Choose whether the batch fails as a whole or produces an explicitly partial result. Both are possible product policies. Silently producing an apparently complete archive containing only C1 is not an adequate specification.

Write the principal, resources, effects and decision points. State your batch rule, what happens when the permission service is unavailable, and what a later revocation means for downloading a completed archive. Include an allowed case, a foreign-tenant document, a revocation before generation and a revocation after generation but before download.

Try it before opening the [review notes](../practice/02-export-review.md).

If stuck, first replace “authorized” with the exact identity and permission you mean. Next mark every point that can create or disclose a copy. Finally, replay the same request with one permission changed between two points.

Completion means another engineer can determine the expected outcome of each trace without guessing your policy. It also means the record includes a legitimate successful export and a limit you have not resolved, such as revocation during an active transfer. Keep the counterexample that most changed your requirement; it is evidence of what your review discovered.

Source note: primary sources inspected on 13 September 2026. The service, event traces, batch policy options and worksheet are original constructed examples. OWASP's [Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html), particularly its permission-validation and testing guidance, provides further reading. This chapter defines intended behaviour; it does not verify a running service.
