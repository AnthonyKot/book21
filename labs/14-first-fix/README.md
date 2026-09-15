# First fix: a dated triage decision lab

Companion to [essay 14](../../essays/14-first-fix.md). Requires Python 3.12 or newer; no third-party packages, Docker, Java, scanner account or network access. Tested with Python 3.12.3. Run commands from `labs/14-first-fix`.

This is a decision-record lab. The CVEs, vendor-linked CVSS vectors, dated EPSS values and KEV membership are real source data. Service deployments, dependency versions, artifact markers, owners, capacity, timings and evidence classifications are fictional. They are not scan results, observed exploits or a real incident, and nothing here asserts that essay 13's images contain these vulnerabilities. No Grype or Trivy scan, exploit or patch was executed for this unit.

## Files

- `data/threat-snapshot.json`: EPSS dated 2026-09-14; KEV catalog 2026.09.14; source URLs and SHA-256 hashes of the raw fetched data; vendor URLs, base vectors and score attribution. The full raw KEV capture is retained by the author; the public file keeps the two relevant membership results.
- `data/packet.json`: three fictional deployments at 09:00 UTC. `fixture-sha256:` values are deterministic exercise labels, not hashes of deployed artifacts. `knownPath` is a supplied evidence classification, not a scanner finding.
- `data/guided-decisions.json`: the worked 09:00 decisions discussed in the essay.
- `data/update-1300/packet.json`: the independent task's evidence at 13:00 UTC, including team capacity. It carries no `knownPath` classification: classifying the evidence is part of the task.
- `triage.py`: the record validator and the reopen scheduler.
- `contract_tests.py`: tests of both.

## Guided: the 09:00 records

```bash
python3 triage.py
python3 contract_tests.py
```

Expected: three records accepted at the frozen clock, and 23 tests pass. The tests cover ownership, review time, plan and reopening fields, item coverage, evidence association, the narrow-deferral basis, no closure state, and the scheduler's reopening rules.

The 24-hour maximum review interval is a scenario rule, not an industry SLA. Every action needs a review checkpoint; a patch-now checkpoint is not necessarily when the upgrade finishes. CISA catalog dates are not the fictional company's deadlines.

Negative control, advancing time without refreshing decisions:

```bash
python3 triage.py --at 2026-09-16T09:00:00Z
```

Expected exit 1: all three records rejected for expired review times. The default clock is frozen, so a default pass never certifies current decisions.

Ask the scheduler which records need review against a packet:

```bash
python3 triage.py --packet data/packet.json --at 2026-09-15T12:00:00Z --reopen
```

A record is queued when its review time has arrived, when the packet's artifact or evidence revision differs from the one the record cites, or when that comparison evidence is missing. An item with no record is also queued. Queuing asks for a human decision; it changes nothing.

## Independent: decide again at 13:00

Use the [worksheet](../../practice/14-triage-worksheet.md). See what needs review:

```bash
python3 triage.py --packet data/update-1300/packet.json --reopen
```

Write your decisions as a JSON list in the same format as `data/guided-decisions.json` (keep the file outside `data/`). Because the 13:00 packet has no classification, a `deferred` record must state its own `deferralBasis`: `specific-exploit-blocked` or `prerequisites-absent`. Then validate at the packet clock:

```bash
python3 triage.py --packet data/update-1300/packet.json --decisions my-decisions-1300.json
```

The validator checks the record contract only. It cannot tell whether a stated deferral basis is true, and a decision set can pass it and still be poorly reasoned; the worksheet and review guide cover the judgment.

## Limits

Keep probability, percentile and known exploitation distinct. EPSS 0.14718 is about a 14.7% estimated probability of observed exploitation of that CVE in the next 30 days; its 0.96473 percentile is a ranking. Neither predicts compromise of one deployment. KEV absence is only absence from the captured list. The two scores use CVSS 3.0 and 3.1; keep their vectors.

The scheduler receives packets with known item IDs and explicit timestamps. It does not authenticate an evidence producer, poll a deployment system, monitor threat feeds or keep an audit log. Whoever maintains the evidence must advance its revision when relevant configuration or advisory facts change; the scheduler cannot detect a change nobody reports.

The lab never closes a vulnerability as fixed. Real closure would need evidence of the deployed repair, allowed and forbidden behaviour, and any remaining exposure. The fictional engineering estimates and capacity are separate from your weekly study budget.
