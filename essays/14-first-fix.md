# The highest severity is not automatically the first fix

At the start of this fictional triage meeting, two rows say **9.8** and one says **7.5**. The first remediation slot goes to the 7.5.

Nobody has changed the scores. The difference is in the deployments. One 9.8 row describes a service that lacks a prerequisite for the advisory's demonstrated exploit. The other lacks the evidence needed to decide. The 7.5 row describes an exposed route whose prerequisites are present and whose process can read confidential files.

That decision needs a written explanation. Otherwise “context matters” can become a convenient way to postpone difficult upgrades. The useful skill is to connect evidence to an action, preserve the uncertainty, and specify exactly when the decision must be reopened.

## Put three records on the table

The [companion decision lab](https://github.com/AnthonyKot/book21/tree/main/labs/14-first-fix) freezes threat data and a scenario clock. Its CVE advisories, EPSS values and KEV membership come from inspected public sources. Its services, dependency versions, deployment evidence, owners and effort estimates are **authored scenario facts**. They are not scanner output, observations of a real company or findings against the images from [essay 13](13-build-inventory.md).

The scenario begins at **2026-09-15 09:00 UTC**:

| Record | Service and advisory | Base severity | Supplied deployment evidence |
|---|---|---|---|
| R1 | Ledger administration; CVE-2022-22965 | 9.8, CVSS 3.0 | Executable Spring Boot JAR; restricted administrative ingress; no WAR deployment in the reviewed artifact |
| R2 | Document files; CVE-2024-38816 | 7.5, CVSS 3.1 | Public functional resource route, filesystem location, Reactor Netty, no Spring Security HTTP Firewall |
| R3 | Legacy preview; CVE-2022-22965 | 9.8, CVSS 3.0 | Public endpoint; affected version recorded; deployment packaging evidence missing |

The full packet identifies components, versions and evidence revisions. Its artifact identifiers are explicitly fictional markers, not hashes of deployed software. In real work, replace those markers with independently verified artifact identities and link the actual deployment evidence. A plausible inventory match is not enough to settle applicability.

CVE-2022-22965 concerns Spring data binding. The vendor describes an exploit requiring JDK 9 or later, Tomcat and a WAR deployment; it states that the default executable-JAR deployment is not vulnerable to that specific exploit. The same advisory warns that the underlying issue is more general and other exploitation methods may exist. R1's packaging evidence narrows one path; it does not justify declaring the vulnerability absent. [Spring advisory](https://spring.io/security/cve-2022-22965/).

CVE-2024-38816 concerns functional static-resource handling. The advisory identifies `RouterFunctions` with a `FileSystemResource` location and notes blocking behavior when the Spring Security HTTP Firewall, Tomcat or Jetty is in use. R2 deliberately specifies Reactor Netty and no such firewall. Its scenario therefore supplies the relevant prerequisites instead of silently assuming every Spring application is exposed. [Spring advisory](https://spring.io/security/cve-2024-38816/).

R3 has neither R1's evidence nor proof of safety. “We have not found a WAR” is different from “we inspected the selected deployment and it is an executable JAR.” Missing evidence creates an investigation task.

## Read each number as the answer to its own question

CVSS Base describes intrinsic vulnerability severity under its stated metrics. Preserve the version and vector alongside the shorthand number. Here the scores are calculated from the vectors linked by the vendor: one describes high confidentiality, integrity and availability impact; the other describes high confidentiality impact without integrity or availability impact. Neither vector knows which file route your service exposes or whether today's inventory belongs to yesterday's image. [FIRST CVSS specification](https://www.first.org/cvss/v3.1/specification-document).

The frozen EPSS response is dated **2026-09-14**:

| Advisory | EPSS probability | Percentile | In the captured KEV catalog? |
|---|---:|---:|---|
| CVE-2022-22965 | 0.99638 | 0.99948 | Yes |
| CVE-2024-38816 | 0.14718 | 0.96473 | No |

EPSS estimates the probability of exploitation activity being observed for a published CVE in the next 30 days. It is not a forecast that this particular deployment will be compromised. The percentile is a relative ranking, not another probability. For the second row, 0.14718 means approximately 14.7%; 0.96473 does not mean a 96.5% chance of attack on your service. [FIRST explanation](https://www.first.org/epss/faq), [dated API response](https://api.first.org/data/v1/epss?cve=CVE-2022-22965,CVE-2024-38816&date=2026-09-14).

The KEV snapshot is catalog version **2026.09.14** from CISA's official data mirror. Inclusion records known exploitation in the wild; it does not establish exploitation of our fictional service. Absence from that snapshot does not establish that exploitation has never happened or will not happen. Keep the retrieval time and catalog version so the statement remains checkable when the catalog changes. [CISA's data repository](https://github.com/cisagov/kev-data).

The directive that introduced the catalog set remediation timeframes for federal civilian agencies. Copying a catalog due date into this fictional company's ticket would not establish its own obligations or a sensible local response deadline. Our one-hour, two-hour and next-day deadlines below are explicit exercise choices. [CISA's original announcement](https://content.govdelivery.com/accounts/USDHSCISA/bulletins/2fa7e0a).

A score-only sort would put both critical rows ahead of R2. An EPSS-only sort would do the same. Multiplying severity by EPSS and dividing by estimated patch hours would add arithmetic without establishing a calibrated model of local loss. It would also conceal the most important distinction here: R3's deployment state is unknown.

## Choose an action that can change the outcome

For **R2, patch-now** means beginning containment and remediation now. Jon, the fictional service owner, has a two-hour initial slot. The plan disables the exposed resource route while testing an upgrade to a supported fixed release. Before restoring service, verification must cover both forbidden file access and intended document downloads. The old advisory's first fixed version is historical information, not a recommendation to deploy that old release today.

Disabling a route has an availability cost. Record which users lose functionality, who accepts that interruption and how a secure service will be restored. If the full upgrade takes longer than expected, the response is to maintain effective containment and escalate the plan, rather than silently re-enable the route to meet a ticket deadline. No actual route change or CVE repair was performed for this decision exercise.

For **R3, investigate** is work with an owner and a one-hour deadline. Lea retrieves the deployed artifact and servlet/container configuration while coordinating restriction of public access. Investigation is not a lower-priority holding area. Confirmation of the WAR prerequisites would accelerate containment and repair. Failure to obtain evidence by the deadline also requires escalation; uncertainty has not become a mitigating control.

For **R1, deferred** means a narrow scheduling decision until the following morning. Mira prepares a tested upgrade, checks that the stated ingress restriction holds and owns the remaining risk. The high EPSS and KEV inclusion keep the issue urgent. The reason for not taking the first remediation slot is the inspected mismatch with the described exploit, not a claim that 9.8 is unimportant. The packet estimates four engineer-hours for its upgrade work; that estimate is fictional and revisable.

These actions can proceed together. Giving R2 the first bounded remediation slot does not require everyone to stop investigating R3 or preparing R1. The table describes different kinds of work, not a universal ordering of every vulnerability.

A stricter organization might accelerate R1 as well. That is a defensible response if it explains the consequences and resources. The lab checks a stated decision contract; it cannot prove that our allocation maximizes security. NIST's SSDF calls for gathering enough risk information to plan a response and then implementing that response. It does not turn one public score into the whole decision. [SSDF 1.1, RV.2](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-218.pdf).

## Make the deferral expire in code as well as prose

From the lab directory:

```bash
python3 triage.py
python3 contract_tests.py
```

The first command accepts the three supplied decision records at the frozen scenario time. The second runs **12 checks** on the record contract. Every decision needs an owner, rationale, plan, review time and reopening trigger. It must reference the packet's current artifact and evidence revision. The exercise allows no closure state, because a plan is not proof of remediation.

A deferred record also needs the packet's documented basis for the narrow deferral. An unknown path cannot satisfy that condition. The validator reads the supplied classification; it does not discover the deployment or verify the truth of the prose. A sentence can be present and still be wrong. Human review remains necessary.

Advance the explicit clock:

```bash
python3 triage.py --at 2026-09-16T09:00:00Z
```

All three old records are rejected. Their review times have arrived or passed. This is a verified control on decision freshness, not a simulated exploit. The clock is an argument so the exercise remains reproducible next month; running it without that argument does not certify today's decisions.

Expiry is only half the problem. At noon on the first day, the changed exercise introduces a new R1 deployment: a WAR on Tomcat, now publicly exposed. The next-morning deadline is still in the future. A scheduler that checks only the clock would keep the old deferral alive after its stated basis has disappeared.

The repair is to reopen review when the artifact or relevant evidence revision changes, as well as when time expires. Reopening is a request for a new decision. It does not automatically patch software or prove that an attacker succeeded.

<!--mission-->

## Exercise: the evidence changed before the deadline

Use the [worksheet](../practice/14-triage-worksheet.md) before the [review guide](../practice/14-triage-review.md). First write your own three decisions, including what you would verify before trusting each scenario fact. Compare them with the supplied reasoning only after saving your attempt.

Then run `python3 exercise_tests.py`. The supplied deadline-only scheduler passes five checks and fails four. Repair `reopen_exercise.py` so a changed artifact, changed evidence revision or missing required comparison evidence triggers review before expiry. Preserve an unchanged scheduled decision and avoid reopening an unrelated item's record. Do not overwrite the old decision to make it appear current.

The unpublished reference passed all nine checks. Removing its evidence-revision comparison produces two failures, demonstrating why checking artifact identity alone misses a configuration change around unchanged bytes. Your written response must also explain what the new WAR and ingress facts do to R1's earlier reasoning.

This unit uses real advisory and threat snapshots with fictional deployment evidence. No Grype or Trivy scan, vulnerability reproduction, dependency upgrade or production mitigation was executed. What was executed is the record validator and its repaired review trigger. The distinction keeps a useful practice artifact from becoming a false security claim.

Plan **10–12 hours within the existing 10–15-hour study week** for source reading, your first decisions, the code exercise and a delayed reassessment. These are planning allowances, separate from the fictional team's remediation estimates. For employment, practise defending the decision to a service owner. For consulting, state the evidence available, exclusions, response ownership and retest conditions. Both routes need an explanation of why this action comes first—and what would make that explanation stop being true.
