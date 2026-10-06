"""Verify login protection on an owned, loopback-only synthetic MySQL fixture.

Reuse the release rehearsal's validated SQL/startup boundaries. No production DB.
"""
import concurrent.futures
import hashlib
import importlib.util
import json
import os
import subprocess
from pathlib import Path
import re
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[3]
EVIDENCE = Path('/private/tmp/demp-login-protection-20261006/mysql')
spec = importlib.util.spec_from_file_location('release_rehearsal', ROOT / 'docs/verification/deployment-runtime-and-data-verification/mysql_rehearsal.py')
legacy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(legacy)
legacy.ROOT = ROOT
legacy.EVIDENCE = EVIDENCE
legacy.DATABASE = 'demp_release_' + time.strftime('%Y%m%d_%H%M%S')
legacy.DATABASES = {legacy.DATABASE, legacy.DATABASE + '_missing'}
CHECKS = legacy.CHECKS


def login(base, username, password):
    assert urllib.parse.urlparse(base).hostname == '127.0.0.1'
    data = urllib.parse.urlencode({'username': username, 'password': password}).encode()
    request = urllib.request.Request(base + '/api/member/login', data=data,
        headers={'Content-Type': 'application/x-www-form-urlencoded'}, method='POST')
    try:
        response = urllib.request.urlopen(request, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers, json.loads(response.read())


def assert_state(username, count, empty_times=False):
    row = legacy.sql("SELECT failed_login_count, login_failure_window_started_at IS NULL, login_blocked_until IS NULL FROM member WHERE username='" + username + "';").stdout.strip()
    if empty_times:
        legacy.check(row == f'{count}\t1\t1', username + ': reset state committed')
    else:
        legacy.check(row.split('\t')[0] == str(count), username + ': failure count committed')


def start_server_in_timezone(database, label, zone):
    assert database in legacy.DATABASES and zone in ('+00:00', '+09:00')
    credentials = dict(line.split('=', 1) for line in (EVIDENCE / 'mysql.env').read_text().splitlines())
    address = (EVIDENCE / 'mysql-port.txt').read_text().strip()
    assert re.fullmatch(r'127\.0\.0\.1:\d+', address)
    env = dict(os.environ)
    for name in list(env):
        if name.startswith(('SPRING_', 'JDBC_', 'DATABASE_', 'S3_', 'JWT_', 'APP_')) or name in ('JAVA_OPTS', 'JAVA_TOOL_OPTIONS', 'DEMP_JAR_PATH', 'PORT'):
            env.pop(name)
    env.update(JDBC_DATABASE_URL=f'jdbc:mysql://{address}/{database}?allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC',
               DATABASE_USERNAME='demp_verify', DATABASE_PASSWORD=credentials['MYSQL_PASSWORD'],
               S3_ACCESS_KEY='synthetic-unused-access', S3_SECRET_KEY='synthetic-unused-secret',
               S3_STATIC='ap-northeast-2', S3_BUCKET='synthetic-unused-bucket', AWS_EC2_METADATA_DISABLED='true',
               JWT_SECRET='r' * 32, PATH=str(Path(legacy.JAVA).parent) + os.pathsep + env['PATH'])
    command = [str(ROOT / 'run.sh'), '--server.address=127.0.0.1', '--server.port=0',
               "--spring.datasource.hikari.connection-init-sql=SET time_zone = '" + zone + "'"]
    log_path = EVIDENCE / (label + '.log')
    log = log_path.open('w')
    process = subprocess.Popen(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    legacy.SERVERS.append((process, log))
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        content = log_path.read_text()
        if process.poll() is not None:
            raise AssertionError(label + ': startup exited; see ' + str(log_path))
        ready = re.search(r'Tomcat started on port (\d+)', content)
        if ready and 'Started DempApplication' in content:
            legacy.check(True, label + ': current JAR validates, pool session time_zone=' + zone)
            return 'http://127.0.0.1:' + ready.group(1), process
        time.sleep(0.2)
    raise AssertionError(label + ': startup timeout')


def main():
    import subprocess
    label = subprocess.check_output(['docker', 'inspect', '--format', '{{index .Config.Labels "demp.verification"}}', legacy.CONTAINER], text=True).strip()
    legacy.check(label == 'release-20261005', 'owned MySQL container label')
    legacy.check((EVIDENCE / 'mysql-port.txt').read_text().startswith('127.0.0.1:'), 'MySQL loopback only')
    legacy.create_database(legacy.DATABASE)
    legacy.sql(legacy.legacy_fixture())
    seed = (ROOT / 'src/main/resources/local/data.sql').read_text()
    password_hash = re.search(r"'local-member', '([^']+)'", seed).group(1)
    legacy.sql("INSERT INTO member(member_id,username,password) VALUES(990,'legacy-login','" + password_hash + "'); INSERT INTO member_roles(member_member_id,roles) VALUES(990,'ROLE_USER');")
    before = legacy.sql('SELECT member_id,username,password FROM member;').stdout
    for name in ['announcement-body-images.sql', 'announcement-compensation.sql', 'announcement-education.sql', 'announcement-publication.sql', 'content-reaction.sql', 'member-login-protection.sql']:
        legacy.sql((ROOT / 'src/main/resources/db/manual' / name).read_text())
        legacy.check(True, 'actual MySQL SQL applied: ' + name)
    legacy.check(legacy.sql('SELECT member_id,username,password FROM member;').stdout == before, 'existing member identity and BCrypt unchanged by SQL')
    assert_state('legacy-login', 0, True)
    first, first_process = start_server_in_timezone(legacy.DATABASE, 'server-one', '+00:00')
    second, second_process = start_server_in_timezone(legacy.DATABASE, 'server-two', '+09:00')
    legacy.check(first_process.pid != second_process.pid, 'two independent JVMs share one MySQL DB')
    legacy.check(login(first, 'legacy-login', 'password')[0] == 200, 'legacy member still logs in')
    for index in range(4):
        legacy.check(login(first if index % 2 == 0 else second, 'legacy-login', 'wrong')[0] == 401, 'first four distributed failures return 401')
    assert_state('legacy-login', 4)
    status, headers, body = login(second, 'legacy-login', 'wrong')
    legacy.check(status == 429 and 890 <= int(headers['Retry-After']) <= 900 and body['errorCode'] == 429, 'fifth failure returns 429 and remaining seconds')
    assert_state('legacy-login', 5)
    blocked_until = legacy.sql("SELECT login_blocked_until FROM member WHERE username='legacy-login';").stdout
    observed_status = login(first, 'legacy-login', 'password')[0]
    legacy.check(observed_status == 429, f'other JVM cannot bypass block: expected 429, actual {observed_status}')
    legacy.check(legacy.sql("SELECT login_blocked_until FROM member WHERE username='legacy-login';").stdout == blocked_until, 'blocked retry does not extend deadline')
    legacy.stop_server(first_process)
    restarted, restarted_process = start_server_in_timezone(legacy.DATABASE, 'server-restarted', '+00:00')
    legacy.check(login(restarted, 'legacy-login', 'password')[0] == 429, 'JVM restart retains DB block')
    legacy.sql("UPDATE member SET login_blocked_until=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND WHERE username='legacy-login';")
    legacy.check(login(restarted, 'legacy-login', 'password')[0] == 200, 'expired block allows login against actual MySQL timestamp')
    assert_state('legacy-login', 0, True)
    legacy.member(second, 'concurrent-login')
    barrier = threading.Barrier(8)
    def attempt(index):
        barrier.wait(timeout=10)
        return login(second if index % 2 else restarted, 'concurrent-login', 'wrong')[0]
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        statuses = list(pool.map(attempt, range(8)))
    legacy.check(sorted(statuses) == [401] * 4 + [429] * 4, 'eight concurrent HTTP failures across two JVMs serialize at five')
    assert_state('concurrent-login', 5)
    legacy.check(login(restarted, 'concurrent-login', 'password')[0] == 429, 'concurrent limit also rejects correct password')
    legacy.check(login(second, 'legacy-login', 'password')[0] == 200, 'another account remains independent')
    backup = legacy.dump_database()
    missing = legacy.DATABASE + '_missing'
    legacy.create_database(missing)
    legacy.sql(backup, missing)
    legacy.sql('ALTER TABLE member DROP COLUMN failed_login_count;', missing)
    legacy.start_server(missing, 'missing-login-column', expected_failure='missing column [failed_login_count]')


if __name__ == '__main__':
    status = 'failed'
    try:
        main()
        status = 'passed'
    finally:
        for process, log in legacy.SERVERS:
            legacy.stop_server(process)
            log.close()
        result = {'status': status, 'checks': CHECKS, 'production_database_verified': False,
                  'database_name': legacy.DATABASE, 'jar_sha256': hashlib.sha256((ROOT / 'build/libs/demp-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest(),
                  'manual_sql_sha256': hashlib.sha256((ROOT / 'src/main/resources/db/manual/member-login-protection.sql').read_bytes()).hexdigest()}
        (EVIDENCE / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
