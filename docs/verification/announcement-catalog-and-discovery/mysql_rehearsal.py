"""Verify catalog, views and ordering using the existing owned MySQL fixture.

Only fresh demp_release_* databases are written; no production connection.
Reuse the established container ownership, loopback and credential guards.
"""
import argparse
import concurrent.futures
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import urllib.parse

ROOT = Path(__file__).resolve().parents[3]
parser = argparse.ArgumentParser()
parser.add_argument('--evidence', type=Path, required=True)
args = parser.parse_args()
spec = importlib.util.spec_from_file_location('employment_rehearsal', ROOT / 'docs/verification/separate-employment-type/mysql_rehearsal.py')
fixture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fixture)
db = fixture.legacy
FRONTEND = ROOT.parent / 'frontend'
TECHNOLOGIES = re.findall(r"\['([A-Za-z_]+)'\s*,", (FRONTEND / 'src/data/technologies.ts').read_text())
POSITIONS = re.findall(r'"([A-Z_]+)"', (FRONTEND / 'src/data/positions.ts').read_text())


def migrate(database, with_hits):
    fixture.migrate(database, True)
    if with_hits:
        db.sql((ROOT / 'src/main/resources/db/manual/announcement-view-count.sql').read_text(), database)


def detail(base, item_id, token, record=False):
    return db.request(base, f'/api/announce/detail/{item_id}' + ('' if record else '?recordView=false'), token=token)


def listing(base, **conditions):
    return db.request(base, '/api/announce?' + urllib.parse.urlencode({'size': 100, **conditions}))


