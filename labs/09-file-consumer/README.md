# Stored XML, active consumers — essay 9 lab

Companion to [essay 9](https://anthonykot.github.io/book21/essays/09-file-consumer.html). A synthetic, intentionally vulnerable local service. Run it on loopback only.

Requires Java 21, Maven 3.9+, Python 3 and curl. Tested with Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1 and Spring Security 7.1.1. The first Maven run downloads dependencies. Ports 8089 (API) and 8090 (fixture) must be free for manual runs; tests use random ports.

One account, `alice` / `local-only`. HTTP Basic is a loopback convenience. CSRF protection stays enabled, so POSTs need the session cookie and token from `/csrf`.

## 1. Guided demonstration: the preview parser

```sh
mvn test
mvn -q package -DskipTests
java -jar target/file-consumer-1.0.jar --lab.hardened=false
```

In another terminal, from this directory:

```sh
python3 demo.py
```

The script uploads four documents and previews each: an ordinary invoice, a file entity, an HTTP entity and an external DTD. It reads the synthetic target URIs from `/lab/targets` and prints the resolver and HTTP-hit deltas. It also checks that storing a document resolves nothing.

Permissive preview returns 200 for all four. The ordinary invoice gives `Cedar & Sons`; the other three give `SERVER-ONLY-SYNTHETIC-NOTE`. The HTTP entity and external DTD each add one fixture hit.

Stop the server with Ctrl-C and restart with `--lab.hardened=true` (the default). Rerun the script: the ordinary invoice still returns 200, the three reference cases return 422, and no resolution or HTTP hit occurs.

One upload and preview by hand:

```sh
curl -sS -u alice:local-only -c cookies.txt http://127.0.0.1:8089/csrf > csrf.json
TOKEN=$(python3 -c 'import json; print(json.load(open("csrf.json"))["token"])')
ID=$(curl -sS -u alice:local-only -b cookies.txt -H "X-CSRF-TOKEN: $TOKEN" \
  -H 'Content-Type: application/xml' --data-binary @samples/invoice.xml \
  http://127.0.0.1:8089/api/uploads)
curl -sS -u alice:local-only -b cookies.txt -H "X-CSRF-TOKEN: $TOKEN" \
  -X POST -w '\n[HTTP %{http_code}]\n' "http://127.0.0.1:8089/api/uploads/$ID/preview"
```

### Guided tests and negative control

Default `mvn test` runs 36 cases: `ParserReproductionTest` (18, expects the permissive flaw) and `ParserRepairTest` (18, expects the repair). Both share `PreviewCases`. The negative control runs the repair assertions against a permissive application and must exit nonzero:

```sh
mvn -Dtest=ParserRepairTest -Dtest.hardened=false test
```

Expected: five failures (file entity, HTTP entity, external DTD, internal DTD, UTF-16 entity), thirteen passes, no errors. It switches the whole parser policy, so it does not show that each individual setting is necessary.

## 2. Independent task: release review for partner statements

Use the [worksheet](https://anthonykot.github.io/book21/practice/09-file-worksheet.html). This release adds two operations on stored uploads:

- `POST /api/uploads/{id}/statement` totals a partner statement and returns, for example, `2 lines, 1500 cents: Toner & paper; Delivery`. See `samples/statement.xml` for the format.
- `GET /api/uploads/{id}/receipt` returns the SHA-256 and size of the stored bytes. Support uses it to identify any submission, including one that XML processing rejects.

The release policy, the legitimate behaviour to preserve and the deliverables are in the worksheet. Run the review with `lab.hardened=true`. An ordinary statement by hand, reusing `TOKEN` and `cookies.txt` from above:

```sh
ID=$(curl -sS -u alice:local-only -b cookies.txt -H "X-CSRF-TOKEN: $TOKEN" \
  -H 'Content-Type: application/xml' --data-binary @samples/statement.xml \
  http://127.0.0.1:8089/api/uploads)
curl -sS -u alice:local-only -b cookies.txt -H "X-CSRF-TOKEN: $TOKEN" \
  -X POST -w '\n[HTTP %{http_code}]\n' "http://127.0.0.1:8089/api/uploads/$ID/statement"
curl -sS -u alice:local-only "http://127.0.0.1:8089/api/uploads/$ID/receipt"
```

Write your own tests under `src/test/java/lab/`. Extending `UploadHttp` gives you an authenticated client with CSRF, `upload`, `preview`, `statement` and `receipt` helpers, and fixture counters reset before each test. Name a class `…Test` to include it in `mvn test`. `/lab/targets` returns the synthetic file and HTTP URIs to reference.

### After saving your attempt

`ReleaseReviewCheck` checks the stated release contract over HTTP. Default `mvn test` does not run it, and it skips itself unless `-DreviewCheck=true` is set, so an IDE's "run all tests" will not show its results early. It is a comparison for your own evidence, not a substitute for it; opening the file first turns the task into a guided one, so leave it closed in your IDE's project tree until you have saved your attempt.

```sh
mvn -DreviewCheck=true '-Dtest=*Test,ReleaseReviewCheck' test
```

## Fixture details and scope

- **Intake.** `POST /api/uploads` accepts a raw `application/xml` body (not multipart) and stores at most 8192 bytes under a server-generated UUID outside the web root. 201 means stored, not validated. Wrong media type returns 415, an oversized body 413, an unknown ID 404.
- **Invoice format.** One unnamespaced, attribute-free `invoice` containing exactly one attribute-free `title`. The title is text or CDATA, nonblank and at most 120 Java UTF-16 code units. Comments and processing instructions inside `invoice` or `title` are rejected; those outside the root are ignored. Invalid invoices return 422. Parser depth is capped at 32.
- **Statement format.** One unnamespaced, attribute-free `statement` containing one or more `line` elements. Each `line` has exactly one attribute, `cents`, with 1–9 decimal digits, and a nonblank text description of at most 120 characters. Invalid statements return 422.
- **Lab safety guard.** Every XML consumer installs `Fixture.confine`, which counts each external resolution request and refuses URIs other than the three synthetic targets. It does not decide whether a consumer should resolve references. Where a consumer lets resolution proceed, the JDK processor really opens the file or makes the HTTP request. The lab therefore demonstrates file and network access without exposing arbitrary host files. The targets are reachable from your terminal too: "server-only" names their role in the application's policy, not an operating-system boundary.
- **Status codes.** A missing CSRF token returns 401 in this Basic-auth fixture, because the CSRF check runs before Basic authentication. Treat that as observed behaviour of this fixture, not a universal CSRF status.
- **Not covered.** No tenant authorization (one account), raw-file download, browser rendering, archive extraction, schema validation, XSLT, XInclude processing or Java deserialization. No aggregate disk quota, rate limit, processing deadline, hostile concurrency or crash recovery. No expansion-bomb test. The temporary store is not a production upload service. Normal shutdown removes temporary files; a forced kill may leave them in the system temporary directory.
