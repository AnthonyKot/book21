# Stored-file consumer review

Open this after saving your [worksheet](09-file-worksheet.md) attempt. Everything below counts as assistance once you have read it. If you are stuck partway, read only the next hint in section 2 and record that you did.

## 1. Judge the attempt before comparing answers

A sound release review shows the following, whatever design it chose.

**Inventory.** Each operation that reads stored bytes gets a verdict backed by evidence: an observed status and counter delta, or configuration inspected in the code and then confirmed by running it. "Uses a parser, therefore vulnerable" and "doesn't look like XML parsing, therefore fine" are both unsupported until checked. A useful inventory also distinguishes reading bytes from interpreting them.

**Reproduction.** A violation is shown with the smallest document that causes it, and the effect is recorded directly: file text in a response, or a nonzero resolution or HTTP-hit count. A `422` alone does not show that nothing was contacted.

**Verdict.** "No change needed" is a legitimate conclusion when every operation's evidence supports it and the negative control uses a deliberately broken copy. For this release that verdict does not survive the statement cases in section 3.

**Decision.** The write-up says where the policy is enforced and why that location suits the next consumer as well as this one. It says what happens if a setting is unsupported. It records at least one design that was tried or considered and rejected, with evidence rather than preference.

**Tests.** Refusal tests assert zero resolution and zero contact as well as the status. Legitimate tests pin exact output. The negative control fails against the original release, or a described broken copy, for the intended reason: assertion failures, not a startup error. The tests follow from the written policy, so they include the cases the policy names explicitly: an unused `DOCTYPE`, and an encoding other than UTF-8.

**Limits.** At least one honest gap, such as another XML API or library, schema or transformation processing, resource exhaustion, or the lab's single account.

## 2. Progressive hints, if you are stuck

1. List the operations in `UploadController`. For each, follow the stored bytes to the first class that does something other than copy or hash them. Which JDK API does that class use, and which settings does it apply before parsing?
2. The guided repair configured one `DocumentBuilderFactory`. Any other XML API has its own properties and its own defaults. First observe what the code actually does with a file entity, an HTTP entity, an external DTD and a parameter entity, checking the counters as well as the status. Then choose a route: configure that API from its own documentation, or send the bytes through a parser configuration you have already verified.
3. Test your candidate settings one at a time against every case the policy names, including a `DOCTYPE` that declares nothing it uses. A setting can stop contact while still accepting the `DOCTYPE`. Rejecting a `DOCTYPE` after the parser has already processed it can be too late.

## 3. What the release contains

**Preview** uses the guided `DocumentBuilderFactory` policy. With `lab.hardened=true` it refuses every `DOCTYPE` before resolution, so it meets the release policy.

**Receipt** reads the stored bytes and hashes them. It never interprets them as XML: a hostile document returns its hash, and the counters stay at zero. Rejecting such documents here would break support's stated need to identify rejected submissions.

**Statement** does not meet the policy. `StatementReader` uses the JDK streaming API, `XMLInputFactory.newDefaultFactory()`, with no DTD or external-access settings. On Java 21.0.12 its behaviour was:

| Document stored and totalled | Observed with the release code |
|---|---|
| Entity naming the fixture file | `200`, the note text as the line description; one resolution |
| Entity naming the fixture HTTP address | `200`, the note text; one HTTP hit |
| External DTD, entity not even used | `200`; one HTTP hit |
| External parameter entity | `200`; one HTTP hit |
| Internal `DOCTYPE`, declarations unused | `200` |
| UTF-16 file entity | `200`, the note text |

The `StatementReader` resolver looks like a control but is the lab safety guard. For the fixture URIs it returns `null`, and the `XMLResolver` contract says the processor then resolves the entity "using its default mechanism." The same trap appears in the guided SAX resolver. [XMLResolver](https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/javax/xml/stream/XMLResolver.html)

