# Review the whole response path — essay 11 lab

Java 21, Maven 3.9+, Python 3 and curl. Executed on Java 21.0.12, Maven 3.9.16, Spring Boot 4.1.1, Framework 7.0.9, Security 7.1.1 and H2 2.4.240. Loopback port 8092. Synthetic users `alice` (Cedar) and `bob` (Birch), both password `local-only`. No real documents or external requests.

Read [review/BRIEF.md](review/BRIEF.md) and [review/candidate.diff](review/candidate.diff) before the walkthrough if you want a first unassisted review. The diff is a prepared comparison; its tenant-scoped candidate is the actual runnable repository. The unchanged shared cache is the missing context.

```sh
mvn '-Dtest=ReviewReproductionTest#ownCold+foreignCold' test
mvn test
mvn -q package -DskipTests
java -jar target/security-property-1.0.jar --lab.partition=false --lab.fixtures=true
```

The two cold-cache tests pass despite the shared-cache flaw. The full default suite runs 20 cases:10 reproduction and10 repair. Reproduction tests intentionally expect the leak. In another terminal, from this directory:

```sh
python3 demo.py
```

The script executes curl, obtains Alice’s CSRF token and cookie for fixture resets, and records actual statuses, response bodies and cache state. It verifies reset requests succeeded; authoring checks separately asserted all expected read results. Sequence:

1. Bob requests C-1001 before it is cached:404.
2. Alice requests it:200, `CEDAR-PRIVATE-SUMMARY`.
3. Bob requests it again:200 and Cedar’s summary in candidate mode.
4. Alice repeats:200 without another body load.

It then repeats in the other direction with B-2001 and checks an anonymous request gets401. Stop Java and restart with `--lab.partition=true --lab.fixtures=true` (partition=true is default). Both foreign reads now return404, before and after another tenant warms the cache. Owner repeats still avoid a body reload.

`bodyLoads` counts repository body-load attempts, including denied misses; it is not every SQL query or a performance benchmark. A warm unauthorized hit in candidate mode does not invoke that loader. The cache is an application `ConcurrentHashMap`, not browser/CDN caching.

## Manual curl sequence

On a freshly started candidate-mode server:

```sh
curl -sS -u bob:local-only -w '\n[HTTP %{http_code}]\n' http://127.0.0.1:8092/api/documents/C-1001/preview
curl -sS -u alice:local-only -w '\n[HTTP %{http_code}]\n' http://127.0.0.1:8092/api/documents/C-1001/preview
curl -sS -u bob:local-only -w '\n[HTTP %{http_code}]\n' http://127.0.0.1:8092/api/documents/C-1001/preview
```

Expect404/200/200; repaired mode404/200/404. All requests use real HTTP authentication. Caller-supplied `?tenant=cedar` is ignored. Principal-to-tenant mapping is fixed for these two synthetic users; membership changes and ownership transfers are not implemented.

## Repair and negative control

The guided repair keys cached data by both tenant and document ID. It retains the scoped repository query and derives tenant server-side. A cache miss throws404 when no row matches. A mapping function throwing an exception does not establish a cache entry.

```sh
mvn -Dtest=ReviewRepairTest -Dtest.partition=false test
```

Expected: two warm cross-tenant failures, eight passes, no errors. The cold-cache denials remain green. Turning off caching would also prevent this leak, but the guided repair preserves repeated own-tenant hits. There is no size/TTL policy, content-update API, membership change or distributed invalidation in this fixed three-document fixture.

## Independent exercise: archived downloads

Preview returns a short summary, never the full-content field. Download returns full content. Policy permits an owner’s summary even after archive, but forbids full downloads when the current authorization check observes archived state. The public download implementation is deliberately unfinished even with `lab.partition=true`.

```sh
python3 demo.py --exercise
mvn -Dtest=ArchiveExercise test
```

The fixture archive command commits before the next request and deliberately leaves cached data in place. The starter incorrectly returns full content after archive, both with an existing cache entry and with an empty cache. Two of six exercise tests fail. Repair `DownloadService.download` by using the existing current-state authorization query before returning full content, while retaining tenant isolation, visible archived summaries and downloads of unaffected documents. Do not use a cached decision as current authorization.

After repair, run all 26 cases:

```sh
mvn '-Dtest=*Test,ArchiveExercise' test
```

`ArchiveExercise` intentionally falls outside Maven’s default naming filter. The private reference was executed but is not published. These are sequential archive-then-download cases. The policy decision occurs at the fresh check; no claim is made about aborting a response already authorized before a concurrent archive.

## Fixture controls and limits

`lab.fixtures=false` by default disables `/lab/state`, `/lab/reset` and `/lab/archive/{id}` with404. Enable only for this local demonstration. All endpoints require authentication and the fixture POSTs retain CSRF protection. Archive is tenant-scoped; reset clears all synthetic state for repeatability, not a production administration feature. Run demos sequentially and never reset with requests in flight.

No HTTP intermediary cache, multi-process deployment, timing channel, concurrency benchmark, eviction pressure, production revocation propagation or content-mutation consistency is tested. The map’s concurrency properties do not establish authorization. The preview loader also caches full content internally; the preview HTTP response selects only the summary. For production, assess whether that additional cached data is necessary.
