# Lab 5 — a token for the wrong recipient

A local Spring Boot fixture for essay 5. It uses the Cedar/Birch invoice boundary from lab 4, narrowed to one read endpoint. There is no database, worker, real identity provider or password-reset endpoint.

Requirements: Java 21 and Maven. Verified with Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1 and Spring Security 7.1.1. The first Maven run needs internet access to fetch dependencies. Run commands from this directory.

## Test both versions

```sh
mvn test
```

There are 26 tests: thirteen inherited/specific cases in each of `AudienceReproductionTest` and `AudienceRepairTest`. The reproduction suite passes when the audience flaw is present. Both suites also check legitimate access, tenant isolation, absent credentials, expired/future tokens, wrong issuer/key, missing expiry, missing/blank subjects and an unknown local subject.

For the negative control, temporarily change `lab.audience-check=true` to `false` in `AudienceRepairTest`, then run:

```sh
mvn test -Dtest=AudienceRepairTest
```

Expect two assertion failures (billing audience and missing audience) and eleven passes. Restore `true` afterwards. A server startup or dependency error does not count as detecting the flaw.

## Run and inspect

```sh
mvn -q package -DskipTests
java -jar target/token-boundary-1.0.jar --lab.audience-check=false
```

The service binds to `127.0.0.1:8082`. In another terminal:

```sh
billing_token=$(curl -fsS http://127.0.0.1:8082/lab/tokens/billing)
curl -sS -w '\n[HTTP %{http_code}]\n' \
  -H "Authorization: Bearer $billing_token" \
  http://127.0.0.1:8082/api/invoices/C-1001
```

Expect 200 and Cedar's invoice. Stop the service and restart with `--lab.audience-check=true`. Obtain a **new** billing token and repeat: expect 401. The keys change on restart, so using an old token would test signature rejection instead of audience rejection.

Fetch `/lab/tokens/valid` and repeat with that token: C-1001 gives 200, B-2001 gives 404. Change the token kind to inspect the other cases:

| Kind | Meaning | With audience checking enabled |
|---|---|---|
| `valid` | Alice, document audience, API purpose | 200 for C-1001 |
| `billing` | Alice, billing audience | 401 |
| `no-audience` | Audience absent | 401 |
| `expired` / `future` | Time outside accepted interval by ten minutes | 401 |
| `wrong-issuer` / `wrong-key` | Incorrect issuer or signature under a different key | 401 |
| `no-expiry` | Required expiration absent | 401 |
| `no-subject` / `blank-subject` | Required subject absent or blank | 401 |
| `unknown-user` | Cryptographically valid but subject not in local directory | 403 |
| `reset` | Document audience, password-reset purpose | 200 before the practice repair |
| `no-purpose` | No token purpose | 200 before the practice repair |

## What to inspect and change

- `SecurityConfig.java`: trusted public key, allowed algorithm and validator list. The mode switch changes only audience validation.
- `TokenFixtures.java`: synthetic token variants. Standard variants live for five minutes. The future variant becomes valid after ten minutes and expires after fifteen; the expired variant had a validity window from twenty to ten minutes ago. Both failures are well outside Spring's default 60-second skew.
- `InvoiceController.java`: maps the authenticated subject to a local tenant, then checks the invoice. The issuer has already been constrained to one configured issuer.
- Practice: create `PurposeRepairTest` extending `TokenHttp` and use the same audience-enabled `@SpringBootTest` configuration as `AudienceRepairTest`. Write tests for `reset` and `no-purpose`, observe their failure, then require `token_use=api-access`. Keep the existing tests unchanged. The recovery-flow review in the worksheet is a separate design task.

The unauthenticated token dispenser is a teaching device. All keys are generated in memory; no key files, real credentials, external issuer requests or private data are involved. Keep this fixture local. It intentionally accepts reset tokens until the practice repair and is not a production authentication template.

The lab uses bearer headers and stateless security, with no Basic or cookie authentication. It does not implement login, logout, refresh tokens, discovery, key rotation, recovery-token consumption or immediate credential revocation. The custom `token_use` field is a local contract, not a universal JWT standard.
