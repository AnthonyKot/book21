# Review brief: tenant-scoped lookup

Constructed authoring fixture, not a captured production patch or a measured coding-model result. `candidate.diff` compares an unscoped lookup with the repository lookup implemented here. The pre-patch text is a prepared comparison; it is not an executed baseline. The candidate behavior is runnable with `lab.partition=false`. The normal source also contains the guided repair selected by `lab.partition=true`.

Candidate claim to evaluate: “The repository now binds the caller’s tenant, so the preview cannot disclose another tenant’s document.”

Property: every preview response must contain only the authenticated tenant’s summary, whether the application cache is empty or populated. Tenant selection comes from the authenticated principal, not a query parameter. Ordinary repeated previews should still use the cache.

Read the diff, then `DocumentController`, `Tenants`, `PreviewService`, `DocumentStore` and `SecurityConfig`. Name every branch that can return summary data. Before opening the tests or essay, write the conditions under which each branch reaches the repository. Save your observations as a first review pass.

The archive/download exercise has a different rule: own-tenant archived summaries remain visible, but full-content downloads must be refused when the download authorization check observes archived state. A preview cache hit is not a download authorization decision. The exercise does not require stopping a response already authorized before a concurrent archive.
