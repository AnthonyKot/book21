# Essay 5 — review after attempting the worksheet

The programming task adds one condition to token acceptance. The recovery task asks you to specify a different operation; a passing API suite does not establish a working password-reset system.

## The API must distinguish the purpose

Before repair, both `reset` and `no-purpose` should reach the invoice endpoint as Alice and return Cedar's data. Your new assertions should fail because they expected refusal. A compilation error, stale token signed before restart or server startup failure does not demonstrate the missing purpose check.

A sound repair requires `token_use=api-access` alongside the existing validators. It handles a missing value as refusal. It does not substitute a purpose-only validator for issuer, audience or timestamp validation, and it does not defer the check to one invoice controller method.

The reference solution adds a null-safe claim validator to the existing list. Its two new tests were run before the repair and failed with actual 200 responses, then the full suite was run after the repair. The code remains unpublished so the worksheet still requires an implementation decision. Exact results are recorded in the author's evidence rather than treated as proof of your solution.

The existing audience reproduction tests should still pass: their `billing` and `no-audience` tokens have the correct API purpose. The purpose rule repairs a different missing condition without concealing the audience demonstration. The ordinary valid token must still retrieve Cedar's invoice and remain unable to retrieve Birch's.

## The account must come from the authorized recovery operation

The review fragment validates one credential and then changes an account selected separately. Your design must remove that disconnect. It can derive the target account from a protected recovery record or from authenticated claims whose purpose and recipient have been checked. If a username remains in the request, it must not redirect the operation to another account.

For Alice's token paired with Bob's username, either reject the mismatch or eliminate the unrelated account selector and act only on the authorized target. Your API contract should make the chosen behaviour unambiguous. Bob's password must not change.

The recovery credential should authorize a bounded password-change operation. It must not become a general login or invoice credential merely because it names a recognized user. This is why a purpose rule matters even after the audience check passes.

## One use is a state-transition guarantee

Expiry answers whether a credential is still within its time window. It does not record consumption. A unique token ID also does not enforce one use without a checked state transition.

A candidate design records a protected recovery request and atomically transitions it from usable to consumed while authorizing the password change. Explain how concurrent requests contend for that transition. Two independent checks followed by two writes can both succeed; merely writing `used=true` eventually is insufficient.

Also explain failure and retry: if consumption succeeds but the password update fails, what outcome can the user safely retry? A transaction may combine these operations when they share a suitable store. A distributed design needs its own failure policy. None of these mechanisms is implemented by the read-only token lab.

## Account changes and old credentials

State whether reset invalidates existing sessions, access tokens, both, or neither immediately. Explain the mechanism and timing. A local JWT signature check does not observe a password database update by itself. The design must connect the reset event to subsequent access decisions if immediate invalidation is promised.

Keep “token expires soon” separate from “the next request after reset is denied.” Each can be a chosen policy, but they require different evidence.

## Transfer and limitations

The unverified-signature Academy task concerns trusting edited claims without cryptographic verification. Our local audience case retains verification and fails later in token acceptance. Explain that difference rather than reducing both to “JWT is insecure.” The password-reset task examines whether the credential actually controls the operation that changes the password.

A complete worksheet contains observed API results and a reviewable recovery contract. It also names untested behaviours such as remote key rotation, malformed claim types, stolen valid tokens or consumption under concurrency. Record assistance honestly; completing the supplied fixture is practice, not evidence of independently reviewing an unfamiliar identity system.
