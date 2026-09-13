# Browser authority — essay 6 lab

Synthetic, intentionally vulnerable local training fixture. Java 21; Spring Boot 4.1.1 / Spring Security 7.1.1; Maven 3.9.16 used during authoring. The [official starter list](https://docs.spring.io/spring-boot/reference/using/build-systems.html#using.build-systems.starters) documents the `spring-boot-starter-webmvc` coordinate used here. Browser runner: Node 22.21.0, Playwright 1.61.1 and its Chromium headless shell 149.0.7827.55. Dependency downloads require internet access; no external services are used by the running examples.

One session-local Cedar invoice flag stands in for an approval. There is one user, `alice` / `local-only`. No real customer data, persistent store or identity provider. Do not expose either server beyond loopback or reuse the credentials/configuration in production. Stop with Ctrl-C after manual work. All sessions and approval flags disappear on application restart.

Run from this directory with Java 21+, Maven and Node 22 available:

```sh
mvn test
mvn -q package -DskipTests
npm ci
npx playwright install chromium --only-shell
npm run test:browser
```

On Linux, install the Playwright system dependencies if the browser reports missing libraries; `npx playwright install-deps chromium` uses your OS package manager and may require administrator access. The runner needs free loopback ports 8083/8084. It runs all three modes and stops the servers afterward. Do not leave a manually started application using those ports during automation.

Expected: 12 Java HTTP tests pass and 18 browser scenarios pass. Reproduction tests assert the vulnerable outcome intentionally; green reproduction tests do not mean secure code. Java tests use random server ports and a fresh logged-in session per test. Browser scenarios also use fresh sessions. There is no worker case in this browser-focused fixture.

Negative controls (these commands should exit nonzero):

```sh
mvn -Dtest=BrowserRepairTest -Dtest.csrf=false test
npm run test:browser -- --negative
```

Expected: two failures/four passes for Java; four failures/two passes for Chromium. The browser negative control runs final-repair expectations while disabling both guided controls. Restore `mvn test` afterward if you want the last Surefire report to represent the passing suite.

Manual reproduction, in separate terminals:

```sh
java -jar target/browser-authority-1.0.jar --lab.csrf=false --lab.safe-render=false
```

```sh
node attacker.mjs
```

Sign in at <http://127.0.0.1:8083/login>, then open <http://127.0.0.1:8084> in the same browser and submit the forged form. Expect approval. Restart the application with `--lab.csrf=true --lab.safe-render=false`, sign in again, and repeat: expect 403 and `approved:false` at `/api/state`. The application's own approval button works in both modes. Use a fresh browser context/session when comparing initial states: approval stays true within a session once set.

The browser runner contains the executable `note` payload and checks its actual approval effect with CSRF protection enabled. To inspect it manually, put that payload into the trusted page's `note` query parameter using URL encoding. The final configuration is `--lab.csrf=true --lab.safe-render=true` (also the defaults). It inserts note text without parsing markup.

Both local ports are different origins but same-site. The session cookie is explicitly Strict and HttpOnly. Cookies have no port isolation: the untrusted-page server deliberately does not log or use cookie values. No CORS response access is granted, and no CSP is configured. HTTP loopback does not test Secure-cookie/TLS deployment, hostile sibling-host cookie isolation or other browser engines.

## Changed practice

`/summary?title=...` deliberately remains vulnerable through `src/main/resources/static/summary.js` in every mode. Preserve a real `strong` element while inserting the title as text. The existing `--practice` checks expose this unfinished task:

```sh
npm run test:browser -- --practice
```

Initially one fails and one passes. After your repair and a fresh `mvn -q package -DskipTests`, both should pass. Then rerun the normal browser and Java suites. Do not count a supplied test or hinted repair as an independent discovery. Add a changed payload or a structural assertion of your own and explain what it tests.
