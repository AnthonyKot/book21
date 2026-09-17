# Build credential: a local trace of workflow authority

Companion to [essay 15](../../essays/15-build-credential.md). Requires Python 3.12 or newer with PyYAML (`pip install pyyaml`), plus `openssl` and `curl` on the path. No network access, Docker, Java or GitHub account. Tested with Python 3.12.3, PyYAML 6.0.1 and OpenSSL 3.0.13. Run commands from `labs/15-build-credential`.

`runner.py` is a model of a hosted CI runner. It reads a workflow file, decides which jobs an event triggers, selects the tree `actions/checkout` would check out, decides which secrets and `GITHUB_TOKEN` scopes the job receives, executes the `run` steps in a scratch directory with dummy credentials, and records every outbound request. The rules are modelled from the GitHub Actions documentation read on 2026-09-16 (events, secrets, token permissions, `actions/checkout` inputs) and from the SLSA 1.2 provenance format. Nothing here contacts GitHub, and every credential is a dummy value in `platform/secrets.json`.

## Files

- `workflows/release-v1.yml`: the first release design, `pull_request_target` plus `push`, one job that builds and publishes.
- `workflows/release-v2.yml` and `workflows/ci.yml`: the repaired division. Third-party actions are pinned to full commit SHAs, resolved on 2026-09-16.
- `repo/base/`: the service, its tests and `scripts/build.sh`.
- `repo/contribution-feature/`: a legitimate fork contribution (a feature and its test).
- `repo/contribution-build/`: a fork contribution that changes `scripts/build.sh` to report the environment to an outside host and to write to the repository with whatever credentials it finds.
- `events/`: five events. `fork-pr-*` are `pull_request` from a fork; `fork-prt-*` are `pull_request_target`; `push-main` is a push to `main` after merging the feature.
- `runner.py`, `attest.py`: the runner and the Ed25519 signing helpers (openssl). The first run creates `platform/builder.key` and `builder.pub`; the private key stands for the platform's signing identity.
- `guided_tests.py`: ten tests over the runner's output.
- `consumer/verify.py`: the starter for the independent task.
- `consumer_check.py`: the post-attempt check. **Opening it shows the scenario list, which is part of the answer.** Run it only after your verifier, tests and note are saved.

## Guided: trace one change through both designs

```bash
python3 runner.py --workflow workflows/release-v1.yml --event events/fork-prt-build.json
python3 runner.py --workflow workflows/ci.yml          --event events/fork-pr-build.json
python3 runner.py --workflow workflows/release-v2.yml  --event events/push-main.json
```

Each run prints a transcript and writes `runs/<workflow>-<event>/` with the transcript, `summary.json` (jobs, steps, outbound requests and which carried a secret) and, for a release from `main`, `app.tar.gz`, `provenance.json` and `provenance.sig`. Any workflow can be run against any event; a workflow the event does not trigger reports no jobs.

What to read in the transcript: which tree the checkout selected and why; the token scopes and their source; which secrets the job received; each outbound request, its result at the listener, and whether it carried a secret. The listener stands for every host the runner can reach: the registry (`/upload`, requires the release token), the platform API (`PUT /repos/...`, requires a token with `contents: write`) and anywhere else (`/telemetry`).

```bash
python3 guided_tests.py
```

Expected: 10 pass. Three tests describe what `release-v1.yml` gives the telemetry pull request; seven describe the repair: the release workflow ignores pull requests, a fork's pull request still builds and tests, receives no repository secret, cannot write with its token, the release checks out the pushed commit without persisting a token, the release token appears only in the publish upload, and the release carries platform-signed provenance for the uploaded bytes.

Negative control, the repair tests against the first design:

```bash
python3 guided_tests.py --release workflows/release-v1.yml
```

Expected: 3 failures, 7 passes. The passes include the `ci.yml` cases, which do not depend on the release workflow.

## Independent: the trusted consumer

Use the [worksheet](../../practice/15-release-worksheet.md). Implement `consumer/verify.py` under this contract:

```bash
python3 consumer/verify.py --artifact PATH --provenance PATH --signature PATH --builder-key platform/builder.pub
```

Exit 0 accepts, 1 rejects; the first output line is `ACCEPT: …` or `REJECT: …`. A genuine delivery to test against is any `runs/release-v2-push-main/` output. To construct other deliveries, run the runner with your own event files (see `events/` for the fields: `event_name`, `repository`, `ref` or `base_ref`, `from_fork`, `pull_request.number`, `base_tree`, `contribution`) or with a copy of a workflow; the platform signs provenance for any job whose `permissions` include `id-token: write` and `attestations: write` and which runs the attest step. `attest.verify_signature` and `attest.sha256_file` are available to your verifier.

After your attempt is saved:

```bash
python3 consumer_check.py
```

It builds deliveries at run time and reports each accept/reject against the intended outcome, with the reason your verifier printed. It cannot judge your policy text, your tests or your limits note.

## Scope and limits

The runner implements: `on` with `push`, `pull_request` and `pull_request_target` and their `branches` filter; workflow- and job-level `permissions` including the read cap for fork pull requests; secrets withheld from fork pull requests; `actions/checkout` default refs per event, an explicit `ref` equal to the pull request head, and `persist-credentials`; `actions/upload-artifact` and `download-artifact` between jobs; `actions/attest-build-provenance` as a platform-side signature; `${{ }}` expansion for the few contexts the workflows use, including `||`. It does not implement approval gates for first-time contributors, environments, reusable workflows, matrices, containers, caches, `workflow_run`, or expression injection into `run` scripts. Tree identifiers are hashes of the working tree, not git commit ids, and a fork's branch is modelled as the base plus the contribution, so the pull request head and the merge result are the same tree here; the transcript's checkout label says which one the rule selected.

## Sources modelled (read 2026-09-16)

- [Events that trigger workflows](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows): refs and `GITHUB_SHA` per event; the `pull_request_target` warning; fork pull requests receive no secrets beyond a read-only `GITHUB_TOKEN`.
- [Secure use reference](https://docs.github.com/en/actions/reference/security/secure-use): `pull_request_target` and `workflow_run` must not check out untrusted code; least-privilege token; pinning actions to a full commit SHA.
- [Using secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets): "With the exception of `GITHUB_TOKEN`, secrets are not passed to the runner when a workflow is triggered from a forked repository."
- [Workflow syntax, `permissions`](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax): unnamed scopes become `none`; fork pull requests are capped at read.
- [actions/checkout](https://github.com/actions/checkout): `ref` default per event; `persist-credentials` default `true`.
- [SLSA 1.2 build provenance](https://slsa.dev/spec/v1.2/build-provenance) and [verifying artifacts](https://slsa.dev/spec/v1.2/verifying-artifacts): statement shape, `builder.id`, expectations, rejecting unrecognised external parameters.
- [Artifact attestations](https://docs.github.com/en/actions/concepts/security/artifact-attestations): what an attestation links and that it "is not a guarantee that an artifact is secure".

Behaviour on the real service can differ from this model and can change; the lab teaches the shape of the rules, and the pages are the authority.
