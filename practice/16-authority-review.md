# Workload authority review

Open this after saving your [worksheet](16-authority-worksheet.md) policy, tests and notes. Everything below counts as assistance once you have read it. If you are stuck partway, read only the next hint in section 2 and record that you did.

## 1. Judge the attempt before comparing policies

No single policy is correct. A sound one shows the following.

**Every trust claim excludes a named workload.** The reasons column says what each claim keeps out: the repository keeps out forks, the ref keeps out branches, the event keeps out pushes and pull requests of the same workflow, the workflow reference keeps out other workflows on the same branch. A claim with no named exclusion is either redundant or not understood; a missing claim admits a workload the reader did not intend.

**The subject is not enough on its own.** The default subject encodes repository and ref (or pull request, or environment) but not the workflow. Trusting `sub` alone admits every workflow on `main`.

**Statements name the task's prefixes.** Deletion is allowed under the staging prefix, not under `artifacts:*`. A wildcard that also covers releases is the defect the task exists to expose. An explicit deny on releases, secrets and deployments is a good defence in depth, but it does not excuse an over-wide allow, because the next statement someone adds will not have it.

**The job can do its task.** Listing needs the prefix resource; writing the report needs the reports prefix. A policy that fails the task while refusing everything else has moved the problem, not solved it.

**The session bound follows from the job.** Fifteen minutes is the smallest session the modelled provider issues and it covers a ten-minute job. Thirty minutes meets the stated rule at its boundary and needs a reason for the extra fifteen. A one-hour maximum breaks the rule; a reader who chooses it must argue the rule is wrong, not ignore it.

**The widening is decided, not absorbed.** Adding `artifacts:get` on releases to this role gives a leaked cleanup session read access to every release. Splitting the read into a second role with its own trust conditions, or reading sizes from an inventory the job does not need credentials for, keeps the deletion role narrow. Any of the three is defensible with its consequence stated; silently widening the role is not.

**Own tests exercise the refusing claim.** A test that only checks "refused" is weaker than one that checks which claim refused, because a policy can refuse the right workload for the wrong reason and admit it later when that reason changes.

**The limits note names what identity cannot do.** The role is granted to the job, not to the code the job runs; a job that executes untrusted code can still spend the role's authority inside the session. The token says which workflow asked, not whether that workflow was reviewed.

**A handover someone could act on.** Order of cut-over, what the old key still protects until revoked, and the retest that shows it dead.

## 2. Progressive hints, if you are stuck

1. Write the cleanup job's five claims in a column. For each, write one workload the platform also signs tokens for that differs only in that claim. That column is your trust policy and its reasons.
2. List the four task requests and, next to each, the narrowest resource pattern that matches it and nothing under `releases/`. Then run the lab's authorizer against a release deletion with your draft.
3. Set the session maximum to the smallest value the model allows and ask whether the job can finish in it. Only widen with a reason.

## 3. What the check sends

The post-attempt check issues tokens at 02:00 and asks for the longest session your role permits.

| Request | Intended outcome | What it exercises |
|---|---|---|
| The scheduled cleanup job on `main` assumes the role | Admitted | The trust policy admits the job |
| List staging; get and delete a 41-day-old staging artifact; write the report | Allow | The task's four requests |
| Delete the current release; publish a release | Deny | Resource scoping under `artifacts:` |
| Read the staging and production database passwords | Deny | Action families the task does not use |
| Deploy to staging | Deny | The deployment family |
| The session thirty minutes after the job started | Deny | The session bound against the organisation rule |
| A pull request run of `cleanup.yml` | Refused | The ref or event claim |
| A manual (`workflow_dispatch`) run of `cleanup.yml` on `main` | Refused | The event claim |
| `ci.yml` on `main` | Refused | The workflow claim |
| `cleanup.yml` on a feature branch | Refused | The ref claim |
| A fork's `cleanup.yml` on its `main` | Refused | The repository claim |

## 4. Policies that look finished

Each was run through the check. Counts are mismatches out of sixteen outcomes. The starter as shipped stops at the first outcome, because it states no session maximum; the first row below is the starter with only a maximum filled in.

| Policy | Mismatches | What it gets wrong |
|---|---|---|
| Empty trust, no permissions, 900 s | 9 | Admits every workload the issuer signs for, and allows the job nothing |
| Trust on repository only | 4 | Admits the manual run, the pull request run, `ci.yml` on `main`, and the feature branch |
| Trust on repository and ref only | 2 | Admits the manual run and `ci.yml` on `main` |
| `artifacts:*` on `artifacts:*` | 2 | Allows deleting and publishing releases |
| Correct trust and scope, one-hour session | 1 | Session alive thirty minutes after the job started |
| Correct trust and scope, 1,800-second session | 0 | Meets the stated rule exactly; the review question is whether a session twice the job's length was needed |
| Reference design (section 5) | 0 | |

An empty trust policy is the sharpest lesson in the table: with no conditions, the broker checks only that the issuer signed the token, so any job on the platform gets the role.

## 5. Reference evidence

Recorded while authoring. None of it is evidence of your attempt.

- Guided tests: 16 pass (3 static-key, 13 role design). Negative control `--broad`: 3 fail (pull request and `ci.yml` obtain the publisher role; the publisher can delete a release), 13 pass.
- Trace: static key allows all four requests now and a year later; publisher session allows the task and denies the other three; session dead at sixteen minutes; token refused at six minutes; preview, `ci.yml` and fork refused with the named claim; other-audience token and deployer role refused; retired issuer key refused after rotation, new key accepted.
- Private reference role: trust on `aud`, repository, ref, event, workflow reference; allow list/get/delete under the staging prefix and put under reports; explicit deny on releases, secrets and deployments; 900-second maximum. 0 mismatches. The variants above were derived from it by changing one thing each.

## 6. Limits

The broker and authorizer are a model of documented rules for one provider's exchange and evaluation order; the model omits the clock-skew window, session policies, permission boundaries, organisation policies, resource-based policies and session tags, any of which changes what a real policy must say. Subject claims use GitHub's earlier default format. The model has no metadata-only verb, so a role that must read release sizes grants `artifacts:get` on the whole artifact; a real provider's finer verbs would let the widening be narrower than this lab allows. The check judges sixteen outcomes; it cannot judge the reasons column, the tests, the widening decision or the limits note, and a policy can pass it with reasons that are wrong. Nothing here establishes that the job's code is safe, only which job the credential was issued to.
