"""Consumer-side verification of a delivered artifact and its provenance. Your implementation.

Command line contract (the post-attempt check calls it exactly like this):

    python3 consumer/verify.py --artifact PATH --provenance PATH --signature PATH --builder-key PATH

Exit 0 to accept the artifact for deployment, exit 1 to reject it. Print one line first:
"ACCEPT: <what was established>" or "REJECT: <the first reason>". Where the expected values come
from is your design decision; the worksheet states the deployment's trust facts.
"""
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import attest  # noqa: E402  (attest.verify_signature and attest.sha256_file are available)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', required=True)
    parser.add_argument('--provenance', required=True)
    parser.add_argument('--signature', required=True)
    parser.add_argument('--builder-key', required=True)
    args = parser.parse_args()
    print('REJECT: verifier not implemented')
    return 1


if __name__ == '__main__':
    sys.exit(main())
