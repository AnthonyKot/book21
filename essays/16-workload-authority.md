# A workload carries only the authority its task needs

The release token that leaked in [essay 15](15-build-credential.md) was a string somebody generated in March 2024, pasted into a repository secret, and never thought about again. Ask what it can do and the answer is the same today as on the day it was made: anything the account allows, from anywhere, for as long as nobody notices. Ask which job it was for and there is no answer. The token is a credential without an identity.

That is the ordinary state of automation credentials, and it is why a leak from one build job becomes an incident for the whole account. The repair in essay 15 stopped a contributor's script from reaching the token. This essay is about the token itself: what a job should hold instead, for how long, and who decides.

The [companion lab](https://github.com/AnthonyKot/book21/tree/main/labs/16-workload-authority) is a local model of the three parties involved: an issuer that signs a short-lived identity token describing a job, a broker that exchanges that token for a temporary credential bound to a role, and an authorizer that decides each request from the role's permissions. It is the shape of GitHub's OpenID Connect token and a cloud provider's web-identity role assumption, with an explicit clock so expiry and rotation can be shown. It contacts no provider. Its rules come from the documentation listed with read dates in the lab README; treat its output as a trace of those rules.

## What the leaked key can do

Run the lab's first design: one static key, `deployer`, shared by every job.

```
key created 2024-03-02T10:00:00Z, expires None
  allow artifacts:put    artifacts:releases/service/1.5.tar.gz   (the task)
  allow secrets:get      secrets:prod/db-password
  allow artifacts:delete artifacts:releases/service/1.4.tar.gz
  allow deploy:run       deploy:production
A year later, the same key:  allow secrets:get secrets:prod/db-password
```

Three properties of the key make the leak expensive, and they are separate. Its **scope** is everything, because a key made for "the pipeline" is granted what any pipeline step might ever need. Its **lifetime** is unbounded, because nobody wants to rotate a secret that a dozen jobs depend on. And it has no **identity**: when the authorizer sees it, it learns nothing about which job, which commit or which event is behind the request, so it cannot refuse a request on those grounds even if it wanted to.

Improving any one of these on its own helps less than it seems. A narrowly scoped key that lives forever still gives an attacker its full scope forever. A key rotated weekly that allows everything still gives a week of everything. The three properties are one design decision, and the mechanism that ties them together is identity.

## The job proves who it is; the broker decides what that is worth

The second design has no static key. When the publish job runs, it asks the platform for an identity token. The platform signs a set of claims about the job that the job cannot alter:

```json
{"sub": "repo:doc-approval/service:ref:refs/heads/main",
 "repository": "doc-approval/service", "ref": "refs/heads/main", "event_name": "push",
 "job_workflow_ref": "doc-approval/service/.github/workflows/release-v2.yml@refs/heads/main",
 "exp": "2026-09-17T09:05:00Z"}
```

GitHub's token carries these claims and more, with a default subject of the form `repo:ORG/REPO:ref:refs/heads/BRANCH`, or `:pull_request`, or `:environment:NAME`. The token is short-lived and is issued only to a job whose workflow grants it `id-token: write`.

The job presents the token to the broker and names the role it wants. The broker verifies the signature against the issuer keys it trusts, checks that the token was minted for this broker and has not expired, then evaluates the role's **trust policy**: a list of claim values the token must carry. The lab's `release-publisher` role trusts exactly the release workflow on `main` in the canonical repository, on a push. If the claims satisfy it, the broker issues a **role session**, a credential that carries the role's permissions and an expiry. In AWS terms this is `AssumeRoleWithWebIdentity`: no long-term credential is needed to call it, the session lasts from fifteen minutes up to the role's maximum, and any session policy can only narrow what the role allows.

The trace of the same four requests under this design:

```
release-publisher session issued, expires 09:15:00Z (role maximum 900s)
  allow artifacts:put    artifacts:releases/service/1.5.tar.gz   (the task)
  deny  secrets:get      secrets:prod/db-password                implicit deny
  deny  artifacts:delete artifacts:releases/service/1.4.tar.gz   implicit deny
  deny  deploy:run       deploy:production                       implicit deny
Same credential, sixteen minutes later:  deny (credential expired at 09:15:00Z)
The identity token, presented six minutes after issue:  refused (token expired at 09:05:00Z)
```

The permission policy is written the way cloud policies are evaluated: every request is denied unless a statement allows it, and an explicit deny overrides any allow. The publisher role has one allow statement, `artifacts:put` on `artifacts:releases/service/*`. Nothing had to deny the secret read; it was never allowed.

## The trust policy is where "which job" is enforced

The same platform signs tokens for every job it runs. That is the part people miss when they say a pipeline "uses OIDC now". The lab asks the broker for the publisher role on behalf of three other jobs, all with valid signatures from the same issuer:

```
preview job of an unmerged pull request:   refused: ref='refs/pull/57/merge' does not satisfy 'refs/heads/main'
ci.yml on main (right branch, wrong workflow): refused: job_workflow_ref ... ci.yml ... does not satisfy ... release-v2.yml
a fork's own release-v2.yml on its main:   refused: repository='mallory/service' does not satisfy 'doc-approval/service'
```

Each refusal names a claim. The trust policy is the boundary from essay 15 written down a second time, now on the consumer's side of the credential: a release is a push to `main` of the canonical repository by the release workflow, and only that identity gets the release credential. GitHub's own configuration guide puts the minimum bluntly: you must define at least one condition on the subject, so that untrusted repositories cannot obtain tokens for your role. A trust policy that names only the repository lets every pull request, branch and workflow in it assume the role; the lab's negative control shows exactly that, while still expiring on time and still refusing forks.

Two more refusals complete the picture. A token minted for a different audience is refused before its claims are read: a token meant for one service must not be accepted by another, which is the lesson of [essay 5](05-token-boundary.md) applied to machines. And the publisher's token is refused the deployer role, whose trust policy requires an `environment` claim the publish job does not carry. One job, one role, one task.

## Rotation is the platform's problem now

The lab ends its trace by retiring the issuer's signing key and generating a new one. Tokens signed with the retired key are refused; a fresh token is accepted; no job's configuration changed, because no job ever held a key. Lifetime and rotation have moved from something each team must remember to something the platform does.

The limits are worth stating as plainly as the gains. A role session is still a credential; leaked within its lifetime, it does what the role allows, which is why the lab's roles keep sessions to fifteen minutes and why the scope still has to be narrow. The trust policy is only as precise as the claims: a job that runs arbitrary code, like the fork's build script in essay 15, can request a token in that job's name, so the identity is the job's, not the code's. The model omits things a real provider adds, among them a short clock-skew window after expiry, session policies, permission boundaries and organisation-wide policies. And nothing here decides *which* actions a task needs; that remains a design judgment, and it is the practice.

<!--mission-->

## Practice: write the policy for a job that deletes things

A scheduled workflow, `cleanup.yml`, runs on `main` every night. It lists the staging artifacts, deletes the ones older than thirty days and writes a short report. It runs for a few minutes. Today it uses the static key.

Write the `staging-cleanup` role in the lab's `policies/cleanup.json`: the trust policy that admits this job and no other workload the platform signs for, the permission statements that allow the task and nothing beyond it, and a session lifetime you can defend. The [worksheet](../practice/16-authority-worksheet.md) states the job's identity claims and the resource names. Then write your own tests: the task's requests allowed, requests beyond it denied, other workloads refused the role, and a time at which the session must be dead.

The lab's `policy_check.py` runs its own requests against your policy after your attempt is saved. Leave it closed until then. The worksheet also adds a second requirement partway through; how you change the policy for it, and what you refuse to change, is part of the deliverable.

The essay's claim is that scope, lifetime and identity are one decision. Your policy tests that claim if a reader can see, from the trust policy alone, which job it was written for.
