"""CD는 검증된 실행물만 전송하고 실패를 성공으로 표시하지 않는다."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch, Mock
import os
import base64
import io
import contextlib

ROOT = Path(__file__).parents[1]


class BackendCDTests(unittest.TestCase):
    def module(self):
        path = ROOT / 'scripts/run_backend_cd.py'
        self.assertTrue(path.exists(), '백엔드 CD 실행 진입점이 없다')
        spec = importlib.util.spec_from_file_location('run_backend_cd', path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def test_backend_cd_is_disabled_and_does_not_rebuild_artifact(self):
        path = ROOT / '.github/workflows/demp-backend-cd.yml'
        self.assertTrue(path.exists(), '기본 비활성 백엔드 CD workflow가 없다')
        import subprocess
        # Ruby의 YAML parser로 workflow mapping을 실제 해석한다.
        result = subprocess.run(['ruby', '-ryaml', '-rjson', '-e',
            'puts JSON.generate(YAML.load_file(ARGV[0]))', str(path)],text=True,capture_output=True,check=True)
        workflow = json.loads(result.stdout)
        self.assertEqual(workflow['concurrency'], {'group':'demp-backend-production','cancel-in-progress':False})
        job = workflow['jobs']['deploy']
        self.assertEqual(job['timeout-minutes'],20)
        self.assertIn("vars.DEMP_BACKEND_CD_ENABLED == 'true'", job['if'])
        self.assertEqual(job['environment'],'production')
        triggers = workflow.get('on', workflow.get('true'))
        self.assertEqual(triggers['workflow_run']['workflows'],['DEMP CI'])
        self.assertEqual(triggers['workflow_run']['branches'],['main'])
        commands = '\n'.join(step.get('run','') for step in job['steps'])
        self.assertIn('scripts/prepare_backend_cd.py',commands)
        self.assertIn('scripts/run_backend_cd.py',commands)
        self.assertNotIn('gradlew',commands)
        auth = next(step for step in job['steps'] if step.get('uses','').startswith('google-github-actions/auth@'))
        self.assertEqual(auth['with']['service_account'],'${{ vars.DEMP_BACKEND_CD_SERVICE_ACCOUNT }}')

    def test_remote_command_is_exact_and_current_main_mismatch_is_refused(self):
        module = self.module()
        env = dict(CD_RUN_ID='100',CD_RUN_ATTEMPT='2',CD_HEAD_SHA='a'*40,CD_ARCHIVE_DIGEST='b'*64,
                   CD_PROJECT='project-687f2332-ff8d-4a13-95a',CD_ZONE='asia-northeast3-a',CD_INSTANCE='deploy-vm')
        values = module.metadata(env)
        self.assertEqual(module.remote_command(values), 'deploy-back 100 2 ' + 'a'*40 + ' ' + 'b'*64)
        module.require_main(values, 'a'*40)
        with self.assertRaises(ValueError):
            module.require_main(values,'c'*40)
        for changes in ({'CD_RUN_ID':'100;id'}, {'CD_RUN_ATTEMPT':'0'}, {'CD_INSTANCE':'other-vm'},
                        {'CD_HEAD_SHA':'bad'}, {'CD_RUN_ATTEMPT':'01'}):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                module.metadata({**env,**changes})

    def test_cleanup_incomplete_is_distinct_from_success_and_error(self):
        module = self.module()
        values = dict(run_id=100,run_attempt=2,head_sha='a'*40,archive_digest='b'*64)
        success = module.summary(values,0)
        incomplete = module.summary(values,2)
        failed = module.summary(values,1)
        self.assertIn('검증 완료',success)
        self.assertIn('cleanup-incomplete',incomplete)
        self.assertNotIn('검증 완료',incomplete)
        self.assertIn('실패',failed)

    def test_zip_digest_works_without_python_311_file_digest(self):
        module = self.module()
        self.assertTrue(hasattr(module, 'archive_digest'), 'Python 3.10에서도 실행할 ZIP 해시 함수가 필요하다')
        with tempfile.TemporaryDirectory() as directory:
            archive = Path(directory)/'payload.zip'
            raw = b'verified ZIP bytes' * 100000
            archive.write_bytes(raw)
            with patch.object(hashlib, 'file_digest', create=True, side_effect=AssertionError('Python 3.11 전용')):
                self.assertEqual(module.archive_digest(archive), hashlib.sha256(raw).hexdigest())

    def test_runner_transmits_exact_bytes_and_removes_temporary_keys_even_on_incomplete_cleanup(self):
        self.runner_case(code=2)

    def test_main_change_after_tunnel_ready_refuses_ssh_and_cleans_up(self):
        self.runner_case(changed_main=True)

    def runner_case(self, code=0, changed_main=False):
        module = self.module()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root/'.cd').mkdir()
            raw = b'fixture verified immutable ZIP'
            (root/'.cd/payload.zip').write_bytes(raw)
            wire = b'\x00\x00\x00\x0bssh-ed25519\x00\x00\x00\x20' + b'k'*32
            env = dict(CD_RUN_ID='100', CD_RUN_ATTEMPT='2', CD_HEAD_SHA='a'*40,
                CD_ARCHIVE_DIGEST=hashlib.sha256(raw).hexdigest(),
                CD_PROJECT='project-687f2332-ff8d-4a13-95a', CD_ZONE='asia-northeast3-a', CD_INSTANCE='deploy-vm',
                CD_SSH_KEY='-----BEGIN OPENSSH PRIVATE KEY-----\nfixture\n',
                CD_HOST_KEY='ssh-ed25519 '+base64.b64encode(wire).decode(), RUNNER_TEMP=directory,
                GITHUB_STEP_SUMMARY=str(root/'summary.md'))
            tunnel = Mock()
            tunnel.poll.return_value = None
            transferred = []
            key_paths = []
            def send(command, **options):
                self.assertEqual(command[0], 'ssh')
                key = Path(command[command.index('-i')+1])
                key_paths.append(key)
                self.assertEqual(key.stat().st_mode & 0o777, 0o600)
                self.assertIn('StrictHostKeyChecking=yes', command)
                self.assertEqual(command[-1], module.remote_command(module.metadata(env)))
                transferred.append(options['stdin'].read())
                return Mock(returncode=code)
            prior = Path.cwd()
            os.chdir(root)
            try:
                with patch.dict(os.environ, env, clear=True), patch.object(module, '_main_sha',
                        side_effect=['a'*40, ('b' if changed_main else 'a')*40]), \
                        patch.object(module.subprocess, 'Popen', return_value=tunnel), \
                        patch.object(module.subprocess, 'run', side_effect=send), \
                        patch.object(module.socket, 'create_connection', return_value=contextlib.nullcontext()), \
                        contextlib.redirect_stdout(io.StringIO()):
                    if changed_main:
                        with self.assertRaisesRegex(ValueError, 'main'):
                            module.main()
                    else:
                        self.assertEqual(module.main(), code)
                        self.assertIn('cleanup-incomplete', (root/'summary.md').read_text())
            finally:
                os.chdir(prior)
            self.assertEqual(transferred, [] if changed_main else [raw])
            self.assertFalse(any(root.glob('demp-backend-cd-*')))
            self.assertTrue(all(not key.exists() for key in key_paths))
            tunnel.terminate.assert_called_once()
            tunnel.wait.assert_called_once_with(timeout=10)


if __name__ == '__main__':
    unittest.main()
