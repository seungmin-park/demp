"""Run a freshly packaged DEMP application against isolated local H2."""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from verify_ci import require


def request(base, path, method='GET', payload=None, form=None, token=None):
    headers = {}
    body = None
    if form is not None:
        body = urllib.parse.urlencode(form).encode()
        headers['Content-Type'] = 'application/x-www-form-urlencoded'
    elif payload is not None:
        body = json.dumps(payload).encode()
        headers['Content-Type'] = 'application/json'
    if token:
        headers['X-AUTH-TOKEN'] = token
    operation = urllib.request.Request(base + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(operation, timeout=10) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()


def json_response(base, path, expected_status=200, **options):
    status, body = request(base, path, **options)
    require(status == expected_status, f'{path}: expected HTTP {expected_status}, got {status}')
    return json.loads(body)


def check_reactions(base, expected_documentation):
    session = json_response(base, '/api/member/login', method='POST', form={'username': 'local-member', 'password': 'password'})
    token = session['jwt']
    question_path = '/api/question/-1/reaction'
    answer_path = '/api/answer/-1/reaction'
    require(json_response(base, question_path, expected_status=401, method='PUT', payload={'reaction': 'RECOMMEND'})['errorCode'] == 401,
            'Unauthenticated reaction accepted')
    recommended = json_response(base, question_path, method='PUT', payload={'reaction': 'RECOMMEND'}, token=token)
    require(recommended == {'recommend': 1, 'dislike': 0, 'myReaction': 'RECOMMEND'}, 'Question recommendation mismatch')
    repeated = json_response(base, question_path, method='PUT', payload={'reaction': 'RECOMMEND'}, token=token)
    require(repeated == recommended, 'Repeated recommendation changed count')
    require(json_response(base, answer_path, method='PUT', payload={'reaction': 'DISLIKE'}, token=token)
            == {'recommend': 0, 'dislike': 1, 'myReaction': 'DISLIKE'}, 'Answer dislike mismatch')
    question = json_response(base, '/api/question/detail/-1', token=token)
    require({key: question[key] for key in ('recommend', 'dislike', 'myReaction')} == recommended, 'Question reaction was not committed')
    require(json_response(base, question_path, method='PUT', payload={'reaction': 'DISLIKE'}, token=token)
            == {'recommend': 0, 'dislike': 1, 'myReaction': 'DISLIKE'}, 'Reaction switch mismatch')
    require(json_response(base, question_path, method='PUT', payload={'reaction': 'NONE'}, token=token)
            == {'recommend': 0, 'dislike': 0, 'myReaction': 'NONE'}, 'Reaction cancellation mismatch')
    question = json_response(base, '/api/question/detail/-1', token=token)
    require({key: question[key] for key in ('recommend', 'dislike', 'myReaction')}
            == {'recommend': 0, 'dislike': 0, 'myReaction': 'NONE'}, 'Cancellation was not committed')
    answers = json_response(base, '/api/answer/-1', token=token)
    answer = next(item for item in answers if item['answerId'] == -1)
    require({key: answer[key] for key in ('recommend', 'dislike', 'myReaction')}
            == {'recommend': 0, 'dislike': 1, 'myReaction': 'DISLIKE'}, 'Question cancellation changed answer reaction')
    status, documentation = request(base, '/docs/index.html', token=token)
    require(status == 200 and documentation == expected_documentation, 'Served documentation mismatch')


def run(jar, expected_documentation, evidence):
    root = Path(__file__).resolve().parents[1]
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else 'java'
    environment = dict(os.environ, AWS_EC2_METADATA_DISABLED='true')
    server_log = evidence / 'server.log'
    command = [java, '-jar', str(jar), '--spring.profiles.active=local', '--server.port=0', '--server.address=127.0.0.1',
               '--spring.datasource.url=jdbc:h2:mem:ci-' + uuid.uuid4().hex + ';MODE=MySQL;DB_CLOSE_DELAY=-1',
               '--spring.jpa.hibernate.ddl-auto=create-drop', '--file.dir=' + str(evidence / 'uploads')]
    with server_log.open('w') as log:
        process = subprocess.Popen(command, cwd=root, env=environment, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 60
            port = None
            while time.monotonic() < deadline:
                require(process.poll() is None, 'Verification server exited; see ' + str(server_log))
                match = re.search(r'Tomcat started on port (\d+)', server_log.read_text())
                if match:
                    port = int(match.group(1))
                    break
                time.sleep(0.2)
            require(port is not None, 'Server startup timed out; see ' + str(server_log))
            base = f'http://127.0.0.1:{port}'
            flow = subprocess.run([os.sys.executable, 'scripts/verify_local_flow.py'], cwd=root,
                                  env=dict(environment, DEMP_LOCAL_FLOW_BASE_URL=base), capture_output=True, text=True, timeout=120)
            flow_log = evidence / 'local-flow.log'
            flow_log.write_text(flow.stdout + flow.stderr)
            require(flow.returncode == 0, 'Local API flow failed; see ' + str(flow_log))
            check_reactions(base, expected_documentation)
            return {'local_api_flow': 'passed', 'reaction_commit_switch_cancel': 'passed',
                    'unauthenticated_reaction_http': 401, 'served_docs_match': True, 'database': 'isolated in-memory H2'}
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
            print(f'Verification server stopped: {process.returncode}', flush=True)
