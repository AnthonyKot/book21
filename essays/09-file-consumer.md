# A file becomes a program in the component that opens it

The upload succeeds. No network request leaves the service. The server stores the bytes under a new identifier and returns `201`.

The preview is where the file acquires consequences.

In this constructed invoice importer, a short XML document makes the preview parser open a second file. Its contents become the invoice title. Change the reference to an HTTP address and the parser makes a request instead. The upload handler contains neither a file-read instruction for that second file nor an HTTP client call. Both actions belong to the component that interprets the stored document.

“Program” in the title describes that interpretation: the document directs work. This lab does not execute Java, launch a shell or demonstrate remote code execution. It demonstrates external resource resolution, which is already more authority than the invoice feature needs.

## The file that is missing from the upload

The importer accepts this ordinary invoice:

```xml
<invoice><title>Cedar &amp; Sons</title></invoice>
```

Preview returns `Cedar & Sons`. The feature needs a title, including ordinary escaped characters. It has no requirement to load document definitions, include other files or contact a supplier while parsing.

Now give it a different document. The actual lab substitutes the URI of a newly created synthetic fixture file:

```xml
<!DOCTYPE invoice [
  <!ENTITY note SYSTEM "file:///temporary-fixture-path.txt">
]>
<invoice><title>&note;</title></invoice>
```

