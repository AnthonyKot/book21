"""A local model of a hosted CI runner, used to trace which context a change runs in.

It reads a workflow file, decides which jobs an event triggers, checks out the tree the event
selects, decides which secrets and token permissions the job receives, executes the run steps in
a scratch directory with dummy credentials, and records every outbound request the steps make.

The trigger, checkout and credential rules are a model of GitHub's documented behaviour (see the
README for the pages and the date they were read). The model is small on purpose: it does not
implement expressions beyond a few contexts, matrices, containers, caches or reusable workflows,
and nothing here contacts GitHub. Treat its output as a trace of the documented rules, not as a
run on the real service.
"""
import argparse
import base64
import hashlib
import http.server
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from datetime import datetime, timezone
from pathlib import Path

import yaml

import attest

ROOT = Path(__file__).resolve().parent
PLATFORM = ROOT / 'platform'
BUILDER_ID = 'https://lab.local/platform/hosted-runner'
BUILD_TYPE = 'https://lab.local/platform/workflow/v1'
PROVENANCE_PREDICATE = 'https://slsa.dev/provenance/v1'
SIMULATED_ACTIONS = ('actions/checkout', 'actions/upload-artifact', 'actions/download-artifact',
                     'actions/attest-build-provenance')


# ---------------------------------------------------------------- trees and events

def tree_id(directory):
    """A stable identifier for a working tree: stands in for a commit id."""
    digest = hashlib.sha1()
    for path in sorted(Path(directory).rglob('*')):
        if path.is_file():
            digest.update(str(path.relative_to(directory)).encode())
            digest.update(path.read_bytes())
    return digest.hexdigest()


def overlay(base, contribution, destination):
    """Copy the base tree, then apply the contribution's files on top (the merge result)."""
    shutil.copytree(base, destination, dirs_exist_ok=True)
    if contribution:
        shutil.copytree(contribution, destination, dirs_exist_ok=True)


def load_event(path):
    event = json.loads(Path(path).read_text())
    for key in ('base_tree', 'contribution'):
        if event.get(key):
            event[key] = str((ROOT / event[key]).resolve())
    event.setdefault('base_tree', str(ROOT / 'repo' / 'base'))
    event.setdefault('repository', 'doc-approval/service')
    return event


def event_trees(event, scratch):
    """Return the trees an event exposes: head (the contribution) and merge (contribution on base)."""
    head = scratch / 'tree-head'
    merge = scratch / 'tree-merge'
    if event['event_name'] in ('pull_request', 'pull_request_target'):
        # The fork's branch is the base plus the contribution; with no divergence the merge is the same tree.
        overlay(event['base_tree'], event['contribution'], head)
        overlay(event['base_tree'], event['contribution'], merge)
    else:
        overlay(event['base_tree'], event.get('contribution'), head)
        shutil.copytree(head, merge)
    return {'head': head, 'merge': merge, 'base': Path(event['base_tree'])}


# ---------------------------------------------------------------- documented rules

def triggered_jobs(workflow, event):
    """Jobs run when the workflow lists the event; branch filters apply to the target branch."""
    on = workflow.get('on', workflow.get(True)) or {}  # PyYAML reads a bare `on` key as boolean True
    if isinstance(on, list):
        on = {name: {} for name in on}
    if isinstance(on, str):
        on = {on: {}}
    if event['event_name'] not in on:
        return []
    spec = on[event['event_name']] or {}
    branches = spec.get('branches')
    if branches:
        target = event.get('base_ref') or event.get('ref', '').removeprefix('refs/heads/')
        if target not in branches:
            return []
    return list(workflow['jobs'].keys())


def token_permissions(workflow, job, event):
    """GITHUB_TOKEN scopes for a job. A `permissions` key sets every unnamed scope to none;
    a pull_request from a fork can never exceed read."""
    declared = job.get('permissions', workflow.get('permissions'))
    if declared is None:
        scopes = {'contents': 'write', 'pull-requests': 'write', 'packages': 'write'}  # permissive default
        source = 'repository default (permissive)'
    elif isinstance(declared, str):
        scopes = {'contents': declared, 'pull-requests': declared, 'packages': declared}
        source = f'workflow shorthand {declared}'
    else:
        scopes = dict(declared)
        source = 'explicit permissions key'
    if event['event_name'] == 'pull_request' and event.get('from_fork'):
        scopes = {k: ('read' if v == 'write' else v) for k, v in scopes.items()}
        source += '; capped at read for a fork pull request'
    return scopes, source


