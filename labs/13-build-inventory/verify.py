"""Check the measured fixture contract, not general SBOM completeness."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--out', type=Path, default=Path(__file__).resolve().parent / 'out')
parser.add_argument('--negative', action='store_true', help='substitute the Maven BOM as the final image inventory')
args = parser.parse_args()
root = args.out
load = lambda p: json.loads(p.read_text())
digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
maven = load(root / 'maven.cdx.json')
components = lambda d: {(c.get('group'), c['name'], c.get('version')) for c in d['components']}
maven_components = components(maven)
# For these Maven JAR fixtures only, accept the explicit ?type=jar qualifier.
# Do not collapse arbitrary classifiers, ecosystems or other qualifiers.
def has_jar(document, purl):
    return any(c.get('purl') in {purl, purl + '?type=jar'} for c in document['components'])

checks = []
def check(name, passed):
    checks.append(bool(passed))
    print(('PASS ' if passed else 'FAIL ') + name)

check('Maven includes resolved spring-webmvc', has_jar(maven, 'pkg:maven/org.springframework/spring-webmvc@7.0.9'))
check('Maven test scope intentionally excluded', not any(n.startswith('junit-') for _, n, _ in maven_components))
check('Separate helper absent from app Maven BOM', not any(n == 'export-helper' for _, n, _ in maven_components))
check('Identical app JAR bytes in A and B', digest(root/'a/app.jar') == digest(root/'b/app.jar'))
check('Different helper JAR bytes in A and B', digest(root/'a/export-helper.jar') != digest(root/'b/export-helper.jar'))
a, b = load(root/'a/image.json'), load(root/'b/image.json')
check('Different immutable local image IDs', a['Id'] != b['Id'])
move = load(root/'tag-move.json')
check('Actual candidate tag moved A to B', move['before'] == a['Id'] and move['after'] == b['Id'])
for label, version in [('a','1.0.0'),('b','1.1.0')]:
    stage = root / label
    image = load(stage/'image.json')
    raw = load(stage/'image.syft.json')
    cdx = load(stage/'image.cdx.json')
    candidate = maven if args.negative else cdx
    names = components(candidate)
    check(f'{label}: scanner subject equals inspected image ID', raw['source']['metadata']['imageID'] == image['Id'])
    check(f'{label}: pinned scanner and merged filesystem scope', raw['descriptor']['version'] == '1.51.1' and raw['descriptor']['configuration']['search']['scope'] == 'squashed')
    check(f'{label}: final inventory identifies helper {version}', has_jar(candidate, f'pkg:maven/book21/export-helper@{version}'))
    check(f'{label}: final inventory includes base musl package', any(n == 'musl' for _, n, _ in names))
    check(f'{label}: legitimate Spring component retained', has_jar(candidate, 'pkg:maven/org.springframework/spring-webmvc@7.0.9'))
    helpers = [p for p in raw['artifacts'] if p.get('purl') == f'pkg:maven/book21/export-helper@{version}']
    with zipfile.ZipFile(stage/'export-helper.jar') as jar:
        props = jar.read('META-INF/maven/book21/export-helper/pom.properties').decode()
    check(f'{label}: helper identity has JAR metadata and image location', len(helpers) == 1 and f'version={version}' in props and any(loc['path'] == '/opt/book21/tools/export-helper.jar' for loc in helpers[0]['locations']))
    check(f'{label}: both exchange documents use CycloneDX 1.6', maven['bomFormat'] == cdx['bomFormat'] == 'CycloneDX' and maven['specVersion'] == cdx['specVersion'] == '1.6')
    print(f'OBSERVED {label}: Maven {len(maven["components"])} components; image {len(cdx["components"])} components; raw {len(raw["artifacts"])} packages.')
print(f'{sum(checks)}/{len(checks)} checks pass; {len(checks)-sum(checks)} fail. Fixture coverage only.')
raise SystemExit(0 if all(checks) else 1)
