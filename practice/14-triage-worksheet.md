# Defend the next action

Use [essay 14](../essays/14-first-fix.md) and the [decision lab](https://github.com/AnthonyKot/book21/tree/main/labs/14-first-fix). Save a first attempt before opening `guided-decisions.json` or this [review guide](14-triage-review.md).

## A first pass without the worked decisions

Read `packet.json`, `threat-snapshot.json` and the two vendor advisories. Mark which statements come from public sources and which are supplied fictional deployment evidence. For real work, name the artifact, configuration, inventory or access test you would need to confirm each deployment statement.

| Record | Your action | Evidence that changes priority | Missing evidence | Owner and review time |
|---|---|---|---|---|
| R1 | | | | |
| R2 | | | | |
| R3 | | | | |

Explain your first remediation slot without multiplying CVSS, EPSS and estimated effort into an invented risk score. State what can proceed in parallel. Describe the availability cost of containment, permitted behavior to preserve, and what evidence would justify restoring service. Do not claim to have reproduced either CVE.

Identify the EPSS score date and KEV catalog version. Translate the probability and percentile in the second advisory separately. Explain why absence from KEV and missing deployment evidence cannot be used as equivalent reasons to defer.

## Make the record concrete

Run the README's guided commands after saving your decisions. If you wrote a JSON decision file, validate it with `python3 triage.py --decisions your-decisions.json`. The program checks the fixture contract, not the quality of your rationale. A more conservative response than the worked example can be valid if its tradeoff is explained.

Run the explicit next-day negative control. Record the three rejection reasons and distinguish an expired review from a completed patch. The default clock is a reproducibility device; it must not be mistaken for today's time.

## Independent change: noon arrives before the deadline

Read `changed-event.json`. Write a new assessment of R1 before editing code. Which earlier statement no longer applies? What would you do now, who owns it, and what would you test before declaring the route contained or the software fixed?

Run `python3 exercise_tests.py`. Save the initial failures. Repair `reopen_exercise.py` so an artifact change or relevant evidence revision change triggers review even if time remains. Missing comparison evidence should also reopen review. Preserve unrelated-item behavior and the old decision record.

After the nine cases pass, add one changed condition yourself: a new advisory interpretation represented by an evidence revision, an unchanged event just before expiry, or a different artifact under the same service name. Predict the result before running it. Explain why review is a human decision point rather than an automatic assertion that a CVE is exploitable.

## Handover and retention

Prepare either an internal service-owner note or a bounded consulting triage note. Include the public-source dates, supplied versus verified evidence, action, owner, checkpoint, containment cost, remaining uncertainty and reopening conditions. Neither route should describe this exercise as a production assessment.

Plan 10–12 hours within the existing 10–15-hour week: about 2 source reading, 3 first-pass triage, 3 code and negative controls, and 2–4 handover and delayed review. Record actual time and assistance. The fictional team's remediation estimates are not your study timetable.

Later, explain why the highest score did not automatically receive the first patch slot, and identify a change that would reverse that decision. Missing reports of reader practice are not evidence of either success or failure.
