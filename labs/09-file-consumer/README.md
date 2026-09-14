# Stored XML, active consumer — essay 9 lab

Java 21, Maven 3.9+, Python 3 and curl. Tested on Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1 / Security 7.1.1. One synthetic account (`alice` / `local-only`), no production data. Run on loopback only. Ports 8089 (API) and 8090 (fixture) must be free.

```sh
mvn test
mvn -q package -DskipTests
java -jar target/file-consumer-1.0.jar --lab.hardened=false
```

In another terminal, from this directory:

```sh
python3 demo.py
```

Standalone execution defaults to fixture port 8090; `/lab/targets` returns the actual bound port. Automated authoring runs use random ports (`--lab.fixture-port=0`) to avoid conflicts.

The script executes curl requests, obtains a CSRF token with its session cookie, uploads four XML documents and previews each. It reads only fixture URLs returned by `/lab/targets`: a newly created synthetic text file and two local HTTP resources. Upload returns 201 without resolving anything. Vulnerable previews return 200: `Cedar & Sons` for ordinary XML; `SERVER-ONLY-SYNTHETIC-NOTE` for file, HTTP and external-DTD cases. HTTP and DTD previews each add one fixture hit; the file case adds no HTTP hit. All three entity cases add one resolver callback.

Stop the Java process with Ctrl-C. Restart with `--lab.hardened=true` (the default) and rerun the demo. Ordinary preview still returns 200; all three external-reference previews return 422 with zero new resolver callbacks or HTTP hits. Startup creates fresh upload storage and fixture state. Normal shutdown removes the process's temporary files; a forced kill may leave them in the system temporary directory.

A single upload and preview can also be sent manually. Save the token and cookie together:

```sh
curl -sS -u alice:local-only -c cookies.txt http://127.0.0.1:8089/csrf > csrf.json
TOKEN=$(python3 -c 'import json; print(json.load(open("csrf.json"))["token"])')
ID=$(curl -sS -u alice:local-only -b cookies.txt -H "X-CSRF-TOKEN: $TOKEN" \
  -H 'Content-Type: application/xml' --data-binary @samples/invoice.xml \
  http://127.0.0.1:8089/api/uploads)
curl -sS -u alice:local-only -b cookies.txt -H "X-CSRF-TOKEN: $TOKEN" \
  -X POST -w '\n[HTTP %{http_code}]\n' "http://127.0.0.1:8089/api/uploads/$ID/preview"
```

`POST /api/uploads` accepts a raw `application/xml` body, not multipart input, and stores at most 8192 bytes under a server-generated UUID outside the web root. It does not validate the XML or mark it trusted. `POST /api/uploads/{id}/preview` reopens those bytes. The invoice subset is one unnamespaced `invoice` element containing exactly one unnamespaced, attribute-free `title`, whose text/CDATA is nonblank and at most 120 Java UTF-16 code units. Whitespace between elements is allowed. Comments and processing instructions inside `invoice` or `title` are rejected by this narrow shape policy. Those outside the root are ignored by title extraction; no stylesheet processor is invoked. Parser depth is capped at 32. A malformed or disallowed invoice returns 422; wrong media type 415; oversized upload 413; unknown UUID 404. Predefined `&amp;` is supported without a DTD.

CSRF is retained. Use the cookie and token on POSTs; in this Basic-auth fixture a missing CSRF token returns 401 because the CSRF check precedes Basic authentication. This is an observed rejection, not a universal CSRF status contract. A valid token without Basic authentication also returns 401.

## Verification and negative control

Default `mvn test` runs 36 guided cases (18 in each mode), including actual HTTP upload/preview operations and a direct parser depth boundary test. The reproduction class expects the vulnerability. Tests run sequentially because fixture counters are shared.

```sh
mvn -Dtest=ParserRepairTest -Dtest.hardened=false test
```

Expected: five failures (file entity, HTTP entity, external DTD, internal DTD and UTF-16 entity), thirteen passes, no errors. This restores the deliberately permissive parsing policy. It does not isolate the necessity of each individual hardening setting.

## Independent exercise: the later summary consumer

With hardened preview, run:

```sh
python3 demo.py --summary
mvn -Dtest=SummaryExercise test
```

`SummaryWorker.summarize` reopens the stored file with the permissive parser. Its HTTP adapter is synchronous; direct worker tests avoid inventing a queue or timing guarantee. A rejected preview does not delete the upload, and no prior-preview requirement exists. The starter fails three of five exercise tests. Repair this consumer so every parse enforces the same no-DTD policy, preserves ordinary summaries, and rejects file, HTTP and harmless internal-DTD inputs with 422 and no resolution/contact. Do not “fix” it by returning empty output or disabling all summaries.

`SummaryExercise` deliberately falls outside Maven's default test naming filter. After repairing it, run all 41 cases:

```sh
mvn '-Dtest=*Test,SummaryExercise' test
```

The private reference was executed during authoring; it is not shipped here. The public summary route stays unfinished even when `lab.hardened=true`.

## Scope

The custom resolver bounds the vulnerable demonstration to three exact synthetic URIs; it throws for other references. Returning null for an allowed fixture URI lets the real JDK parser open it. This demonstrates file and network access, not unrestricted host filesystem access or a network-isolation boundary. The hardened parser refuses DOCTYPE, disables external entities/DTD loading and XInclude, sets empty external access lists, keeps secure processing enabled and rejects resolver fallback. Configuration failure aborts processing instead of retrying insecurely.

One account is not a tenant-authorization implementation. There is no raw-file download, browser rendering, archive extraction, schema validation, XSLT or Java deserialization. The per-upload and parser limits do not implement aggregate disk quotas, request-rate limits, a processing deadline, hostile concurrent workloads or crash recovery. Avoid using the temporary store as a production upload service.
