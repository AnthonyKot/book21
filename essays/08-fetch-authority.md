# A fetch endpoint borrows the server's network access

The document service refuses a URL for an internal report. The same service returns that report when the request starts at an approved partner address.

The partner responds with a redirect. The HTTP client follows it.

This constructed case has a destination check, an authenticated caller and an ordinary GET request. No SQL expression or script changes meaning. Instead, the application lets another response decide where its next request goes. The check authorized the starting point; the client treated that as permission to continue somewhere else.

The [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/08-fetch-authority) adds a partner-document fetch to the fictional service. Its rule is narrow: Alice may fetch from one configured partner origin. She may not use this feature to reach the synthetic internal-report origin. Both are local fixtures, so the entire failure can be reproduced without touching an actual private network or cloud service.

## Follow the second request

Three listeners run on loopback:

| Listener | Role | Default port |
|---|---|---:|
| Spring API | Authenticates Alice and fetches a submitted URL | 8086 |
| Partner fixture | Returns a document or a fixed redirect | 8087 |
| Internal fixture | Returns a synthetic report and counts requests | 8088 |

The word *internal* describes the destination the policy forbids. It is not a claim that this fixture is unreachable from your terminal. All three listeners are on your machine; there is no firewall separating the test client from the internal listener. What the experiment proves is that the fetch feature can be induced to cross its intended destination boundary.

An ordinary call asks the API to retrieve:

```text
http://127.0.0.1:8087/document
```

The partner returns `PARTNER-INVOICE-C-1001`. Now ask for `/to-internal` on the same approved origin. Its response is:

```http
HTTP/1.1 302 Temporary Redirect
Location: http://127.0.0.1:8088/report
```

