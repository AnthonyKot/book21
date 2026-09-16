# Fetch authority — essay 8 lab

Synthetic SSRF fixture: Java 21, Maven 3.9.16, Spring Boot 4.1.1 / Security 7.1.1, Java 21 HTTP client and built-in HttpServer. No real internal services, cloud metadata, external URLs or credentials are used. Initial Maven dependency downloads require internet access.

Run from this directory:

```sh
mvn test
mvn -q package -DskipTests
java -jar target/fetch-authority-1.0.jar --lab.follow-redirects=true
```

The process starts the authenticated API at `127.0.0.1:8086`, the partner fixture at 8087, a documents mirror at 8089 and the synthetic internal fixture at 8088. Leave all four ports free. The mirror is a second approved origin used only by the practice task; the guided demonstration uses the partner and internal fixtures. Stop with Ctrl-C; fixture servers stop with the application. HTTP Basic `alice` / `local-only` is only a local authentication convenience. All interfaces bind to loopback. Do not deploy this fixture publicly.

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

Guided tests: 22 real-HTTP cases across reproduction and repair. Each configuration runs eleven cases:

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

Fixture and API ports are random in tests, with counters reset between sequential cases. Do not enable parallel test execution against a shared fixture. Reproduction tests intentionally assert the flaw. Negative control:

```sh
mvn -Dtest=RedirectRepairTest -Dtest.follow=true test
```

Expected nonzero exit: the two redirect rows fail, nine pass. No DNS or network firewall is being tested. The internal fixture is reachable directly from your machine; the topology models a forbidden destination without creating actual network separation. Guided input accepts only the exact configured partner origin. The fixed redirect handlers point only to local fixtures and never use a caller-supplied Location. Keep them that way.

## Practice: extend the approved destinations

`/api/follow-one` is the practice endpoint. Its starter reuses the guided single-origin, no-redirect fetch. The task, stated in `practice/08-fetch-worksheet.md`, is to support **two** approved origins — the partner and the mirror (port 8089) — and one `302` between them, while the internal origin stays unreachable and no redirect loop hangs the endpoint. The worksheet states the requirement; the review guide (`practice/08-fetch-review.md`) holds the hints and worked answer. Read the review guide only after saving your own attempt and tests.

A post-attempt check in two parts encodes the required observable outcomes and is excluded from the default `mvn test`:

```sh
mvn -DreviewCheck=true -Dtest='FetchPolicy*' test
```

Leave `FetchPolicyCheck.java` and `FetchPolicyDemoFlagCheck.java` closed until your attempt is saved. Any design that produces the required outcomes passes; it does not grade your policy statement, your own tests or your stated limits.

Limits: this is a destination/redirect lab, not a production URL-fetching library. It allows HTTP loopback deliberately, has no DNS resolution/pinning policy, no egress firewall, no streaming body-size cap and no concurrent-work budget. Connection and request timeouts are two seconds; stalled-body and resource-bound behaviour are not verified. A production fetcher needs resource controls, TLS, reviewed proxy behaviour, destination permissions and an error-handling policy suited to its actual use.