The `XMLInputFactory` documentation lists `SUPPORT_DTD` with default `true`, and `IS_SUPPORTING_EXTERNAL_ENTITIES` with an unspecified default. The JDK implementation used here resolved external entities. Oracle's JAXP guide notes that the StAX processor does not support `FEATURE_SECURE_PROCESSING`, so external-access properties must be set directly. Setting secure processing, as the DOM code does, is not an available shortcut here. [XMLInputFactory](https://docs.oracle.com/en/java/javase/21/docs/api/java.xml/javax/xml/stream/XMLInputFactory.html), [JAXP security guide](https://docs.oracle.com/en/java/javase/21/security/java-api-xml-processing-jaxp-security-guide.html)

## 4. Designs that look finished

Each row below is an actual variant of the private reference, run against the reference tests and the review check.

| Change to the statement reader | What still happens |
|---|---|
| External entities off only | The external DTD is still fetched (one HTTP hit). An unused `DOCTYPE` and the parameter-entity document are accepted. An HTTP entity inside an otherwise nonempty description vanishes silently, without contact: `Item &x;` becomes `Item `. |
| DTD support off only | No contact in the tested cases. Documents that use a declared entity fail with an undeclared-entity error and `422`, but a `DOCTYPE` whose declarations go unused, an external DTD reference and a parameter entity are all accepted with `200`, and the policy requires `422`. |
| Reject the DTD event only, default factory | Every `DOCTYPE` returns `422`, but the external DTD and parameter entity were already fetched before the event arrived. |
| DTD support off, external entities off, external DTD access empty, and reject the DTD event | Meets every tested case, and ordinary statements still total. |

Other designs can be sound. For example, statements could be parsed through the same hardened DOM builder that preview uses, or through a shared factory both consumers obtain, with the lines then read from the tree. An executed version of the first option, which parses statements with the guided `builder(true)` and configures no StAX settings at all, passes all 36 guided cases and all 9 review checks. Behaviour, not the choice of API, decides the verdict. A shared, fail-closed construction point also answers the "next consumer" question better than settings copied into each class.

Scanning the raw bytes for an ASCII `<!DOCTYPE` cannot match the UTF-16 case, where each character is encoded in two bytes, and duplicates work the parser has to do anyway. Disabling the statement operation, or returning `422` for everything, violates the behaviour you were asked to preserve.

## 5. Reference evidence

Recorded while authoring. None of it is evidence of your attempt.

- Guided suite: 36 pass. The preview negative control gives 5 failures and 13 passes.
- `ReleaseReviewCheck` against the public release code: 6 failures (file, HTTP, external DTD, parameter entity, unused `DOCTYPE`, UTF-16) and 3 passes (ordinary totals, invalid shape, receipt).
- The private reference's own learner-style tests fail 6 of 8 against the release code.
- The private reference passes all 53 cases: 36 guided, 8 of its own tests and 9 review checks.
- The incomplete variants in section 4 fail these review checks:
  - External entities off only: 3 (external DTD, parameter entity, unused `DOCTYPE`).
  - DTD support off only: the same 3.
  - Reject the DTD event only: 2 (external DTD, parameter entity).

To compare after your attempt:

```sh
mvn -DreviewCheck=true '-Dtest=*Test,ReleaseReviewCheck' test
```

The check covers the stated contract through HTTP. It cannot judge your inventory, your reasoning or tests you did not write. Passing it after reading this guide is assisted evidence.

## 6. Limits of the task

The review covers two XML APIs of one JDK version in a synthetic service. It does not show how third-party StAX implementations, JAXB, XML libraries in Spring or Jackson, `Validator` or `TransformerFactory` behave. Each has its own configuration and needs its own observation. No entity-expansion or other resource-exhaustion case is run. The lab has one account, so tenant authorization is untested. A policy enforced at every current consumer still depends on future code using the shared construction point, which is a maintenance question for code review and, later in this book, for detection rules.
