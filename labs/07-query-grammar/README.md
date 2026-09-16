# Query grammar — essay 7 lab

A synthetic, intentionally vulnerable training service. Java 21, Maven 3.9.16, Spring Boot 4.1.1, Spring Framework 7.0.9, Spring Security 7.1.1 and H2 2.4.240 were used during authoring. Dependencies are pinned by the Boot parent. No external database, browser tooling or paid API is required. Initial dependency downloads need internet access.

Run only on loopback. The single account is `alice` / `local-only`; it maps to Cedar in server code. Basic authentication over local HTTP is a fixture convenience, not a production authentication design. No real data or credentials belong here. The in-memory database contains three Cedar invoices and one Birch invoice, recreated on restart. No worker or production database deployment is modeled.

From this directory, with Java 21 and Maven on PATH:

```sh
mvn test
mvn -q package -DskipTests
java -jar target/query-grammar-1.0.jar --lab.bind-title=false
```

The server binds to `127.0.0.1:8085`. In a second terminal:

```sh
curl -sS -u alice:local-only --get \
  --data-urlencode "title=Office" http://127.0.0.1:8085/api/search
curl -sS -u alice:local-only --get \
  --data-urlencode "title=' OR 1=1 -- " http://127.0.0.1:8085/api/search
curl -sS -u alice:local-only --get \
  --data-urlencode "title=O'Brien" http://127.0.0.1:8085/api/search
```

Expect Office to return C-1001; the injected predicate to return all four rows, including B-2001 (order unspecified because the injected comment removes ORDER BY); the apostrophe to produce HTTP 500 with a generic error. Add `-w '\n[HTTP %{http_code}]\n'` to show status codes.

Stop with Ctrl-C and restart with `--lab.bind-title=true` (also the default). Office still works, the attack returns HTTP 200 with `[]`, and O'Brien returns C-1002. Search uses exact equality: `%` is not a wildcard here. API responses contain synthetic invoice records, not just IDs.

The normal suite contains 22 real-HTTP cases across reproduction and repair classes. Reproduction tests pass by confirming the flaw. This negative control must exit nonzero:

```sh
mvn -Dtest=QueryRepairTest -Dtest.bind-title=false test
```

Expected: three failures (two injections and the apostrophe), eight passes, zero errors. Run `mvn test` to restore passing test reports afterward.

## Deliberate limits and practice

`GET /api/invoices/B-2001` denies Alice with 404. `GET /api/unscoped/B-2001` returns it despite using a bound ID: it is a deliberately faulty authorization counterexample in both modes. Do not treat `bind-title=true` as a secure mode for the entire project.

`GET /api/sorted?sort=amount` is the practice endpoint. Its contract, stated in `practice/07-query-worksheet.md`, permits exactly `id`, `title` and `amount`, ascending, with ID as the final tie-breaker; missing or empty sort defaults to `id`; unknown keys and expressions must receive 400. Keep the title repair and tenant predicate. The review guide holds the hints and worked answer; read it after saving your attempt.

A post-attempt check is excluded from the default `mvn test` and gated behind a system property. Leave `SortContractCheck.java` closed until your attempt and your own regression are saved, then run:

```sh
mvn -DreviewCheck=true -Dtest=SortContractCheck test
```

The authoring reference passes its six cases plus the 22 guided cases. It remains private; no reader completion is implied.

The database user is a fixture bootstrap account, not a least-privilege production role. H2 reproduces this mechanism; it is not evidence about every SQL dialect, ORM or driver. No database console is exposed. Error bodies hide SQL details but do not prevent injection. The app retains default CSRF settings and exposes only GET handlers.
