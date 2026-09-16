# Browser authority review

Read after attempting the [worksheet](06-browser-worksheet.md). This is a review guide, not evidence that you completed the task. The hints below help without giving the answer; everything after the stop line is the worked answer.

## Hints, in order

Record each hint you open and when.

**Hint 1 — find the interpretation.** Trace `title` from `location.search` into the DOM assignment in `summary.js`. Determine which operation treats the combined string as markup.

**Hint 2 — two things must survive.** The requirement keeps one real `strong` element *and* the exact title text. Ask which of those the note repair's technique preserves on its own, and which needs a separate step.

**Hint 3 — inspect the resulting DOM.** Check the title element's exact text, its children and the server-side approval flag. Absence of an alert is weak evidence. The negative control should fail because a security property is violated, not because the server failed to start.

**Stop here if you are still attempting.** Everything below is the worked answer.


The foreign form can send a credentialed write without permission to read a response in JavaScript. The form-shaped fetch demonstrates the distinction especially clearly: with CSRF disabled, its promise rejects under CORS while the invoice changes. The custom-header request adds a preflight; in this configuration the actual POST is not sent. The two origins are same-site, so Strict does not suppress the session cookie in the tested requests.

With CSRF enabled, missing and invalid tokens must fail before approval. A valid token still must not authorize Birch's invoice. A passing tenant test does not establish deliberate initiation by Alice.

The injected note script runs in the trusted page's origin. It can fetch the token and make the same request the page could make. HttpOnly prevents script access to the session cookie; it does not prevent authenticated fetches. Fix the rendering sink rather than trying to strengthen the CSRF token until it somehow distinguishes trusted and injected same-origin scripts.

## What a summary repair must preserve

A satisfactory implementation constructs the formatting element separately and puts the title into its text content. It replaces the summary contents with that element. The exact title survives, one real `strong` element remains, and attacker-supplied markup creates no executable descendants. Concatenating the title into another HTML string moves the same flaw around.

The supplied executable-title test fails against the public starter; the ordinary punctuation test passes. After the private authoring reference repair, both pass, as do the eighteen original browser scenarios and twelve Java HTTP cases. This verifies the proposed exercise is solvable; the reference implementation is not published as a completed reader attempt.

A good added regression checks a changed element or handler shape and inspects DOM structure as well as the protected state. An arbitrary sleep followed by “no alert appeared” provides weaker evidence than verifying the input became a text node. Run that regression against the original code and retain its meaningful failure.

Scope remains limited to a plain-text title in this element. Rich HTML would require a separate policy and suitable sanitization. Attribute values, URLs, script contexts and other renderers need their own reasoning. Production TLS, session lifecycle and multiple browser engines were not assessed here.

## Evidence and explanation

Your handover should identify the source-to-sink path, show a local reproduction, explain the change, retain legitimate behavior and state the untested scope. Preserve the distinction between supplied examples, your own tests and independent Academy work. A model-assisted patch can be useful without being independent performance evidence.

If the explanation stops at “CORS is broken” or “sanitize input,” repeat the request trace and identify the precise enforcement point. If setup consumes the week, record the next command and resume there. The ten-to-fifteen-hour budget serves sustained practice, not a deadline to claim mastery.
