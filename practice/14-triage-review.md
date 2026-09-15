# Triage decision review

Open this after saving your [worksheet](14-triage-worksheet.md) records and notes. Everything below counts as assistance once you have read it. If you are stuck partway, read only the next hint in section 2 and record that you did.

## 1. Judge the attempt before comparing decisions

No single decision set is correct. A sound 13:00 triage shows the following.

**Evidence, not the old record.** Each rationale cites what changed at 13:00 and what did not. Copying a 09:00 rationale forward with new timestamps is the main failure this task is designed to expose: the lab's validator accepts exactly that, which is why the validator cannot judge the work.

**Prerequisites read against the advisory.** For each CVE, the note says which vendor-listed conditions the evidence shows present, absent or unknown, and what artifact, configuration or access test would confirm the supplied facts. A scanner match is treated as a reason to look, not as a verdict.

**Containment treated as work with a cost.** Where an action removes or keeps removed a legitimate function, the note names who loses it and what verified protection would justify restoring it. Restoring a route because a deadline or a request arrived, without new protection evidence, is not sound.

**Missing containment escalated.** A requested control that has not happened is recorded as not in place, with an escalation and a checkpoint, rather than assumed.

**A real order for the afternoon.** The order says what is delayed and what risk that accepts. "Everything is urgent" without an order does not use the capacity information.

**Reversal conditions that could actually occur.** Each record names evidence that would change the decision, specific enough that someone else could recognise it.

**A stated deferral basis that the evidence supports.** The validator accepts any listed basis; the note has to show that the vendor conditions behind it are actually absent or blocked in this deployment.

**A handover someone could act on.** The note leads with the afternoon's decisions and order, names owners and checkpoints, separates supplied evidence from what was verified, and states what would reopen each decision. A consulting version adds the evidence available to the assessor, exclusions and the retest that would confirm containment. A handover that restates the records without the order and its costs is not yet useful.

## 2. Progressive hints, if you are stuck

1. Put the 09:00 rationale for each record next to its 13:00 evidence. Mark each sentence of the old rationale as still true, now false or not addressed.
2. For each CVE, list the advisory's conditions as a checklist and fill it in from the 13:00 evidence only. Then ask whether any control assumed at 09:00 has actually been verified since.
3. Rank the items by what could go wrong this afternoon if nobody touched them, then by how quickly each can be made safer. Where those rankings disagree, the order needs a stated reason.

## 3. What the 13:00 evidence changes

**R3, legacy-preview: the decision reverses.** The retrieved deployment is a WAR on Tomcat with JDK 11 and spring-webmvc 5.3.17 on a public endpoint: every condition the vendor lists for the described exploit is present, and the CVE is KEV-listed with a very high EPSS. "Investigate" no longer describes the work. The requested restriction has not been applied, so containment is missing, not pending. A sound record escalates containment now, rather than waiting for the edge team at 14:00, including an explicit, owner-accepted option to take the public route offline, and starts the upgrade. Deferral has no basis in this evidence. The validator rejects a deferral that states no basis, but it would accept a false one, so this is a judgment the note has to carry.

**R2, document-files: the action holds, the plan changes.** The code still meets the advisory's conditions (functional static resources, filesystem location, Reactor Netty, no HTTP Firewall). Containment stopped the traversal request but also broke a customer's legitimate downloads. Re-enabling the unchanged route would restore the exposure. Sound plans keep it disabled while the upgrade's failing test is fixed. If service must return sooner, it comes back only behind a protection the advisory names or an equivalent verified control, with both the forbidden request and the Birch Legal downloads re-tested. Communicating the outage window is part of the plan.

**R1, ledger-admin: the basis of the deferral weakened.** Packaging still blocks the described exploit: same executable JAR. But the 09:00 rationale also relied on restricted administrative ingress, and the audit found a VPN range and 41 external support accounts behind it. Two responses are defensible. One restricts ingress now and accelerates the upgrade into today's 16:00 integration slot. The other keeps a short deferral to that slot only after ingress is restricted and verified. Renewing the 09:00 deferral unchanged is not sound: one of its stated facts is false.

**R4, report-renderer: a match that does not justify the afternoon.** The advisory requires RouterFunctions static resources with a FileSystemResource location; the deployed route listing shows neither, and the service is internal. A narrow deferral to routine upgrading is sound if the note says what was verified: that the listing came from the same artifact as the match. It should also name a reopening trigger, such as resource handling being added or ingress changing. Spending one of the two free engineers here would take time from R3 or R2.

**Order.** R3 containment first (public, prerequisites present, no control in place). R2's upgrade next (contained, but a customer is without downloads). R1's ingress restriction is a quick change that fits alongside; its upgrade waits for 16:00. R4 takes no afternoon slot. Reasonable alternatives exist, such as putting R1's ingress change first because it takes minutes; the point is a stated tradeoff.

## 4. Records that look finished

Each was run through the validator against the 13:00 packet.

| Decision set | Validator | Why it still falls short, or does not |
|---|---|---|
| The four worked records described in section 3 | Accepts all four | Meets the rubric; alternatives in section 3 are also sound. |
| The 09:00 records renewed with new evidence links, a 17:00 review and R1's old basis restated, plus R4 as patch-now on "scanner match" | **Accepts all four** | Keeps R3 investigating a confirmed public exposure, renews R1 on a disproved fact, and spends effort on R4 with no evidence. Hygiene passes; judgment fails. |
| The worked records with R3 changed to deferred and no basis stated | Rejects R3: no documented basis | The contract catches a deferral that names no basis. |
| The worked records with R3 deferred on a stated basis of `prerequisites-absent` | **Accepts all four** | The basis is false: the retrieved deployment shows every prerequisite present. Only the note and a reviewer can catch this. |

## 5. Reference evidence

Recorded while authoring. None of it is evidence of your attempt.

- `contract_tests.py`: 23 pass (13 contract, 7 scheduler, 3 update-packet checks).
- 09:00 records accepted at 09:00; rejected at 09:00 the next day (all three expired).
- Scheduler at 13:00: R1 evidence changed; R2 and R3 review time reached and evidence changed (R3 artifact too); R4 has no record.
- The four decision sets in section 4 produced the validator results shown.

## 6. Limits

The advisories and threat data are real dated snapshots; every deployment fact, capacity figure and timing is fictional. Nothing was scanned, exploited or patched. The rubric judges reasoning against supplied evidence; it cannot verify that a real deployment matches its evidence record. A real triage would also ask for things the packet omits, such as exploitation logs, a working rollback and the service owners' own risk acceptance.
