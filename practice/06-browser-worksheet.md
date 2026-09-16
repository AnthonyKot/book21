# Browser authority worksheet

Work only against the synthetic [essay 6 lab](https://github.com/AnthonyKot/book21/tree/main/labs/06-browser-authority) and your assigned Academy instances. Start with the [essay](../essays/06-browser-authority.md). Save your attempt before opening the review guide, which holds the hints.

## Time plan

Provisional; no learner has reported actual times. Browser setup is the largest first-time cost and is reused by nothing later in the series.

| Part | Work | Estimate |
|---|---|---|
| Setup | Maven, Node, Playwright Chromium (first time only) | 0.5–1.5 h |
| A — guided | Predictions, three modes reproduced, trace kept | 2–3 h |
| B — independent | The summary title task below | 2–3.5 h |
| Delayed check | Two to four days later, without notes | 0.5 h |
| Required total | | 5–8.5 h of a 10-hour week |
| C — optional | Two named Academy labs | 2–4 h |

Stop rules: if Playwright or its system libraries are not working after about an hour, record the blocker, finish the Java part and carry the browser part over. If after about two hours of Part B the executable-title check still fails, open Hint 1 in the review guide and record it as assistance.

Before executing, predict these separately for a form POST, a form-shaped credentialed fetch and a JSON/custom-header fetch from port 8084:

| Question | Prediction | Observed evidence |
|---|---|---|
| Does the browser send the approval POST? | | |
| Does it attach the session cookie? | | |
| Can the initiating page's script read the response? | | |
| Does the invoice become approved? | | |

Record the origins, whether the request is same-site, the cookie flags and the configuration. Explain why a CORS console error alone does not answer the last question. Do not put real cookie values into your report.

Reproduce all three modes. For CSRF-only mode, explain how the injected script obtains a token without reading the HttpOnly cookie. Identify the exact source, sink and resulting authority. Retain the failing and passing test summaries and one request trace with session values redacted.

## Changed task: a bold summary title

With both guided repairs enabled, inspect `/summary?title=...`. Its title comes from the URL and must appear as literal text inside exactly one `strong` element. Users need to write `<`, `>`, `&` and quotes without losing characters. They do not need to supply their own markup.

1. Predict what the current renderer does with an image error handler embedded in the title.
2. Run `npm run test:browser -- --practice` and explain the failure. A changed title is the input; the ordinary approval remains the protected effect.
3. Repair the rendering boundary. Keep CSRF protection, the trusted page's form and the resource check.
4. Rebuild the JAR; rerun the practice checks and all existing tests. Add a test with a different markup shape or an assertion ruling out extra child elements. Verify that your new test fails against the old renderer.
5. Explain what the fix does not establish about rich HTML, URLs, another page or another browser engine.

Keep a short evidence record: date, versions, time spent, reproduction, patch, allowed/denied outcomes, help used and remaining uncertainty. AI-written code and supplied checks are assisted evidence. Neither reading this worksheet nor passing its tests establishes independent mastery.

Deliverables: the source-to-sink trace in one or two sentences; your patch; the practice checks passing; your own added test with its failure against the original renderer; and the scope statement from step 5.

## Independent transfer (optional)

Attempt these without a solution tab first:

- [CSRF vulnerability with no defenses](https://portswigger.net/web-security/csrf/lab-no-defenses): describe the source of the victim's request authority and the evidence needed by a repaired action.
- [DOM XSS in document.write sink using source location.search](https://portswigger.net/web-security/cross-site-scripting/dom-based/lab-document-write-sink): find the changed sink; explain why the local `textContent` repair cannot simply be assumed to exist there.

Use the authorized Academy lab environment only. Lab pages were checked while authoring; completion by the reader has not been reported. If you use hints, walkthroughs or AI, label that attempt assisted and retry a changed case later.

## Your evidence

- Date and time spent on A, B and C:
- Hints opened (which, when and why):
- Your added test, and what it failed against:
- Delayed check (two to four days later, without notes): from memory, name the sink and the two things the repair had to preserve; compare with your saved answer.
- Next unanswered question:

Then compare with the [review guide](06-browser-review.md).
