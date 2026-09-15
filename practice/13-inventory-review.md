# Review the association before trusting the inventory

Compare with your saved [worksheet](13-inventory-worksheet.md). Hints and supplied reasoning count as assistance.

## Explain the measured mismatch

Maven describes the application's resolved dependency graph under the configured scopes. The helper is another project, copied into the image later. Its absence from the app BOM is expected; presenting that document as the whole image inventory is the error. The image also adds Alpine and Java runtime components outside that application graph.

Both copied application JARs are identical. The helper's embedded Maven version and JAR hash change, and the resulting image IDs differ. Both images pass a real HTTP status check and a separate helper invocation. Those four smoke observations say nothing about whether the web endpoint uses the helper or whether the inventory is complete.

The guided contract checks 21 observations. Substituting the Maven BOM for the final component list fails four checks: helper and musl for each image. The positive Spring component checks remain true. Syft's 112 native packages and 1,336 CycloneDX components use different denominators; the latter includes 1,223 file components and one operating-system component. Do not report a coverage percentage from these counts.

## Progressive hints

1. A tag is a mutable label. Compare the independently supplied selected image ID with the record, while preserving the exercise's requested-tag check.
2. Parse the native scan and inspect `source.metadata.imageID`. Relabeling an old record must not relabel the scan's subject. Reject missing or malformed required evidence.
3. Compute SHA-256 from each supplied report's actual bytes and compare it with the corresponding recorded hash. Checking one report does not bind the other.

The executed private reference passes all nine contract tests. The public starter passes only matching A, matching B and rejection of an unrelated tag; six rejection cases fail. With the source-ID comparison removed from the private solution, the relabeled-record case fails, even though tag, record ID and report hashes agree. That is the separate check the negative control tests.

No complete solution file is published here. Your extra variation is outside these nine reference tests. Explain its result rather than assuming the supplied checks exhaust possible malformed input or all SBOM formats.

## What remains unproved

The pairing record is unsigned. If someone can replace all evidence consistently, matching hashes do not establish an honest producer or trustworthy collection process. The caller must select the image independently and use that immutable identity for subsequent work; resolving a mutable tag again can select another image.

The exercise binds both report files to the recorded association; it does not fully validate the exchange-format schema, reconstruct every dependency, validate every context field in the sidecar, or prove that two arbitrary reports have identical semantics. `record.py` assumes reports were produced together by the lab runner; a trusted production pipeline requires its own controls.

A package URL is also an identification claim. This fixture's comparator accepts a known Maven JAR with and without `?type=jar`; it does not treat all qualifiers or different inferred namespaces as equivalent. Follow the location evidence and inspect the archive when tools disagree.

A useful handover names the image ID, platform, generator/version, merged-filesystem scope, reports and hashes, and exclusions such as later mounts or downloads. Keep findings about identity, component presence and vulnerability exposure separate. No vulnerability scan or professional assessment was performed here.
