"""Check the current JAR and manual employment SQL on the owned MySQL fixture.

Only fresh demp_release_* databases are written. Existing fixture DBs remain.
Synthetic credentials stay in the container/private evidence, never stdout.
"""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parents[3]
parser = argparse.ArgumentParser()
parser.add_argument('--evidence', type=Path, required=True)
args = parser.parse_args()
EVIDENCE = args.evidence
EVIDENCE.mkdir(parents=True, exist_ok=True)
spec = importlib.util.spec_from_file_location('release_rehearsal', ROOT / 'docs/verification/deployment-runtime-and-data-verification/mysql_rehearsal.py')
legacy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(legacy)
legacy.ROOT = ROOT
legacy.EVIDENCE = EVIDENCE
legacy.JAVA = str(Path(os.environ['JAVA_HOME']) / 'bin/java')
legacy.DATABASE = 'demp_release_' + time.strftime('%Y%m%d_%H%M%S')
legacy.DATABASES = {legacy.DATABASE, legacy.DATABASE + '_missing'}
STARTED_CONTAINER = False
TYPES = ['REGULAR', 'CONTRACT', 'CONVERSION_INTERNSHIP', 'EXPERIENTIAL_INTERNSHIP']


def prepare_container():
    global STARTED_CONTAINER
    details = json.loads(subprocess.check_output(['docker', 'inspect', legacy.CONTAINER], text=True))[0]
    legacy.check(details['Config']['Labels'].get('demp.verification') == 'release-20261005', 'owned synthetic container label')
    bindings = details['HostConfig']['PortBindings']['3306/tcp']
    legacy.check(all(binding['HostIp'] == '127.0.0.1' for binding in bindings), 'MySQL binds only loopback')
    credentials = dict(item.split('=', 1) for item in details['Config']['Env'] if '=' in item)
    legacy.check(credentials.get('MYSQL_USER') == 'demp_verify', 'synthetic DB user')
    credential_path = EVIDENCE / 'mysql.env'
    credential_path.touch(mode=0o600, exist_ok=True)
    credential_path.chmod(0o600)
    credential_path.write_text('MYSQL_PASSWORD=' + credentials['MYSQL_PASSWORD'] + '\n')
    if not details['State']['Running']:
        subprocess.run(['docker', 'start', legacy.CONTAINER], check=True)
        STARTED_CONTAINER = True
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        ready = subprocess.run(['docker', 'exec', legacy.CONTAINER, 'sh', '-c',
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --batch --skip-column-names -e "SELECT 1"'], capture_output=True, text=True)
        if ready.returncode == 0:
            break
        time.sleep(0.2)
    else:
        raise AssertionError('owned MySQL did not become ready')
    port = subprocess.check_output(['docker', 'port', legacy.CONTAINER, '3306/tcp'], text=True).strip()
    legacy.check(bool(re.fullmatch(r'127\.0\.0\.1:\d+', port)), 'published loopback port')
    (EVIDENCE / 'mysql-port.txt').write_text(port)


def migrate(database, include_employment):
    legacy.create_database(database)
    legacy.sql(legacy.legacy_fixture(), database)
    legacy.sql("INSERT INTO announcement(id,announcement_type,min_career,max_career,name,content,payment,title,access_url,job_position,started_date,dead_line_date) VALUES(900,'EMP',0,3,'기존 회사','기존 본문',4500,'기존 고용 형태 미확인','https://example.test/legacy','BACKEND','2026-10-01 09:00:00','2026-11-01 18:00:00');", database)
    names = ['announcement-body-images.sql', 'announcement-compensation.sql', 'announcement-education.sql',
             'announcement-publication.sql', 'content-reaction.sql', 'member-login-protection.sql', 'answer-cursor-index.sql']
    if include_employment:
        names.append('announcement-employment-type.sql')
    for name in names:
        legacy.sql((ROOT / 'src/main/resources/db/manual' / name).read_text(), database)
    legacy.check(True, 'manual SQL applied: ' + ('with' if include_employment else 'without') + ' employment column')


def main():
    prepare_container()
    migrate(legacy.DATABASE, True)
    legacy.check(legacy.sql('SELECT title,name,payment,employment_type FROM announcement WHERE id=900;').stdout.strip()
                 == '기존 고용 형태 미확인\t기존 회사\t4500\tNULL', 'legacy title/company/amount preserved and employment remains NULL')
    seed = (ROOT / 'src/main/resources/local/data.sql').read_text()
    password_hash = re.search(r"'local-member', '([^']+)'", seed).group(1)
    legacy.sql("INSERT INTO member(member_id,username,password) VALUES(990,'employment-admin','" + password_hash + "'); INSERT INTO member_roles(member_member_id,roles) VALUES(990,'ROLE_USER'),(990,'ROLE_ADMIN');")
    base, process = legacy.start_server(legacy.DATABASE, 'mysql-employment')
    token = legacy.request(base, '/api/member/login', 'POST', form={'username': 'employment-admin', 'password': 'password'})['jwt']
    legacy.request(base, '/api/admin/me', token=token)
    legacy.check(legacy.request(base, '/api/announce/detail/900', token=token)['employmentType'] is None, 'legacy HTTP employment is NULL')
    form = {'title': '고용 형태 리허설', 'company': 'DEMP', 'type': 'EMP', 'position': 'BACKEND',
            'startedDate': '2026-10-07T09:00', 'deadLineDate': '2026-11-01T18:00',
            'content': '<p>인턴 지원 안내</p>', 'language': 'JAVA', 'recruitmentAudience': 'NEW',
            'minCareer': '0', 'maxCareer': '0', 'publicationStatus': 'PUBLISHED'}
    created = {}
    for value in TYPES:
        submitted = {**form, 'title': value, 'accessUrl': 'https://example.test/' + value, 'employmentType': value}
        legacy.request(base, '/api/admin/announcements', 'POST', form=submitted, token=token, expected=201)
        row = legacy.sql("SELECT id,employment_type,recruitment_audience FROM announcement WHERE title='" + value + "';").stdout.strip().split('\t')
        legacy.check(row[1:] == [value, 'NEW'], value + ': independent committed employment/audience')
        created[int(row[0])] = value
        detail = legacy.request(base, '/api/announce/detail/' + row[0], token=token)
        legacy.check((detail['employmentType'], detail['recruitmentAudience']) == (value, 'NEW'), value + ': detail contract')
    listing = legacy.request(base, '/api/announce?size=8')['content']
    legacy.check({item['id']: item['employmentType'] for item in listing if item['id'] in created} == created, 'all created employment types in public list')
    related = legacy.request(base, '/api/announce/scroll', token=token)
    legacy.check({item['id']: item['employmentType'] for item in related if item['id'] in created} == created, 'all created employment types in related response')
    edited_id = next(iter(created))
    changed = {**form, 'accessUrl': 'https://example.test/REGULAR', 'employmentType': 'EXPERIENTIAL_INTERNSHIP'}
    legacy.request(base, f'/api/admin/announcements/{edited_id}', 'PATCH', form=changed, token=token)
    legacy.check(legacy.sql(f'SELECT employment_type FROM announcement WHERE id={edited_id};').stdout.strip() == 'EXPERIENTIAL_INTERNSHIP', 'admin PATCH commits changed employment')
    legacy.request(base, f'/api/admin/announcements/{edited_id}', 'PATCH', form={**changed, 'employmentType': 'INVALID'}, token=token, expected=400)
    legacy.check(legacy.sql(f'SELECT employment_type FROM announcement WHERE id={edited_id};').stdout.strip() == 'EXPERIENTIAL_INTERNSHIP', 'invalid enum rejected without changing saved value')
    changed.pop('employmentType')
    legacy.request(base, f'/api/admin/announcements/{edited_id}', 'PATCH', form=changed, token=token)
    legacy.check(legacy.sql(f'SELECT employment_type FROM announcement WHERE id={edited_id};').stdout.strip() == 'NULL', 'admin can clear employment to unknown')
    legacy.request(base, f'/api/admin/announcements/{edited_id}', 'PATCH', form={**changed, 'type': 'EDU', 'employmentType': 'CONVERSION_INTERNSHIP'}, token=token)
    legacy.check(legacy.sql(f'SELECT announcement_type,employment_type,recruitment_audience FROM announcement WHERE id={edited_id};').stdout.strip() == 'EDU\tNULL\tNULL', 'education conversion clears employment and recruitment audience')
    legacy.stop_server(process)
    missing = legacy.DATABASE + '_missing'
    migrate(missing, False)
    legacy.start_server(missing, 'mysql-missing-employment', expected_failure='missing column [employment_type]')


if __name__ == '__main__':
    status = 'failed'
    try:
        main()
        status = 'passed'
    finally:
        for process, log in legacy.SERVERS:
            legacy.stop_server(process)
            log.close()
        if STARTED_CONTAINER:
            subprocess.run(['docker', 'stop', legacy.CONTAINER], check=True)
        result = {'status': status, 'checks': legacy.CHECKS, 'production_database_verified': False,
                  'database': legacy.DATABASE, 'jar_sha256': hashlib.sha256((ROOT / 'build/libs/demp-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest(),
                  'sql_sha256': hashlib.sha256((ROOT / 'src/main/resources/db/manual/announcement-employment-type.sql').read_bytes()).hexdigest()}
        (EVIDENCE / 'mysql-result.json').write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n')
