"""현재 main의 성공한 push CI에서 나온 정확한 실행물만 허용한다."""
import copy
import hashlib
import importlib
import io
import json
from pathlib import Path
import os
import tempfile
import unittest
from unittest.mock import patch
import zipfile


class BackendCITests(unittest.TestCase):
    def setUp(self):
        self.assertTrue(Path(__file__).with_name('prepare_backend_cd.py').is_file(),
                        '백엔드 CD 실행물 준비 진입점이 아직 없다')
        self.module = importlib.import_module('prepare_backend_cd')
        self.run = dict(id=100, run_attempt=2, name='DEMP CI', event='push', head_branch='main',
                        head_sha='a' * 40, status='completed', conclusion='success',
                        repository={'full_name': 'seungmin-park/demp'}, path='.github/workflows/ci.yml')
        self.jobs = [dict(name='DEMP verify', run_id=100, run_attempt=2, status='completed', conclusion='success')]
        self.artifacts = [dict(id=7, name='deployment-demp-100-2', expired=False,
                               size_in_bytes=1234, digest='sha256:' + 'b' * 64,
                               workflow_run={'id': 100, 'head_sha': 'a' * 40})]

    def validate(self):
        return self.module.require_ci(self.run, self.jobs, self.artifacts, 'a' * 40)

    def test_accepts_successful_current_main_push_exact_attempt(self):
        self.assertEqual(self.validate()['id'], 7)

    def test_rejects_pr_manual_old_main_failed_incomplete_wrong_workflow(self):
        changes = [dict(event='pull_request'), dict(event='workflow_dispatch'), dict(head_branch='feature'),
                   dict(head_sha='c' * 40), dict(conclusion='failure'), dict(status='in_progress'),
                   dict(name='fake CI'), dict(repository={'full_name': 'attacker/demp'}),
                   dict(path='.github/workflows/fake.yml'), dict(id=True), dict(run_attempt=0)]
        original = self.run.copy()
        for change in changes:
            self.run = {**original, **change}
            with self.subTest(change=change), self.assertRaises(ValueError):
                self.validate()

    def test_rejects_missing_required_job_skipped_job_and_wrong_attempt(self):
        original = copy.deepcopy(self.jobs)
        cases = [[], [dict(name='Other', status='completed', conclusion='success')],
                 [{**original[0], 'conclusion': 'skipped'}],
                 [{**original[0], 'run_attempt': 1}],
                 original + [dict(name='extra', run_id=100, run_attempt=2, status='completed', conclusion='failure')]]
        for jobs in cases:
            self.jobs = jobs
            with self.subTest(jobs=jobs), self.assertRaises(ValueError):
                self.validate()

    def test_rejects_expired_oversized_duplicate_and_wrong_artifact_attempt(self):
        original = copy.deepcopy(self.artifacts)
        cases = [[], original * 2, [{**original[0], 'name': 'deployment-demp-100-1'}],
                 [{**original[0], 'expired': True}], [{**original[0], 'digest': None}],
                 [{**original[0], 'size_in_bytes': 256 * 1024 * 1024 + 1}],
                 [{**original[0], 'size_in_bytes': 0}],
                 [{**original[0], 'workflow_run': {'id': 101, 'head_sha': 'a' * 40}}]]
        for artifacts in cases:
            self.artifacts = artifacts
            with self.subTest(artifacts=artifacts), self.assertRaises(ValueError):
                self.validate()

    def raw(self, **changes):
        jar = io.BytesIO()
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr('META-INF/MANIFEST.MF',
                             'Main-Class: org.springframework.boot.loader.launch.JarLauncher\nStart-Class: example.App\n')
        record = dict(component='demp', repository='seungmin-park/demp', event='push',
                      headSha='a' * 40, sourceSha='a' * 40, treeSha='b' * 40,
                      runId=100, runAttempt=2,
                      files={'demp/service.jar': hashlib.sha256(jar.getvalue()).hexdigest()})
        record.update(changes)
        output = io.BytesIO()
        with zipfile.ZipFile(output, 'w') as archive:
            archive.writestr('component.json', json.dumps(record))
            archive.writestr('demp/service.jar', jar.getvalue())
        return output.getvalue()

    def test_accepts_payload_matching_ci_metadata_and_digest(self):
        raw = self.raw()
        self.assertIsNone(self.module.require_payload(raw, 'sha256:' + hashlib.sha256(raw).hexdigest(), self.run))

    def test_rejects_tampered_zip_digest(self):
        with self.assertRaisesRegex(ValueError, 'digest'):
            self.module.require_payload(b'tampered', 'sha256:' + 'b' * 64, self.run)

    def test_rejects_payload_from_other_source_attempt_event_or_jar_hash(self):
        for change in (dict(sourceSha='c' * 40), dict(runAttempt=1), dict(event='pull_request'),
                       dict(files={'demp/service.jar': '0' * 64})):
            raw = self.raw(**change)
            with self.subTest(change=change), self.assertRaises(ValueError):
                self.module.require_payload(raw, 'sha256:' + hashlib.sha256(raw).hexdigest(), self.run)

    def test_rejects_boolean_artifact_and_job_attempt(self):
        self.run['run_attempt'] = 1
        self.jobs[0]['run_attempt'] = True
        self.artifacts[0]['name'] = 'deployment-demp-100-1'
        with self.assertRaises(ValueError):
            self.validate()
        self.jobs[0]['run_attempt'] = 1
        self.artifacts[0]['workflow_run']['id'] = 100
        self.artifacts[0]['workflow_run']['head_sha'] = 'a' * 40
        self.artifacts[0]['size_in_bytes'] = True
        with self.assertRaises(ValueError):
            self.validate()

    def test_pages_keep_jobs_and_artifacts_from_all_pages(self):
        raw = '[{"jobs":[{"name":"first"}]},{"jobs":[{"name":"second"}]}]'
        with patch.object(self.module.subprocess, 'check_output', return_value=raw) as command:
            self.assertEqual(self.module.pages('/fixture?per_page=100', 'jobs'),
                             [{'name': 'first'}, {'name': 'second'}])
        self.assertEqual(command.call_args.args[0],
                         ['gh', 'api', '--paginate', '--slurp', '/fixture?per_page=100'])

    def test_main_writes_validated_bytes_attempt_and_safe_env_metadata(self):
        raw = self.raw()
        self.artifacts[0]['digest'] = 'sha256:' + hashlib.sha256(raw).hexdigest()
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'output'
            environment_file = Path(directory) / 'github-env'
            def api(path):
                return {'object': {'sha': 'a' * 40}} if path.endswith('/git/ref/heads/main') else self.run
            def pages(path, key):
                return self.jobs if key == 'jobs' else self.artifacts
            with patch.object(self.module, 'api', side_effect=api), patch.object(self.module, 'pages', side_effect=pages), \
                    patch.object(self.module, 'download', return_value=raw), \
                    patch.dict(os.environ, {'GITHUB_ENV': str(environment_file)}), \
                    patch('sys.argv', ['prepare_backend_cd', '--run-id', '100', '--output', str(output)]):
                self.module.main()
            self.assertEqual((output / 'payload.zip').read_bytes(), raw)
            self.assertEqual(json.loads((output / 'metadata.json').read_text())['runAttempt'], 2)
            self.assertIn('CD_RUN_ATTEMPT=2\n', environment_file.read_text())
            self.assertIn('CD_HEAD_SHA=' + 'a' * 40 + '\n', environment_file.read_text())

    def test_download_stops_oversized_stream_before_writing_output(self):
        class Process:
            def __init__(self):
                self.stdout = io.BytesIO(b'x' * 129)
                self.killed = False
            def poll(self):
                return -9 if self.killed else None
            def kill(self):
                self.killed = True
            def wait(self):
                return -9 if self.killed else 0
        process = Process()
        with patch.object(self.module.subprocess, 'Popen', return_value=process), \
                patch.object(self.module, 'MAX_ARCHIVE', 128), self.assertRaisesRegex(ValueError, 'limit'):
            self.module.download(7)
        self.assertTrue(process.killed)


if __name__ == '__main__':
    unittest.main()
