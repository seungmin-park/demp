"""현재 main의 검증된 CI 실행물을 준비한다."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

REPOSITORY = 'seungmin-park/demp'
MAX_ARCHIVE = 256 * 1024 * 1024


def positive(value):
    return type(value) is int and value > 0


def require_ci(run, jobs, artifacts, main_sha):
    expected = dict(name='DEMP CI', event='push', head_branch='main', head_sha=main_sha,
                    status='completed', conclusion='success', path='.github/workflows/ci.yml')
    if (not re.fullmatch('[a-f0-9]{40}', main_sha or '') or not isinstance(run, dict)
            or any(run.get(k) != v for k, v in expected.items())
            or run.get('repository', {}).get('full_name') != REPOSITORY
            or not positive(run.get('id')) or not positive(run.get('run_attempt'))):
        raise ValueError('Only successful CI for the current main push can deploy')
    if not jobs or not any(job.get('name') == 'DEMP verify' for job in jobs):
        raise ValueError('Required verification job missing')
    if any(job.get('status') != 'completed' or job.get('conclusion') != 'success'
           or not positive(job.get('run_id')) or not positive(job.get('run_attempt'))
           or job.get('run_id') != run['id'] or job.get('run_attempt') != run['run_attempt'] for job in jobs):
        raise ValueError('Every job must succeed for the exact CI attempt')
    name = f'deployment-demp-{run["id"]}-{run["run_attempt"]}'
    selected = [artifact for artifact in artifacts if artifact.get('name') == name]
    if len(selected) != 1:
        raise ValueError('Expected exactly one artifact for this CI attempt')
    artifact = selected[0]
    origin = artifact.get('workflow_run') or {}
    if (not positive(artifact.get('id')) or artifact.get('expired') is not False
            or not positive(artifact.get('size_in_bytes')) or artifact['size_in_bytes'] > MAX_ARCHIVE
            or not re.fullmatch('sha256:[a-f0-9]{64}', artifact.get('digest') or '')
            or origin.get('id') != run['id'] or origin.get('head_sha') != main_sha):
        raise ValueError('Artifact origin, size, expiry or digest invalid')
    return artifact


def unique_json(raw):
    def pairs(items):
        result = {}
        for key, value in items:
            if key in result:
                raise ValueError('JSON duplicate key')
            result[key] = value
        return result
    def invalid_constant(value):
        raise ValueError('Invalid JSON constant')
    try:
        return json.loads(raw, object_pairs_hook=pairs, parse_constant=invalid_constant)
    except (json.JSONDecodeError, UnicodeError) as error:
        raise ValueError('Invalid JSON') from error


def require_payload(raw, digest, run):
    if len(raw) > MAX_ARCHIVE or 'sha256:' + hashlib.sha256(raw).hexdigest() != digest:
        raise ValueError('Artifact ZIP digest mismatch')
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            infos = archive.infolist()
            manifests = [item for item in infos if item.filename == 'component.json']
            jars = [item for item in infos if item.filename == 'demp/service.jar']
            if len(manifests) != 1 or manifests[0].file_size > 1024 * 1024:
                raise ValueError('Expected one bounded component manifest')
            if len(jars) != 1 or jars[0].file_size > MAX_ARCHIVE:
                raise ValueError('Expected one bounded JAR')
            record = unique_json(archive.read(manifests[0]))
            expected = dict(component='demp', repository=REPOSITORY, event='push', headSha=run['head_sha'],
                            sourceSha=run['head_sha'], runId=run['id'], runAttempt=run['run_attempt'])
            if (not isinstance(record, dict) or any(record.get(k) != v for k, v in expected.items())
                    or type(record.get('runId')) is not int or type(record.get('runAttempt')) is not int
                    or not re.fullmatch('[a-f0-9]{40}', record.get('treeSha') or '')):
                raise ValueError('Artifact component/source/run identity differs')
            jar = archive.read(jars[0])
            if record.get('files') != {'demp/service.jar': hashlib.sha256(jar).hexdigest()}:
                raise ValueError('Artifact JAR digest mismatch')
            # VM은 모든 ZIP 경로/파일 종류와 Boot manifest까지 독립 검사한다.
    except (zipfile.BadZipFile, RuntimeError, OSError, NotImplementedError) as error:
        raise ValueError('Invalid artifact ZIP') from error


def api(path):
    return json.loads(subprocess.check_output(['gh', 'api', path], text=True))


def pages(path, key):
    result = json.loads(subprocess.check_output(['gh', 'api', '--paginate', '--slurp', path], text=True))
    return [item for page in result for item in page[key]]


def download(artifact_id):
    # gh는 redirect를 처리한다. signed URL이나 stderr 원문을 로그로 출력하지 않는다.
    process = subprocess.Popen(['gh', 'api', f'repos/{REPOSITORY}/actions/artifacts/{artifact_id}/zip'],
                               stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    try:
        raw = process.stdout.read(MAX_ARCHIVE + 1)
        if len(raw) > MAX_ARCHIVE:
            raise ValueError('Artifact download exceeds size limit')
        if process.wait() != 0:
            raise ValueError('Artifact download failed')
        return raw
    finally:
        if process.poll() is None:
            process.kill()
        process.wait()
        process.stdout.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-id', type=int, required=True)
    parser.add_argument('--output', type=Path, default=Path('.cd'))
    args = parser.parse_args()
    if args.run_id <= 0:
        parser.error('Invalid run ID')
    base = f'repos/{REPOSITORY}'
    run = api(f'{base}/actions/runs/{args.run_id}')
    jobs = pages(f'{base}/actions/runs/{args.run_id}/attempts/{run["run_attempt"]}/jobs?per_page=100', 'jobs')
    artifacts = pages(f'{base}/actions/runs/{args.run_id}/artifacts?per_page=100', 'artifacts')
    main_sha = api(base + '/git/ref/heads/main')['object']['sha']
    artifact = require_ci(run, jobs, artifacts, main_sha)
    raw = download(artifact['id'])
    require_payload(raw, artifact['digest'], run)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output / 'payload.zip').write_bytes(raw)
    metadata = dict(runId=run['id'], runAttempt=run['run_attempt'], headSha=run['head_sha'],
                    artifactId=artifact['id'], archiveDigest=artifact['digest'][7:])
    (args.output / 'metadata.json').write_text(json.dumps(metadata, indent=2) + '\n')
    if os.environ.get('GITHUB_ENV'):
        with open(os.environ['GITHUB_ENV'], 'a') as stream:
            for name, value in (('CD_RUN_ID', run['id']), ('CD_RUN_ATTEMPT', run['run_attempt']),
                                ('CD_HEAD_SHA', run['head_sha']), ('CD_ARCHIVE_DIGEST', artifact['digest'][7:])):
                stream.write(f'{name}={value}\n')
    print(f'검증된 백엔드 준비: CI {run["id"]}, attempt {run["run_attempt"]}, source {main_sha}')


if __name__ == '__main__':
    main()
