# Browser authority worksheet

Work only against the synthetic [essay 6 lab](https://github.com/AnthonyKot/book21/tree/main/labs/06-browser-authority) and your assigned Academy instances. Start with the [essay](../essays/06-browser-authority.md). Reserve 10–12 hours within your 10–15-hour study week; carry work forward if setup takes longer. These are planning allowances, not measured learner completion times.

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

<details><summary>Hint 1 — find the interpretation</summary>

Trace `title` from `location.search` into the DOM assignment. Determine which operation treats the combined string as markup.

</details>
<details><summary>Hint 2 — preserve structure separately</summary>

Create the required formatting element as structure. Insert the untrusted title through a text-only operation. Avoid concatenating it into the HTML string.

</details>
<details><summary>Hint 3 — inspect the resulting DOM</summary>

Check the title element's exact text, its children and the server-side approval flag. Absence of an alert is weak evidence. The negative control should fail because a security property is violated, not because the server failed to start.

</details>

## Independent transfer

Attempt these without a solution tab first:

- [CSRF vulnerability with no defenses](https://portswigger.net/web-security/csrf/lab-no-defenses): describe the source of the victim's request authority and the evidence needed by a repaired action.
- [DOM XSS in document.write sink using source location.search](https://portswigger.net/web-security/cross-site-scripting/dom-based/lab-document-write-sink): find the changed sink; explain why the local `textContent` repair cannot simply be assumed to exist there.

Use the authorized Academy lab environment only. Lab pages were checked while authoring; completion by the reader has not been reported. If you use hints, walkthroughs or AI, label that attempt assisted and retry a changed case later.

Write a five-sentence handover. For employment, address the engineer reviewing your patch. For consulting, address the owner of a narrowly scoped assessment and include the retest and exclusions. Do not present a local fixture as a comprehensive product assessment.

Then compare with the [review guide](06-browser-review.md).