def available_secrets(event):
    """Repository secrets reach the job unless the event is a pull_request from a fork."""
    secrets = json.loads((PLATFORM / 'secrets.json').read_text())
    if event['event_name'] == 'pull_request' and event.get('from_fork'):
        return {}, 'none: pull_request from a fork receives only GITHUB_TOKEN'
    return secrets, 'all repository secrets'


def default_checkout(event, trees):
    """What actions/checkout selects without a `ref` input."""
    name = event['event_name']
    if name == 'pull_request':
        return trees['merge'], f"refs/pull/{event['pull_request']['number']}/merge (contribution merged onto base)"
    if name == 'pull_request_target':
        return trees['base'], 'refs/heads/main (the base branch, not the contribution)'
    return trees['head'], f"{event['ref']} (the pushed commit)"


def context_values(event, trees, secrets, token):
    head_sha = tree_id(trees['head'])
    values = {
        'github.sha': tree_id(trees['merge']) if event['event_name'] == 'pull_request' else (
            tree_id(trees['base']) if event['event_name'] == 'pull_request_target' else head_sha),
        'github.ref': (f"refs/pull/{event['pull_request']['number']}/merge" if event['event_name'] == 'pull_request'
                       else 'refs/heads/main' if event['event_name'] == 'pull_request_target' else event['ref']),
        'github.event_name': event['event_name'],
        'github.repository': event['repository'],
        'github.event.pull_request.head.sha': head_sha if 'pull_request' in event else '',
        'github.token': token,
    }
    for name, value in secrets.items():
        values[f'secrets.{name}'] = value
    values['secrets.GITHUB_TOKEN'] = token
    return values


def expand(text, values):
    """Expand the few `${{ }}` forms the lab uses, including `a || b`."""
    def repl(match):
        expr = match.group(1).strip()
        for part in [p.strip() for p in expr.split('||')]:
            value = values.get(part, '')
            if value:
                return value
        return ''
    return re.sub(r'\$\{\{(.*?)\}\}', repl, str(text))


# ---------------------------------------------------------------- the outside world

class World(http.server.BaseHTTPRequestHandler):
    """Every host the runner can reach: a registry, the platform API and anywhere else."""
    log = []
    tokens = {}

    def log_message(self, *args):
        pass

    def _read(self):
        length = int(self.headers.get('Content-Length') or 0)
        return self.rfile.read(length)

    def _respond(self, code, body=b''):
        self.send_response(code)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        body = self._read()
        auth = self.headers.get('Authorization', '')
        entry = {'method': 'POST', 'path': self.path, 'authorization': auth, 'bytes': len(body),
                 'body': body[:4000].decode('utf-8', 'replace')}
        if self.path == '/upload':
            secrets = json.loads((PLATFORM / 'secrets.json').read_text())
            ok = auth == f"Bearer {secrets['RELEASE_TOKEN']}"
            entry['result'] = 'accepted' if ok else 'rejected: bad release token'
            World.log.append(entry)
            return self._respond(201 if ok else 401)
        entry['result'] = 'received'
        World.log.append(entry)
        return self._respond(200)

    def do_PUT(self):
        body = self._read()
        auth = self.headers.get('Authorization', '')
        token = auth.removeprefix('token ').strip()
        perms = World.tokens.get(token)
        entry = {'method': 'PUT', 'path': self.path, 'authorization': auth, 'bytes': len(body), 'body': ''}
        if perms is None:
            entry['result'] = '401: unknown token'
            code = 401
        elif perms.get('contents') == 'write':
            entry['result'] = '200: repository content written with GITHUB_TOKEN'
            code = 200
        else:
            entry['result'] = f"403: token has contents:{perms.get('contents', 'none')}"
            code = 403
        World.log.append(entry)
        return self._respond(code)


def start_world():
    server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), World)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server, f'http://127.0.0.1:{server.server_address[1]}'


# ---------------------------------------------------------------- provenance

def make_provenance(subject_path, subject_name, event, values, workflow_path, job_name, private_key):
    """The platform (not the job's steps) records how the artifact was produced and signs it."""
    statement = {
        '_type': 'https://in-toto.io/Statement/v1',
        'subject': [{'name': subject_name, 'digest': {'sha256': attest.sha256_file(subject_path)}}],
        'predicateType': PROVENANCE_PREDICATE,
        'predicate': {
            'buildDefinition': {
                'buildType': BUILD_TYPE,
                'externalParameters': {
                    'workflow': {'repository': values['github.repository'], 'ref': values['github.ref'],
                                 'path': workflow_path},
                    'event': values['github.event_name'],
                },
                'internalParameters': {'job': job_name},
                'resolvedDependencies': [
                    {'uri': f"git+https://lab.local/{values['github.repository']}@{values['github.ref']}",
                     'digest': {'gitCommit': values['github.sha']}},
                ],
            },
            'runDetails': {
                'builder': {'id': BUILDER_ID},
                'metadata': {'invocationId': f"{values['github.repository']}/{workflow_path}/{int(time.time())}",
                             'startedOn': datetime.now(timezone.utc).isoformat(timespec='seconds')},
            },
        },
    }
    payload = json.dumps(statement, indent=1, sort_keys=True).encode()
    signature = attest.sign(private_key, payload)
    return payload, signature


