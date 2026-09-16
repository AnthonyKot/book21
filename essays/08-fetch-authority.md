# A fetch endpoint borrows the server's network access

The document service refuses a URL for an internal report. The same service returns that report when the request starts at an approved partner address.

The partner responds with a redirect. The HTTP client follows it.

This constructed case has a destination check, an authenticated caller and an ordinary GET request. No SQL expression or script changes meaning. Instead, the application lets another response decide where its next request goes. The check authorized the starting point; the client treated that as permission to continue somewhere else.

The [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/08-fetch-authority) adds a partner-document fetch to the fictional service. Its rule is narrow: Alice may fetch from one configured partner origin. She may not use this feature to reach the synthetic internal-report origin. Both are local fixtures, so the whole failure reproduces without touching an actual private network or cloud service.

## Follow the second request

Three listeners run on loopback: the Spring API that authenticates Alice and fetches a submitted URL, a partner fixture that returns a document or a fixed redirect, and an internal fixture that returns a synthetic report and counts the requests it receives. The word *internal* names the destination the policy forbids. It is not a claim that the fixture is unreachable from your terminal: all three listen on your machine, with no firewall between the test client and the internal listener. What the experiment shows is that the fetch feature can be induced to cross its intended destination boundary.

An ordinary call asks the API to retrieve `http://127.0.0.1:8087/document`, and the partner returns `PARTNER-INVOICE-C-1001`. Now ask for `/to-internal` on the same approved origin. Its response is a redirect:

```http
HTTP/1.1 302 Found
Location: http://127.0.0.1:8088/report
```

