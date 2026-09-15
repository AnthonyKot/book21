# Review the detection claim

Compare with your saved [worksheet](12-detector-worksheet.md). This guide gives reasoning and expected outcomes; using its hints makes the attempt assisted.

## Two separate truths

The request must not change the structure of the title predicate. The vulnerable direct, alias and wrapper routes all return other titles for the attack. The local taint rule reports only direct and alias: its source and JDBC sink are in the same method. The wrapper remains vulnerable when the scanner is silent.

Bound values do not become SQL text. `O'Brien` checks legitimate punctuation as well as parameter handling; a repair that simply rejects every apostrophe would lose useful behavior. The default repaired route passes the malicious and legitimate cases. Its negative control deliberately calls the direct implementation and must fail the attack and apostrophe tests.

The base matrix expects nine observations and passes all nine while reporting just two of five known vulnerable cases. The four SQL-safe cases are bound, repaired in the default configuration, constant, and header-bound. These authored counts are neither a representative benchmark nor a claim about authorization.

## Progressive hints for the header task

1. First compare the value-producing expression in `HeaderCases` with `pattern-sources`. Did the JDBC sink change at all?
2. The source list can recognize an additional request-reading call. Preserve the existing parameter source and the sink's focus on the SQL expression.
3. Test a concatenated header value and a separately bound header value. If both report, inspect which part of the JDBC call your sink marks. Adding a source does not establish a call graph through the helper.

The executed private reference extends the source alternatives and adds positive/negative header annotations. Its rule test group passes, and the exercise matrix passes 9/9: three vulnerable cases reported, two vulnerable wrappers missed, zero of four SQL-safe cases reported. The public starter fails only the newly required local header finding. Its extended negative control removes both source names and fails the three required findings (3 failures / 6 passes). The additional variation you choose is not included in those reference counts.

Do not interpret `todoruleid` as an exemption accepted by a security owner. It is a test annotation for a known improvement. A retained limitation needs an explicit review or investigation plan in the handover.

## Deciding what deeper analysis would add

A wrapper model can mark its SQL argument as a sink at the caller, if that accurately reflects the helper contract. Its usefulness must be checked against safe callers and future changes. Global taint tracking instead offers a way to follow influenced values across methods, including non-value-preserving steps such as concatenation. CodeQL's source/sink and path-query facilities support investigating that connection.

The essay did not execute CodeQL. “This configuration should model the edge” is a proposal. “This query reports the path on this extracted database” requires an actual recorded run. Account for extraction coverage, library models, source/sink definitions and resource cost before relying on a negative result.

A useful handover might say: “Our local rule reports the two parameter-to-SQL flows and the new local header flow in the specified fixture revision. Bound inputs remain unreported. Both execution helpers still contain demonstrated injection and require manual review or further modeling.” Specify whether you wrote, copied or received help with the rule. No real employer, client, production system or independent learner outcome was assessed here.