The first declaration introduces a document type definition, or DTD. Within it, `note` names an external entity. When the parser encounters `&note;`, it can retrieve the referenced resource and incorporate its contents into the document. The uploaded bytes contain the reference, not the text eventually returned as the title. This is XML external entity injection, usually shortened to XXE. [Oracle’s XML security guide](https://docs.oracle.com/en/java/javase/21/security/java-api-xml-processing-jaxp-security-guide.html)

The [runnable lab](https://github.com/AnthonyKot/book21/tree/main/labs/09-file-consumer) creates a file containing `SERVER-ONLY-SYNTHETIC-NOTE`. Its permissive preview returns that exact text. Another fixture serves the same note over loopback HTTP; an entity pointing there produces one observed request. The network effect resembles essay 8’s SSRF, but the entry point is now XML interpretation rather than an explicit fetch URL.

A custom resolver confines this deliberately vulnerable demonstration to three exact fixture URIs: the text file, the HTTP note and an HTTP DTD. Other external references are rejected. The file read and HTTP requests are real; unrestricted access to arbitrary host files is not demonstrated. The fixture is also accessible from your terminal. “Server-only” names its role in the application’s policy, not an operating-system boundary separating you from the process.

## Three decisions hidden inside “accept a file”

Follow the bytes through the lab:

```text
XML request body → UUID-named temporary file → preview parser → title text
                                          ↘ summary consumer → title text
```

The upload endpoint accepts only the `application/xml` media type, reads at most 8193 bytes and rejects anything exceeding the 8192-byte allowance. It creates the storage name itself and places the file outside the web root. It never uses a submitted filename as a path.

These choices constrain intake and storage. They do not establish that the document is an acceptable invoice. A caller can label malicious XML `application/xml`; the same label appears on our ordinary and external-entity examples. Uploaded MIME information is not trustworthy evidence of content safety. [OWASP file-upload guidance](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)

Here, `201` means “stored,” not “validated.” Even malformed XML can be stored within the byte limit. The later preview performs parsing and checks the invoice shape. This separation makes the moment of interpretation visible. A real product could validate before committing an upload, but its validation parser would need the same restrictions before it touched the bytes.

The second decision is what the parser may do while reading. The third is what the resulting document may mean to the application. Our shape check requires one unnamespaced `invoice` with exactly one attribute-free `title`, containing only text or CDATA. The title must be nonblank and at most 120 Java UTF-16 code units. Comments and processing instructions inside the invoice are rejected too. Those outside the root do not contribute to title extraction; this consumer invokes no stylesheet processor. This deliberately small format is implemented as DOM checks, not an XML Schema.

A shape check after parsing is too late to prevent a parser’s external request. In the vulnerable case, the imported note is ordinary text by the time `getTextContent()` runs. Rejecting an unexpected element afterwards cannot reverse the earlier file read or network contact.

## Run the two moments separately

Use Java 21, Maven, Python 3 and curl. The [lab README](https://github.com/AnthonyKot/book21/blob/main/labs/09-file-consumer/README.md) includes individual curl commands and the exact setup. From the lab directory:

```sh
mvn test
mvn -q package -DskipTests
java -jar target/file-consumer-1.0.jar --lab.hardened=false
```

The API listens on `127.0.0.1:8089`; the HTTP fixture uses port 8090. The synthetic account is `alice` / `local-only`. Basic authentication is a loopback convenience, and CSRF protection remains enabled. The walkthrough obtains the session cookie and CSRF token together rather than disabling that unrelated control.

In another terminal:

```sh
python3 demo.py
```

This script runs curl, uploads an ordinary invoice plus three external-reference documents, and previews each. It obtains the generated fixture URIs from `/lab/targets`; there is no reason to substitute a real file or remote service. For every case, it verifies that storage alone changes neither the resolver counter nor the HTTP counter.

With permissive preview, all four previews return `200`. The ordinary result is `Cedar & Sons`; the others contain the synthetic note. The HTTP entity and external DTD each produce one fixture request. The file entity produces a resolver callback and the note in the response, without an HTTP request.

An external DTD moves the declaration itself outside the uploaded document. The fixture returns a small declaration defining `note`. This gives the tests another resolution path without expanding the demonstration into a catalogue of payloads.

## Remove a capability the format does not need

Stop the server and restart with `--lab.hardened=true`, also the default. Rerun the walkthrough. Ordinary XML still produces `Cedar & Sons`. All three external-reference previews return `422`, with no resolver callbacks and no HTTP fixture hits.

The central policy is simple: this invoice format does not permit a DOCTYPE. The configured JDK parser enforces it before processing the DTD:

```java
f.setFeature(
    "http://apache.org/xml/features/disallow-doctype-decl", true);
f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
```

The complete factory also disables external general entities, external parameter entities and external DTD loading; leaves XInclude disabled; and explicitly enables secure processing. These settings are made before creating the `DocumentBuilder`. The lab uses `newDefaultInstance()` to select the JDK implementation and runs on Java 21.0.12. The settings are a tested policy for that processor, not a promise that every XML library accepts the same feature names. [DOM parser configuration](https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/javax/xml/parsers/DocumentBuilderFactory.html), [OWASP’s Java XXE defenses](https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html)

If required configuration fails, the code aborts processing with a server failure. It does not log the problem and retry with a fresh unconfigured parser. Rejection of invalid XML is mapped separately to `422`. Otherwise an environment change could silently restore the very capability the repair removed.

One small API detail deserves inspection. A resolver that returns `null` has not refused access. In the SAX contract, null asks the parser to open the referenced URI normally. Our permissive fixture resolver intentionally does that after its narrow allowlist check. The hardened resolver throws if invoked. Returning an empty replacement would be a different policy again: it could silently change the imported title. [EntityResolver contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/org/xml/sax/EntityResolver.html)

The benign `&amp;` still works without a DTD. A document declaring even a harmless internal entity is rejected: the requirement prohibits DOCTYPE, not merely references containing a suspicious URL. That restriction is an intentional compatibility cost. If a partner genuinely requires DTDs, agree the needed format and resource policy; do not solve the support ticket by enabling arbitrary external access.

## Test the effect, then bound the claim

Each mode has eighteen guided cases. Most use real HTTP upload and preview requests; one directly checks the configured parser’s depth boundary. The suite covers ordinary text, CDATA, file and HTTP entities, external and internal DTDs, UTF-16 XML, malformed input, duplicate titles, title length, inactive XInclude, media type, upload size, missing storage IDs, authentication and CSRF.

The UTF-16 case matters because the parser sees XML after decoding its bytes. A simplistic search for an ASCII spelling of `<!DOCTYPE` is not the control being tested. The same prohibited construct must be refused by the component that recognizes it.

All thirty-six guided cases pass, including the reproduction class that expects permissive behavior. Restore that behavior under the repair assertions:

```sh
mvn -Dtest=ParserRepairTest -Dtest.hardened=false test
```

Five tests fail: file entity, HTTP entity, external DTD, internal DTD and UTF-16 entity. Thirteen still pass. This negative control distinguishes the parser policy from protections that were already present, including the upload limit. It restores the permissive policy as a whole; it does not prove that every individual setting is independently necessary.

The byte boundary is tested at 8192 and 8193 bytes. Separately, a direct parser test accepts depth 32 and rejects 33 before the invoice-shape check is involved. Those are useful limits, but neither is a complete resource budget. Many small uploads can still consume disk space. Concurrency, rate limits, processing deadlines and crash cleanup need separate decisions. No oversized expansion bomb is run here. [JAXP processing limits](https://docs.oracle.com/en/java/javase/21/security/java-api-xml-processing-jaxp-security-guide.html)

Nor does this repair cover every later interpretation. The lab does not perform schema validation, XSLT, archive extraction, browser rendering or Java object deserialization. Those consumers have their own capabilities and controls. There is only one synthetic account; this example also supplies no evidence of tenant isolation. A parser repair should not erase the authorization questions from earlier essays.

<!--mission-->

## Practice: the file is opened again

The preview now rejects the dangerous invoice. The summary consumer still opens its stored bytes with permissive parsing. Run `python3 demo.py --summary` with hardened preview to see the difference on the same upload identifier. The failed preview leaves the file in storage; storage grants no trust and there is no requirement that preview must succeed before summary runs.

Repair `SummaryWorker.summarize` so it enforces the same no-DTD policy whenever it reopens a file. Preserve ordinary summaries, reject external and harmless internal DTDs, and verify no resolution or HTTP contact. Its HTTP adapter invokes the worker synchronously; the tests also call the worker directly. This exercises a second consumer without claiming to test a queue.

Start with the [worksheet](../practice/09-file-worksheet.md), then use the [review guide](../practice/09-file-review.md). The public starter fails three of five exercise tests; the private reference passes all forty-one combined cases. Default `mvn test` excludes `SummaryExercise` by its name. After your repair, use `mvn '-Dtest=*Test,SummaryExercise' test`.

Budget 10–12 hours within the established 10–15-hour week: two for reading and prediction, four for reproduction and repair, two for evidence, and two to four for the named Academy transfer tasks. Carry unfinished work forward and record assistance. For an employment discussion, show why the preview fix missed another consumer. For a consulting handover, identify the consumers actually assessed and the formats left outside scope. The valuable claim is a demonstrated boundary with known limits.
