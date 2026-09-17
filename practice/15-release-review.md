# Release trust review

Open this after saving your [worksheet](15-release-worksheet.md) verifier, tests and note. Everything below counts as assistance once you have read it. If you are stuck partway, read only the next hint in section 2 and record that you did.

## 1. Judge the attempt before comparing designs

No single verifier is correct. A sound one shows the following.

**Expectations come from the consumer.** The builder key, builder id, repository, workflow, ref and event are configured on the consumer's side. A verifier that reads the builder id from the statement and then "checks" it against itself has checked nothing.

**Signature before content.** Every field in the statement is untrusted until the signature verifies with the configured key. A verifier that inspects fields first may still be correct, but only if nothing is accepted before the signature check passes.

**Identity is the digest.** The artifact is identified by its SHA-256 matching a subject digest, not by its file name. A renamed delivery of the same bytes is the same artifact.

**Authority, not just authenticity.** A valid signature from the trusted platform shows the platform made the statement. It does not show the build was a release. The same platform signs builds of pull requests, of other branches and of forks; the ref, event, repository and workflow path are what distinguish a release from a preview.

**Over-rejection is a defect.** An earlier release from `main` is still a genuine release. Provenance does not say whether a commit is the newest, and a verifier that demands the newest commit is enforcing something provenance cannot tell it.

**Unrecognised parameters are a decision.** SLSA's guidance is to reject unrecognised external parameters. A verifier may accept them if it says so; it should not ignore them silently.

**The constructed rejection is real.** The reader's own negative test produces a delivery the runner or an edit actually creates, and the reason the verifier prints is the reason the delivery is bad.

**The limits note names a residual.** Something specific remains unproven after acceptance: what the source commit contains, whether the merge was reviewed, whether the platform itself was compromised, whether the build's dependencies were what the script expected.

**A handover someone could act on.** It states what is verified, what is rejected, what is still trusted, and what changes if the producer moves.

## 2. Progressive hints, if you are stuck

1. Write the trust facts from the worksheet as a table with two columns, "expected" and "where the verifier reads it from". Every row's second column must be something other than the provenance statement.
2. Ask who else can obtain a signature from the same platform key. Run `ci.yml` on a fork's pull request: it cannot attest, but a pull request from a branch in the same repository, or a push to any other branch, can. Build one with the runner (a workflow copy with `id-token` and `attestations` write) and look at what its statement says about `ref` and `event`.
3. Take a genuine delivery and change one thing at a time: a byte of the artifact, a byte of the statement, the signature, the file name. For each, say what a correct verifier must do and why.

## 3. What the check's deliveries contain

The post-attempt check builds nine deliveries at run time from the lab's runner and platform key.

| Delivery | Intended outcome | What it exercises |
|---|---|---|
| Release from `main`, unmodified | Accept | The baseline |
| Artifact bytes appended after the build | Reject | Digest match |
| Provenance re-signed with a key that is not the platform's | Reject | Signature against the configured key |
| Preview build of an unmerged pull request from an in-repository branch, platform-signed | Reject | Authenticity is not authority: ref `refs/pull/57/merge`, event `pull_request` |
| That preview provenance edited to claim `refs/heads/main`, `push`, `release-v2.yml` | Reject | Signature covers the statement; the edit breaks it |
| Push to `refs/heads/experiment` by a second workflow, platform-signed | Reject | Ref and workflow path |
| A fork's own `main`, same workflow file, same platform | Reject | Canonical repository |
| Same bytes delivered as `service-1.4.tar.gz` | Accept | Digest, not name |
| An earlier release from `main` | Accept | Provenance does not prove newest |

## 4. Verifiers that look finished

Each was run through the check. Counts are mismatches out of nine.

| Verifier | Mismatches | What it gets wrong |
|---|---|---|
| Signature and digest only | 3 | Accepts the preview, the experiment branch and the fork: all genuinely platform-signed |
| Every field checked, signature not verified | 2 | Accepts the re-signed statement and the edited preview |
| Signature, digest and repository; no ref, event or workflow | 2 | Accepts the preview and the experiment branch |
| Full checks, but requires the file name to match the subject name | 1 | Rejects the renamed delivery of the same bytes |
| Reference design (section 5) | 0 | |

The first row is the common shape of a first attempt: it treats "signed by the platform" as "a release". The last row is not wrong about security; it is wrong about identity.

## 5. Reference evidence

Recorded while authoring. None of it is evidence of your attempt.

- Guided tests against `release-v2.yml`: 10 pass (3 vulnerable-workflow cases, 7 repair cases).
- Negative control, repair tests against `release-v1.yml`: 3 fail (release triggers on pull requests; token persisted in the build checkout; no provenance), 7 pass.
- Trace matrix: 3 workflows × 5 events; the release token appears in an outbound request to a non-registry path only under `release-v1.yml` with the telemetry pull request.
- Private reference verifier: signature with the configured key, predicate type, builder id, subject digest, build type, recognised external parameters only, repository, workflow path, ref, event, source commit present. 0 mismatches. The four incomplete variants above were derived from it by removing one check each.

## 6. Limits

The runner is a model of documented rules, not the service; the documentation pages and their read dates are in the lab README, and the rules can change. Dummy credentials were used throughout; nothing was sent to GitHub, and no real secret was exposed. The check judges accept and reject on nine deliveries; it cannot judge the policy text, the reader's tests or the limits note, and a verifier can pass it while reading its expectations from the wrong place. Provenance in the lab is signed with a local key; real attestations use Sigstore and an OIDC identity, whose verification adds certificate and transparency-log checks this lab does not model. Nothing here establishes that the artifact's source is safe, that the merge was reviewed, or that the platform was not compromised.
