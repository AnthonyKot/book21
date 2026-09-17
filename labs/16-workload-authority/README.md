# Workload authority: a local model of identity tokens, a credential broker and an authorizer

Companion to [essay 16](../../essays/16-workload-authority.md). Requires Python 3.12 or newer and `openssl` on the path; no third-party packages, network, Docker or cloud account. Tested with Python 3.12.3 and OpenSSL 3.0.13. Run commands from `labs/16-workload-authority`.

`authority.py` models three parties in one process: an **issuer** that signs short-lived identity tokens whose claims describe a job (the shape of GitHub's OIDC token), a **broker** that exchanges a token for a role session after checking signature, audience, expiry and the role's trust conditions (the shape of a cloud provider's web-identity role assumption), and an **authorizer** that decides requests from the role's permission statements with implicit deny, explicit allow and explicit deny that wins. Static keys with no expiry are modelled too. The clock is explicit. The rules are a model of the documentation listed at the end, read on 2026-09-17; nothing here contacts GitHub or a cloud provider, and every key is generated locally on first run.

## Files

- `policies/v1.json`: one static key, `deployer`, created by hand in 2024, never expiring, allowed everything.
- `policies/v2.json`: no static keys; a broker configuration and two roles, `release-publisher` and `production-deployer`, each with a trust policy over token claims and a permission policy over actions and resources.
- `policies/resources.json`: the resources the authorizer knows, for orientation only; the authorizer matches resource names as strings.
- `policies/cleanup.json`: the starter for the independent task.
- `authority.py`, `signing.py`: the model and its Ed25519 helpers (openssl). The first run creates `platform/issuer.key` and `issuer.pub`; the private key stands for the identity provider's signing key.
- `trace.py`: the guided trace. `guided_tests.py`: sixteen tests over the model.
- `policy_check.py`: the post-attempt check. **Opening it shows the request list, which is part of the answer.** Run it only after your policy, tests and note are saved.

Actions have the form `family:verb` (`artifacts:list|get|put|delete`, `secrets:get`, `deploy:run`); resources have the form `family:path` and can be matched with `*`. A trust policy is a map from claim name to an exact value or a `*` pattern; every listed claim must match. `max_session_seconds` caps how long a session issued for the role can live.

## Guided: one credential, two designs

```bash
python3 trace.py
```

Version 1 shows what the key that leaked in essay 15 can do: everything, now and a year from now. Version 2 issues the publish job an identity token, exchanges it for the `release-publisher` role and shows: the task allowed; a secret read, a release deletion and a deployment denied; the session unusable sixteen minutes later; the identity token itself refused six minutes after issue; the same role refused to a pull request job, to `ci.yml` on `main`, to a fork, to a token for another audience; the publisher refused the deployer role; and an issuer key rotation that invalidates old tokens without touching any job. The transcript is written to `runs/trace.txt`.

```bash
python3 guided_tests.py
```

Expected: 16 pass (3 on the static key, 13 on the role design).

Negative control, a role design that looks repaired:

```bash
python3 guided_tests.py --broad
```

This keeps version 2 but trusts the repository alone and grants `artifacts:*` on every artifact. Expected: 3 failures (the pull request job and `ci.yml` obtain the publisher role; the publisher can delete a release), 13 passes. It still expires and still refuses forks and other audiences, which is why "uses OIDC" is not the same as "carries only the authority its task needs".

## Independent: the cleanup job

Use the [worksheet](../../practice/16-authority-worksheet.md). Write the `staging-cleanup` role in `policies/cleanup.json` in the same schema. To try a policy by hand, build a broker and tokens the way `trace.py` does:

```python
import authority
p = authority.load_policies('policies/v2.json'); mine = authority.load_policies('policies/cleanup.json')
broker = authority.Broker(p['broker'], '2026-09-17T02:00:00Z')
token = authority.issue_token({'repository': 'doc-approval/service', 'ref': 'refs/heads/main', 'event_name': 'schedule',
    'job_workflow_ref': 'doc-approval/service/.github/workflows/cleanup.yml@refs/heads/main', 'sha': 'abc'}, 'lab-broker', '2026-09-17T02:00:00Z')
cred = broker.assume(token, 'staging-cleanup', {**p['roles'], **mine['roles']})
authority.decide(cred, 'artifacts:delete', 'artifacts:staging/service/pr-38.tar.gz', '2026-09-17T02:00:00Z')
```

Write your own tests as a Python file that does this for the requests you decide matter, including workloads that must not obtain the role and a time at which the session must be dead. After your attempt is saved:

```bash
python3 policy_check.py
```

It reports each outcome against the intended one with the broker's or authorizer's reason. It cannot judge your reasoning, your tests or your decision on the widened requirement.

## Scope and limits

The model implements: signed claims with `iat`/`exp` (five-minute tokens), audience, trust conditions with exact or wildcard values, role sessions with a maximum lifetime, identity-based permission statements with wildcards, implicit deny and explicit deny. It does not implement: JWT encoding or a key-discovery endpoint, the five-minute clock-skew window a real provider allows, session policies, permissions boundaries, organisation-level policies, resource-based policies, session tags or attribute-based conditions, or approval gates. Subject claims use GitHub's earlier default format; repositories created after mid-July 2026 use an immutable format that includes owner and repository IDs. A cloud provider's own evaluation order and quotas are the authority; the model teaches the shape.

## Sources modelled (read 2026-09-17)

- [About OpenID Connect in GitHub Actions](https://docs.github.com/en/actions/concepts/security/openid-connect) and the [OIDC reference](https://docs.github.com/en/actions/reference/security/oidc): token claims, default `sub` formats, the short-lived token, `id-token: write`.
- [Configuring OpenID Connect in AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws): trust policy conditions on `aud` and `sub`; "you must define at least one condition, so that untrusted repositories can't request access tokens".
- [AssumeRoleWithWebIdentity](https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html): temporary credentials, `DurationSeconds` from 900 seconds to the role's maximum, session policies that can only narrow.
- [OIDC federation](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_oidc.html): do not store long-term credentials outside the provider; five-minute skew window past `exp`.
- [Policy evaluation logic](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic_policy-eval-denyallow.html): implicit deny, explicit allow required, explicit deny overrides.

The cloud pages are AWS because they document the exchange precisely; the model is provider-neutral, and the book has not yet chosen the reader's cloud provider.
