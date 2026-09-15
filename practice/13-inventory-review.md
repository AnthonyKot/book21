# Release inventory review

Open this after saving your [worksheet](13-inventory-worksheet.md) assessment. Everything below counts as assistance once you have read it. If you are stuck partway, read only the next hint in section 2 and record that you did.

## 1. Judge the attempt before comparing verdicts

A sound assessment shows the following.

**Facts checked, not read.** For each record, the hash comparison was computed from the report bytes, and the subject was read from inside the report rather than from `claimedSubject`. A table that repeats the records' own descriptions is not an assessment.

**The artifact kind named.** The assessment distinguishes an image inventory from an application dependency inventory, and a scan of the selected image from a scan of a related object: the same tag at another time, the same release name for another platform.

**An answer that cites only what can carry it.** Agreeing records that describe something else are listed as not supporting the answer, even when they agree.

**A useful next request.** The sign-off request targets the remaining gap: for example, the untrimmed native scan with its configuration, or direct inspection of the helper JAR inside the selected image by digest.

**Honest limits.** Even the right record describes files present in one platform's merged filesystem at scan time. It does not show reachability, other platforms, later mounts or downloads, or who produced the record.

## 2. Progressive hints, if you are stuck

1. For each record, compute the SHA-256 of its report file and compare it with `reportSha256`. Then open the report and find the subject it names: `source.metadata.imageID` in Syft JSON, `metadata.component` in CycloneDX.
2. Compare each subject with `selection.json`. For records that do not name an image, ask what their generator can see. For records naming a platform, compare it with the selection.
3. Answer the advisory question using only the records whose subject, platform and bytes you could establish. Check what the others say about the helper, and why that cannot count.

## 3. What each record is

| Record | Facts | Verdict |
|---|---|---|
| R1: release pipeline job 412 | Hash matches. The native scan names the selected image ID; linux/amd64, squashed. Excerpt of this lab's measured scan of image B. Helper 1.1.0 at `/opt/book21/tools/export-helper.jar`. | **describes** |
| R2: release pipeline job 409 | Hash matches, and it claims the same tag as R1. The report names a different image ID: image A, scanned before the tag moved. Helper 1.0.0. | **does-not-describe**: the right label at the wrong time |
| R3: application build | Hash matches. CycloneDX from the Maven plugin; the subject is the application module. Lists Spring dependencies; no helper, no Alpine packages, no image identity. | **partial**: true for the application's dependency graph, not an inventory of the image |
| R4: compliance export | Claims the selected image ID, and its CycloneDX metadata names it. But the recorded hash does not match the report bytes, and the report lists helper 1.0.0 where the pipeline scan of the same image shows 1.1.0. The file was changed after its hash was recorded, and the edited helper entry still carries identifiers from the 1.1.0 artifact; this condition is authored. | **cannot-rely** (or does-not-describe): content that disagrees with its own recorded hash is not evidence of anything |
| R5: registry scanner | Hash matches. The report names a different image ID and an OCI index digest for the release name; linux/arm64. Authored: the lab built only amd64. It shows helper 1.1.0. | **does-not-describe**: another object and platform, even though it agrees |

**Release answer:** no. The selected image contains helper 1.1.0, not 1.0.0, supported by R1 alone. R2 and R4 say 1.0.0 but cannot carry the answer; R5 agrees but describes another platform; R3 cannot see the helper at all.

**Before signing off**, a reasonable request is the complete native scan for R1 with its configuration file, or an independent look at the helper JAR's embedded Maven properties inside the image selected by digest. R1 is one excerpted scan from one producer, and nothing here authenticates that producer.

## 4. Assessments that look finished

Each was run through `review_check.py`.

| Assessment | Result | What went wrong |
|---|---|---|
| The verdicts in section 3 | 0 failures | None. |
| R1, R2 and R4 accepted as describing the release, answering "yes, 1.0.0 present" from R2 and R4 | 3 failures | Trusted the claimed subject and label; never compared report bytes or the subject inside the report. |
| The application BOM and the arm64 scan counted as image evidence, supporting "no" with R1, R3 and R5 | 3 failures | The right conclusion, reached with evidence that cannot carry it. |

The second row is the essay's mechanism in miniature. The tag was right and the component list was well formed, and the evidence still belonged to a different image.

## 5. Reference evidence

Recorded while authoring. None of it is evidence of your attempt.

- Guided: `verify.py` 21/21; the negative inventory control fails the 4 helper and `musl` checks; `pairing_tests.py` 9 pass over the two real records.
- Measured on Docker Engine 29.1.3 with the overlay2 image store: image B's local ID equals the SHA-256 of its configuration blob, checked from an exported image.
- The packet was generated from the lab's measured scans; R4's alteration, R5's scan, producers, times and the advisory are authored.
- `review_check.py`: the section 3 verdicts pass; the two assessments in section 4 fail 3 checks each.

To compare after your attempt:

```bash
python3 review_check.py --assessment your-assessment.json
```

It recomputes hashes and subjects, then checks your verdicts and release answer. It cannot judge your reasons or your request for further evidence.

## 6. Limits

Excerpts keep subjects and relevant components but omit most packages and the scanner configuration. The packet does not test registry manifests end to end, multi-platform builds, signed attestations or vulnerability databases. A hash binds bytes to a record; it does not establish an honest producer.
