# A useful detector has known misses

The rule reports two findings. The HTTP tests demonstrate three vulnerable paths. Both results are correct.

This is a constructed Java lab, deliberately small enough to inspect. A request supplies a document title. Several methods concatenate that title into SQL; others bind it as a value. One vulnerable method delegates execution to a helper. That is an ordinary change in code organization. They also change what our detector can see.

The useful result is a precise account of that difference. You can hand another engineer a rule, explain which regressions it catches, and show a failure that still requires another control. That is the first step from finding individual bugs to maintaining detection.

## Start with a result the scanner did not produce

In [essay 7](07-query-grammar.md), binding kept a title from changing SQL grammar. The [companion lab](https://github.com/AnthonyKot/book21/tree/main/labs/12-known-misses) returns to that mechanism with three synthetic titles: `Budget`, `Payroll` and `O'Brien`. There are no tenant or authentication claims in this fixture. It listens on loopback; keep its intentionally vulnerable endpoints local.

Start it using the README, then send:

```bash
curl -G --data-urlencode "title=' OR 1=1 -- " \
  http://127.0.0.1:8093/search/wrapped
```

The observed response is:

```json
["Budget","Payroll","O'Brien"]
```

The supplied title did not merely fail to match. It changed the predicate and selected other titles. The application executes this path:

```java
public List<String> wrapped(HttpServletRequest request) {
    String title = request.getParameter("title");
    return execute("SELECT title FROM documents WHERE title = '"
        + title + "' ORDER BY id");
}
private List<String> execute(String sql) {
    return jdbc.queryForList(sql, String.class);
}
```

The HTTP reproduction establishes the failure independently of any warning. A scanner can help locate this code; it does not define whether the behavior is acceptable. Keep that ordering when evaluating tools. Otherwise, the set of reported problems quietly becomes your definition of all possible problems.

The lab also contains direct concatenation and a version that first assigns the SQL to a local variable. Both respond to the same input. The wrapper changes the program structure without restoring the boundary between SQL text and title data.

## Give the rule a small contract

We will ask Semgrep to follow values returned by calls named `getParameter` into the SQL argument of `JdbcTemplate.queryForList`. The contract deliberately names one source shape and one sink API. It does not claim to cover every way an HTTP value can reach every database API.

A structural search pattern can recognize a call and its argument shape without depending on indentation. Here we use those patterns inside a *taint rule*: the source marks potentially untrusted data, and the sink marks the expression where that data would become SQL text. The executed rule is:

```yaml
rules:
  - id: request-sql
    languages: [java]
    severity: WARNING
    message: Request data reaches SQL text. Verify the flow and bind values separately.
    mode: taint
    pattern-sources:
      - pattern: $REQ.getParameter(...)
        exact: true
    pattern-sinks:
      - patterns:
          - pattern: (org.springframework.jdbc.core.JdbcTemplate $J).queryForList($SQL, ...)
          - focus-metavariable: $SQL
```

`$REQ`, `$J` and `$SQL` are pattern variables. The typed receiver narrows the sink to the specified JDBC class. The source pattern is broader: it recognizes the method name without requiring a servlet receiver type. A different class with that method name could therefore need triage. Narrowing sources is a possible later refinement, with new fixtures to check its consequences.

The important line is the focus on `$SQL`. The later arguments can legitimately contain request data. Treating the entire call as the security-sensitive expression would blur the distinction the previous SQL lesson established. There is no universal sanitizer here: a bound argument is kept outside SQL text rather than blessed as safe for every future use.

Taint analysis follows the local assignment in the alias case, so the rule reports it even though the request-reading call and database call occupy different statements. Semgrep documents this source/sink model and its distinction between local and interprocedural analysis. This lab pins **Semgrep 1.177.0**, and its scan JSON records `engine_requested: OSS`. We ran Community Edition, without enabling Pro analysis. [Taint analysis documentation](https://docs.semgrep.dev/writing-rules/data-flow/taint-mode/overview).

## A green test has a denominator

From the lab directory, after installing the pinned requirements:

```bash
semgrep scan --metrics=off --disable-version-check --test rules/
python3 check.py
```

The first command checks annotated rule examples. `ruleid` marks a required finding; `ok` marks a required non-finding. The wrapper example uses `todoruleid`, which records an intended future improvement without making the current test fail. Semgrep's displayed `1/1` is one rule test group, not a count of secure programs. [Rule-testing documentation](https://docs.semgrep.dev/writing-rules/testing-rules).

The second command scans the actual runnable application and checks exact finding locations. It rejects scanner errors, an unexpected engine/version, missing scanned Java files and unexpected findings. Its matrix deliberately distinguishes vulnerability from expected detection:

| Application case | Injection demonstrated? | Base rule reports it? |
|---|---|---|
| Direct concatenation | Yes | Yes |
| SQL assigned locally, then executed | Yes | Yes |
| SQL passed to execution helper | Yes | No |
| Parameter bound separately | No, tested attack stays data | No |
| Repaired endpoint, default configuration | No, tested attack stays data | No |
| Constant query ignoring the request | No request influence in this fixture | No |

Six matrix checks pass. Two of the three known vulnerable cases are reported; none of the three SQL-safe cases is reported. These counts describe this authored set. It was chosen to expose boundaries, not sampled to estimate production precision or recall. Passing an expectation that says “miss this vulnerable case” documents a limitation; it does not resolve it.

Run `python3 check.py --negative` as a separate control. It replaces the rule's sources with one that matches nothing, so the two positive cases must fail. A harness that still passed would not protect the rule's useful behavior.

## Fix the application while maintaining the detector

The repaired endpoint uses:

```java
return jdbc.queryForList(
    "SELECT title FROM documents WHERE title = ? ORDER BY id",
    String.class, request.getParameter("title"));
```

The attack now returns `[]`. An ordinary `Budget` search still returns that title; `O'Brien` works without stripping its apostrophe. The JDBC overload passes arguments separately to a prepared statement. [Spring Framework 7.0.9 API](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html#queryForList(java.lang.String,java.lang.Class,java.lang.Object...)).

The HTTP suite reproduces the attack and an ordinary request on each vulnerable route, and checks attack handling and legitimate titles on the bound routes. With `lab.bind=false`, the repaired route deliberately calls the concatenating implementation: its attack and apostrophe checks fail, while its ordinary-title check still passes. That negative control explains why an ordinary success alone would have been weak evidence.

The vulnerable variants remain available for teaching. “Repaired endpoint” does not mean every route in this application is repaired. Nor does the absence of a finding prove the repair. We trust this particular conclusion because the code separates SQL and values, the relevant behavior was executed, and removing the control changes the result.

A finding deserves the same discipline in the other direction. Inspect the source, the actual sink argument, transformations and reachability. Confirm the defect before proposing a patch. Avoid an automatic replacement that changes a JDBC overload or parameter ordering without application tests.

## The missing edge is a design question

In the wrapper, the recognized source is in one method and the JDBC sink is in another. Our local rule does not establish that cross-method connection. Moving the same SQL into a helper consequently removes the finding without removing the injection.

One response is to model that particular helper as a sink in the calling method. That may be appropriate for a small, stable internal API, provided its argument really is executable SQL and its callers are tested. It also creates maintenance work when the helper changes. Another response is analysis that follows calls across methods.

CodeQL's Java libraries offer global data flow and global taint tracking. For this example, the latter is relevant because concatenation changes the value while retaining input influence. A proposed analysis would select request-derived values as sources, select the SQL argument as a sink, and follow the call into `execute`. A path result would make that connection inspectable. Global analysis requires more resources and can lose precision; its usefulness still depends on extraction, library models and source/sink definitions. [CodeQL Java data-flow guide](https://codeql.github.com/docs/codeql-language-guides/analyzing-data-flow-in-java/).

**That is a source-backed investigation plan, not an executed CodeQL result.** No CodeQL database or query was run for this essay. Whether a particular query catches this exact fixture remains unmeasured. Semgrep also documents interprocedural capabilities in its Pro engine; the observed miss belongs to our rule and engine configuration, not to every configuration of the product.

A clean report can therefore support a narrow statement: these files were scanned with this rule and configuration, and no modeled path was reported. It cannot answer the tenant-cache question from [essay 11](11-security-property.md), or establish authorization merely because a query binds its values.

<!--mission-->

## Practice: the code moved on, the rule did not

The lab now contains a reporting module written after the rule. Its five query methods take input from requests in ways the guided cases did not cover. The rule's current results on that module are a measurement, not a verdict: some findings may be missing and some may be wrong.

The [worksheet](../practice/12-detector-worksheet.md) sets out the deliverables:

1. A truth ledger for the module's cases, each entry backed by an HTTP observation and the code path, established before you trust any scanner output.
2. A rule change, or a decision not to change the rule, for each difference between the ledger and the findings, with annotated rule examples that pin the behavior you chose.
3. A measured matrix over the guided and new cases that loses no guided finding and adds no false alarm on guided safe code.
4. A coverage statement another engineer can challenge: what the rule now reports, what it deliberately misses or over-reports, and who must review what it cannot see.

Attempt it before opening the [review guide](../practice/12-detector-review.md), which holds hints, the authored ledger and a post-attempt check. The worksheet's time estimates are provisional. A clean scan of this module would prove as little as it did for the wrapper: the useful result is a rule whose blind spots are named.
