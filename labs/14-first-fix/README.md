# First fix: a dated triage decision lab

Companion to [essay 14](../../essays/14-first-fix.md). Requires Python 3.12 or newer; no third-party Python packages, Docker, Java server, scanner account or network access is needed to run the frozen exercise. Tested with Python 3.12.3.

This is a decision-record exercise. The CVEs, vendor-linked CVSS vectors, dated EPSS values and KEV membership are real source data. Service deployments, old dependency versions, artifact markers, owners, evidence classifications and estimates are fictional. They are not scan results or observed exploits. Nothing here asserts that essay 13's images contain these vulnerabilities. No Grype/Trivy scan or CVE patch was executed for this unit.

## Inspect the evidence

- `data/threat-snapshot.json`: EPSS dated 2026-09-14; KEV catalog 2026.09.14; source URLs and SHA-256 hashes of the raw fetched data; vendor URLs, base vectors and score attribution. The full raw KEV capture is retained locally by the author; the public file includes the two selected membership results.
- `data/packet.json`: three fictional deployments at scenario time 2026-09-15 09:00 UTC. `fixture-sha256:` values are deterministic labels for the exercise, not hashes of deployed artifacts. `knownPath` is supplied evidence classification, not a scanner finding.
- `data/guided-decisions.json`: one defensible worked decision set. Write your own attempt before reading it if using the exercise for independent practice.
- `data/changed-event.json`: a new WAR deployment and changed ingress before the old decision expires.

R1's executable-JAR evidence excludes only the described CVE-2022-22965 exploit. The vendor warns about other possible exploitation paths. It does not establish that the vulnerability is absent. R2 deliberately supplies the prerequisites and lack of blocking conditions from CVE-2024-38816's advisory. R3's missing packaging evidence means unknown, not safe. Historical fixed-version information in the advisories is not a recommendation to deploy an old release now.

## Run the guided contract

From `labs/14-first-fix`:

```bash
python3 triage.py
python3 contract_tests.py
```

Expected: three records accepted at the frozen clock; **12 tests pass**. Checks cover ownership, review time, plan/reopening fields, item coverage, evidence association, narrow deferral eligibility and no unsupported closure state. Passing checks establish compliance with this record format and exercise policy, not correct prose or optimal prioritization.

The 24-hour maximum review interval is a scenario rule, not an industry SLA. Every action requires a review checkpoint; the patch-now checkpoint is not necessarily the time a complete upgrade finishes. CISA catalog dates are not substituted for the fictional company's deadlines.

For the negative control, advance time without refreshing decisions:

```bash
python3 triage.py --at 2026-09-16T09:00:00Z
```

Expected exit 1: all three records rejected for expired review times. The default clock is deliberately frozen, so a default pass never certifies current live decisions.

You can create a separate decision JSON file and pass `--decisions path/to/your-decisions.json`. Keep the evidence associations and required fields. Review the reasoning manually: the validator checks a documented basis for deferral but does not assign the first patch slot or decide that another action is forbidden. A stricter immediate response to R1 may be defensible.

## Changed exercise

Use the [worksheet](../../practice/14-triage-worksheet.md). Inspect the changed-event description, then:

```bash
python3 exercise_tests.py
```

The supplied `reopen_exercise.py` checks only deadlines: **5 pass, 4 fail**. Modify only the scheduler to reopen a relevant record when its artifact or evidence revision changes, or required comparison evidence disappears. Preserve unchanged pre-deadline behavior, exact-deadline reopening, unrelated-item isolation and the original decision record. Nine contract cases are provided; they are not a comprehensive event ingestion or date parser test suite.

An unpublished reference passed **9/9**. Removing its evidence-revision comparison caused **2 failures / 7 passes**: unchanged artifact with changed ingress evidence, and missing revision. Those runs verify scheduler behavior, not a mitigation of either CVE.

This function receives already routed scenario events with known decision IDs and valid explicit timestamps. It does not authenticate an event producer, poll a deployment system, monitor threat feeds or persist an audit log. Whoever maintains real evidence must advance its revision when relevant configuration or advisory evidence changes; the scheduler cannot detect a change nobody reports.

## Source and decision limits

Keep probability, percentile and known exploitation distinct. EPSS 0.14718 is about 14.7% estimated probability of observed CVE exploitation in the next 30 days; its percentile 0.96473 is a ranking. Neither predicts compromise of one deployment. KEV absence is only absence from the captured list. Preserve both score versions and vectors; the two scores use CVSS 3.0 and 3.1 respectively.

The exercise never closes a vulnerability as fixed. Real closure would require evidence of the deployed repair, allowed and forbidden behavior, and any remaining exposure. The fictional two-hour and four-hour engineering estimates are separate from the learner's 10–15-hour weekly study budget. Follow the worksheet's 10–12-hour planning allowance and record actual time and assistance.