# ---------------------------------------------------------------- running a job

def run_job(name, job, workflow, workflow_path, event, trees, world_url, out_dir, transcript):
    scopes, scope_source = token_permissions(workflow, job, event)
    secrets, secret_note = available_secrets(event)
    token = 'ghs_' + hashlib.sha256(f'{name}{time.time()}'.encode()).hexdigest()[:20]
    World.tokens[token] = scopes
    values = context_values(event, trees, secrets, token)
    say = transcript.say
    say(f'\n== job {name}')
    say(f'   event {event["event_name"]} on {event["repository"]}'
        + (f" from fork {event['head_repository']}" if event.get('from_fork') else ''))
    say(f'   GITHUB_TOKEN scopes {json.dumps(scopes)} [{scope_source}]')
    say(f'   secrets available: {", ".join(secrets) or "-"} [{secret_note}]')
    workdir = out_dir / f'job-{name}'
    workdir.mkdir(parents=True)
    env = {**os.environ,
           'GITHUB_REPOSITORY': event['repository'], 'GITHUB_EVENT_NAME': event['event_name'],
           'GITHUB_SHA': values['github.sha'], 'GITHUB_REF': values['github.ref'],
           'GITHUB_API_URL': world_url, 'LAB_REGISTRY_URL': world_url, 'LAB_EGRESS_URL': world_url}
    for key, value in (workflow.get('env') or {}).items():
        env[key] = expand(value, values)
    for key, value in (job.get('env') or {}).items():
        env[key] = expand(value, values)
    checked_out = None
    outcome = {'job': name, 'steps': [], 'artifacts': {}, 'provenance': None}
    for index, step in enumerate(job['steps'], 1):
        uses = step.get('uses', '')
        action = uses.split('@')[0]
        with_ = {k: expand(v, values) for k, v in (step.get('with') or {}).items()}
        if action == 'actions/checkout':
            ref = with_.get('ref')
            if ref and ref == values['github.event.pull_request.head.sha']:
                source, label = trees['head'], f'explicit ref {ref[:12]} (the pull request head: the contribution as pushed)'
            elif ref and ref == values['github.sha']:
                source, default_label = default_checkout(event, trees)
                label = f'explicit ref {ref[:12]} = github.sha, {default_label}'
            elif ref:
                raise ValueError(f'checkout ref {ref!r} is not a ref this model resolves')
            else:
                source, label = default_checkout(event, trees)
            shutil.copytree(source, workdir, dirs_exist_ok=True)
            checked_out = tree_id(source)
            persist = str(with_.get('persist-credentials', 'true')).lower() != 'false'
            if persist:
                (workdir / '.git').mkdir(exist_ok=True)
                header = base64.b64encode(f'x-access-token:{token}'.encode()).decode()
                (workdir / '.git' / 'config').write_text(
                    f'[http "{world_url}/"]\n\textraheader = AUTHORIZATION: basic {header}\n')
            say(f'   step {index}: checkout -> {label}; tree {checked_out[:12]}; '
                f'GITHUB_TOKEN persisted in .git/config: {"yes" if persist else "no"}')
            outcome['steps'].append({'step': index, 'checkout': label, 'tree': checked_out, 'persist': persist})
        elif action == 'actions/upload-artifact':
            src = workdir / with_['path']
            stored = out_dir / 'artifacts' / with_['name']
            stored.parent.mkdir(exist_ok=True)
            shutil.copy(src, stored)
            say(f'   step {index}: upload-artifact {with_["name"]} sha256 {attest.sha256_file(stored)[:12]}')
            outcome['artifacts'][with_['name']] = str(stored)
        elif action == 'actions/download-artifact':
            stored = out_dir / 'artifacts' / with_['name']
            dest = workdir / with_.get('path', '.') / stored.name.replace(stored.name, 'app.tar.gz')
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy(stored, dest)
            say(f'   step {index}: download-artifact {with_["name"]} -> {dest.relative_to(workdir)}')
        elif action == 'actions/attest-build-provenance':
            if scopes.get('id-token') != 'write' or scopes.get('attestations') != 'write':
                say(f'   step {index}: attest-build-provenance refused: needs id-token and attestations write')
                outcome['steps'].append({'step': index, 'attest': 'refused'})
                continue
            subject = workdir / with_['subject-path']
            payload, signature = make_provenance(subject, with_['subject-path'], event, values, workflow_path,
                                                 name, PLATFORM / 'builder.key')
            (out_dir / 'provenance.json').write_bytes(payload)
            (out_dir / 'provenance.sig').write_bytes(signature)
            shutil.copy(subject, out_dir / 'app.tar.gz')
            say(f'   step {index}: provenance signed by the platform for sha256 '
                f'{attest.sha256_file(subject)[:12]} (ref {values["github.ref"]}, event {values["github.event_name"]})')
            outcome['provenance'] = str(out_dir / 'provenance.json')
        elif uses:
            say(f'   step {index}: uses {uses} (not simulated)')
        elif 'run' in step:
            step_env = dict(env)
            for key, value in (step.get('env') or {}).items():
                step_env[key] = expand(value, values)
            script = expand(step['run'], values)
            before = len(World.log)
            result = subprocess.run(['bash', '-e', '-c', script], cwd=workdir, env=step_env,
                                    capture_output=True, text=True)
            label = step.get('name') or script.strip().splitlines()[0][:50]
            say(f'   step {index}: run "{label}" exit {result.returncode}')
            for line in (result.stdout + result.stderr).strip().splitlines()[-4:]:
                say(f'      | {line}')
            requests = World.log[before:]
            for req in requests:
                leaked = [k for k, v in secrets.items() if v and v in (req['body'] + req['authorization'])]
                say(f'      outbound {req["method"]} {req["path"]} -> {req["result"]}'
                    + (f'; carries secret {", ".join(leaked)}' if leaked else ''))
            outcome['steps'].append({'step': index, 'run': label, 'exit': result.returncode,
                                     'requests': [{**r, 'carries_secret': [k for k, v in secrets.items()
                                                                          if v and v in (r['body'] + r['authorization'])]}
                                                  for r in requests]})
            if result.returncode != 0:
                say('   job failed')
                break
    return outcome