A redirect supplies a new target URI, which may be on another origin. The only protocol fields that matter here are the numeric status code and `Location`. [HTTP redirection and Location](https://www.rfc-editor.org/rfc/rfc9110.html#section-10.2.2)

In vulnerable mode, the outgoing client follows that response. The internal listener returns `INTERNAL-SYNTHETIC-REPORT`, and the API passes the text back to Alice. The call to the internal listener was issued by the server-side HTTP client, not by Alice's browser.

That is server-side request forgery, or SSRF. The requester influences what the application contacts, using the application's outbound connectivity. In a real deployment that connectivity can differ from the caller's: a workload can often reach services the public internet cannot. The local fixture demonstrates the request path, not real-world network separation. Unlike essay 6's forged browser action, this failure does not depend on a session cookie or a readable cross-origin response. The Java client makes the second request, and a browser CORS policy would not constrain it.

## A plausible check that stops too early

The service parses the supplied text into a `URI` and requires the exact configured scheme, host and port, rejecting user information and fragments:

```java
if (!"http".equals(uri.getScheme())
        || !"127.0.0.1".equals(uri.getHost())
        || uri.getPort() != partner.getPort()
        || uri.getRawUserInfo() != null
        || uri.getRawFragment() != null) {
    throw denied();
}
```

Parsing matters because a URL's visible text is not its destination. A hostname containing an approved string need not be that host, and user information is a separate component. The lab rejects a hostname with an added suffix and a URL containing `alice@` before either fixture receives a request, and it validates and connects with the same parsed URI rather than checking one representation and sending another. [Java 21 URI components](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html)

Those checks all remain in the vulnerable mode. Its mistake is one client setting:

```java
.followRedirects(HttpClient.Redirect.ALWAYS)
```

The application calls the destination check once. Automatic following then happens inside the HTTP client, without calling that check again. `Redirect.NORMAL` is no safer for this purpose: its name excludes HTTPS-to-HTTP downgrades, not a change of host or port. [Java redirect policies](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.Redirect.html)

## Reproduce the borrowed access

Use Java 21 and Maven, following the [README](https://github.com/AnthonyKot/book21/blob/main/labs/08-fetch-authority/README.md). From the lab directory:

```sh
mvn test
mvn -q package -DskipTests
java -jar target/fetch-authority-1.0.jar --lab.follow-redirects=true
```

One process starts every listener; leave ports 8086–8089 free. Its synthetic account is `alice` / `local-only`; Basic authentication over loopback HTTP is only a fixture convenience. In another terminal:

```sh
curl -sS -u alice:local-only --get \
  --data-urlencode 'url=http://127.0.0.1:8087/to-internal' \
  -w '\n[HTTP %{http_code}]\n' \
  http://127.0.0.1:8086/api/fetch
curl -sS -u alice:local-only http://127.0.0.1:8086/lab/targets
```

Expect `200` and the synthetic internal report, then `internalHits:1`. The `/lab/targets` endpoint only reads counters; it issues no fetch. A direct attempt to fetch the internal URL receives `403` without increasing the counter.

The difference is decisive: the initial check blocks the obvious request, but the redirect reaches the same forbidden listener. An allowed entry point has become a way to select a disallowed destination.

The fixtures also record whether they received an `Authorization` header, and that flag stays false. The outbound request is newly built and does not copy Alice's API credential. SSRF can matter even when no credential is forwarded: the server's ability to reach the destination is already useful authority. Copying credentials or a request body would add consequences; this lab does neither.

## Repair the promised feature first

Stop and restart with `--lab.follow-redirects=false`, also the default. The repaired client uses `HttpClient.Redirect.NEVER`. The service still validates the initial URI, then accepts only an upstream `200`; a redirect becomes `502` at the API, and an initially forbidden URL still produces `403` before contact.

The approved `/document` continues to work, `/to-internal` now returns `502`, and `internalHits` stays zero. A redirect from `/to-document` to the partner's own document is rejected too. That is deliberate: the guided feature promises direct document retrieval, not redirect support. Disabling automatic redirection is a documented SSRF defense because it stops a later location from bypassing an earlier input check. When the product needs redirects, each additional destination needs its own decision before connection. [OWASP SSRF prevention](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html)

Do not replace the hit assertion with "the response did not contain the report." A server can make an unwanted request and discard its response, and the internal service may already have recorded an event or done work. This fixture's internal request only increments a counter and returns text; no destructive action is modeled. Checking the counter still distinguishes no contact from contact whose result was hidden.

## What the guided tests establish

Each configuration runs eleven HTTP cases against real listeners on random ports, with counters reset between sequential cases. The reproduction class expects the vulnerable behavior, so all twenty-two cases pass; the arithmetic and full table are in the README. The two configurations differ on exactly two rows: a redirect to the internal target, and a redirect to the partner's own document. Re-running the repair tests with following re-enabled fails exactly those two and passes the other nine, which shows that direct-destination validation alone does not satisfy the repair.

## The check that stops SSRF and the boundary it does not reach

Disabling redirects fixes this feature, but it is worth being precise about what the destination check does and does not establish, because the practice task widens it.

The check admits one literal address and port. It does not resolve caller-selected domain names. Extending it to hostnames introduces DNS: the address checked during validation must be the address actually connected to, and a separate check followed by independent client resolution can observe different answers. IPv4 and IPv6 both matter. This lab neither reproduces nor repairs DNS rebinding. [OWASP domain and address validation](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html#domain-name)

Network controls are a separate enforcement point. A deployment can restrict a workload's outgoing connections through network policy or a controlled egress proxy, so an application mistake does not expose every reachable service. The allowed set must follow the feature's real requirements: a service that legitimately calls a private partner cannot use "reject every private address" as its whole policy. No firewall or proxy is installed or tested here.

Destination safety is also not resource safety. The client sets two-second connection and request timeouts, but the lab buffers small fixed responses with no streaming byte limit, and does not test stalled bodies, decompression, concurrency or a total time budget. The runnable example establishes the redirect boundary, not a general-purpose safe fetching library.

<!--mission-->

## Practice: extend the approved destinations, not the authority

The integration is growing. The product now retrieves partner documents from two approved origins: the original partner and a new documents *mirror*. For some documents the partner replies with a single `302`, to the mirror or to its own document, and that redirect must be honoured. The forbidden internal origin must stay unreachable, including through a redirect, and the outbound request must still carry no caller credential.

Your job is the still-unfinished `/api/follow-one` endpoint. The essay's guided repair refuses every redirect and knows only one origin, so it does not meet this requirement. Decide the destination policy this feature now needs, enforce it for both the first request and any redirect, and prove your decision with your own evidence. The [worksheet](../practice/08-fetch-worksheet.md) states the requirement, the behaviour to preserve and what to hand in; it does not name the fix. Save your attempt before opening the [review guide](../practice/08-fetch-review.md), which holds the hints and the worked answer.

The reproduction and repair from this essay are the mechanism; applying it to more than one approved origin, over a redirect, is the transfer. It is not a new class of vulnerability, and passing tests support only the redirect and destination behaviour you exercised, not DNS, egress or resource limits.

Source note: primary sources inspected on 14 September 2026 and rechecked on 16 September 2026. The JDK fixture emits `302` with the reason phrase "Temporary Redirect"; RFC 9110 names code 302 "Found", and the reason text does not change the numeric code or make it a 307. The service, fixtures and decisions are original constructed examples. Redirect handling and destination validation draw on Java's [HttpClient.Redirect](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.Redirect.html) and [URI](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html) documentation, [RFC 9110 §10.2.2](https://www.rfc-editor.org/rfc/rfc9110.html#section-10.2.2) and the OWASP [SSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html).