def main():
    fixture.prepare_container()
    migrate(db.DATABASE, True)
    db.check(db.sql('SELECT title,name,payment,hits FROM announcement WHERE id=900;').stdout.strip()
             == '기존 고용 형태 미확인\t기존 회사\t4500\t0', 'legacy text/company/amount preserved; views start at zero')
    columns = db.sql("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND ((TABLE_NAME='announcement' AND COLUMN_NAME='job_position') OR (TABLE_NAME='language' AND COLUMN_NAME='languages')) ORDER BY TABLE_NAME;").stdout
    db.check(columns.strip().splitlines() == ['announcement\tjob_position\tvarchar', 'language\tlanguages\tvarchar'], 'actual MySQL catalog columns are VARCHAR')
    db.check(len(set(TECHNOLOGIES)) == 66 and 'React' in TECHNOLOGIES and len(set(POSITIONS)) == 31, 'shared frontend catalog contains 66 technologies and 31 roles')
    seed = (ROOT / 'src/main/resources/local/data.sql').read_text()
    password_hash = re.search(r"'local-member', '([^']+)'", seed).group(1)
    db.sql("INSERT INTO member(member_id,username,password) VALUES(990,'catalog-admin','" + password_hash + "'); INSERT INTO member_roles(member_member_id,roles) VALUES(990,'ROLE_USER'),(990,'ROLE_ADMIN');")
    base, process = db.start_server(db.DATABASE, 'mysql-catalog')
    token = db.request(base, '/api/member/login', 'POST', form={'username': 'catalog-admin', 'password': 'password'})['jwt']
    db.request(base, '/api/admin/me', token=token)
    form = {'company': 'DEMP', 'type': 'EMP', 'position': 'SRE', 'title': 'catalog-all-technologies',
            'startedDate': '2026-10-01T09:00', 'deadLineDate': '2030-11-01T18:00',
            'content': '<p>공통 기술 항목 왕복 검증</p>', 'language': ','.join(TECHNOLOGIES),
            'accessUrl': 'https://example.test/catalog-all', 'publicationStatus': 'PUBLISHED'}
    db.request(base, '/api/admin/announcements', 'POST', form=form, token=token, expected=201)
    item_id = int(db.sql("SELECT id FROM announcement WHERE title='catalog-all-technologies';").stdout.strip())
    stored = db.sql(f'SELECT languages FROM language WHERE announcement_id={item_id} ORDER BY languages;').stdout.splitlines()
    db.check(set(stored) == set(TECHNOLOGIES) and len(stored) == 66, 'all 66 technologies committed independently in MySQL')
    db.check(set(detail(base, item_id, token)['language']) == set(TECHNOLOGIES), 'all technologies restored in HTTP detail')
    for tech in TECHNOLOGIES:
        items = listing(base, languages=tech, positions='SRE')['content']
        db.check([item['id'] for item in items] == [item_id], tech + ': stored technology searchable')
    role_ids = {}
    for role in POSITIONS:
        submitted = {**form, 'title': 'catalog-role-' + role, 'position': role, 'language': 'KOTLIN,React', 'accessUrl': 'https://example.test/catalog-role-' + role}
        db.request(base, '/api/admin/announcements', 'POST', form=submitted, token=token, expected=201)
        created_id = int(db.sql("SELECT id FROM announcement WHERE title='catalog-role-" + role + "';").stdout.strip())
        db.check(detail(base, created_id, token)['position'] == role, role + ': create/commit/detail contract')
        role_ids[created_id] = role
        found = listing(base, positions=role, languages='KOTLIN')['content']
        db.check(created_id in [row['id'] for row in found], role + ': role and technology search')
    listing_rows = listing(base)['content']
    db.check({row['id']: row['position'] for row in listing_rows if row['id'] in role_ids} == role_ids, 'all 31 created roles survive public list mapping')
    db.check(detail(base, item_id, token)['hits'] == 0, 'catalog/admin/list verification does not count visits')
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        counted = list(executor.map(lambda _: detail(base, item_id, token, record=True)['hits'], range(16)))
    db.check(sorted(counted) == list(range(1,17)), '16 concurrent MySQL views return exact totals 1..16')
    db.check(db.sql(f'SELECT hits FROM announcement WHERE id={item_id};').stdout.strip() == '16', 'all concurrent views committed in separate SQL read')
    db.request(base, f'/api/announce/detail/{item_id}', expected=401)
    db.request(base, '/api/announce/detail/9223372036854775807', token=token, expected=404)
    db.request(base, f'/api/announce/detail/{item_id}?recordView=invalid', token=token, expected=400)
    changed = {**form, 'title': 'catalog-edited', 'position': 'AI_RESEARCH', 'language': 'PYTHON,PYTORCH,RAG,React'}
    db.request(base, f'/api/admin/announcements/{item_id}', 'PATCH', form=changed, token=token)
    db.check(detail(base, item_id, token)['hits'] == 16, 'admin edit and rejected requests preserve views')
    edited = detail(base, item_id, token)
    db.check(edited['position'] == 'AI_RESEARCH' and set(edited['language']) == {'PYTHON','PYTORCH','RAG','React'}, 'edited catalog values restored exactly')
    filtered = listing(base, positions='AI_RESEARCH', languages='RAG,CPP')['content']
    db.check([row['id'] for row in filtered] == [item_id], 'technology OR within role AND filter returns exact result')
    db.request(base, f'/api/admin/announcements/{item_id}', 'PATCH', form={**changed, 'position': 'INVALID'}, token=token, expected=400)
    db.check(detail(base, item_id, token)['position'] == 'AI_RESEARCH', 'invalid role leaves committed selection intact')
    # Different deadlines and ties; legacy expired row must come last.
    ids = list(role_ids)
    db.sql('UPDATE announcement SET dead_line_date=NULL;')
    db.sql(f"UPDATE announcement SET dead_line_date='2030-10-10 18:00:00' WHERE id IN ({ids[0]},{ids[1]}); UPDATE announcement SET dead_line_date='2030-10-20 18:00:00' WHERE id={ids[2]}; UPDATE announcement SET dead_line_date='2000-01-01 00:00:00' WHERE id=900; UPDATE announcement SET recruitment_closed=1,dead_line_date='2030-10-09 18:00:00' WHERE id={ids[3]};")
    all_ids = sorted([900,item_id,*ids], reverse=True)
    expected_deadline = [ids[1],ids[0],ids[2],*[i for i in all_ids if i not in ids[:3]]]
    expected_views = [item_id,*[i for i in all_ids if i != item_id]]
    for order, expected in [('LATEST',all_ids), ('DEADLINE',expected_deadline), ('VIEWS',expected_views)]:
        rows = listing(base, orderBy=order)['content']
        db.check([row['id'] for row in rows] == expected, order + ': exact MySQL order and ID tie breaker')
        paged = []; page = 0
        while True:
            chunk = listing(base, orderBy=order, size=8, page=page)
            paged.extend(row['id'] for row in chunk['content'])
            if chunk['last']: break
            page += 1
            assert page < 10
        db.check(paged == expected, order + ': complete ordered pagination without duplicates or omissions')
    db.request(base, '/api/announce?orderBy=UNKNOWN', expected=400)
    db.stop_server(process)
    base, process = db.start_server(db.DATABASE, 'mysql-catalog-restart')
    restarted = detail(base, item_id, token)
    db.check(restarted['hits'] == 16 and restarted['position'] == 'AI_RESEARCH' and set(restarted['language']) == {'PYTHON','PYTORCH','RAG','React'}, 'views and expanded catalog survive JVM restart')
    db.stop_server(process)
    missing = db.DATABASE + '_missing'; migrate(missing, False)
    db.start_server(missing, 'mysql-missing-announcement-hits', expected_failure='missing column [hits]')


if __name__ == '__main__':
    status = 'failed'
    try:
        main(); status = 'passed'
    finally:
        for process, log in db.SERVERS:
            db.stop_server(process); log.close()
        if fixture.STARTED_CONTAINER:
            subprocess.run(['docker', 'stop', db.CONTAINER], check=True)
        result = {'status': status, 'checks': db.CHECKS, 'production_database_verified': False, 'database': db.DATABASE,
                  'jar_sha256': hashlib.sha256((ROOT / 'build/libs/demp-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest(),
                  'sql_sha256': hashlib.sha256((ROOT / 'src/main/resources/db/manual/announcement-view-count.sql').read_bytes()).hexdigest()}
        (fixture.EVIDENCE / 'mysql-result.json').write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n')
