# Review every response path — essay 11 lab

Companion to [essay 11](https://anthonykot.github.io/book21/essays/11-security-property.html). A synthetic, intentionally vulnerable local service. Run it on loopback only.

Requires Java 21, Maven 3.9+, Python 3 and curl. Tested with Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1, Spring Framework 7.0.9, Spring Security 7.1.1 and H2 2.4.240. The first Maven run downloads dependencies. Port 8092 must be free for manual runs; tests use random ports.

Two synthetic users, both with password `local-only`: `alice` (tenant Cedar) and `bob` (tenant Birch). HTTP Basic is a loopback convenience. CSRF protection stays enabled, so POSTs need the session cookie and token from `/csrf`.

## 1. Guided review: release 1, the tenant-scoped lookup

For an unassisted first pass, start with [review/release-1-BRIEF.md](review/release-1-BRIEF.md) and [review/release-1-candidate.diff](review/release-1-candidate.diff) before the essay walkthrough.

```sh
mvn '-Dtest=ReviewReproductionTest#ownCold+foreignCold' test
mvn test
mvn -q package -DskipTests
java -jar target/security-property-1.0.jar --lab.partition=false --lab.fixtures=true
```

The two cold-cache tests pass against the flawed candidate. In another terminal, from this directory:

```sh
python3 demo.py
```

The script resets fixture state, then records status, body and cache state for:

1. Bob previews C-1001 before it is cached: 404.
2. Alice previews it: 200, `CEDAR-PRIVATE-SUMMARY`.
3. Bob previews it again: 200 and Cedar's summary in candidate mode.
4. Alice repeats: 200 without another body load.

It then repeats in the other direction with B-2001 and checks that an anonymous request gets 401. Stop the server and restart with `--lab.partition=true --lab.fixtures=true`. Both foreign previews now return 404, before and after the other tenant warms the cache, and owner repeats still avoid a body reload.

The same sequence by hand, on a freshly started candidate-mode server:

```sh
curl -sS -u bob:local-only -w '\n[HTTP %{http_code}]\n' http://127.0.0.1:8092/api/documents/C-1001/preview
curl -sS -u alice:local-only -w '\n[HTTP %{http_code}]\n' http://127.0.0.1:8092/api/documents/C-1001/preview
curl -sS -u bob:local-only -w '\n[HTTP %{http_code}]\n' http://127.0.0.1:8092/api/documents/C-1001/preview
```

Expect 404/200/200; with `--lab.partition=true`, 404/200/404. A caller-supplied `?tenant=cedar` is ignored.

### Guided tests and negative control

`ReviewReproductionTest` (10, expects the shared-key flaw) and `ReviewRepairTest` (10, expects the repair) share `PreviewCases`. The negative control runs the repair assertions against the shared key and must exit nonzero:

```sh
mvn -Dtest=ReviewRepairTest -Dtest.partition=false test
```

Expected: two failures (both warm cross-tenant directions), eight passes, no errors. The cold-cache denials stay green while the property is broken.

## 2. Independent review: release 2 candidate

Use the [worksheet](https://anthonykot.github.io/book21/practice/11-review-worksheet.html). The source in this directory already contains release 2, as a pull request would put it in front of you:

- [review/release-2/EXPLANATION.md](review/release-2/EXPLANATION.md), the candidate's change summary;
- [review/release-2/candidate.diff](review/release-2/candidate.diff), the change against release 1;
- `ExportCandidateTest`, the tests supplied with the candidate. Default `mvn test` runs them with the guided suites: 27 cases.

The property, the behaviour to preserve and the deliverables are in the worksheet. Save your initial scope and predictions before additional tests. Keep one review note linking the path inventory, observations, verdict, a competing explanation you tested, any repair, and your own regression tests. An unresolved question needs a named next check; a passing supplied suite is not a review verdict. Release 2 adds two endpoints:

- `POST /api/documents/{id}/export` returns an export identifier (CSRF token required).
- `GET /api/exports/{exportId}` returns the exported content.

For manual state changes, `POST /lab/archive/{id}` is available with `--lab.fixtures=true`; it requires the document's owner and a CSRF token. Choose state changes and request ordering from your own predictions. The following snippet demonstrates only an active export round trip and the cookie/token pattern:

```sh
curl -sS -u alice:local-only -c cookies.txt http://127.0.0.1:8092/csrf > csrf.json
TOKEN=$(python3 -c 'import json; print(json.load(open("csrf.json"))["token"])')
EXPORT=$(curl -sS -u alice:local-only -b cookies.txt -H "X-CSRF-TOKEN: $TOKEN" \
  -X POST http://127.0.0.1:8092/api/documents/C-1001/export)
curl -sS -u alice:local-only -w '\n[HTTP %{http_code}]\n' "http://127.0.0.1:8092/api/exports/$EXPORT"
```

Write your own tests under `src/test/java/lab/`. Extending `ReviewHttp` gives you `preview`, `download`, `export`, `fetchExport` and a CSRF-aware `post`, with `store` (including `archive` and the `bodyLoads` counter), `previews` and `exports` available for setup. Cache and fixture state reset before each test. Name the class `…Test` to include it in `mvn test`.

### After saving your attempt

`ReleaseReviewCheck` checks the stated property through HTTP. Default `mvn test` does not run it, and it skips itself unless `-DreviewCheck=true` is set, so an IDE's "run all tests" will not show its results early. It compares with your own evidence; it does not replace it. Opening the file first turns the review into a guided one, so leave it closed in your IDE's project tree until you have saved your attempt. Avoid expanding it in an IDE test explorer; scenario names can also disclose the check before execution.

```sh
mvn -DreviewCheck=true '-Dtest=*Test,ReleaseReviewCheck' test
```

## Fixture details and scope

- **Data.** Three documents in an in-memory H2 database: C-1001 and C-1002 (Cedar), B-2001 (Birch). Each has a summary, full content and an `archived` flag. State is recreated on restart.
- **Tenant mapping.** `Tenants` maps the two principals to fixed tenants in code. Membership changes and ownership transfers are not implemented.
- **Cache.** `PreviewService` is an application `ConcurrentHashMap`, not a browser, CDN or HTTP intermediary cache. It has no size limit, TTL or distributed invalidation. The preview loader caches the whole document record, including full content, although the preview response returns only the summary.
- **Counters.** `bodyLoads` counts repository body-load attempts, including denied misses. It does not count every SQL query, such as `requireDownload`, and is not a performance measure.
- **Fixture endpoints.** `lab.fixtures=false` (the default) disables `/lab/state`, `/lab/reset` and `/lab/archive/{id}` with 404. They require authentication; POSTs keep CSRF protection. Reset clears documents' archive state, the cache and export links for repeatability; it is not a production administration feature. Run demonstrations sequentially and never reset with requests in flight.
- **Exports.** Export records live in memory and disappear on restart.
- **Not covered.** No multi-process deployment, timing channel, concurrency benchmark, eviction pressure, revocation propagation across processes, or cancellation of a response already authorized before a concurrent archive.
