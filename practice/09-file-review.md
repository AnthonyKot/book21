# Stored-file consumer review

Compare after your attempt at the [worksheet](09-file-worksheet.md). These are progressive hints, not a substitute for independent evidence.

## Hint 1: storage preserves the input

201 means the byte limit and storage operation succeeded. XML validity and safety were not established. The upload identifier changes the reference to the file, not its trust level. A preview failure leaves these bytes available to the summary path.

## Hint 2: follow the second call

Read `SummaryWorker.summarize` through `UploadStore.read` to `InvoiceParser.title`. Identify the policy argument supplied by the worker. The preview flag configures a different call path; it does not change this argument. A resolver returning null permits default resolution, whereas throwing rejects it.

## Hint 3: enforce policy whenever bytes are interpreted

Reuse the configured restrictive parser for the summary consumer. Retain shape checks and legitimate output. A stored “validated” boolean or a previous preview result cannot replace restrictions on a newly created parser. If replacing stored XML with a validated domain record is proposed instead, explain exactly what is persisted and prove no raw XML is reopened; that is a larger design change than this exercise requires.

## Evidence to expect

The starter has three failures and two passes: file and HTTP references and harmless internal DTDs are accepted by the summary when they should be denied; ordinary and malformed cases already behave as required. The private reference passes all 41 combined cases. Run the worker tests and its HTTP adapter; do not satisfy the tests by disabling ordinary summaries. Restoring the permissive argument must make the relevant exercise cases fail again.

No resolver call and no HTTP hit support the no-resolution claim for these fixtures. The synthetic note appearing in the permissive file response supports an actual read. None of these observations establishes arbitrary filesystem exposure, tenant isolation, a safe transformation pipeline or a complete denial-of-service defense.

For an unfamiliar system, list every consumer that can parse, validate, transform or render the bytes. Distinguish those inspected from those merely identified. A browser rendering bug or an archive extraction path needs its own mechanism and repair; the DOM settings in this exercise do not transfer automatically.
