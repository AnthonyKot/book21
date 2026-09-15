# Release 2 candidate — change summary

*Provenance: the release 2 code, its tests and this summary were produced with an AI coding assistant while the book was being written, as a teaching fixture. They are not a captured production patch, an observed coding-model failure or a benchmark result. Treat every statement below as a claim to check.*

## What changed

**Downloads now enforce archive state.** `DownloadService.download` calls `DocumentStore.requireDownload` before serving content, so a document that has been archived is no longer downloadable while its summary stays visible in preview. Previously the download reused the preview cache and served full content regardless of archive state.

**New export links.** Support asked for a way to hand a document's content to a colleague without re-sending the file. A tenant user calls `POST /api/documents/{id}/export`, which authorizes the document against its current state (own tenant, not archived) and returns an export identifier. `GET /api/exports/{exportId}` returns the full content. Export identifiers are random UUIDs, held in memory, and every fetch checks that the caller's tenant created the export, so a leaked identifier gives another tenant nothing.

**Test support.** `ReviewHttp` gained a CSRF-aware `post` helper and export helpers.

## Tests

`ExportCandidateTest` (7 cases, all green with the existing 20):

- active download returns full content
- archived download is refused after a warm preview
- archived summary remains visible
- export round trip returns full content
- exporting an archived document is refused
- another tenant cannot fetch the export
- unknown export identifier is refused

All 27 tests pass with `mvn test`.

## Security notes

Both new operations reuse the existing tenant mapping and the existing current-state authorization query. No caller-supplied tenant is read. Export state is not persisted; a restart clears it, which is acceptable for the support use case.
