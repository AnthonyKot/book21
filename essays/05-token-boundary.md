# A signed token answers only the question you validate

A billing service receives an access token for Alice. Later, someone controlling that service presents the token to our document API. The API checks the signature, recognizes the issuer, confirms that the token has not expired, and returns Cedar's invoice.

The token was not forged. It was used at the wrong destination.

This constructed case adds an authentication boundary to essay 4's tenant check. The invoice still belongs to Alice's tenant, so the resource lookup allows it. What failed earlier was the decision to accept this credential for the document API at all. A correct object-authorization check cannot compensate for every mistake in establishing the requester.

The [local Spring Boot lab](https://github.com/AnthonyKot/book21/tree/main/labs/05-token-boundary) demonstrates the failure and its repair. It reuses Cedar, Birch and the invoice boundary, but narrows the application to a read endpoint. There is no database, export worker or real identity provider in this fixture. That keeps the question visible: what must this API establish before it treats a token as Alice's credential?

## The claim that nobody read

The relevant payload fields in the billing token are:

```json
{
  "iss": "https://issuer.example.test",
  "sub": "alice",
  "aud": ["billing-api"],
  "token_use": "api-access"
}
```

This is an excerpt, not a complete token. The fixture also supplies issuance, not-before and expiration times. Its RSA signature is produced with the same synthetic issuer key used for legitimate document tokens.

`iss` identifies the issuer. `sub` identifies the subject within that issuer's identity system. `aud` names the intended recipient or recipients. Our document API expects `document-api`; the token says `billing-api`.

Suppose the billing service is compromised after receiving Alice's token legitimately. The attacker has the token, not the signing key. Altering its audience would invalidate the signature, but alteration is unnecessary if the document API never evaluates that audience. The API accepts the original, correctly signed statement and gives it a meaning the issuer did not assign.

RFC 8725 describes this class of substitution and requires recipient validation where an issuer serves multiple applications. The important observation in our example is that the distinguishing evidence was present in the token throughout. [JWT best practices, sections 2.7 and 3.9](https://www.rfc-editor.org/rfc/rfc8725.html#section-3.9)

## Separate four questions

A signed JWT in the compact format used here has a header, a payload and a signature. Decoding the first two makes their contents readable; it does not verify them. The signature protects integrity under a particular key and algorithm. It does not encrypt the payload or make every claim suitable for every application.

Follow the receiving path as four decisions:

| Decision | What the document API needs to establish |
|---|---|
| Cryptographic verification | Signature verifies under a key trusted for this issuer, using an allowed algorithm |
| Token acceptance | Issuer, recipient, time limits and required claims fit this API's token profile |
| Local identity | The validated issuer and subject correspond to a recognized account |
| Resource authorization | That account may perform this action on this invoice now |

Here a *token profile* means the claims and validation rules the application agrees to accept. The presence of a claim in a standard does not make it mandatory in every JWT application. Our profile explicitly requires an expiration and a nonempty subject, as well as the expected audience.

A signature check is meaningful only after deciding which key is trusted. The lab supplies one public key directly and permits RS256. It does not take a key or a key-fetching URL from the caller's token. Real deployments may use configured issuer metadata and key sets; those introduce trust and rotation decisions that this fixture does not exercise.

Similarly, `alice` alone is not a universal account identifier. This lab accepts exactly one issuer, then maps its subject into the local tenant directory. A multi-issuer system must preserve the issuer-subject relationship rather than treating equal subject strings as the same person.

## Reproduce the wrong acceptance

Use Java 21 and Maven. The [lab README](https://github.com/AnthonyKot/book21/blob/main/labs/05-token-boundary/README.md) contains the setup and commands. Run `mvn test` first, then start the service with audience checking disabled:

```sh
mvn -q package -DskipTests
java -jar target/token-boundary-1.0.jar --lab.audience-check=false
```

The fixture binds to loopback on port 8082. Its unauthenticated `/lab/tokens/{kind}` endpoint dispenses synthetic tokens for this exercise. That endpoint is deliberately not an authentication service to deploy; it lets you obtain each counterexample without building a login system. Signing keys exist only in memory and change on restart.

In another terminal:

```sh
billing_token=$(curl -fsS http://127.0.0.1:8082/lab/tokens/billing)
curl -sS -w '\n[HTTP %{http_code}]\n' \
  -H "Authorization: Bearer $billing_token" \
  http://127.0.0.1:8082/api/invoices/C-1001
```

The vulnerable mode returns Cedar's invoice with `200`. Restart with `--lab.audience-check=true`, obtain a new token from the restarted fixture, and repeat the request. It returns `401`. A `valid` token from `/lab/tokens/valid` still receives `200` for C-1001 and `404` for Birch's B-2001.

The restart instruction matters: a token signed before restart will fail under the new key. Reusing it would produce a denial for the wrong reason and would not demonstrate audience enforcement.

## Add a condition without removing the others

The decoder in both modes starts from the fixture's trusted public key. Its validation list retains issuer and timestamp checks:

```java
checks.add(JwtValidators.createDefaultWithIssuer(TokenFixtures.ISSUER));
checks.add(new JwtClaimValidator<Instant>("exp", value -> value != null));
checks.add(new JwtClaimValidator<String>("sub",
        value -> value != null && !value.isBlank()));
```

The repaired mode adds:

```java
checks.add(new JwtClaimValidator<List<String>>("aud",
        audiences -> audiences != null
                && audiences.contains(TokenFixtures.AUDIENCE)));
```

The expected audience is application configuration, not a request parameter. A missing audience must also fail. Comparing only with the first list element would impose a different policy from checking that this API is one of the intended recipients.

Finally, the list becomes the decoder's validator:

```java
decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(checks));
```

Read that operation carefully when reviewing a patch. Installing an audience-only validator could discard the issuer and timestamp checks previously used by the decoder. Our change composes the required checks explicitly. The tests keep the other rejection cases present so that adding one control cannot silently remove them.

Spring Security 7.1.1 documents both custom validators and Boot's audience configuration. This lab declares its own decoder to expose the mechanism; the code shown is not a claim that Spring automatically selects this application's audience. Its default timestamp validation allows 60 seconds of clock skew, so expired and future fixtures are placed ten minutes outside the accepted interval. Boundary tests would need a controlled clock. [Spring resource-server JWT validation](https://docs.spring.io/spring-security/reference/7.1/servlet/oauth2/resource-server/jwt.html)

## What the executed cases establish

The two test classes each run thirteen cases against a real HTTP server:

| Credential or request | Audience check off | Audience check on |
|---|---|---|
| Valid document token, Cedar invoice | 200 | 200 |
| Valid document token, Birch invoice | 404 | 404 |
| Billing audience | 200: flaw reproduced | 401 |
| Missing audience | 200: flaw reproduced | 401 |
| Missing bearer token | 401 | 401 |
| Expired | 401 | 401 |
| Not yet valid | 401 | 401 |
| Wrong issuer | 401 | 401 |
| Wrong signing key | 401 | 401 |
| Missing expiration | 401 | 401 |
| Missing subject | 401 | 401 |
| Blank subject | 401 | 401 |
| Unknown local subject | 403 | 403 |

The unknown subject passes the token checks but has no account in this application. Its `403` is an application decision, distinct from the `401` for an unacceptable credential. These statuses describe the fixture's policy; they are not a universal map for every authentication deployment.

For the negative control, run the repair assertions with audience checking disabled. Exactly the billing-audience and missing-audience assertions fail; the other eleven pass. That result shows the tests can distinguish this missing check while retaining legitimate access and the existing rejection cases.

This is narrower than proving a complete token implementation. We have not tested remote key discovery, rotation, every malformed claim, every algorithm, or a compromised issuer. The lab's tenant lookup still matters after authentication. Issuing Alice a document token does not let her read Birch's invoice.

## A reset token asks a different question

Now obtain the `reset` fixture. It has the document audience, but its signed `token_use` claim is `password-reset`. The audience-repaired API still accepts it. This is the deliberately unfinished boundary for your practice.

Audience answers where a token is intended to be accepted. Within that destination, different operations may need different credentials. Our hypothetical issuer has reused an audience across recovery and ordinary API access. The API must distinguish their purposes, or the issuer and consumers must adopt separate profiles that prevent the overlap.

`token_use` is an application-specific claim in this fixture, not a universal JWT field with automatic framework enforcement. A different system might distinguish token kinds through an explicit type, separate audiences or keys, or a defined protocol profile. The requirement is that a credential for one operation cannot silently become a credential for another.

Account recovery has another binding to inspect. Consider this illustrative, unexecuted review fragment:

```text
validateResetToken(request.token)
changePassword(request.username, request.newPassword)
```

Even a valid reset token is insufficient if the changed account comes from an unrelated form field. The recovery operation must bind the authorized subject to the account whose password changes. A token issued for Alice must not reset Bob, however the request spells its username.

Expiry also does not imply single use. Two requests can present the same valid credential before it expires. A recovery implementation needs a consumption rule and enforcement that prevents both from succeeding, including concurrent attempts. That is an additional state-transition problem; adding a purpose validator to our read-only API does not implement it. [OWASP recovery guidance](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)

## A credential can remain valid after the account changes

A signed token records claims issued at a particular time. Local verification does not, by itself, tell the API that Alice logged out, changed her password or lost a permission afterwards.

Compare a server-side session whose record the service checks on each request: disabling that record can affect the next lookup. A self-contained token can avoid that lookup, but then immediate revocation requires another mechanism or a different promise. Short expiration bounds some exposure; it does not mean the token was revoked at the moment of the account change.

Decide which events must affect access and when. Current account checks, a credential version, token denylisting or an issuer-mediated status check are possible design ingredients with different availability and state costs. The lab implements none of those revocation mechanisms. It uses stateless bearer authentication, while invoice authorization is evaluated separately against local tenant data.

This distinction connects back to the export worker. Preserving an old valid token would not establish the current document permission required by essay 2. Authentication evidence and the product's current access decision have different lifetimes.

<!--mission-->

## Practice: keep recovery credentials out of the API

Use the [worksheet](../practice/05-token-worksheet.md). Keep audience checking enabled. Write your own tests, in a new class extending `TokenHttp`, showing that `reset` and `no-purpose` tokens are refused by the invoice API while `valid` still works and every existing repair test remains intact. Observe your assertions fail against the current API first. Then decide the rule the acceptance profile needs, including what an absent value means, and enforce it without weakening any existing check.

Write a separate recovery review: trace a reset credential to the account it may change, its permitted effect and its consumption record. Specify the outcomes for Alice's token paired with Bob's username, a second use, two simultaneous uses, and ordinary API use. This part is a design artifact; no recovery endpoint exists in the lab.

Then attempt two independent Academy labs: [JWT authentication bypass via unverified signature](https://portswigger.net/web-security/jwt/lab-jwt-authentication-bypass-via-unverified-signature) and [Password reset broken logic](https://portswigger.net/web-security/authentication/other-mechanisms/lab-password-reset-broken-logic). The first tests a more basic missing boundary than our audience case; the second changes the credential format and workflow. Record hints or solution use as assistance.

Save your attempt before opening the [review notes](../practice/05-token-review.md), which hold the hints for both parts.

Completion means your evidence distinguishes an acceptable API credential from a recovery credential, preserves legitimate reads, and explains which recovery guarantees you have specified but not implemented.

Source note: inspected on 13 September 2026. Claim meanings and signed-token representation follow [RFC 7519, sections 3–4](https://www.rfc-editor.org/rfc/rfc7519.html). Issuer/subject binding and separation of token kinds follow [RFC 8725, sections 3.8–3.12](https://www.rfc-editor.org/rfc/rfc8725.html). Framework behaviour was checked against Spring Security 7.1.1 and executed on Spring Boot 4.1.1, Java 21.0.12 and Maven 3.9.16. The tokens, issuer, service, attack scenario and recovery fragment are original synthetic examples. The Academy assignments were inspected, not completed on the reader's behalf.
