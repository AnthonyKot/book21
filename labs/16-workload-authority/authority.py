"""A local model of workload identity: an identity token, a credential broker and an authorizer.

Three parties, all running in this process:

- The **issuer** signs a short-lived identity token for a job. Its claims describe the workload
  (repository, ref, event, workflow, environment) the way GitHub's OIDC token does. The lab
  writes them as JSON plus a detached Ed25519 signature instead of a JWT.
- The **broker** exchanges a token for temporary credentials. It verifies the signature against
  the issuer keys it trusts, checks audience and expiry, evaluates the role's trust conditions
  against the claims, and issues a credential that expires. This is the AssumeRoleWithWebIdentity
  step of a cloud provider.
- The **authorizer** decides each request with the role's permission policy: implicit deny,
  explicit allow needed, explicit deny wins, wildcards in actions and resources.

Static keys (version 1) are modelled too: a credential with no identity token, no expiry and a
permission policy of its own. Nothing here contacts a cloud provider; the rules are a model of the
documentation cited in the README, and the clock is explicit so expiry and rotation can be shown.
"""
import fnmatch
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path

import signing

ROOT = Path(__file__).resolve().parent
PLATFORM = ROOT / 'platform'
TOKEN_SECONDS = 300  # identity tokens are short-lived; the credential lifetime is the role's decision


def instant(value):
    if isinstance(value, datetime):
        return value
    return datetime.fromisoformat(value.replace('Z', '+00:00'))


def stamp(when):
    return when.astimezone(timezone.utc).isoformat(timespec='seconds').replace('+00:00', 'Z')


class Denied(Exception):
    """A refused exchange or request, with the reason a real service would log."""


# ------------------------------------------------------------------ identity tokens

def subject(claims):
    """GitHub's default `sub` format: environment, pull request, or ref."""
    repo = claims['repository']
    if claims.get('environment'):
        return f"repo:{repo}:environment:{claims['environment']}"
    if claims.get('event_name') == 'pull_request':
        return f'repo:{repo}:pull_request'
    return f"repo:{repo}:ref:{claims['ref']}"


def issue_token(workload, audience, now, key_path=PLATFORM / 'issuer.key', issuer='https://lab.local/issuer'):
    """The platform signs claims describing the job that asked for a token."""
    now = instant(now)
    claims = {
        'iss': issuer, 'aud': audience,
        'iat': stamp(now), 'exp': stamp(now + timedelta(seconds=TOKEN_SECONDS)),
        **workload,
    }
    claims['sub'] = subject(claims)
    payload = json.dumps(claims, sort_keys=True).encode()
    return {'claims': claims, 'payload': payload, 'signature': signing.sign(key_path, payload)}


# ------------------------------------------------------------------ the broker

def matches(expected, actual):
    """Trust and policy conditions: exact string, or a fnmatch pattern when it contains * or ?."""
    if actual is None:
        return False
    if any(ch in expected for ch in '*?'):
        return fnmatch.fnmatchcase(actual, expected)
    return expected == actual


class Broker:
    def __init__(self, config, now):
        self.audience = config['audience']
        self.keys = [ROOT / p for p in config['trusted_issuers']]
        self.default_session = config.get('default_session_seconds', 900)
        self.now = instant(now)

    def verify(self, token):
        if not any(k.exists() and signing.verify_signature(k, token['payload'], token['signature']) for k in self.keys):
            raise Denied('token signature does not verify against any trusted issuer key')
        claims = json.loads(token['payload'])
        if claims.get('aud') != self.audience:
            raise Denied(f"token audience {claims.get('aud')!r} is not this broker")
        if instant(claims['exp']) <= self.now:
            raise Denied(f"token expired at {claims['exp']} (now {stamp(self.now)})")
        return claims

    def assume(self, token, role_name, roles, duration_seconds=None):
        claims = self.verify(token)
        role = roles.get(role_name)
        if role is None:
            raise Denied(f'no such role {role_name}')
        for key, expected in role['trust'].items():
            if not matches(expected, claims.get(key)):
                raise Denied(f"trust policy of {role_name}: claim {key}={claims.get(key)!r} does not satisfy {expected!r}")
        if not role.get('max_session_seconds'):
            raise Denied(f'role {role_name} sets no max_session_seconds')
        duration = duration_seconds or min(self.default_session, role['max_session_seconds'])
        if duration > role['max_session_seconds']:
            raise Denied(f"requested {duration}s exceeds the role's maximum session of {role['max_session_seconds']}s")
        return {
            'kind': 'role-session', 'role': role_name, 'subject': claims['sub'],
            'issued': stamp(self.now), 'expires': stamp(self.now + timedelta(seconds=duration)),
            'permissions': role['permissions'],
        }


def static_credential(name, keys):
    key = keys[name]
    return {'kind': 'static-key', 'role': name, 'subject': f'static-key:{name}', 'issued': key['created'],
            'expires': key['expires'], 'permissions': key['permissions']}


# ------------------------------------------------------------------ the authorizer

def decide(credential, action, resource, now):
    """Return ('allow'|'deny', reason). Implicit deny; an explicit deny overrides any allow."""
    now = instant(now)
    if credential.get('expires') and instant(credential['expires']) <= now:
        return 'deny', f"credential expired at {credential['expires']}"
    allowed = None
    for statement in credential['permissions']:
        if any(matches(a, action) for a in statement['actions']) and any(matches(r, resource) for r in statement['resources']):
            if statement['effect'] == 'deny':
                return 'deny', f'explicit deny: {statement}'
            allowed = statement
    if allowed:
        return 'allow', f'allowed by {allowed}'
    return 'deny', 'implicit deny: no statement allows this action on this resource'


def load_policies(path):
    return json.loads(Path(path).read_text())


def ensure_issuer_key():
    if not (PLATFORM / 'issuer.key').exists():
        signing.generate_keypair(PLATFORM / 'issuer.key', PLATFORM / 'issuer.pub')
        return True
    return False
