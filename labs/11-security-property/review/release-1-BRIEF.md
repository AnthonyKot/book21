# Release 1 review brief: tenant-scoped lookup

Guided review for essay 11. Constructed teaching fixture, not a captured production patch or a measured coding-model result. `release-1-candidate.diff` compares an unscoped lookup with the repository lookup implemented here; the pre-patch line is a prepared comparison, not an executed baseline. Run the candidate with `lab.partition=false`. The source also contains the guided repair, selected by `lab.partition=true` (the default).

Candidate claim to evaluate: "The repository now binds the caller's tenant, so the preview cannot disclose another tenant's document."

Property: every preview response contains only the authenticated tenant's summary, whether the application cache is empty or populated. The tenant comes from the authenticated principal, not a query parameter. Repeated previews by the owner should still use the cache.

For an unassisted first pass, read the diff, then `DocumentController`, `Tenants`, `PreviewService` and `DocumentStore`. Name every branch that can return summary data and the conditions under which each reaches the repository. Save that pass before opening the tests or the essay walkthrough.
