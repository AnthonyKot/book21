# Fetch authority — essay 8 lab

Synthetic SSRF fixture: Java 21, Maven 3.9.16, Spring Boot 4.1.1 / Security 7.1.1, Java 21 HTTP client and built-in HttpServer. No real internal services, cloud metadata, external URLs or credentials are used. Initial Maven dependency downloads require internet access.

Run from this directory:

```sh
mvn test
mvn -q package -DskipTests
java -jar target/fetch-authority-1.0.jar --lab.follow-redirects=true
```

The process starts the authenticated API at `127.0.0.1:8086`, partner fixture at port 8087 and synthetic internal fixture at 8088. Leave all three ports free. Stop with Ctrl-C; fixture servers stop with the application. HTTP Basic `alice` / `local-only` is only a local authentication convenience. All interfaces bind to loopback. Do not deploy this fixture publicly.

```sh
curl -sS -u alice:local-only --get \
  --data-urlencode 'url=http://127.0.0.1:8087/document' \
  http://127.0.0.1:8086/api/fetch
curl -sS -u alice:local-only --get \
  --data-urlencode 'url=http://127.0.0.1:8087/to-internal' \
  -w '\n[HTTP %{http_code}]\n' http://127.0.0.1:8086/api/fetch
curl -sS -u alice:local-only http://127.0.0.1:8086/lab/targets
```

Expect partner content for `/document`, internal synthetic content for `/to-internal`, and `internalHits:1` after the redirect. The counter is cumulative until restart. A direct fetch of `http://127.0.0.1:8088/report` receives 403 without incrementing it. `/lab/targets` only returns URLs and counters; it does not fetch either fixture. Its `sawAuthorization` flag should stay false: outbound requests do not copy the incoming API credential.

Restart with `--lab.follow-redirects=false` (also the default). The partner document still returns 200. `/to-internal` returns 502 and the internal counter stays zero. `/to-document`, a relative redirect to a legitimate document, also returns 502: the guided policy deliberately permits no redirects.

Normal tests: 22 real-HTTP cases across reproduction and repair. The fixture ports and API port are random in tests, with counters reset between sequential cases. Do not enable parallel test execution against a shared fixture. Reproduction tests intentionally assert the flaw. Negative control:

```sh
mvn -Dtest=RedirectRepairTest -Dtest.follow=true test
```

Expected nonzero exit: two redirect tests fail, nine pass. No DNS or network firewall is being tested. The internal fixture is reachable directly from your machine; the topology models a forbidden destination without creating actual client/server network separation. Initial input accepts only the exact configured partner origin. Its fixed redirect handlers point only to these local fixtures and never use a caller-supplied Location. Keep them that way.

## One-hop practice

`/api/follow-one` initially delegates to the no-redirect fetch. With `lab.follow-redirects=false`, implement at most one 302 redirect: resolve Location against the first URI, validate the resolved URI with the same destination policy BEFORE sending, and require the final response to be 200. A denied redirected destination, missing/invalid Location, second redirect or non-200 result maps to 502. An invalid initial destination remains 403. Never enable automatic following to satisfy the exercise.

```sh
mvn -Dtest=OneHopExercise test
```

Initially one fails (legitimate relative redirect) and four pass. After your repair:

```sh
mvn '-Dtest=*Test,OneHopExercise' test
```

The private reference passes 27 tests. Removing its redirected-destination validation makes the internal-target exercise test fail with one real internal hit. Add your own changed test; the supplied cases cover direct success, relative success, internal denial, a loop and a two-redirect chain. They do not cover every redirect status, malformed Location or timeout.

Limits: this is a destination/redirect lab, not a production URL-fetching library. It allows HTTP loopback deliberately, has no DNS resolution/pinning policy, no egress firewall, no streaming body-size cap and no concurrent-work budget. Connection and request timeout settings are two seconds; stalled-body/resource-bound behavior is not verified. It buffers fixed small text responses. A production fetcher needs resource controls, TLS, reviewed proxy behavior, destination permissions and an error/response-handling policy suited to its actual use.

Default `mvn test` runs only the 22 guided cases because `OneHopExercise` is outside the default test naming filter. Use the combined command above to verify your exercise together with the guided cases (27 total).
