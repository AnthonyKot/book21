# Inventory the artifact that was built

Runnable companion to [essay 13](https://anthonykot.github.io/book21/essays/13-build-inventory.html). This is an authored local packaging exercise. It creates two images and compares Maven dependency evidence with final-image inventories. No CVE, exploitability or complete-inventory claim is made.

## Setup

Measured environment: Linux/AMD64, Docker Engine 29.1.3, Java 21.0.12, Maven 3.9.16, Python 3.12, curl, Syft 1.51.1; Spring Boot 4.1.1, CycloneDX Maven plugin 2.9.3 and CycloneDX 1.6 output. Use a working Docker daemon and Java 21/Maven/Python/curl on PATH. The Dockerfile pins the Temurin Java 21 Alpine base by registry digest and the runner selects `linux/amd64`. Other platforms were not tested. Downloads require network access and storage for the runtime image, Maven dependencies and two local build contexts.

Install Syft 1.51.1 using the [official release](https://github.com/anchore/syft/releases/tag/v1.51.1), verifying the downloaded archive against the release checksums before extraction. For the measured Linux/AMD64 environment, from this lab directory:

```bash
mkdir -p .tools
curl -L -fsS https://github.com/anchore/syft/releases/download/v1.51.1/syft_1.51.1_linux_amd64.tar.gz -o .tools/syft_1.51.1_linux_amd64.tar.gz
curl -L -fsS https://github.com/anchore/syft/releases/download/v1.51.1/syft_1.51.1_checksums.txt -o .tools/checksums.txt
(cd .tools && sha256sum --check --ignore-missing checksums.txt && \
  tar -xzf syft_1.51.1_linux_amd64.tar.gz syft)
```

Stop if checksum verification fails. The following commands all run from `labs/13-build-inventory` in a clone of the book. If Syft or Maven is elsewhere, pass its absolute path with `--syft` or `--maven`.

## Build, observe and compare

```bash
python3 run.py --syft "$PWD/.tools/syft"
python3 verify.py
python3 record.py
```

The runner builds the application once, resolves its dependency tree, builds helper 1.0.0 and 1.1.0 separately, and copies the identical application JAR into two image contexts. It generates native Syft JSON and CycloneDX 1.6 JSON from each image ID. It runs two actual HTTP smoke checks, invokes each packaged helper separately, and stops both containers. Published ports bind only to host loopback. The tiny unauthenticated `/status` endpoint has no production data.

The runner creates or replaces only these lab tags: `book21-inventory:a`, `book21-inventory:b`, and `book21-inventory:candidate`. At the end it moves `candidate` from A to B and records both observed IDs. It does not push images. Output goes to ignored `out/`; rerunning replaces that evidence, so copy it elsewhere first if retaining an attempt. Maven builds use `clean` on these two lab projects.

Expected `verify.py`: **21/21 pass**. There are no application unit-test counts in this lab; the two HTTP and two helper invocations are smoke checks, while the 21 checks compare produced artifacts. Maven's application BOM has 39 library components. Each image has 112 native package records; each CycloneDX image document has 1,336 components, including 1,223 files. These are observed counts from the pinned fixture, not comparable denominators for completeness.

The retained [observed.json](observed.json) summarizes the authoring run. Your image IDs may differ: use your local output, not copied snapshot IDs. The application hash must match between your A and B; helper hashes and image IDs must differ. Maven's application BOM excludes the independent helper and base operating-system packages. Final image inventories must include the corresponding helper version, `musl`, and the legitimate Spring Web MVC dependency.

Inspect:

- `out/maven.cdx.json` and `out/dependency-tree.log`: application dependency views, with test/provided/system scopes excluded from the BOM.
- `out/a/` and `out/b/`: exact copied JARs, Docker inspection, native scan, exchange document, smoke observations and pairing record.
- `out/tag-move.json`: actual local tag resolution before and after moving the candidate tag.
- `out/*build*.log`, `out/image-*.log`, `out/scan-*.log`: build/scanner diagnostics.

The plugin output path is set on its execution because Spring Boot's parent configures an execution-level path of its own. Our final BOM is `app/target/bom.json`; the runner copies it to `out/maven.cdx.json`.

## Negative inventory control

```bash
python3 verify.py --negative
```

Expected nonzero exit: **17 pass, 4 fail**. The helper and base `musl` expectations fail for each image when the application Maven BOM is substituted as the candidate final component list. The scanner identity checks still examine the real raw scans. This mutation tests component-scope acceptance; it does not simulate scanner failure or corrupt a source report.

## Inspect one immutable image manually

```bash
image_id=$(docker image inspect book21-inventory:a --format '{{.Id}}')
.tools/syft scan "docker:$image_id" --config syft.yaml --scope squashed \
  -o syft-json=out/manual.syft.json \
  -o cyclonedx-json@1.6=out/manual.cdx.json
```

The configuration disables app-update checks, online Java enrichment, host Maven repository consultation and transitive resolution from archive POMs. Preserve the config with results. A native scan retains subject identity and locations that help investigate differences. The CycloneDX format also includes file components; filter according to the comparison question.

## Independent task: stale pairing after a tag moves

Use the [worksheet](https://anthonykot.github.io/book21/practice/13-inventory-worksheet.html) before the review. Generate records first, then:

```bash
python3 exercise_tests.py
```

The public `identity_exercise.py` deliberately compares only the tag: **3 pass, 6 fail**. Modify that verifier, preserving the generated fixtures and tests. Accept both correctly paired images and reject a stale record after a tag move, swapped native or CycloneDX reports, a relabeled record with the wrong scanner subject, a missing required field, and malformed native JSON even if its hash was recorded. The selected image ID is an independent input supplied by the caller. The private reference passed **9/9**; removing its source-ID comparison caused **1 failure / 8 passes**.

The exercise verifies correspondence of two report byte strings to a recorded image. It is not comprehensive CycloneDX schema validation, a signed attestation, or verification of an untrusted producer. The sidecar does not make coordinated edits to an image, reports and record trustworthy. Configuration and app hashes are context fields; the exercise's acceptance contract checks image selection and both report hashes, not every context field.

## Limits and cleanup

The helper is a separately invoked utility; the web application's entrypoint does not load it. Identifying it in the image is not a reachability result. Some package identifiers are inferred from archive metadata; even Maven namespaces can differ between the two tools. The comparator handles only its known Maven JAR URL variants, not arbitrary namespace aliases or qualifiers.

The scan covers the merged filesystem of one Linux/AMD64 image. It does not describe host packages, future volumes, downloaded plugins, subsequent container writes, or every historical layer. No vulnerability database or security benchmark was run. No byte-for-byte rebuild guarantee is claimed across environments.

Containers are stopped and automatically removed by the runner. Images and generated output remain for practice. When finished, remove the three named lab tags with `docker image rm book21-inventory:candidate book21-inventory:b book21-inventory:a`; retain your evidence before deleting `out/`. The base image may be shared with other work, so leave its cleanup to your normal Docker maintenance.
