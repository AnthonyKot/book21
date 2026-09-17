# The build system is a production credential

A reviewer reads a pull request and decides whether its code is safe to merge. That is the wrong question to ask about a change to a build workflow. The right question is what the change can do *before* anyone merges it, while it is running on the build machine with the project's own credentials.

An automated build holds things a contributor is not trusted with: a token that can write to the repository, a secret that can publish a release, network access from inside the project's account. Whether an outside contribution reaches those depends on one thing the reviewer cannot see in the diff: which event started the workflow, and which tree that event told the runner to check out.

The [companion lab](https://github.com/AnthonyKot/book21/tree/main/labs/15-build-credential) is a small model of a hosted runner. It reads a real workflow file, applies GitHub's documented rules for which jobs an event triggers, which secrets and token permissions a job receives, and which commit `actions/checkout` selects, then runs the workflow's shell steps in a scratch directory with dummy credentials and records every outbound request. It contacts nothing real. Its rules are modelled from the GitHub Actions documentation, listed with read dates in the lab README; treat its output as a trace of those rules, not as a run on the service.

## One repository, two contributions

The subject is a document-approval service. A contributor named Casey opens a pull request from a fork that adds a withdraw feature with a test. A second contributor, from a different fork, opens a pull request titled "Add build telemetry" that changes `scripts/build.sh`. The new lines still run the tests and package the app. They also read the environment and post it to a host the contributor controls, and try to write to the repository through the checked-out credentials. Nothing in the diff is syntactically alarming. It looks like a build tweak.

Read either diff and you cannot tell what it will do. That depends on the workflow that runs it.

## The first design gives the change a credential

Version 1 of the release workflow builds a preview for every pull request so reviewers can try it, and publishes when `main` changes:

```yaml
on:
  pull_request_target:
    branches: [main]
  push:
    branches: [main]
permissions:
  contents: write
env:
  RELEASE_TOKEN: ${{ secrets.RELEASE_TOKEN }}
steps:
  - uses: actions/checkout@... # pinned
    with:
      ref: ${{ github.event.pull_request.head.sha || github.sha }}
  - run: bash scripts/build.sh
```

The `pull_request_target` trigger is the entire problem, and it is easy to reach for. A plain `pull_request` workflow from a fork runs with no repository secrets and a read-only token, so a maintainer who wants the preview job to have the publish secret often switches the trigger to `pull_request_target`, which does carry secrets. GitHub's documentation is explicit that this trigger, combined with checking out the pull request's code, "may lead to security vulnerabilities" including "granting unintended access to write privileges or secrets."

The lab shows exactly that transfer. Run the telemetry pull request against version 1:

```
event pull_request_target from fork
GITHUB_TOKEN scopes {"contents": "write"}
secrets available: RELEASE_TOKEN
step 1: checkout -> explicit ref 7bc0451… (the pull request head: the contribution as pushed)
step 2: run "bash scripts/build.sh" exit 0
   outbound POST /telemetry  -> received; carries secret RELEASE_TOKEN
   outbound PUT  …/README.md  -> 200: repository content written with GITHUB_TOKEN
step 3: run "publish"        -> accepted; carries secret RELEASE_TOKEN
```

Three things crossed the boundary. The release token left the runner in the telemetry request. The workflow's write-scoped `GITHUB_TOKEN`, which `actions/checkout` had written into the git config of the checked-out tree, let the contributor's script commit to the repository. And the contributor's build published a release. The workflow's author never intended a fork's pull request to do any of these. The event and the `ref` decided it.

The single most consequential line is the `ref`. Its author wanted the preview to build the contributor's actual code, so they checked out `github.event.pull_request.head.sha`. Under `pull_request_target` that is the correct way to run untrusted code in a job that already holds the repository's secrets. The default checkout for `pull_request_target` is the *base* branch, precisely so untrusted code does not run by default; overriding it to the fork's head undoes that protection.

## The boundary is the event, not the review

The fix is not to scan the build script harder. It is to arrange that the job holding the credential never runs contributor-controlled code, and the job that runs contributor code never holds the credential. That division follows from what the events already do.

A `pull_request` from a fork is safe to run because the platform withholds the credentials: with the exception of `GITHUB_TOKEN`, secrets are not passed to a workflow triggered from a fork, and that `GITHUB_TOKEN` is read-only for a fork's pull request. A `push` to `main` happens only after a maintainer has merged, so the code is no longer contributor-controlled. Version 2 puts each kind of work on the event that fits it:

```yaml
# ci.yml — every pull request, including forks
on: { pull_request: { branches: [main] } }
permissions: { contents: read }
# builds and tests the merged tree; no secret to leak, read-only token
```

```yaml
# release-v2.yml — only after a commit reaches main
on: { push: { branches: [main] } }
jobs:
  build:   # checks out with persist-credentials: false, runs build.sh, uploads the artifact
  publish: # needs: build; downloads the artifact and runs no repository script
```

Run the same telemetry pull request now and the release workflow does not trigger at all; `ci.yml` builds it with no secret and a token that cannot write. The lab confirms the fork's telemetry request goes out carrying an empty token, and its attempt to write the repository returns `403: token has contents:read`. Casey's legitimate feature still builds and tests on the same path. Publication happens only from `main`, in a `publish` job that runs none of the repository's shell scripts, so a change to `build.sh` can never reach the release token. The guided tests (ten cases) assert both halves: the fork pull request keeps the secret in and cannot write, and the release from `main` carries the release token only in the publish upload.

This is a division of authority, not a cleverer filter. `pull_request_target` still has legitimate uses, such as labelling a pull request, but only in a job that does not check out or execute the contributor's code.

## What the release proves, and what it does not

The repaired `publish` job also records provenance for what it ships. GitHub's artifact attestations, built on the SLSA provenance format, have the build platform sign a statement binding the artifact's digest to the workflow, repository, commit and event that produced it. The lab models this with an Ed25519 signature over an in-toto statement whose `subject` is the artifact's SHA-256 and whose `externalParameters` record the repository, ref and event.

A consumer of the artifact, a deployment system in another account, can then check the signature and compare the statement against what it expects of a release. SLSA's own guidance is that a verifier should check these expected values and "reject unrecognized fields" in the external parameters rather than assume a match.

The limit matters as much as the mechanism, and both GitHub and SLSA state it plainly. Provenance says where an artifact came from. It does not say the artifact is safe. It does not establish that the source code is free of vulnerabilities, and it does not by itself cover a compromise of the build platform. A signed statement that an artifact was built by this workflow from this commit is exactly as trustworthy as the decision, made earlier, about what that workflow was allowed to run. Provenance records the boundary. It does not draw it.

<!--mission-->

## Practice: verify what an untrusted producer sends you

The repaired workflow signs provenance for each release. The independent task is on the other side of that boundary: you are the trusted consumer, and a producer you do not control sends you an artifact and its signed provenance.

Write `consumer/verify.py`, which the lab calls with an artifact, a provenance statement, its signature and the build platform's public key, and which exits zero to accept the artifact for deployment or non-zero to reject it. The [worksheet](../practice/15-release-worksheet.md) states the deployment's trust facts: which repository, ref and event constitute a real release, and which build platform you trust. Decide what the provenance must establish before you deploy, where those expected values come from, and enforce it.

The lab's `consumer_check.py` hands your verifier a set of deliveries, some genuine and some not, and reports where your accept or reject disagrees with the intended outcome. Leave that file closed until your verifier and your own tests are written; its scenario list is part of the answer. Produce your verifier, your own tests with at least one delivery you construct and expect to reject, and a short note on what your check cannot establish even when it accepts.

The essay's claim is that provenance records a boundary rather than drawing one. Your note earns that claim only if it names something a fully verified, correctly signed release still does not tell you.
