# Query grammar review

Read after attempting the [worksheet](07-query-worksheet.md). A supplied explanation is assistance, not evidence that you independently found the failure. The hints below help without giving the answer; everything after the stop line is the worked answer.

## Hints, in order

Record each hint you open and when.

**Hint 1 — classify the input.** A title is a comparison value. A sort key chooses query structure. Trace whether the caller's actual characters need to enter the SQL text at all.

**Hint 2 — the program owns the choices.** If the caller's text must not enter the SQL, something the application wrote has to. Ask what finite set of things the contract allows, and what happens to anything outside it before the database is called.

**Hint 3 — inspect both outcomes.** Check 400 for unsupported choices and the exact ID order for valid choices. Empty or unordered results do not preserve the feature. A SQL error is not the intended validation response.

**Stop here if you are still attempting.** Everything below is the worked answer.


The injected apostrophe closes the title literal. The new OR branch makes the tenant-and-title branch insufficient to constrain the whole result. The comment removes the remaining quote and ordering clause. The tenant value was bound correctly; the title was not. Preparing SQL after that concatenation preserves the attack's altered program.

The repaired query binds both values. An attack string becomes an exact title comparison that finds no row. O'Brien remains a valid title and succeeds. A blacklist that drops apostrophes would damage that legitimate case; a class-name search for PreparedStatement would miss the vulnerable branch.

## Review the sort repair

The sort endpoint needs a closed mapping from `id`, `title` and `amount` to code-owned SQL fragments. Unsupported keys produce 400 before query execution. Caller-controlled text must not pass through as the mapped fragment. Retain the tenant parameter and the ID tie-breaker.

The private reference throws a dedicated invalid-sort exception and maps it to 400 at the controller. Validating at the web boundary with `ResponseStatusException(HttpStatus.BAD_REQUEST)` is another sound choice. What is not sound is letting a plain unchecked exception surface: it becomes a 500, not a client error, so the translation must be explicit wherever you put it. The check tests the status contract as well as the query behaviour.

Designs that look finished: always sorting by ID (passes the order cases for the default only and drops the feature); removing the tenant predicate (orders correctly, leaks Birch); binding the requested column name as a value (`ORDER BY ?` with `"amount"` orders by a constant); a loose character check that lets `amount DESC` through because it is "letters and a space"; and letting the database error stand in for validation.

The mapping's outputs may appear in the SQL text because the program owns those exact choices. This is different from concatenating the request value merely because it passed a loose “letters and punctuation” check. Binding `"amount"` as a value is also not a way to ask for the amount identifier.

Before repair, the post-attempt check has four passes and two failures: the valid orderings work; an unknown key and an unapproved expression fail the required status assertions. The private authoring reference passes all six plus the original twenty-two tests. It verifies that the task is solvable while preserving earlier behaviour; it does not establish reader mastery.

Good regressions, since the worksheet no longer lists them: the fixture has no equal titles or amounts, so the original order cases never prove the ID tie-breaker — a tied pair under an isolated fixture does; an unapproved expression of another shape (`id;`, `title--`, a subselect); a whitespace or case policy (`Amount`, ` id`), decided and stated rather than assumed. Retain a negative result against code that violates the chosen property.

## Keep the findings separate

`/api/unscoped/B-2001` deliberately remains an authorization defect even after both query repairs. Its SQL does exactly what it says; it lacks a tenant restriction. Do not describe that remaining access as proof that parameterization failed. Conversely, do not mark the whole application secure because title binding and sort validation pass.

Near transfer, stated plainly: the essay body already teaches that a structural choice needs a closed vocabulary owned by the program, so the mapping idea is not the discovery here. What the task leaves to you is doing it without breaking the guided repairs, deciding where an invalid request becomes a client error and making that translation explicit, and finding a property the supplied contract cannot verify. A model attempt (not a learner) ran on these pages before publication and said the same.

H2 is the tested database dialect. This exercise does not verify your production ORM's raw-query paths, driver configuration, database permissions or other interpreters. A least-privilege database account limits effects but cannot substitute for correctly constrained reads.

A useful handover names the input, interpreter, resulting effect, repair and remaining limitation. It should distinguish code supplied by the book, your independently written regressions and assisted Academy work. If the parser trace is still unclear, return to the assembled SQL before adding another tool or lab.
