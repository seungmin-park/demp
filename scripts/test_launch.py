"""Observable launch contract; the JVM boundary is a local executable fixture."""

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


LAUNCHER = Path(__file__).resolve().parents[1] / 'run.sh'


class LaunchContractTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.work = Path(self.directory.name)
        self.jar = self.work / 'candidate with space.jar'
        self.jar.write_text('local synthetic artifact')
        java = self.work / 'java'
        java.write_text('#!/usr/bin/env python3\nimport json,os,sys\n'
                        'print(json.dumps({"args":sys.argv[1:],"pid":os.getpid()}))\n'
                        'sys.exit(int(os.environ.get("FAKE_JAVA_EXIT","0")))\n')
        java.chmod(0o755)
        self.env = dict(os.environ)
        for name in ('PORT', 'JAVA_OPTS', 'JAVA_TOOL_OPTIONS', 'SPRING_PROFILES_ACTIVE'):
            self.env.pop(name, None)
        self.env.update(PATH=str(self.work) + os.pathsep + self.env['PATH'],
                        DEMP_JAR_PATH=str(self.jar))

    def launch(self, *arguments):
        process = subprocess.Popen(['bash', str(LAUNCHER), *arguments], cwd=self.work,
                                   env=self.env, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        output, error = process.communicate(timeout=10)
        return process, output, error

    def test_default_port_profile_and_no_secret_files(self):
        """기본 운영 기동은 후보 JAR만 실행하고 비밀 파일을 만들지 않는다."""
        process, output, error = self.launch()
        self.assertEqual(process.returncode, 0, error)
        result = json.loads(output)
        jar_option = result['args'].index('-jar')
        self.assertCountEqual(result['args'][:jar_option], ['-Dserver.port=8080', '-Dspring.profiles.active=prod'])
        self.assertEqual(result['args'][jar_option:], ['-jar', str(self.jar)])
        self.assertEqual({path.name for path in self.work.iterdir()}, {self.jar.name, 'java'})

    def test_configured_options_and_application_arguments(self):
        """포트·프로필·JVM 옵션·앱 인자를 손실 없이 전달한다."""
        self.env.update(PORT='8087', SPRING_PROFILES_ACTIVE='staging', JAVA_OPTS='-Xms128m -Xmx512m')
        process, output, error = self.launch('--server.address=127.0.0.1')
        self.assertEqual(process.returncode, 0, error)
        args = json.loads(output)['args']
        jar_option = args.index('-jar')
        self.assertCountEqual(args[:jar_option], ['-Xms128m', '-Xmx512m', '-Dserver.port=8087', '-Dspring.profiles.active=staging'])
        self.assertEqual(args[jar_option:], ['-jar', str(self.jar), '--server.address=127.0.0.1'])

    def test_exec_keeps_process_identity_and_exit_status(self):
        """프로세스 관리자가 JVM에 종료 신호를 보내고 실제 종료 코드를 받는다."""
        self.env['FAKE_JAVA_EXIT'] = '23'
        process, output, _ = self.launch()
        self.assertEqual(process.returncode, 23)
        self.assertEqual(json.loads(output)['pid'], process.pid)

    def test_missing_artifact_refuses_before_starting_java(self):
        """누락된 산출물은 JVM 실행 전에 명확히 거절한다."""
        self.env['DEMP_JAR_PATH'] = str(self.work / 'missing.jar')
        process, output, error = self.launch()
        self.assertNotEqual(process.returncode, 0)
        self.assertEqual(output, '')
        self.assertIn('JAR not found', error)

    def test_invalid_port_refuses_before_starting_java(self):
        """포트 경계 밖과 비숫자 입력은 JVM 실행 전에 거절한다."""
        for port in ('0', '65536', 'abc'):
            with self.subTest(port=port):
                self.env['PORT'] = port
                process, output, error = self.launch()
                self.assertNotEqual(process.returncode, 0)
                self.assertEqual(output, '')
                self.assertIn('PORT must be', error)


if __name__ == '__main__':
    unittest.main()
