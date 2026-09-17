"""Ed25519 signing helpers (openssl) shared by the token issuer, the broker and any checker.

Signatures are made over the exact message bytes. The issuer private key stands for a platform
identity provider's signing key (GitHub publishes its OIDC signing keys at a JWKS endpoint);
a verifier needs only the public keys it trusts.
"""
import hashlib
import subprocess
import tempfile
from pathlib import Path


def generate_keypair(private_path, public_path):
    private_path = Path(private_path)
    public_path = Path(public_path)
    private_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['openssl', 'genpkey', '-algorithm', 'ed25519', '-out', str(private_path)],
                   check=True, capture_output=True)
    subprocess.run(['openssl', 'pkey', '-in', str(private_path), '-pubout', '-out', str(public_path)],
                   check=True, capture_output=True)


def sign(private_path, message: bytes) -> bytes:
    with tempfile.TemporaryDirectory() as tmp:
        msg = Path(tmp, 'msg')
        sig = Path(tmp, 'sig')
        msg.write_bytes(message)
        subprocess.run(['openssl', 'pkeyutl', '-sign', '-inkey', str(private_path), '-rawin',
                        '-in', str(msg), '-out', str(sig)], check=True, capture_output=True)
        return sig.read_bytes()


def verify_signature(public_path, message: bytes, signature: bytes) -> bool:
    """True only if the signature was made over exactly these bytes by the matching private key."""
    with tempfile.TemporaryDirectory() as tmp:
        msg = Path(tmp, 'msg')
        sig = Path(tmp, 'sig')
        msg.write_bytes(message)
        sig.write_bytes(signature)
        result = subprocess.run(['openssl', 'pkeyutl', '-verify', '-pubin', '-inkey', str(public_path),
                                 '-rawin', '-in', str(msg), '-sigfile', str(sig)], capture_output=True)
        return result.returncode == 0


def sha256_file(path) -> str:
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()
