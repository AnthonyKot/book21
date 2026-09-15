# A file becomes a program in the component that opens it

The upload succeeds. No network request leaves the service. The server stores the bytes under a new identifier and returns `201`.

The preview is where the file acquires consequences.

In this constructed invoice importer, a short XML document makes the preview parser read a second file and return its contents as the invoice title. Change one reference to an HTTP address and the parser makes a request instead. The upload handler contains no instruction to read that file or call that address. The document directs the work, and the component that interprets it carries it out. That is the sense of “program” here: the lab does not execute code or launch a shell.

## The reference is in the file; the read happens in the parser

The importer accepts this ordinary invoice:

```xml
<invoice><title>Cedar &amp; Sons</title></invoice>
```

Preview returns `Cedar & Sons`. The feature needs a title, including ordinary escaped characters. It has no requirement to load definitions, include other files or contact anyone while parsing.

Now store a different document. The lab substitutes the URI of a synthetic temporary file:

```xml
<!DOCTYPE invoice [
  <!ENTITY note SYSTEM "file:///temporary-fixture-path.txt">
]>
<invoice><title>&note;</title></invoice>
```

The `DOCTYPE` introduces a document type definition, or DTD. Inside it, `note` names an *external entity*. When the parser meets `&note;`, it can retrieve the referenced resource and splice its contents into the document. The uploaded bytes contain only the reference; the text that comes back as the title was never uploaded. This is XML external entity injection, usually shortened to XXE. [Oracle’s JAXP security guide](https://docs.oracle.com/en/java/javase/21/security/java-api-xml-processing-jaxp-security-guide.html)

In the [runnable lab](https://github.com/AnthonyKot/book21/tree/main/labs/09-file-consumer), the permissive preview returns the fixture file's text, `SERVER-ONLY-SYNTHETIC-NOTE`. An entity naming the fixture's HTTP address produces a request that its counter records. So does a `DOCTYPE` that points to an external DTD. The network effect resembles essay 8's SSRF, but nobody submitted a URL to fetch: the request came from interpreting a stored document. A lab safety guard limits resolution to the three synthetic targets. Within that limit the file reads and HTTP requests are real.

## Storing bytes decides nothing about them

Follow the bytes:

```text
XML request body → UUID-named file outside the web root → preview parser → title
```

The upload endpoint accepts only `application/xml`, caps the body at 8192 bytes and generates the storage name. These controls bound what is stored and where. They say nothing about what the document will make a parser do. A caller chooses the media-type label, and both the ordinary and the hostile document carry the same one. [OWASP file-upload guidance](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)

So `201` means “stored,” not “validated.” The interpretation happens later, when a component opens the bytes. A product could validate before accepting an upload, but that validator is itself a parser and needs the same restrictions before it reads anything.

The lab separates these two moments so you can watch them. Its [README](https://github.com/AnthonyKot/book21/blob/main/labs/09-file-consumer/README.md) starts the permissive service and runs a script that stores four documents and previews each one. Storing leaves the resolution and HTTP counters unchanged; the permissive preview then changes them.

## Remove the capability the format does not need

The invoice format has no use for a `DOCTYPE`. The repair makes the parser refuse one before processing it, and closes the related loading paths:

```java
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
```

These run on a `DocumentBuilderFactory` from `newDefaultInstance()`, the JDK's built-in implementation, before any builder is created. They are a tested policy for that processor. Other XML libraries do not necessarily accept the same feature names. If a required setting is unavailable, the code fails with a server error. It does not log a warning and carry on with an unconfigured parser, because that would silently restore the capability the repair removed. [DocumentBuilderFactory](https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/javax/xml/parsers/DocumentBuilderFactory.html), [OWASP’s Java XXE guidance](https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html)

Restart with `--lab.hardened=true`, the default, and rerun the script. The ordinary invoice still previews. The three reference documents return `422`, and neither counter moves.

One detail from the permissive code deserves attention because it looks like a control. That code installs an entity resolver, and for permitted fixture URIs the resolver returns `null`. In the SAX contract, `null` does not mean “refuse.” It asks the parser to open the URI itself. The hardened resolver throws instead. An empty replacement would be a third policy: the document would be accepted with its title quietly changed. [EntityResolver contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/org/xml/sax/EntityResolver.html)

The rule also has a compatibility cost. A document that declares a harmless internal entity is rejected too, because the requirement prohibits `DOCTYPE`, not only suspicious URLs. If a partner genuinely needs DTDs, agree on the format and a resource policy explicitly. Do not answer the support ticket by switching external access back on.

## Check the effect, not the response

The preview also checks the invoice's shape: one title, text only, bounded length. That check runs on the parsed result. By the time it sees the title, the entity has already been resolved, so rejecting the document afterwards cannot undo the file read or the network contact. Only a restriction on the parser itself acts early enough.

The tests therefore assert the effect as well as the status. For every reference case they check the resolver count and the HTTP hits, not just the absence of the note in the body. One case sends the same external entity encoded as UTF-16. A filter searching the raw bytes for the ASCII text `<!DOCTYPE` would miss it. The parser decodes before it recognizes markup, so the parser is where the refusal has to happen.

A negative control shows what these tests can detect. Run the repair assertions against the permissive policy and the reference cases fail, while the intake and shape cases still pass. The earlier controls were never what stopped the reference. The README lists the exact cases and command.

## The policy belongs to each parser, not to the bytes

The repair configured one factory in one method. It says nothing about what happens when other code opens the same stored bytes. A later job, an import, a validator or a library that parses XML on your behalf each creates its own processor, with its own defaults. Even inside the JDK, the XML APIs do not share one switch. A successful preview, or a rejected one, is not a certificate attached to the file: the bytes stay in storage exactly as they arrived.

That changes the review question. “Did we harden the XML parser?” has no stable answer. Ask instead: which components interpret these bytes, what is each allowed to load, and what evidence shows it? Answer it by enumerating the consumers, observing each one's behaviour against the policy, and choosing where the policy should be enforced so the next consumer cannot skip it.

The lab bounds this lesson. Its depth and size limits are not a complete resource budget, and XML is only one format that gets interpreted: schema validation, transformations, archive extraction, rendering and deserialization each need the same question asked of their own consumer.

<!--mission-->

## Practice: review the release before it ships

The lab's next release adds two operations on stored uploads: a partner-statement total and a receipt that support uses to identify submitted bytes. You are the reviewer before release. The [worksheet](../practice/09-file-worksheet.md) states the parser policy and the behaviour that must keep working. It does not say whether either operation is unsafe, where to look or how to repair anything.

Produce four things:
1. An inventory of every path that interprets stored bytes, with evidence for each verdict.
2. A reproduction of any violation you find.
3. A verdict on the release. If something must change, an enforcement decision that explains why your design meets the policy; if nothing must change, the evidence that it already does.
4. Tests you wrote, with a negative control: failing against the original code and passing after your change, or, if no change is needed, failing against a deliberately broken copy you describe.

Attempt it before opening the [review guide](../practice/09-file-review.md), which holds the progressive hints and a comparison check.

The worksheet gives provisional time estimates for setup, the guided run, the release review and optional transfer labs. Record your actual time; nothing in the book has measured it yet. The task mirrors a real release review in one important way: nobody tells you in advance whether the new code is safe.
