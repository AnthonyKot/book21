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

The [lab README](https://github.com/AnthonyKot/book21/blob/main/labs/14-first-fix/README.md) runs a small validator over the three records at the frozen scenario time. Every decision needs an owner, rationale, plan, review time and reopening trigger, and it must reference the packet's current artifact and evidence revision. There is no closure state, because a plan is not proof of remediation. A deferred record also needs a documented basis for the narrow deferral; an unknown path cannot supply one.

The validator reads the supplied classification. It does not discover the deployment or verify the prose, and a sentence can be present and still be wrong. Advance the explicit clock a day and all three records are rejected, because their review times have passed. That is a control on freshness, not on judgment.

Time is only one way a decision goes stale. A deferral written at 09:00 rests on specific facts: this artifact, this packaging, this ingress. If any of those change before the review time, the decision is describing a deployment that no longer exists. So the lab's scheduler reopens a record when its review time arrives *or* when the current artifact or evidence revision differs from the one the decision cites, and it treats missing comparison evidence as a reason to look, not a reason to relax.

Reopening is a request for a new decision. It does not patch software, prove that an attacker succeeded, or say what the new decision should be. A queue of reopened records is where the triage work starts again, with the same questions as before and different evidence.

A record that passes every check can still be the wrong decision. The validator can insist that a deferral names its basis and expires; only a person can tell whether the basis still holds.

<!--mission-->

## Practice: decide again at 13:00

Four hours later, the evidence has moved. The lab's 13:00 packet updates all three services and adds a fourth scanner match, and the team has limited engineering time for the afternoon. The scheduler reports which records need review. It does not say what to do about them.

The [worksheet](../practice/14-triage-worksheet.md) sets out what the records must contain. Produce:

1. A decision record for each item at 13:00, in the lab's format, which passes the validator.
2. For each item, a short note: what changed, whether the action changes and why, what you would verify before trusting the new evidence, and what would reverse your decision.
3. The order in which the afternoon's engineering time goes, and the cost of that order.

Changing a decision and keeping one are both legitimate outcomes; each needs its reason. Attempt it before opening the [review guide](../practice/14-triage-review.md), which holds a rubric and a worked set of decisions.

The advisories and threat data are the real dated snapshots used above; every deployment fact, owner and timing is fictional. The worksheet gives provisional time estimates. The essay's claim is tested only if your 13:00 records say, for each service, what would make them stop being true.
