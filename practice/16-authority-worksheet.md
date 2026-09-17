# Workload authority worksheet

Use with [essay 16](../essays/16-workload-authority.md) and the [companion lab](https://github.com/AnthonyKot/book21/tree/main/labs/16-workload-authority). Save your part B policy, tests and note before opening `policy_check.py` or the [review guide](16-authority-review.md). The GitHub and AWS pages listed in the lab README are the only outside reading you need.

## Plan the week

These are authoring estimates derived from the steps below, not measured learner times. Record your actual time.

| Part | What it involves | Provisional estimate |
|---|---|---|
| Setup | Python 3.12 and openssl; no packages, network or account | 0.25 hour |
| Guided | Read the essay and the documentation pages, run the trace, the tests and the negative control, part A | 1.5–2 hours |
| Independent | Part B: policy written with reasons, own tests, the widened requirement, note on limits, then the post-attempt check | 2.5–4 hours |
| Delayed check | The final evidence-record question, about a week later | 0.5 hour |
| Optional | Part C, reading a real role or service account you are permitted to inspect | 1–2 hours, later week |

The required parts total about 4.75–6.75 hours. Keep the week bounded:

- If setup fails for more than an hour, record what failed and answer part A from the trace quoted in the essay and the documentation pages.
- If you have spent about two hours on part B without a policy that admits the cleanup job and allows its four task requests, save what you have, open only the first hint in the review guide, mark it as assistance, or carry the work into next week.
- Leave part C for a later week unless the required parts finished early.

Start of attempt:

- Date and repository revision:
- Material already seen (hints, AI assistance, the check file):

## A. Guided: read the refusals

Run the trace, the tests and the negative control, then answer from the transcripts and the documentation, not from the essay's summary.

1. For the leaked static key, name the three properties that make the leak expensive and, for each, the line of `policies/v1.json` that sets it.
2. The publish job's secret read is denied by "implicit deny". What would a reader of the permission policy have to add for it to be allowed, and what would have to be added for it to be denied even if someone later added that allow?
3. Three jobs with valid tokens are refused the publisher role. For each, name the claim that refused it and say what a trust policy that omitted that claim would admit.
4. The negative control passes 13 of 16 tests. Which of the properties from question 1 does it still get right, and which failures show that "uses OIDC" is not the same as "carries only the authority its task needs"?

## B. Independent: the cleanup job's role

### The brief

A scheduled workflow runs on `main` nightly: it lists the staging artifacts, deletes those older than thirty days and writes a run report. It runs for a few minutes and today uses the static key. You are replacing that key with a role.

**Trust facts and task facts.** These are the policy's inputs; they are stated so that the task is designing the policy, not guessing the job.

- The job's identity token carries `repository` `doc-approval/service`, `ref` `refs/heads/main`, `event_name` `schedule`, `job_workflow_ref` `doc-approval/service/.github/workflows/cleanup.yml@refs/heads/main`, and `aud` `lab-broker`. It carries no `environment`.
- The task needs to list `artifacts:staging/service/` (the prefix is the list resource), read and delete artifacts under `artifacts:staging/service/`, and write one file under `artifacts:reports/`. The action names are `artifacts:list`, `artifacts:get`, `artifacts:delete` and `artifacts:put`.
- Releases live under `artifacts:releases/`; secrets under `secrets:`; deployments are `deploy:run` on `deploy:staging` or `deploy:production`. The job needs none of these.
- The job finishes within ten minutes, and its credential is issued when the job starts. An organisation rule for this exercise: a job's credential must not be usable thirty minutes after the job started.

**Requirements.**

1. Write the `staging-cleanup` role in `policies/cleanup.json` in the lab's schema, keeping the role name.
2. Before writing it, decide and write down: which claims the trust policy names and why each one is there; which actions and resources the permissions name and why each statement is as wide as it is and no wider; the session maximum and why.
3. The job must be able to do its task. Refusing the job its own work is a defect, not caution.
4. Write your own tests, as a Python file using the lab's `authority` module: the task requests allowed; requests beyond the task denied; at least two other workloads the same platform signs for refused the role, with the refusing claim; the session dead at a time you choose from the organisation rule.
5. **The widened requirement.** Partway through the quarter the team asks the same job to also record, in its report, the sizes of the current releases, which needs `artifacts:get` on `artifacts:releases/service/*`. Decide whether to widen this role, split the work into a second role, or refuse, and record what each option lets a leaked session do. Your policy file reflects your decision; your note gives the reason and the rejected alternatives.
6. Write a note on what the role cannot protect against.

### Deliverable 1: the policy with reasons

- Trust policy, claim by claim, each with the workload it excludes:
- Permission statements, each with the reason for its width:
- Session maximum and the rule it satisfies:

### Deliverable 2: tests

Save your test file and its output.

### Deliverable 3: the widened requirement

- Decision, and what a leaked session can do under it:
- Rejected alternatives, and what a leaked session could do under each:

### Deliverable 4: the post-attempt check

After deliverables 1–3 are saved, run `python3 policy_check.py`. Record the result. For each mismatch, say whether it exposed a missing condition, an over-wide statement, an over-strict one, or a disagreement with the stated facts that you can defend.

### Limits

- One thing the role cannot prevent a job with this identity from doing:
- One claim you would want in the token that this lab's issuer does not provide:

## C. Optional: a real role

Read one role, service account or workload identity you are permitted to inspect, in any provider. Write its trust conditions, its permission statements and its session maximum in the same three-part form, and mark each statement as needed by the task, wider than the task, or unknown. Do not change anything and do not test against systems you are not authorised to assess.

## Handover

Write a short note for the team that owns the account: which job the new role admits, what it can do, when its sessions die, what the old static key still protects until it is revoked, and the order of the cut-over (create role, switch job, verify, revoke key). For a consulting client, add what you could verify from the outside and the retest that would confirm the key is dead.

## Evidence record

- Actual time per part:
- Assistance used, including hints, AI suggestions and any look at the check or the review guide before your attempt was saved:
- What you could now explain without notes:
- Delayed check, a week later: without reopening your policy, name one workload that could obtain your role if a single trust claim were dropped, and which claim.
