# An SBOM records what a particular build contains

The application JAR has not changed. Its Maven dependency BOM has not changed. The image contains a different version of a utility.

In this constructed release, all three statements are true. Maven packages a small document service, then a container build copies another JAR beside it. Later, that utility changes from version 1.0.0 to 1.1.0. The application still starts and returns the same response. An inventory taken before the copy cannot describe the whole image, however accurately it describes the application's dependencies.

That is the question this essay investigates: **which artifact does this inventory describe?** Before counting components or searching for a vulnerable version, establish the object being counted.

## Follow the extra copy

The [local lab](https://github.com/AnthonyKot/book21/tree/main/labs/13-build-inventory) contains two independent Maven projects. `app` is a Spring Boot service. `helper` is an author-owned command-line utility named `export-helper`. The application does not declare the helper as a dependency. The container's packaging instructions bring them together:

```dockerfile
WORKDIR /opt/book21
COPY app.jar app.jar
COPY export-helper.jar tools/export-helper.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/opt/book21/app.jar"]
```

This excerpt follows a `FROM` line pinned to a Java 21 runtime image by registry digest; the full Dockerfile is in the lab. The image includes an Alpine userspace and Java runtime in addition to the two copied JARs.

The helper is intentionally separate. The lab invokes it explicitly with `java -cp` to verify that the packaged utility runs. The web application's normal entrypoint does not load it. That distinction matters: *present in the image* and *used by this request* are different claims. An inventory can establish useful evidence for the first without establishing the second.

Run the README's setup, then:

```bash
python3 run.py --syft "$PWD/.tools/syft"
python3 verify.py
python3 record.py
```

The runner builds application A and B from the same application JAR, changing only the helper version supplied to the packaging step. It scans each image by its local image ID, checks the service over loopback HTTP, invokes the helper, and stops its containers. It also moves the lab's `candidate` tag from A to B so the later exercise can reproduce a stale association. Nothing is pushed to a registry.

The guided verification passes **21 checks**. The important observations are small enough to inspect directly:

| Evidence | Application Maven BOM | Image A inventory | Image B inventory |
|---|---|---|---|
| Spring Web MVC 7.0.9 | Present | Present | Present |
| `book21:export-helper` | Absent | 1.0.0 | 1.1.0 |
| Alpine `musl` package | Absent | Present | Present |
| Application JAR SHA-256 | Same input used for both images | Same bytes | Same bytes |
| Local image ID | Not the object Maven inventoried | A | B, different from A |

The helper's image location is `/opt/book21/tools/export-helper.jar`. Its embedded Maven properties identify the version. Those properties, the copied file and the scanner result provide an inspectable explanation for the difference. We have not inferred the helper from a filename alone.

## Three useful views of one release

The source POM is a declaration. It names direct dependencies and includes a test starter. Maven resolves that declaration into a dependency graph, including dependencies introduced by other dependencies. Packaging then determines which files enter an artifact. A later image build can add a runtime, operating-system packages or files from elsewhere.

Our CycloneDX Maven plugin runs against the application project and includes compile and runtime scopes. Test, provided and system scopes are explicitly excluded. The resulting BOM contains 39 components. It includes resolved Spring dependencies that were not each written as a direct dependency in our POM; it excludes the declared test starter and its test dependencies. These are deliberate scope choices, not discoveries that those dependencies never existed. The pinned plugin is **2.9.3**, generating CycloneDX **1.6**. [Plugin scope parameters and pinned implementation](https://github.com/CycloneDX/cyclonedx-maven-plugin/blob/cyclonedx-maven-plugin-2.9.3/src/main/java/org/cyclonedx/maven/BaseCycloneDxMojo.java).

The application BOM has no reason to include an independent project that a later Dockerfile copies. The missing helper reveals a mistake only when someone presents that BOM as an inventory of the finished image. The repair is to generate and retain evidence at that later boundary, with a clear relationship to the earlier dependency view.

A software bill of materials is a machine-readable representation of components and their relationships. CycloneDX supplies a format for expressing that information. Selecting the format does not choose the input artifact, discover every component or validate the truth of each identity. Those responsibilities still belong to the generation and verification process. [CycloneDX SBOM overview](https://cyclonedx.org/capabilities/sbom/).

## Ask the scanner about the final image

The runner resolves the local image ID before scanning. For a manual inspection from the lab directory:

```bash
image_id=$(docker image inspect book21-inventory:a --format '{{.Id}}')
.tools/syft scan "docker:$image_id" --config syft.yaml --scope squashed \
  -o syft-json=out/manual.syft.json \
  -o cyclonedx-json@1.6=out/manual.cdx.json
```

This lab pins **Syft 1.51.1**. The explicit `docker:` source selects the local daemon. The `squashed` scope examines the merged image filesystem. The configuration disables update checks, online Java enrichment and consultation of the host's Maven repository; the runner saves both native Syft JSON and CycloneDX JSON from the same scan. Syft supports image and filesystem targets and can inspect Java archives, including nested archives such as the libraries inside a Spring Boot JAR. [Scan targets](https://oss.anchore.com/docs/guides/sbom/scan-targets/), [Java cataloging configuration](https://github.com/anchore/syft/blob/v1.51.1/syft/pkg/cataloger/java/config.go).

The measured image inventory contains **112 package records**: 73 Alpine packages, 38 Java archives and one binary package. Its CycloneDX representation contains **1,336 components**, including 1,223 file components and an operating-system component. Comparing either number directly with Maven's 39 libraries would answer a poorly specified question.

Compare identities and evidence for the components you mean. The same Spring Web MVC package appears with a Maven package URL in both reports, although Maven's URL includes a `type=jar` qualifier. The lab comparator accepts those two explicit forms for its known JAR fixtures. It does not erase arbitrary classifiers or qualifiers from every package identity.

Other discrepancies need investigation. In this run, some Spring Boot module namespaces differ between Maven's resolved coordinates and Syft's inferred package URLs. A string-set difference is therefore not sufficient evidence that a library file is missing. Native Syft output retains locations that help trace a result back into the image. Preserve it rather than assuming an exchange-format conversion retains every useful detail in the same form.

The final scan identifies the separate helper and the base `musl` package. To verify that the acceptance checks depend on the later inventory, run:

```bash
python3 verify.py --negative
```

This deliberately substitutes the application Maven BOM where the checks expect the final image's component list. **Four checks fail**: the helper and base-package expectations for A and B. The legitimate Spring dependency checks still pass. This is an incomplete inventory for the stated target, not evidence that the Maven plugin malfunctioned.

## Record the identity, not just the label

After building B, `book21-inventory:candidate` names B. It previously named A. The application bytes and human-readable tag provide insufficient evidence that an older report belongs to the selected image.

`record.py` writes a local pairing record for each image. It includes the local image ID and platform, hashes of both inventory files, the application JAR hash, the scanner version, filesystem scope and configuration-file hash. Before writing, it checks that the native scanner source identifies the inspected image.

The two application hashes match. The two image IDs differ. That is the concrete reason an application hash cannot stand in for an image identity. The [observed snapshot](https://github.com/AnthonyKot/book21/blob/main/labs/13-build-inventory/observed.json) contains the full values from this authoring run; your rebuild can produce different image IDs, so use the values generated locally.

There are several different digests in this workflow. A local image ID hashes the image configuration, which references filesystem layers. A registry manifest digest and a multi-platform index digest identify different objects. The base-image pin in our Dockerfile is a registry digest; the release-pairing exercise uses local image IDs obtained from Docker inspection. It does not claim these values are interchangeable. [OCI image identity](https://github.com/opencontainers/image-spec/blob/v1.1.1/config.md#imageid), [Docker manifest and platform distinctions](https://docs.docker.com/dhi/explore/security-concepts/digests/).

A matching hash establishes equality with recorded bytes. It does not establish who produced the record or whether the scanner described those bytes accurately. Someone who can replace the image, reports and record together can create another internally consistent set. This unsigned local sidecar is an association check, not a signed attestation or a trustworthy release pipeline. The build-authority chapter will take up that broader boundary.

## Keep the useful limits attached

The repaired workflow retains the application dependency BOM and adds an inventory of the final image. Both have a named scope. Neither proves that all components have been recognized correctly, that every possible dependency is present, or that the application cannot fetch more code later.

The image scan does not include future mounted volumes, writable-container changes, the host kernel or a plugin downloaded after startup. We measured one Linux/AMD64 image variant and a merged filesystem view. Historical layers and other platforms require deliberately different questions. The simple smoke checks establish that these images start and the separate utility can execute; they are not an assessment of application security or vulnerable-code reachability.

No vulnerability database was queried in this essay. A component inventory will help investigate a later advisory, but presence alone does not establish exploitability, priority or a suitable fix. Those decisions belong to the next essay. The current deliverable is narrower: another engineer can identify which artifact was examined, reproduce the component discrepancy, and detect when the report no longer matches the selected image.

<!--mission-->

## Exercise: the tag still matches

Open the [worksheet](../practice/13-inventory-worksheet.md) before the [review guide](../practice/13-inventory-review.md). `identity_exercise.py` contains a deliberately weak verifier: it accepts a record when the tag string matches. Run `python3 exercise_tests.py` after generating the records. The starter passes three checks and fails six.

Repair the verifier so a matching A pair and a matching B pair remain acceptable, while stale records, swapped reports, missing required evidence and an inconsistent scanner subject are rejected. The selected image ID is supplied independently of the record. Check both report hashes and the subject inside the native scan; do not merely copy the requested ID into an old record. An unpublished reference was executed: all nine checks pass. Removing its scanner-subject comparison makes the relabeled-record check fail.

Reserve about **10–12 hours within the existing 10–15-hour weekly budget** for setup, inventory comparison, the independent repair and a short handover. This is an allowance to calibrate, not a measured completion time; initial image and Maven downloads may require extra setup time in another week.

For an employment portfolio, show where this inventory step would sit in a team's release process and who investigates discrepancies. For consulting, name the artifact, platform, collection method and exclusions in the handover. Keep the source revision and assistance used with your evidence. Both routes benefit from a precise inventory claim that survives the next build.
