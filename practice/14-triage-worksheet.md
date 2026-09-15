# Triage decision worksheet

Use with [essay 14](../essays/14-first-fix.md) and the [decision lab](https://github.com/AnthonyKot/book21/tree/main/labs/14-first-fix). Save your part B records and notes before opening the [review guide](14-triage-review.md). The two Spring advisories linked in the essay are the only outside reading you need.

## Plan the week

These are authoring estimates derived from the steps below, not measured learner times. Record your actual time.

| Part | What it involves | Provisional estimate |
|---|---|---|
| Setup | Python 3.12; no packages or network | 0.25 hour |
| Guided | Read the essay and both advisories, run the validator, scheduler and tests, part A | 1.5–2.5 hours |
| Independent | Part B: read the 13:00 packet, write four records and notes, choose the afternoon order, validate | 2.5–4.5 hours |
| Delayed check | The final evidence-record question, about a week later | 0.5 hour |
| Optional | Part C, applying the record to a real advisory in a codebase you may inspect | 1–2 hours, later week |

The required parts total about 4.75–7.75 hours. Keep the week bounded:

- If you have spent about two hours on part B without a first record for each item, save what you have, open only the first hint in the review guide, mark it as assistance, or carry the work into next week.
- Leave part C for a later week unless the required parts finished early.

Start of attempt:

- Date and repository revision:
- Material already seen (worked decisions, hints, AI assistance):

## A. Guided: the 09:00 decisions

The essay gives its first-slot choice in its opening lines. Check that choice against the source: read `data/packet.json` and both advisories, and write in two sentences which evidence supports it and which evidence you would want to confirm. Then run the lab.

1. Mark which facts in the packet come from public sources and which are supplied fictional evidence. For one deployment fact, name the artifact, configuration or access test you would need in real work to confirm it.
2. Translate the EPSS probability and percentile for CVE-2024-38816 into plain language. Why is absence from KEV not a reason to defer?
3. The same CVE, score and threat data appear in R1 and R3. What makes their actions differ?
4. Run the one-day negative control. What does its rejection establish, and what does it not?

## B. Independent: decide again at 13:00

### The brief

`data/update-1300/packet.json` holds the evidence at 13:00: updates for the three services and a fourth scanner match, plus the team's capacity for the afternoon. Run the scheduler against it to see which records need review. The threat snapshot is unchanged.

**Requirements.**

1. Write one decision record per item in the lab's JSON format. The set must pass `triage.py` at the packet clock. The 13:00 packet carries no evidence classification, so a `deferred` record must state its own `deferralBasis` (`specific-exploit-blocked` or `prerequisites-absent`), and your note must show the evidence for it.
2. Each record's rationale must rest on the 13:00 evidence and the advisories, not on the 09:00 record.
3. Keeping an earlier action is a legitimate outcome; so is changing it. Either needs its reason.
4. Availability costs count. If an action removes a legitimate function or keeps it removed, say who loses it and what evidence would justify restoring it.
5. Do not treat a scheduled review, a plan or a passing validator as remediation.

### Deliverable 1: records

Save your decisions file and the validator output.

### Deliverable 2: a note per item

| Item | What changed since 09:00 | Action now, and whether it changed | Why | What you would verify before trusting the new evidence | What would reverse this decision |
|---|---|---|---|---|---|
| R1 | | | | | |
| R2 | | | | | |
| R3 | | | | | |
| R4 | | | | | |

### Deliverable 3: the afternoon

- Order of work for the free engineering time, and who does what:
- What that order delays, and the risk you accept by delaying it:
- One decision you were least sure about, and the evidence that would settle it:

### Limits

Name one thing your records cannot establish, and one piece of evidence the packet does not contain that a real triage would ask for.

## C. Optional: a real advisory

Choose a published advisory for a dependency in a codebase you are permitted to inspect. Write one decision record in the same format: which prerequisites you could confirm, which you could not, the action, the owner and the reopening trigger. Do not test against systems you are not authorized to assess, and do not publish findings about a real system.

## Handover

Write a short triage note for the afternoon: decisions, order, costs, owners, checkpoints and reopening conditions, with public-source dates and supplied-versus-verified evidence distinguished. For an internal team, address the service owners. For a consulting client, add the evidence available to you, exclusions and retest conditions. Label the scenario as fictional.

## Evidence record

- Actual time per part:
- Assistance used, including hints, AI suggestions and any look at the review guide:
- What you could now explain without notes:
- Delayed check, a week later: without reopening your records, state which 13:00 decision would reverse first if the evidence moved again, and why.
