# Fetch authority worksheet

Status: blank practice artifact. Fill it in before opening the review guide, which holds the hints and the worked answer. Reproduce and repair the guided lab from the [essay](../essays/08-fetch-authority.md) first; this task extends it. Use only the lab's synthetic loopback listeners and the assigned Academy instances. Do not probe workplace networks or cloud metadata services.

## Time plan

Provisional; no learner has reported actual times. Maven and Java are already installed for the guided lab, so setup here is only rereading and running the fixture.

| Part | Work | Estimate |
|---|---|---|
| Setup | Reread the essay; run the guided lab once | 0.25–0.5 h |
| A — guided questions | Three questions about the guided repair | 0.5–1 h |
| B — independent | Extend `/api/follow-one` and prove it | 3–5 h |
| Delayed check | Two to four days later, without your notes | 0.5 h |
| Required total | | 4.25–7 h of a 10-hour week |
| C — optional transfer | Two named Academy labs | 2–4 h |

Stop rules: if after about an hour of Part B the guided lab still fails to build or run, record the blocker and move on. If after about three hours you cannot make the mirror path work while keeping the internal origin unreachable, open Hint 1 in the review guide, record it as assistance, and continue. Carry unfinished work forward rather than reading the worked answer.

## Part A — guided questions

Answer from the guided lab you already reproduced.

1. The vulnerable configuration validates the initial URL and still reaches the internal listener. Name the exact step at which the destination it connects to stopped being the destination it checked.
2. Why does asserting "the response body does not contain the report" fail to prove the internal service was not contacted? What does the lab assert instead?
3. The outbound request does not copy Alice's credential, yet the SSRF still matters. Why?

## Part B — extend the approved destinations

### The new requirement

The integration now has **two approved origins**: the original partner and a new documents **mirror** (a second loopback fixture the lab already runs). The feature must retrieve documents from either. For some documents the partner returns a single `302`: to the mirror's document (an absolute `Location` on the other approved origin) or to its own document (a relative `Location`). Both must be honoured.

Unchanged and non-negotiable:

- The forbidden **internal** origin must never be contacted, whether requested directly or reached through a redirect. Denial must happen before the internal listener receives a request.
- The rejections already in the guided lab stay rejected: wrong scheme, user-information in the URL, a fragment, a look-alike host, a malformed URI.
- The outbound request carries no caller credential.
- At most one redirect is followed. An endless or multi-step redirect must not hang the endpoint or keep issuing requests.
- Automatic following stays off for this endpoint whatever the guided demo's `lab.follow-redirects` flag says. That flag exists to show the vulnerable mode of `/api/fetch`; the practice endpoint decides every destination itself.

The endpoint to change is `/api/follow-one` in `FetchService.followOne`. Its starter reuses the guided single-origin fetch, so today it refuses the mirror and every redirect. How you structure the destination policy and map errors is your decision; the guided contract uses `403` for a denied caller destination and `502` for an upstream result the fetch cannot accept.

### What to hand in

1. **Destination policy.** State, in a sentence or two, which destinations this feature now permits and on what basis you decide a URL is one of them. Note one plausible policy you rejected and why.
2. **Implementation.** Your `followOne`, keeping automatic following disabled and the guided `/api/fetch` behaviour intact.
3. **Your own tests.** Decide what needs covering from the requirement: every approved path, every route to the forbidden origin, and the bound. Assert server-side hit counters, not just response bodies. Say in one line why your set is complete.
4. **A negative control.** Break your own policy in one deliberate way, show a test that now fails (for example it records an actual internal hit, or exceeds the one-redirect budget), then restore it. Describe the broken version in one line.
5. **Untested limits.** Name at least two properties your tests do not establish (for example DNS resolution and pinning, egress restrictions, IPv6, response-size or time budgets).

A post-attempt check in two parts (`FetchPolicyCheck`, `FetchPolicyDemoFlagCheck`) encodes the required observable outcomes. Leave both closed until you have saved your own attempt and tests; they are excluded from the default `mvn test`. Run them with:

```sh
mvn -DreviewCheck=true -Dtest='FetchPolicy*' test
```

Any design that produces the required outcomes passes; it does not grade your policy statement, your own tests or your stated limits. Success means another engineer can read your policy sentence, your tests and your negative control and see why the mirror is reachable and the internal origin is not.

## Part C — optional transfer

Attempt without opening solutions first, using only the assigned Academy instances. Record hints or AI assistance.

- [Basic SSRF against the local server](https://portswigger.net/web-security/ssrf/lab-basic-ssrf-against-localhost).
- [SSRF with filter bypass via open redirection](https://portswigger.net/web-security/ssrf/lab-ssrf-filter-bypass-via-open-redirection).

For each, name where the application borrows authority and which check a redirect crosses. These are verified assignments, not completed reader work.

## Your evidence

- Date and time spent on A, B and C:
- Hints opened (which, when and why):
- The policy you rejected, and what decided it:
- Your negative control: the one-line broken version and the test that caught it:
- Delayed check (two to four days later, without notes): from memory, write the destination policy and the one redirect rule, then diff against your saved answer. What did you forget?
- Next unanswered question:
