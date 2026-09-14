# Stored-file consumer worksheet

Use after [essay 9](../essays/09-file-consumer.md). Record your prediction before opening the [review guide](09-file-review.md).

## Preserve your starting point

- Date, Java/Maven version and repository revision:
- Time available this week (10–15 hours total):
- Assistance used, including hints, AI patches or supplied solutions:

The local lab is in [labs/09-file-consumer](https://github.com/AnthonyKot/book21/tree/main/labs/09-file-consumer). Its README contains setup and curl instructions. Use only generated synthetic targets. The summary consumer is intentionally unfinished, even with hardened preview.

## Predict before running

An upload stores bytes and returns 201. Preview returns 422 because those bytes contain a DOCTYPE. Summary opens the same identifier afterwards.

1. What does 201 establish about the file?
2. Which component decides whether an entity is resolved?
3. Does a preview error remove the stored file or constrain another parser?
4. What should happen to a plain invoice using `&amp;`?

| Input | Stored? | Preview result | Summary result before repair | Summary result required |
|---|---|---|---|---|
| Ordinary invoice | | | | |
| File entity | | | | |
| HTTP entity | | | | |
| Harmless internal DTD | | | | |
| Malformed XML | | | | |

## Establish the guided evidence

Run default tests and `python3 demo.py` in each preview mode. For each trace retain status, output, resolver delta and HTTP-hit delta. The demo verifies that upload alone caused no resolution. Explain why an empty response would not establish no external contact.

Run the repair negative control from the README. Record the actual failing cases and exit status. Distinguish failures of assertions from errors that prevented tests from running.

## Change the later consumer

Run `python3 demo.py --summary` with hardened preview. Then run:

```sh
mvn -Dtest=SummaryExercise test
```

Repair `SummaryWorker.summarize`. Every reopening must use the required parser policy. Preserve plain text/CDATA and the invoice-shape checks. File and HTTP references, harmless internal DTDs and malformed XML must produce 422; reference inputs must cause no external resolution or HTTP contact. Do not require a successful preview as a substitute for enforcing the policy here.

After your attempt:

```sh
mvn '-Dtest=*Test,SummaryExercise' test
```

Default `mvn test` only runs the 36 guided cases; it excludes the deliberately unfinished exercise by its name. The combined command runs 41 cases. Reintroduce permissive parsing in a temporary copy and show that the exercise catches it. Retain your patch and explanation, not just the green output.

## Independent transfer

Attempt these named PortSwigger Academy exercises only in their authorized training environments:

- [Exploiting XXE using external entities to retrieve files](https://portswigger.net/web-security/xxe/lab-exploiting-xxe-to-retrieve-files). Identify which result demonstrates external resolution. Describe how its input and authority differ from the local fixture.
- [Exploiting XInclude to retrieve files](https://portswigger.net/web-security/xxe/lab-xinclude-attack). Identify what the attacker controls when they cannot supply a whole XML document. Explain why our local inactive-XInclude test does not establish that every XML consumer disables it.

Do not copy the Academy solution before your attempt. Record hints and assisted completion honestly. If a lab is unavailable, keep the transfer result pending and use a changed local case while documenting its narrower scope; local repetition is not completion of the hosted task.

## Handover and limits

Write a short engineering review identifying the second parser path, the repair and the preserved behavior. For a consulting version, include scope, reproducer, impact, retest and exclusions. State that this is a synthetic demonstration.

Record elapsed time, remaining gap and one delayed check: after a week, trace a fresh consumer from stored bytes to its processor without looking at your patch. Do not mark mastery solely from executing the supplied tests.
