# The browser brings authority to the request

Alice signs in to Cedar's document service and opens an invoice. In another tab, she visits an untrusted page. That page submits a form to the document service. The service receives Alice's session cookie, finds that she may approve C-1001, and records the approval.

Nobody stole her password. The tenant check worked. Alice never chose to approve the invoice.

This constructed case changes the question from the previous two essays. We already asked whether the credential belongs at this API and whether its holder may access this resource. A browser session adds another question: what caused this authenticated request to happen?

The [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/06-browser-authority) follows that question through a forged form and an injected script. Both produce the same unauthorized intention using Alice's legitimate authority. They cross different boundaries, so they require different repairs.

## The cookie travels without the password

The Spring application uses form login and a server-side session. Alice signs in with synthetic credentials, `alice` / `local-only`. The browser stores a `JSESSIONID` cookie and sends it on subsequent matching requests. The application resolves that identifier to its session; the request does not need to contain the password again.

Our application runs at `http://127.0.0.1:8083`. A second local server, representing the untrusted page, runs at `http://127.0.0.1:8084`. These are different **origins** because their ports differ. They are nevertheless **same-site**: changing this port does not change the scheme and site used for the SameSite decision. Origin and site answer different questions. [Origin definition](https://developer.mozilla.org/en-US/docs/Web/Security/Defenses/Same-origin_policy#definition_of_an_origin), [SameSite cookie behavior](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie#samesitesamesite-value)

For a domain name, the site normally uses the scheme and registrable domain. An IP address has no registrable domain, so the site uses its scheme and exact host instead: here, `http` and `127.0.0.1`. [HTML site algorithm](https://html.spec.whatwg.org/multipage/browsers.html#site)

The lab explicitly sets the session cookie to `HttpOnly` and `SameSite=Strict`. Chromium still sends it during our same-site forged submission. Strict constrains cross-site sending; it does not mean “only requests initiated by this origin.” Cookies also have no port isolation. The fixture on 8084 neither logs nor uses cookie values, but it is not a model of a separately cookie-isolated attacker host.

That limitation matters. This experiment isolates a cross-origin write, not the full deployment behavior of hostile sibling domains. A production sibling-host case also depends on domain scoping and the surrounding topology. Do not turn these two loopback ports into a claim that every unrelated website receives a Strict cookie.

The relevant request is ordinary form data:

```http
POST /api/approve HTTP/1.1
Host: 127.0.0.1:8083
Origin: http://127.0.0.1:8084
Sec-Fetch-Site: same-site
Cookie: JSESSIONID=<Alice's synthetic session>
Content-Type: application/x-www-form-urlencoded

invoice=C-1001
```

The server can recognize Alice and authorize the invoice while still missing the forged initiation. That is the failure called cross-site request forgery, or CSRF. The historical name does not restrict the mechanism to requests that modern cookie rules classify as cross-site.

## Watch the write before interpreting the console

From the lab directory, use Java 21, Maven and Node.js. The README contains full setup, including the pinned browser dependency. First run:

```sh
mvn test
mvn -q package -DskipTests
npm ci
npx playwright install chromium --only-shell
npm run test:browser
```

The browser runner starts and stops its own application and untrusted-page servers. Leave ports 8083 and 8084 free. For manual exploration, start the deliberately vulnerable configuration:

```sh
java -jar target/browser-authority-1.0.jar \
  --lab.csrf=false --lab.safe-render=false
```

In another terminal run `node attacker.mjs`. Sign in at port 8083, then visit port 8084 in the same browser and press **Submit forged approval**. The form navigates to a response with `"approved":true`. Its button represents the attacker causing submission; no interaction with the trusted approval form is required. An attacker-controlled page could submit automatically.

Now inspect the automated simple-fetch case. It sends the same form body from port 8084 using `credentials: 'include'`. In vulnerable mode the browser refuses to expose the response to that page's JavaScript, yet the subsequent state check finds the invoice approved. A console CORS error and a successful server-side effect coexist.

CORS controls when browser code can access a cross-origin response. Some requests additionally require a successful preflight before the actual request is sent. Our form-shaped POST does not need that preflight. Our separate fetch using JSON and a custom header does: without permission, Chromium sends no approval POST. This is why “CORS stops requests” is too imprecise to guide a repair. Ask which request shape was sent and whether the state changed. [Fetch CORS protocol](https://fetch.spec.whatwg.org/#http-cors-protocol)

This application grants no cross-origin response access. If a product needs such access, authorize specific trusted origins and credential use deliberately. Reflecting arbitrary origins while permitting credentials can expose authenticated responses, including material intended to protect actions. CORS is also a browser restriction, not an authorization check against arbitrary HTTP clients. [CORS and credentials](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS#requests_with_credentials)

## Give the form evidence the foreign page cannot supply

Restart with CSRF protection enabled and rendering still unsafe:

```sh
java -jar target/browser-authority-1.0.jar \
  --lab.csrf=true --lab.safe-render=false
```

Sign in again; restart discards the in-memory session. The forged form now receives `403`, and the invoice remains unapproved. Alice's own approval button still succeeds.

The vulnerable configuration explicitly disabled Spring Security's CSRF filter. The repair keeps its default protection:

```java
if (csrf) http.csrf(Customizer.withDefaults());
else http.csrf(c -> c.disable());
```

The `else` branch exists only to reproduce the flaw. A real repair would remove that escape hatch. Spring Security 7.1.1 stores the expected token in the session by default and validates protected unsafe requests. Our page receives a `CsrfToken` and places its value in a hidden `_csrf` input. The legitimate form submits that additional evidence; the foreign form supplies neither a valid token nor a way to read one. Missing and invalid tokens are both rejected. [Spring CSRF handling](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

The session cookie and CSRF token have different jobs. The cookie identifies the session and travels automatically when applicable. The token is supplied through the application's page and returned explicitly with the action. It is not proof of a human decision, and it does not replace the invoice permission check. The lab continues to return `404` for B-2001 even when the request has a valid CSRF token.

Keep state changes off safe methods too. The lab rejects `GET /api/approve` with `405` and leaves the state unchanged. Moving approval into a GET handler to make a failing form “work” would change the security property beneath the filter.

## The second page is already inside

With CSRF protection enabled, open the invoice page with an attacker-controlled `note` parameter. The vulnerable renderer does this:

```js
const note = new URLSearchParams(location.search).get('note') || '';
document.getElementById('note').innerHTML = note;
```

The application intends to display a note. `innerHTML` instead asks the browser to interpret that string as HTML. The runnable test supplies an image element with an error handler. When its image fails to load, the handler fetches `/csrf`, reads the token, and posts the approval with that token attached.

All those operations run in the document application's origin. The script does not need to read `JSESSIONID`; the browser attaches it to the same-origin fetches. In the executed CSRF-only mode, the cookie is HttpOnly, the forged foreign form is denied, and the injected script still approves C-1001.

This is DOM-based cross-site scripting, or XSS: untrusted URL data reaches an HTML-parsing sink in page JavaScript. Blocking the foreign form did not remove that sink. The script now has access to the page's own ability to read the token and initiate actions.

A popup would establish that some JavaScript executed. This test establishes a more specific consequence: the injected code reaches the protected business action. It does not steal data, contact an external receiver or establish what a different application would expose.

For a plain-text note, the repair is:

```js
document.getElementById('note').textContent = note;
```

The final mode, `--lab.csrf=true --lab.safe-render=true`, displays the payload literally. No injected image exists, the approval state stays false, and Alice's own form still works. Text containing angle brackets and ampersands remains readable. We changed the interpretation at the rendering boundary instead of trying to enumerate malicious strings. [DOM text insertion](https://developer.mozilla.org/en-US/docs/Web/API/Node/textContent)

That repair has a context. `textContent` fits text inside an ordinary display element. It is not a URL policy, a SQL binding mechanism or permission to put untrusted strings inside executable script. If the product requires rich HTML, use an appropriate maintained sanitizer and a deliberately limited markup policy; escaping everything would change the feature. A carefully deployed Content Security Policy can add protection, but this lab intentionally has no CSP so the sink repair is tested directly. [OWASP XSS prevention guidance](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)

## Three boundaries, three observations

The same browser ran six scenarios in each configuration:

| Observation | Both flaws present | CSRF repaired only | Both guided repairs |
|---|---|---|---|
| Foreign form | 200; approved | 403; unchanged | 403; unchanged |
| Form-shaped fetch from foreign origin | Response blocked; approved | Response blocked; unchanged | Response blocked; unchanged |
| JSON/custom-header fetch | Preflight prevents POST | Preflight prevents POST | Preflight prevents POST |
| Note containing executable markup | Script approves | Script approves | Literal text; unchanged |
| Ordinary note | HTML interpreted | HTML interpreted | Text preserved |
| Alice's own approval form | Succeeds | Succeeds | Succeeds |

The Java suites separately exercise twelve HTTP cases. Running the six repair assertions with CSRF disabled produces exactly two failures: missing and invalid tokens are accepted. Running the six final browser assertions against both vulnerable switches produces four failures. These negative controls establish that the tests distinguish the repaired behavior from the flaws.

This is a synthetic, session-local invoice flag, not a production approval workflow. There is one account, no database, no concurrency model and no claim of browser-wide coverage. The executed browser is pinned Chromium; Safari and Firefox were not tested. Loopback HTTP also does not verify production TLS or Secure-cookie deployment. The intentionally unfinished summary page remains a separate practice boundary even in the guided repaired mode.

The professional skill is tracing authority to its enforcement point. Server-side permission decides which invoice Alice may approve. CSRF protection constrains how a session-authenticated action may be initiated. Safe rendering prevents note data from acquiring the page's scripting authority. Each can work while another boundary fails.

<!--mission-->

## Practice: keep the formatting, remove the interpretation

The lab's `/summary?title=...` page builds a bold title by concatenating input inside `<strong>` markup. Repair it while preserving one real `strong` element and the exact supplied title as text. Keep both guided repairs enabled. Add browser evidence for executable input and for ordinary punctuation, then run the existing suite to check the approval path still works.

Use the [worksheet](../practice/06-browser-worksheet.md) before the [review guide](../practice/06-browser-review.md). The authoring reference was built and executed privately; the public project deliberately leaves this task unsolved.

Budget 10–12 hours: two for reading and prediction, four for reproduction and repair, two for evidence and explanation, and two to four for independent transfer. Stay within the established 10–15-hour week; carry an unfinished task forward instead of adding study debt. Attempt **CSRF vulnerability with no defenses** and **DOM XSS in document.write sink using source location.search** in PortSwigger Academy, linked in the worksheet. Record any hints or AI assistance.

For employment, explain the defect and repair as a focused code-review conversation. For consulting, describe the exact pages, browser and actions assessed, the retest evidence and the untested scope. Both routes need a claim another engineer can verify. Neither route gains evidence of mastery merely because the supplied tests pass.
