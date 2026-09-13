# Essay 2 — review after attempting the worksheet

There is no single batch policy required by the exercise. The expected evidence is consistency between the chosen rule, its effects and its traces.

Under an all-or-nothing policy, loss of C2 permission before generation prevents the batch from producing a downloadable archive, even if C1 remains permitted. If C2 is revoked after the complete archive was generated, denying a later download protects the selected current-permission rule. Deleting or retaining a protected internal archive is a separate lifecycle decision; neither may make it publicly accessible.

Under a partial-result policy, generation may include C1 alone. Record which items were actually included and make the partial result explicit without disclosing another tenant's protected metadata. If the archive was already generated with C2 inside it, changing its status or hiding C2's filename does not remove C2's bytes. Deny that download or produce a newly authorized archive; explain its identity and contents.

A correct allowed case still returns the intended documents to the intended requester. A permission-service outage must not be treated as permission to use the worker's broader storage access. A different user possessing the job identifier fails the example's job-owner rule even if they can authenticate.

Look for these incomplete answers:

- “Authenticated” without a resource/action permission.
- “Check at request time” without a rule for subsequent changes.
- “Deny” measured solely by HTTP status despite later disclosure.
- “Partial success” without specifying the actual contents and client-visible meaning.
- “Recheck” without acknowledging the check/use ordering problem.
- “Log it” offered as a replacement for preventing the disclosure.

The worksheet is ready for discussion when its outcomes can be derived from the written policy. It is not proof of implementation correctness. Retain uncertainty about concurrent revocation or active transfers if those semantics have not yet been designed.
