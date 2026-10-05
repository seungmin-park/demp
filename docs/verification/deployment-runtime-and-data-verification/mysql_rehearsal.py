"""Test the current JAR against one owned, loopback-only MySQL rehearsal container.

This is a verification artifact, not a production migration or deployment runner.
Container creation and its synthetic credential file are recorded in README.md.
"""

import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[3]
EVIDENCE = Path('/private/tmp/demp-release-20261005')
CONTAINER = 'demp-release-mysql-20261005'
JAVA = '/Users/seungmin/.asdf/installs/java/zulu-25.36.205/Contents/Home/bin/java'
CHECKS = []
SERVERS = []
DATABASE = 'demp_release_' + time.strftime('%Y%m%d_%H%M%S')
DATABASES = {DATABASE, DATABASE + '_restore', DATABASE + '_missing'}
RETAINED_PID = None


def check(condition, label):
    if not condition:
        raise AssertionError(label)
    CHECKS.append(label)
    print('PASS: ' + label, flush=True)


def sql(statement, database=None, allow_error=False, admin=False):
    database = database or DATABASE
    assert database in DATABASES and re.fullmatch(r'demp_release_\d{8}_\d{6}(_restore|_missing)?', database)
    command = ['docker', 'exec', '-i', CONTAINER, 'sh', '-c',
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --batch --skip-column-names --default-character-set=utf8mb4 ' + ('' if admin else database)]
    result = subprocess.run(command, input=statement, text=True, capture_output=True, timeout=60)
    if not allow_error and result.returncode:
        raise AssertionError('MySQL statement failed: ' + result.stderr)
    return result


def request(base, path, method='GET', payload=None, form=None, token=None, expected=200, origin=None):
    assert urllib.parse.urlparse(base).hostname == '127.0.0.1'
    headers = {}
    if origin:
        headers['Origin'] = origin
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
        with urllib.request.urlopen(operation, timeout=15) as response:
            status, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read()
    if status != expected:
        raise AssertionError(f'{method} {path}: expected {expected}, got {status}: {raw.decode()}')
    if not raw:
        return None
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return raw.decode()


def start_server(database, label, secret='r' * 32, expected_failure=None, frontend_origin=None):
    credentials = dict(line.split('=', 1) for line in (EVIDENCE / 'mysql.env').read_text().splitlines())
    address = (EVIDENCE / 'mysql-port.txt').read_text().strip()
    assert re.fullmatch(r'127\.0\.0\.1:\d+', address)
    env = dict(os.environ)
    # Prevent unrelated caller profile/config overrides from changing this rehearsal.
    for name in list(env):
        if name.startswith(('SPRING_', 'JDBC_', 'DATABASE_', 'S3_', 'JWT_', 'APP_')) or name in ('JAVA_OPTS', 'JAVA_TOOL_OPTIONS', 'DEMP_JAR_PATH', 'PORT'):
            env.pop(name)
    env.update(JDBC_DATABASE_URL=f'jdbc:mysql://{address}/{database}?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC',
               DATABASE_USERNAME='demp_verify', DATABASE_PASSWORD=credentials['MYSQL_PASSWORD'],
               S3_ACCESS_KEY='synthetic-unused-access', S3_SECRET_KEY='synthetic-unused-secret',
               S3_STATIC='ap-northeast-2', S3_BUCKET='synthetic-unused-bucket', AWS_EC2_METADATA_DISABLED='true',
               PATH=str(Path(JAVA).parent) + os.pathsep + env['PATH'])
    if secret is not None:
        env['JWT_SECRET'] = secret
    if frontend_origin:
        assert re.fullmatch(r'http://127\.0\.0\.1:\d+', frontend_origin)
        env['APP_CORS_ALLOWED_ORIGINS'] = frontend_origin
    jar = ROOT / 'build/libs/demp-0.0.1-SNAPSHOT.jar'
    command = [str(ROOT / 'run.sh'), '--server.address=127.0.0.1', '--server.port=0']
    log_path = EVIDENCE / (label + '.log')
    log = log_path.open('w')
    process = subprocess.Popen(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    SERVERS.append((process, log))
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        content = log_path.read_text()
        if process.poll() is not None:
            if expected_failure:
                check(process.returncode != 0 and expected_failure in content, label + ': expected startup refusal')
                if secret:
                    check(secret not in content, label + ': secret not logged')
                return None
            raise AssertionError(label + ': server exited; see ' + str(log_path))
        ready = re.search(r'Tomcat started on port (\d+)', content)
        if ready and 'Started DempApplication' in content:
            check(expected_failure is None, label + ': current JAR starts with schema validation')
            return 'http://127.0.0.1:' + ready.group(1), process
        time.sleep(0.2)
    raise AssertionError(label + ': startup timeout; see ' + str(log_path))


def stop_server(process):
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)


def member(base, name):
    request(base, '/api/member/save', 'POST', form={'username': name, 'password': 'password'})
    session = request(base, '/api/member/login', 'POST', form={'username': name, 'password': 'password'})
    check(session['username'] == name, 'login returns authenticated member: ' + name)
    member_id = int(sql("SELECT member_id FROM member WHERE username='" + name + "';").stdout.strip())
    return member_id, session['jwt']


def create_database(database):
    assert database in DATABASES
    sql(f"CREATE DATABASE {database} CHARACTER SET utf8mb4; GRANT ALL ON {database}.* TO 'demp_verify'@'%';", admin=True)


def legacy_fixture():
    source = (ROOT / 'src/test/resources/legacy/hibernate5-schema.sql').read_text()
    source = re.sub(r'create sequence (\w+) start with 1001 increment by 1;',
                    r'CREATE TABLE \1 (next_val BIGINT NOT NULL); INSERT INTO \1 VALUES (1001);', source)
    return source.replace('content clob', 'content longtext')


def dump_database():
    result = subprocess.run(['docker', 'exec', CONTAINER, 'sh', '-c',
                             'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --no-tablespaces --set-gtid-purged=OFF ' + DATABASE],
                            capture_output=True, text=True, timeout=60)
    check(result.returncode == 0, 'mysqldump exits successfully')
    path = EVIDENCE / 'mysql-backup.sql'
    path.write_text(result.stdout)
    path.chmod(0o600)
    return result.stdout


def rows(database):
    tables = sql('SHOW TABLES;', database).stdout.splitlines()
    snapshot = {}
    for table in tables:
        assert re.fullmatch(r'\w+', table)
        columns = sql('SHOW COLUMNS FROM ' + table, database).stdout.splitlines()
        order = ','.join('`' + column.split('\t')[0] + '`' for column in columns)
        raw = sql(f'SELECT * FROM `{table}` ORDER BY {order};', database).stdout
        snapshot[table] = hashlib.sha256(raw.encode()).hexdigest()
    return snapshot


def verify_reactions(base, question_id, answer_id, member_id, other_id, token, other_token):
    path = f'/api/question/{question_id}/reaction'
    def react(value, who=token):
        return request(base, path, 'PUT', {'reaction': value}, token=who)
    react('RECOMMEND')
    react('RECOMMEND')
    detail = request(base, f'/api/question/detail/{question_id}', token=token)
    check((detail['recommend'], detail['dislike'], detail['myReaction']) == (1, 0, 'RECOMMEND'), 'repeated recommendation is idempotent in MySQL')
    react('DISLIKE')
    detail = request(base, f'/api/question/detail/{question_id}', token=token)
    check((detail['recommend'], detail['dislike'], detail['myReaction']) == (0, 1, 'DISLIKE'), 'reaction switch commits both counts')
    react('NONE')
    detail = request(base, f'/api/question/detail/{question_id}', token=token)
    check((detail['recommend'], detail['dislike'], detail['myReaction']) == (0, 0, 'NONE'), 'reaction cancellation persists')
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        list(pool.map(lambda index: react('RECOMMEND', token if index % 2 == 0 else other_token), range(12)))
    detail = request(base, f'/api/question/detail/{question_id}', token=token)
    check((detail['recommend'], detail['dislike']) == (2, 0), '12 concurrent requests from two members count once each')
    check(sql(f'SELECT COUNT(*) FROM content_reaction WHERE question_id={question_id};').stdout.strip() == '2', 'exactly two question reaction rows committed')
    request(base, f'/api/answer/{answer_id}/reaction', 'PUT', {'reaction': 'DISLIKE'}, token=other_token)
    answers = request(base, f'/api/answer/{question_id}', token=other_token)
    check(len(answers) == 1 and (answers[0]['dislike'], answers[0]['myReaction']) == (1, 'DISLIKE'), 'answer reaction independently persists')
    violations = [
        (f"INSERT INTO content_reaction(member_id,question_id,reaction) VALUES({member_id},{question_id},'RECOMMEND');", '1062', 'unique member/question'),
        (f"INSERT INTO content_reaction(member_id,question_id,reaction) VALUES(999999,{question_id},'RECOMMEND');", '1452', 'foreign key'),
        (f"INSERT INTO content_reaction(member_id,question_id,answer_id,reaction) VALUES({member_id},{question_id},{answer_id},'NONE');", '3819', 'single target check'),
        (f"INSERT INTO content_reaction(member_id,answer_id,reaction) VALUES({member_id},{answer_id},'INVALID');", '3819', 'reaction value check'),
    ]
    for statement, code, label in violations:
        result = sql(statement, allow_error=True)
        check(result.returncode != 0 and code in result.stderr, 'MySQL rejects ' + label)


def main():
    global RETAINED_PID
    label = subprocess.check_output(['docker', 'inspect', '--format', '{{index .Config.Labels "demp.verification"}}', CONTAINER], text=True).strip()
    check(label == 'release-20261005', 'owned container label')
    port = (EVIDENCE / 'mysql-port.txt').read_text().strip()
    check(port.startswith('127.0.0.1:'), 'MySQL publishes only on loopback')
    create_database(DATABASE)
    sql(legacy_fixture())
    sql("INSERT INTO announcement(id,announcement_type,min_career,max_career,name,content,payment,title,started_date,dead_line_date) VALUES(900,'EMP',0,3,'한글 회사','기존 공고 본문',4500,'기존 한글 공고','2026-10-01 09:00:00','2026-11-01 18:00:00');")
    check(sql('SELECT tag_name FROM hashtag WHERE hashtag_id=1000;').stdout.strip() == 'legacy-preserved', 'fresh translated legacy fixture exists')
    migrations = ['announcement-body-images.sql', 'announcement-compensation.sql', 'announcement-education.sql',
                  'announcement-publication.sql', 'content-reaction.sql']
    hashes = {}
    for name in migrations:
        path = ROOT / 'src/main/resources/db/manual' / name
        hashes[name] = hashlib.sha256(path.read_bytes()).hexdigest()
        sql(path.read_text())
        check(True, 'actual MySQL manual SQL applied: ' + name)
    (EVIDENCE / 'manual-sql-sha256.json').write_text(json.dumps(hashes, indent=2) + '\n')
    base, process = start_server(DATABASE, 'mysql-validate')
    check(sql('SELECT COUNT(*) FROM member;').stdout.strip() == '0', 'production profile creates no local seed')
    check(sql('SELECT tag_name FROM hashtag WHERE hashtag_id=1000;').stdout.strip() == 'legacy-preserved', 'legacy row survived migrations and validate')
    check(sql('SELECT payment,publication_status,salary_status,salary_max FROM announcement WHERE id=900;').stdout.strip() == '4500\tPUBLISHED\tNULL\tNULL', 'migration preserves old amount, publication, and unknown nullable data')
    sql('INSERT INTO announcement(id,min_career,max_career,payment,publication_status) VALUES(901,0,0,NULL,\'DRAFT\');')
    check(sql('SELECT payment FROM announcement WHERE id=901;').stdout.strip() == 'NULL', 'actual MySQL accepts unknown salary as NULL')
    member_id, token = member(base, 'mysql-qa-u')
    check(member_id == 1001, 'legacy member ID table continues at 1001')
    announcement = request(base, '/api/announce/detail/900', token=token)
    check(announcement['title'] == '기존 한글 공고' and '기존 공고 본문' in announcement['content'], 'legacy EMP announcement is readable through current HTTP contract')
    check(sql('SELECT announcement_type,started_date,dead_line_date FROM announcement WHERE id=900;').stdout.strip() == 'EMP\t2026-10-01 09:00:00\t2026-11-01 18:00:00', 'legacy enum and dates survive migration')
    request(base, '/api/question/add', 'POST', payload={'title': 'MySQL 한글 질문', 'content': '<p>한글 본문</p>', 'username': 'ignored', 'hashtags': ['mysql-java']}, token=token)
    listing = request(base, '/api/question?orderBy=createdDate&page=0&size=20')
    question_id = next(item['id'] for item in listing['content'] if item['title'] == 'MySQL 한글 질문')
    check(question_id == 1001, 'legacy question ID table continues at 1001')
    question = request(base, f'/api/question/detail/{question_id}', token=token)
    check(question['username'] == 'mysql-qa-u' and '한글 본문' in question['content'], 'real MySQL author and unicode content persisted')
    answers = request(base, '/api/answer/save', 'POST', payload={'username': 'ignored', 'questionId': question_id, 'answerContent': 'MySQL 한글 답변'}, token=token)
    check(len(answers) == 1 and answers[0]['username'] == 'mysql-qa-u', 'answer uses authenticated author')
    answer_id = answers[0]['answerId']
    other_id, other_token = member(base, 'mysql-qa-v')
    request(base, '/api/question/update', 'PATCH', payload={'questionId': question_id, 'title': '변조', 'content': '변조', 'hashtags': []}, token=other_token, expected=403)
    check(request(base, f'/api/question/detail/{question_id}', token=token)['title'] == 'MySQL 한글 질문', 'non-owner update is forbidden and leaves data unchanged')
    request(base, f'/api/question/detail/{question_id}', token='invalid-token', expected=401)
    check(True, 'invalid JWT refused by actual production security filter')
    request(base, '/api/admin/me', expected=401)
    request(base, '/api/admin/me', token=token, expected=403)
    sql(f"INSERT INTO member_roles(member_member_id,roles) VALUES({member_id},'ROLE_ADMIN');")
    request(base, '/api/admin/me', token=token)
    check(True, 'stored admin role permits existing JWT; user role alone is refused')
    sql(f"DELETE FROM member_roles WHERE member_member_id={member_id} AND roles='ROLE_ADMIN';")
    request(base, '/api/admin/me', token=token, expected=403)
    check(True, 'admin revocation immediately refuses the same JWT')
    verify_reactions(base, question_id, answer_id, member_id, other_id, token, other_token)
    stop_server(process)
    base, process = start_server(DATABASE, 'mysql-restart')
    token = request(base, '/api/member/login', 'POST', form={'username': 'mysql-qa-u', 'password': 'password'})['jwt']
    detail = request(base, f'/api/question/detail/{question_id}', token=token)
    check((detail['title'], detail['recommend'], detail['dislike'], detail['myReaction']) == ('MySQL 한글 질문', 2, 0, 'RECOMMEND'), 'fresh JVM retains question and member reaction')
    check(sql('SELECT COUNT(*) FROM member;').stdout.strip() == '2', 'restart neither seeds nor deletes members')
    check(sql("SELECT content FROM answer WHERE answer_id=" + str(answer_id)).stdout.strip() == 'MySQL 한글 답변', 'answer survives JVM restart')
    stop_server(process)
    backup = dump_database()
    restore = DATABASE + '_restore'
    create_database(restore)
    sql(backup, restore)
    check(rows(DATABASE) == rows(restore), 'all table row hashes match after dump/restore')
    restored_base, restored_process = start_server(restore, 'mysql-restored')
    restored_token = request(restored_base, '/api/member/login', 'POST', form={'username': 'mysql-qa-u', 'password': 'password'})['jwt']
    restored = request(restored_base, f'/api/question/detail/{question_id}', token=restored_token)
    check((restored['recommend'], restored['myReaction']) == (2, 'RECOMMEND'), 'restored DB serves authenticated persisted reaction')
    request(restored_base, '/api/question/add', 'POST', payload={'title': '복구 후 신규 질문', 'content': '복구 후 저장', 'username': 'ignored', 'hashtags': []}, token=restored_token)
    restored_listing = request(restored_base, '/api/question?orderBy=createdDate&page=0&size=20')
    restored_new = [item for item in restored_listing['content'] if item['title'] == '복구 후 신규 질문']
    check(len(restored_new) == 1 and restored_new[0]['id'] == 1002, 'restored DB accepts new writes with preserved ID generator')
    stop_server(restored_process)
    missing = DATABASE + '_missing'
    create_database(missing)
    sql(backup, missing)
    sql('ALTER TABLE announcement DROP COLUMN salary_status;', missing)
    start_server(missing, 'mysql-missing-column', expected_failure='missing column [salary_status]')
    start_server(DATABASE, 'jwt-missing', secret=None, expected_failure="Could not resolve placeholder 'JWT_SECRET'")
    start_server(DATABASE, 'jwt-31-bytes', secret='s' * 31, expected_failure='JWT_SECRET must contain at least 32 UTF-8 bytes')
    base, process = start_server(DATABASE, 'mysql-live', frontend_origin='http://127.0.0.1:5057')
    request(base, '/api/member/login', 'POST', form={'username': 'mysql-qa-u', 'password': 'password'})
    check(True, 'exact 32-byte JWT secret supports startup and login')
    request(base, '/api/member/login', 'POST', form={'username': 'mysql-qa-u', 'password': 'password'}, origin='http://127.0.0.1:5057')
    check(True, 'configured preview Origin can login through production CORS filter')
    request(base, '/api/member/login', 'POST', form={'username': 'mysql-qa-u', 'password': 'password'}, origin='http://unapproved.invalid', expected=403)
    check(True, 'unapproved Origin is still rejected')
    (EVIDENCE / 'mysql-live.json').write_text(json.dumps({'base_url': base, 'pid': process.pid, 'database': DATABASE, 'member_id': member_id, 'question_id': question_id, 'answer_id': answer_id}, indent=2) + '\n')
    RETAINED_PID = process.pid
    print('MYSQL_REHEARSAL=passed; final owned server retained for visible browser verification', flush=True)


if __name__ == '__main__':
    status = 'failed'
    try:
        main()
        status = 'passed'
    finally:
        for process, log in SERVERS:
            if process.pid != RETAINED_PID:
                stop_server(process)
            log.close()
        jar = ROOT / 'build/libs/demp-0.0.1-SNAPSHOT.jar'
        result = {'status': status, 'checks': CHECKS, 'database': 'owned local MySQL; translated repository legacy fixture',
                  'database_name': DATABASE, 'production_database_verified': False, 'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
                  'launcher_sha256': hashlib.sha256((ROOT / 'run.sh').read_bytes()).hexdigest()}
        (EVIDENCE / 'mysql-result.json').write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n')