class Transcript:
    def __init__(self, path, echo=True):
        self.lines = []
        self.path = path
        self.echo = echo

    def say(self, line):
        if self.echo:
            print(line)
        self.lines.append(line)

    def save(self):
        self.path.write_text('\n'.join(self.lines) + '\n')


def run(workflow_path, event_path, out_dir, echo=True):
    workflow = yaml.safe_load(Path(workflow_path).read_text())
    event = load_event(event_path)
    out_dir = Path(out_dir)
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)
    transcript = Transcript(out_dir / 'transcript.txt', echo)
    World.log = []
    World.tokens = {}
    server, world_url = start_world()
    try:
        trees = event_trees(event, out_dir)
        jobs = triggered_jobs(workflow, event)
        transcript.say(f'workflow {Path(workflow_path).name} ({workflow.get("name")}), event {event["event_name"]}'
                       f' -> jobs {jobs or "none (workflow not triggered)"}')
        outcomes = []
        for name in jobs:
            outcomes.append(run_job(name, workflow['jobs'][name], workflow, Path(workflow_path).name, event,
                                    trees, world_url, out_dir, transcript))
        summary = {'workflow': Path(workflow_path).name, 'event': event['event_name'], 'jobs': outcomes,
                   'world': World.log}
        (out_dir / 'summary.json').write_text(json.dumps(summary, indent=1))
        transcript.save()
        return summary
    finally:
        server.shutdown()


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--workflow', required=True)
    parser.add_argument('--event', required=True)
    parser.add_argument('--out', default=None, help='output directory (default runs/<workflow>-<event>)')
    parser.add_argument('--init', action='store_true', help='create the platform signing key first')
    args = parser.parse_args()
    if args.init or not (PLATFORM / 'builder.key').exists():
        attest.generate_keypair(PLATFORM / 'builder.key', PLATFORM / 'builder.pub')
        print(f'platform key created in {PLATFORM.relative_to(ROOT)}/')
    out = args.out or ROOT / 'runs' / f"{Path(args.workflow).stem}-{Path(args.event).stem}"
    run(args.workflow, args.event, out)
    print(f'\nrecorded in {Path(out).relative_to(ROOT) if str(out).startswith(str(ROOT)) else out}/')


if __name__ == '__main__':
    sys.exit(main())
