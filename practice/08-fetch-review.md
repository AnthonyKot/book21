# Fetch authority review

Open this only after saving your worksheet attempt and your own tests. Sections 1 and 2 help without giving the answer. Sections 3 onward contain the worked answer; read them after you have finished or decided to stop.

## 1. Judging any answer

A supplied explanation or a passing reference is not learner evidence. Judge an attempt, including your own, by these questions:

- **The policy is stated, not just coded.** Can you read one or two sentences that say which destinations are approved and how a URL is decided to be one of them?
- **The decision is enforced where the connection happens.** Is every destination that will actually be contacted, including a redirect target, checked before the request goes out?
- **The safe and unsafe cases are both exercised.** Does the evidence show the mirror reachable *and* the internal origin unreachable, by server-side hit counters rather than response text?
- **The negative control fails for the stated reason.** When the policy is deliberately broken, does a test fail and record a real internal hit, not merely a compile error or a changed body?
- **The bound is real.** Does a redirect loop stop without hanging the endpoint or issuing unbounded requests?
- **Limits are named.** Does the write-up say what the tests do not establish?

## 2. Hints, in order

Record each hint you open and when.

**Hint 1 — what does "approved" compare?** The guided check compared one literal host and port. With two approved origins, the check needs to accept a small set. Ask what all four fixtures have in common and what tells them apart.

**Hint 2 — check the destination you will connect to.** A redirect's `Location` produces a new destination. The decision that guarded the first request has to run again on that new destination, before the second request is sent. A relative `Location` must be resolved against the first URI before you can judge it.

**Hint 3 — a loop is a budget, not a puzzle.** One initial request and at most one follow-up satisfy this feature. Do not follow redirects until something returns `200`; decide what a second redirect means and stop.

**Hint 4 — prove contact, not content.** Assert the internal and mirror hit counters. A denied redirect should leave the internal counter at zero; an approved redirect to the mirror should leave a mirror hit.

**Stop here if you are still attempting.** Everything below is the worked answer. Return to your worksheet and record which hint you used.

## 3. What the task requires

The initial URL can be valid while its response directs the client elsewhere. The guided vulnerability authorizes the first URI and then follows automatically; the guided repair refuses every redirect and knows one origin. This task keeps the no-automatic-following rule but widens the approved set to two origins and permits one redirect between them.

A sound `followOne`:

1. Validates the initial URI against a policy that admits **either** approved origin and rejects everything else, including user-information, fragments, wrong scheme and malformed input.
2. Makes one request with automatic following disabled.
3. On a `302`, reads `Location`, resolves it against the first URI (so a relative path becomes absolute), and validates the resolved URI against the **same** policy before any second request.
4. Makes at most one follow-up and rejects a further redirect rather than following it.
5. Maps a denied caller destination to `403` and an unacceptable upstream result to `502` (the fixture's contract, not universal SSRF codes).

The private reference expresses the policy as an allow-list of approved origins matched on scheme, host **and** port. That last word is the point of this fixture.

## 4. Designs that look finished, and what still happens (executed)

Each was run against the post-attempt checks (`mvn -DreviewCheck=true -Dtest='FetchPolicy*' test`); the run logs are kept in the author's evidence, not published.

- **Resolve nothing (treat `Location` as the destination).** A relative `Location` such as `/document` carries no host, so a design that validates and sends it without resolving against the current URI never reaches the approved document. Executed: the approved relative-redirect case fails while the absolute mirror redirect still passes, so a check that only tried an absolute redirect would miss it. Resolve first, then validate.
- **Match on host only.** Because every fixture is on `127.0.0.1`, an allow-list that compares the host and forgets the port admits the internal origin. Executed: `directInternalDenied` and `redirectToInternalNeverContactsInternal` both fail with one real internal hit. This is the trap the shared address sets; comparing host **and** port closes it.
- **Follow the redirect without revalidating the target.** Validating the first URI and then following whatever `Location` says reaches the internal listener through the partner's redirect. Executed: `redirectToInternalNeverContactsInternal` fails with a real internal hit while the direct-internal case still passes, so a test that only tries the direct path would miss it.
- **Follow redirects with no hop limit.** Resolving and revalidating each hop but looping until a `200` never terminates on the partner's self-redirect: the endpoint keeps issuing requests and hangs. Executed: the loop case does not return; the server is now a denial-of-service against itself. One follow-up, then stop.
- **Reuse the demo's HTTP client.** The guided `/api/fetch` client follows redirects when `lab.follow-redirects=true`. A `followOne` that sends through that same client is only safe while the flag is off: switch the demo on and the client auto-follows the partner's redirect to the internal origin before your revalidation runs. Executed: under the flag, `demoFlagDoesNotReopenInternalViaPractice` fails with a real internal hit; the same code passed every check with the flag off. Give the practice endpoint its own never-follow client. A blind model attempt found this before the author did.
- **Reject every redirect (the guided repair, unchanged).** Safe, but the mirror document delivered via the partner's redirect never arrives, so the new requirement is unmet. Executed: the mirror-redirect case fails as a `502`.
- **Hide the body after contact.** Returning `502` while still letting the client reach the internal origin leaves a real internal hit. Checking the counter, not the body, is what distinguishes this from a real fix.

## 5. Reference evidence

Guided suite unchanged: 22 pass in both the lab and the reference. The reference `followOne` passes the eight-case `FetchPolicyCheck` and the two-case `FetchPolicyDemoFlagCheck`. The variants above are the executed negative controls; their logs are part of the author's evidence.

Your own tests need not match the reference. A good set exercises the mirror path (direct and via redirect), the internal origin (direct and via redirect), the one-redirect bound, and a negative control that fails for your stated reason.

## 6. Limits

Near transfer: the essay teaches the redirect mechanism and the destination check on one origin, and the task applies both to two origins over a redirect. It is not diagnosis of an unknown vulnerability class. The fixture uses literal loopback addresses and fixed handlers; it does not test DNS resolution or pinning, separate network reachability, egress policy, IPv6, response-size or time budgets, or production credentials. Passing tests support the redirect and destination behaviour you exercised and nothing more. The requirement list is deliberately complete, and the guided `allowed()` check already compares a port, so part of the task can be reached by generalising visible code; the checks do not grade whether you can say why the port matters, which is what the policy statement and rejected alternative are for. A model attempt (not a learner) was run on these pages before publication; its feedback is recorded in the revision notes. Real study time and learning are unmeasured.
