# Query grammar review

Read after attempting the [worksheet](07-query-worksheet.md). A supplied explanation is assistance, not evidence that you independently found the failure.

The injected apostrophe closes the title literal. The new OR branch makes the tenant-and-title branch insufficient to constrain the whole result. The comment removes the remaining quote and ordering clause. The tenant value was bound correctly; the title was not. Preparing SQL after that concatenation preserves the attack's altered program.

The repaired query binds both values. An attack string becomes an exact title comparison that finds no row. O'Brien remains a valid title and succeeds. A blacklist that drops apostrophes would damage that legitimate case; a class-name search for PreparedStatement would miss the vulnerable branch.

## Review the sort repair

The sort endpoint needs a closed mapping from `id`, `title` and `amount` to code-owned SQL fragments. Unsupported keys produce 400 before query execution. Caller-controlled text must not pass through as the mapped fragment. Retain the tenant parameter and the ID tie-breaker.

The private reference throws a dedicated invalid-sort exception and maps it to 400 at the controller. Web-layer validation is another suitable choice. The exercise tests the status contract as well as the query behavior.

The mapping's outputs may appear in the SQL text because the program owns those exact choices. This is different from concatenating the request value merely because it passed a loose “letters and punctuation” check. Binding `"amount"` as a value is also not a way to ask for the amount identifier.

Before repair, the supplied sort suite has four passes and two failures: the valid orderings work; unknown input and an unapproved expression fail the required status assertions. The private authoring reference passes all six plus the original twenty-two tests. It verifies that the task is solvable while preserving earlier behavior; it does not establish reader mastery.

The fixture has no equal titles or amounts, so passing its original order cases does not prove tie handling. A useful extra test could introduce a tied pair under an isolated test fixture and assert ID order. A different expression is another useful transfer check. Retain a negative result against code that violates the chosen property.

## Keep the findings separate

`/api/unscoped/B-2001` deliberately remains an authorization defect even after both query repairs. Its SQL does exactly what it says; it lacks a tenant restriction. Do not describe that remaining access as proof that parameterization failed. Conversely, do not mark the whole application secure because title binding and sort validation pass.

H2 is the tested database dialect. This exercise does not verify your production ORM's raw-query paths, driver configuration, database permissions or other interpreters. A least-privilege database account limits effects but cannot substitute for correctly constrained reads.

A useful handover names the input, interpreter, resulting effect, repair and remaining limitation. It should distinguish code supplied by the book, your independently written regressions and assisted Academy work. If the parser trace is still unclear, return to the assembled SQL before adding another tool or lab.
