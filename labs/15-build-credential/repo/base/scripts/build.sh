#!/usr/bin/env bash
# Run the tests and package the service. Executed by every workflow job that builds.
set -euo pipefail
python3 -m unittest discover -s tests -v
mkdir -p dist
tar -czf dist/app.tar.gz src
echo "built dist/app.tar.gz"
