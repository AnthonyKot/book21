# Identify the artifact behind the inventory

Use [essay 13](../essays/13-build-inventory.md), the [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/13-build-inventory), and the [review guide](13-inventory-review.md) after your own attempt. No real customer data, cloud environment or registry publication is needed.

## Predict the discrepancy

Before running the build, inspect the application POM and Dockerfile. Explain what the Maven plugin can learn about a JAR copied from the independent helper project. Predict whether a change to that helper must change the application JAR, the final image and the appropriate inventory.

| Question | Prediction | Observation and evidence location |
|---|---|---|
| Is Spring Web MVC present in both inventories? | | |
| Is the helper in the application dependency BOM? | | |
| Which helper version is in A and B? | | |
| Where do the Alpine packages enter? | | |
| Do identical app bytes imply identical images? | | |

## Collect evidence

Run the README's build and verification commands. Save your source revision, tool versions, exact commands and output before rerunning anything. Read the scanner diagnostics. Explain the difference among the 39 Maven components, 112 native package records and 1,336 image CycloneDX components. Pick one component and inspect its identity, version and file location rather than just finding its name in a list.

Run the negative inventory control. Confirm that the four failures concern the helper and base-package expectations, while the Spring checks pass. An installation, startup or JSON-reading failure does not establish that result.

Save the local A and B image IDs, application hashes, helper hashes, two inventories per image, pairing records and actual tag-move observations. Keep local image IDs separate from the pinned base registry digest. Do not label a locally built image ID as a pushed registry manifest digest.

## Independent repair: the verifier trusts a name

Run `python3 record.py` to generate the pairing records, then `python3 exercise_tests.py`. Save the failing results before opening `identity_exercise.py`.

Its input includes a selected image ID from outside the record. Repair the verifier so it checks that selection, the source inside the native scan and the exact bytes of both supplied reports. Missing required evidence must cause rejection. Preserve correct A and B pairs. Keep the test suite and generated artifacts unchanged.

Explain why changing `record['imageID']` alone cannot legitimately associate A's scan with B. Explain why checking only the native report's hash leaves the supplied CycloneDX report unbound. Then add one independently chosen variation: another missing required field, a changed report byte, or a malformed subject structure. Record what you predicted and what actually happened.

## Write a bounded handover

State what was inventoried, by which tool/configuration, for which platform and image identity. Describe the separately copied helper, one unresolved identity discrepancy and what the smoke checks establish. Distinguish present files from code used by a particular request.

For an internal team, identify the release step and owner responsible for regenerating this evidence after packaging. For consulting, describe the collection scope and what a retest would need. Keep both career routes open; the lab is not a production assessment.

Allow 10–12 hours within the established 10–15-hour week: about 2 for reading/setup, 3 for builds and comparison, 3 for the verifier, and 2–4 for investigation, handover and delayed explanation. These are planning allowances. Carry a blocked download or unfinished investigation forward rather than extending the week without limit.

Record actual time, assistance and remaining gaps. On a later day, explain why a valid BOM and a matching tag can still be the wrong evidence for a selected image. Do not mark independent completion when you followed the supplied solution reasoning.