The status line above is what the JDK fixture actually emits. RFC 9110 names code 302 “Found”; the fixture’s reason phrase does not turn it into code 307. The relevant protocol fields here are the numeric status code and Location. A redirect supplies a new target URI, which may be on another origin. [HTTP redirection and Location](https://www.rfc-editor.org/rfc/rfc9110.html#section-10.2.2)

In vulnerable mode, the outgoing client follows that response. The internal listener returns `INTERNAL-SYNTHETIC-REPORT`, and the API passes the text back to Alice. The call to the internal listener was issued by the server-side HTTP client, not by Alice's browser.

That is server-side request forgery, or SSRF. The requester influences what the application contacts using the application's outbound connectivity. In a real deployment, that connectivity can differ from the caller's: services may be reachable from a workload that are not reachable from the public internet. Our local fixture demonstrates the request path, not that real-world network separation.

Unlike essay 6's forged browser action, this failure does not depend on the browser attaching a session cookie or allowing a cross-origin response to be read. The Java client is making the second request. Changing a browser CORS policy would not constrain that client's destination.

## A plausible check that stops too early

The service parses the supplied text into a `URI`. Its initial policy requires the exact configured scheme, host and port, and rejects user information and fragments:

```java
if (!"http".equals(uri.getScheme())
        || !"127.0.0.1".equals(uri.getHost())
        || uri.getPort() != partner.getPort()
        || uri.getRawUserInfo() != null
        || uri.getRawFragment() != null) {
    throw denied();
}
```

This is deliberately a loopback policy. Copying its HTTP/private-address choices into a public URL fetcher would not produce an appropriate production policy.

Parsing matters because the URL's visible text is not its destination. A hostname containing an approved string need not be that host, and user information is a separate component. The lab rejects a hostname with an added suffix and a URL containing `alice@` before either fixture receives a request. It also rejects malformed URIs. It uses the same parsed URI for validation and request construction rather than validating one representation and sending another. [Java 21 URI components](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html)

Those checks all remain in the vulnerable mode. Its mistake is this client configuration:

```java
.followRedirects(HttpClient.Redirect.ALWAYS)
```

The application calls `allowed(url)` once. Automatic following happens afterwards, inside the HTTP client, without calling that application policy again.

Java also offers `Redirect.NORMAL`, but its name does not mean “stay inside my approved destinations.” It excludes HTTPS-to-HTTP redirects, not every change of host or port. The lab makes the setting explicit so it cannot be mistaken for a framework default. [Java redirect policies](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.Redirect.html)

## Reproduce the borrowed access

Use Java 21 and Maven, following the [README](https://github.com/AnthonyKot/book21/blob/main/labs/08-fetch-authority/README.md). From the lab directory:

```sh
mvn test
mvn -q package -DskipTests
java -jar target/fetch-authority-1.0.jar --lab.follow-redirects=true
```

The one process starts all three listeners. Leave ports 8086–8088 free. Its synthetic account is `alice` / `local-only`; Basic authentication over loopback HTTP is only a fixture convenience.

In another terminal:

```sh
curl -sS -u alice:local-only --get \
  --data-urlencode 'url=http://127.0.0.1:8087/to-internal' \
  -w '\n[HTTP %{http_code}]\n' \
  http://127.0.0.1:8086/api/fetch
```

Expect `200` and the synthetic internal report. Then inspect:

```sh
curl -sS -u alice:local-only \
  http://127.0.0.1:8086/lab/targets
```

After that one fetch, `internalHits` is one. This endpoint reads counters; it does not issue another fetch. A direct attempt to fetch the internal URL receives `403` without increasing the counter.

The difference is decisive: the initial destination check blocks the obvious request, but the redirect reaches the same forbidden listener. An allowed entry point has become a way to select a disallowed destination.

The fixtures also record whether they received an Authorization header. That flag remains false. The outbound request is newly constructed and does not copy Alice's API credential. SSRF can therefore matter even when no credential is forwarded: the server's ability to contact the destination is already useful authority. Copying credentials or sensitive request bodies could add consequences, but this lab does neither.

## Repair the promised feature first

Stop and restart with `--lab.follow-redirects=false`, also the default. The repaired client uses:

```java
.followRedirects(HttpClient.Redirect.NEVER)
```

The service still validates the initial URI. It then accepts only an upstream `200`; a redirect becomes `502` at the API. This status means that the upstream response did not satisfy this fetch operation's contract. An initially forbidden URL still produces `403` before contact.

The approved `/document` continues to work. `/to-internal` now returns `502`, and `internalHits` remains zero. A redirect from `/to-document` to the partner's own document is also rejected. That is an intentional limitation: the guided feature promises direct document retrieval, not redirect support.

Disabling automatic redirection is a documented SSRF defense because it prevents a later location from bypassing an earlier input check. When the product needs redirects, each additional destination needs its own decision before connection. [OWASP SSRF prevention](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html)

Do not replace the hit assertion with “the response did not contain the report.” A server can make an unwanted request and then discard its response. The internal service may already have recorded an event or performed work. This fixture's internal request only increments a synthetic counter and returns text; no destructive internal action is modeled. Checking the counter nevertheless distinguishes no contact from contact whose result was hidden.

## What the tests establish

Each configuration runs eleven HTTP cases:

| Case | Automatic following | No following |
|---|---|---|
| Direct partner document | 200; one partner hit | Same |
| Direct internal target | 403; no fixture hit | Same |
| Partner redirects internally | 200; one internal hit | 502; zero internal hits |
| Partner redirects to its document | 200; two partner hits | 502; one partner hit |
| Wrong scheme | 403; no fixture hit | Same |
| User information in URL | 403; no fixture hit | Same |
| Fragment present | 403; no fixture hit | Same |
| Host with added suffix | 403; no fixture hit | Same |
| Malformed URI | 403; no fixture hit | Same |
| Partner returns an error | 502 | Same |
| No API authentication | 401; no partner hit | Same |

The tests use actual HTTP listeners with random ports. Counters reset between sequential cases. Twenty-two cases pass because the reproduction class intentionally expects the vulnerable behavior.

Run the repair tests while re-enabling following:

```sh
mvn -Dtest=RedirectRepairTest -Dtest.follow=true test
```

Exactly the two redirect tests fail; nine pass. The control shows that direct-destination validation alone does not satisfy the repair assertions. It also keeps ordinary partner access working in both modes.

## A hostname check is not an egress boundary

The fixture admits one literal address and port. It does not resolve caller-selected domain names. Extending that code to hostnames introduces DNS: the address checked during validation must correspond to the destination actually used for connection. A separate check followed by independent client resolution can observe different answers. IPv4 and IPv6 both matter. This lab neither reproduces nor repairs DNS rebinding. [OWASP domain and address validation](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html#domain-name)

Network controls provide a separate enforcement point. A deployment can restrict the workload's outgoing connections, often through network policy or a controlled egress proxy, so application mistakes do not make every reachable service available. The allowed set must follow the feature's real requirements. A service legitimately calling a private partner endpoint cannot use “reject every private address” as its whole policy. No firewall or proxy policy is installed or tested by this lab.

There is also a smaller product question: does the user need to supply a complete URL? If the feature only retrieves documents from one integration, an integration key and document identifier may let the server construct a much narrower request. That changes what the caller can choose. It still needs document authorization and careful URL construction; an identifier is not automatically safe merely because it is no longer called a URL.

Finally, destination safety is not resource safety. The client configures two-second connection and request timeouts, but the lab buffers fixed small responses without a streaming byte limit. It does not test stalled bodies, decompression, concurrency limits or a complete elapsed-time budget. A production design must account for those separately. The runnable example establishes the redirect boundary, not a general-purpose safe fetching library.

<!--mission-->

## Practice: permit one redirect without delegating the decision

The partner now needs one relative `302` redirect to its document. Extend `/api/follow-one` with automatic following still disabled. Resolve Location against the current URI, validate the resolved destination before sending, permit at most one additional request, and require a final `200`. Refuse forbidden destinations and further redirects with `502`; preserve the initial `403` rule.

Use the [worksheet](../practice/08-fetch-worksheet.md) before the [review guide](../practice/08-fetch-review.md). The starter denies all redirects, so its legitimate-relative-redirect test fails. The private reference passes all twenty-seven cases. Removing its redirected-destination check makes the internal-target test fail and records a real internal hit. This checks both the useful extension and the boundary it must retain.

Default `mvn test` runs only the twenty-two guided cases: `OneHopExercise` is deliberately outside its default naming filter. After your repair, run `mvn '-Dtest=*Test,OneHopExercise' test` to include all twenty-seven cases.

Plan 10–12 hours within the established 10–15-hour week: two for reading and prediction, four for the local repair, two for evidence, and two to four for the named Academy transfer tasks. Carry unfinished work forward and record hints or AI assistance.

For employment, explain why checking the initial URL was insufficient and show the no-contact regression. For consulting, specify the fetch path, redirect behavior and destinations assessed, alongside the DNS and network controls you did not verify. Neither route needs a claim that a small local demonstration proves the whole network safe.
