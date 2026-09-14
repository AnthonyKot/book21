# Fetch authority worksheet

Read the [essay](../essays/08-fetch-authority.md) and run the [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/08-fetch-authority). Use only its synthetic listeners and assigned Academy instances. Do not probe workplace networks or cloud metadata services.

Before running a request, record:

| Question | Prediction | Observation |
|---|---|---|
| Who supplies the initial URL? | | |
| Which component sends the outbound request? | | |
| Who supplies the redirected destination? | | |
| When does the destination policy run? | | |
| Did the internal fixture receive a request? | | |

Compare direct partner content, a direct forbidden URL, a redirect internally and a relative partner redirect in both configurations. Retain status, body and counter changes. Counters are cumulative during manual work; compare before/after or restart. The test harness resets them between sequential cases.

Explain why a 502 response alone cannot prove no internal contact. Explain why the incoming Basic credential is not necessary for this SSRF effect, and verify that the outgoing request does not copy it. Distinguish the policy-forbidden internal fixture from actual network isolation: your terminal can reach it directly.

## Changed task: follow one approved 302

Keep `lab.follow-redirects=false`. Extend `/api/follow-one`; leave the guided `/api/fetch` behavior intact.

1. Validate the initial URI with the existing policy; invalid initial input remains 403.
2. Fetch once. A 200 succeeds. A 302 may supply one additional destination.
3. Resolve Location against the first URI, including a relative path. Validate the resolved URI BEFORE contact.
4. Permit only one additional request. Require its response to be 200. A further redirect, forbidden target, missing/invalid Location or upstream failure becomes 502.
5. Never forward incoming cookies, Authorization or arbitrary caller headers.

Run `mvn -Dtest=OneHopExercise test` before repair. One test fails because the legitimate relative redirect is not supported; four pass. After repair, `mvn '-Dtest=*Test,OneHopExercise' test` should pass all 27 cases.

The supplied tests cover direct success, relative success, forbidden internal redirect, a loop and a chain requiring two redirects. Add a changed regression, such as an absolute allowed Location, a user-info-bearing Location, a missing Location or another redirect status. State the expected behavior before coding it. Use only fixture-generated local destinations. Keep the fixture from becoming an open redirect to arbitrary outside addresses.

Demonstrate that removing your redirect validation makes the no-internal-contact property fail. A meaningful negative control records an actual internal hit, not merely a failed compilation or unavailable server. Do not claim independent discovery from implementing supplied hints.

<details><summary>Hint 1 — the first permission expires at the redirect</summary>

The existing validator already describes the approved origin. The redirect response provides a new input to that same decision. Letting the HTTP client follow automatically skips your opportunity to decide.

</details>
<details><summary>Hint 2 — resolve, then validate</summary>

Use URI resolution against the original URI. A Location may be relative; a string prefix check is not a substitute for inspecting the resolved scheme, host and port. Map a rejected upstream location to the specified 502 response.

</details>
<details><summary>Hint 3 — a loop is a request budget problem</summary>

One initial request and at most one follow-up are enough for this contract. Do not recurse until something returns 200. Check both server hit counts and final status.

</details>

## Independent transfer

Attempt without opening solutions first:

- [Basic SSRF against the local server](https://portswigger.net/web-security/ssrf/lab-basic-ssrf-against-localhost).
- [SSRF with filter bypass via open redirection vulnerability](https://portswigger.net/web-security/ssrf/lab-ssrf-filter-bypass-via-open-redirection).

Use only the assigned Academy instances and record hints or AI assistance. These are verified assignments, not completed reader work. Explain where each application borrows authority and which check a redirect crosses.

Plan 10–12 hours inside the 10–15-hour week. Keep an evidence pack with versions, commands, allowed/denied requests, counter changes, patch, negative control and untested limits. If interrupted, record the next command and continue later.

For employment, write a concise code-review explanation. For consulting, delimit the assessed fetch workflow and explicitly exclude untested DNS, proxy, firewall and workload-resource controls. Compare your work with the [review guide](08-fetch-review.md) after attempting it.
