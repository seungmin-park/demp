"""Current-JAR answer rehearsal in the existing owned loopback MySQL container.

Creates only a new synthetic database. Credentials are read from the existing
local rehearsal file and never included in the result. No production DB access.
"""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[3]
OUT = Path('/private/tmp/demp-bounded-answers-20261006/mysql')
OUT.mkdir(parents=True, exist_ok=True)
spec = importlib.util.spec_from_file_location('rehearsal', ROOT / 'docs/verification/deployment-runtime-and-data-verification/mysql_rehearsal.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
container_env = json.loads(subprocess.check_output(['docker', 'inspect', '--format', '{{json .Config.Env}}', m.CONTAINER], text=True))
credentials = dict(value.split('=', 1) for value in container_env if value.startswith('MYSQL_PASSWORD='))
assert credentials.get('MYSQL_PASSWORD'), 'owned container lacks rehearsal DB credential'
(OUT / 'mysql.env').write_text('MYSQL_PASSWORD=' + credentials['MYSQL_PASSWORD'] + '\n')
(OUT / 'mysql.env').chmod(0o600)
port = subprocess.check_output(['docker', 'port', m.CONTAINER, '3306/tcp'], text=True).strip()
(OUT / 'mysql-port.txt').write_text(port)
m.EVIDENCE = OUT
m.DATABASE = 'demp_release_' + time.strftime('%Y%m%d_%H%M%S')
m.DATABASES = {m.DATABASE}
status = 'failed'
measurements = {}
plans = {}
retained = None
hashes = {}

try:
    label = subprocess.check_output(['docker', 'inspect', '--format', '{{index .Config.Labels "demp.verification"}}', m.CONTAINER], text=True).strip()
    m.check(label == 'release-20261005' and port.startswith('127.0.0.1:'), 'owned local MySQL and loopback port')
    m.create_database(m.DATABASE)
    m.sql(m.legacy_fixture())
    migrations = ['announcement-body-images.sql', 'announcement-compensation.sql', 'announcement-education.sql',
                  'announcement-publication.sql', 'content-reaction.sql', 'member-login-protection.sql']
    hashes = {}
    for name in migrations:
        path = ROOT / 'src/main/resources/db/manual' / name
        hashes[name] = hashlib.sha256(path.read_bytes()).hexdigest()
        m.sql(path.read_text())
    base, process = m.start_server(m.DATABASE, 'answers-validate')
    member_id, token = m.member(base, 'answer-qa')
    m.sql(f"INSERT INTO question(question_id,content,title,hits,recommend,dislike,member_id) VALUES(900,NULL,'legacy answer question',0,0,0,{member_id});")
    m.sql(f"INSERT INTO answer(answer_id,content,recommend,dislike,member_id,question_id) VALUES(900,NULL,2,1,{member_id},900),(901,'기존 긴 본문',0,0,{member_id},900);")
    legacy = m.request(base, '/api/answer/900', token=token)
    m.check([int(a['answerId']) for a in legacy['content']] == [901, 900] and legacy['content'][1]['content'] is None, 'legacy IDs, counts and nullable body remain readable')
    m.check(m.sql('SELECT tag_name FROM hashtag WHERE hashtag_id=1000;').stdout.strip() == 'legacy-preserved', 'legacy unrelated data is preserved')
    def question(title):
        m.request(base, '/api/question/add', 'POST', {'title': title, 'content': '<p>answer rehearsal</p>', 'username': 'forged', 'hashtags': []}, token=token)
        return next(q['id'] for q in m.request(base, '/api/question?orderBy=createdDate&page=0&size=20')['content'] if q['title'] == title)
    question_id = question('cursor rehearsal')
    other_id = question('other question')
    started = time.monotonic()
    ids = []
    for i in range(41):
        created = m.request(base, '/api/answer/save', 'POST', {'username': 'forged', 'questionId': question_id, 'answerContent': f'<p>답변 {i}</p>'}, token=token)
        m.check(isinstance(created, dict) and isinstance(created['answerId'], str) and created['username'] == 'answer-qa' and created['myReaction'] == 'NONE', f'single authenticated answer {i + 1}')
        ids.append(int(created['answerId']))
    measurements['41_sequential_writes_seconds'] = time.monotonic() - started
    m.check(m.sql(f'SELECT COUNT(*) FROM answer WHERE question_id={question_id};').stdout.strip() == '41', '41 rapid successful writes commit; no new ten-write cutoff')
    all_ids = []
    cursor = None
    sizes = []
    while True:
        path = f'/api/answer/{question_id}' + ('' if cursor is None else '?before=' + cursor)
        page = m.request(base, path, token=token)
        sizes.append(len(page['content']))
        all_ids.extend(int(a['answerId']) for a in page['content'])
        if not page['hasNext']:
            m.check(page['nextCursor'] is None, 'last page has null cursor')
            break
        cursor = page['nextCursor']
        m.check(cursor == str(page['content'][-1]['answerId']), 'cursor equals last returned ID string')
    m.check(sizes == [20, 20, 1] and all_ids == list(reversed(ids)), 'MySQL pages 41 IDs exactly once in descending order')
    first = m.request(base, f'/api/answer/{question_id}', token=token)
    deleted_cursor = int(first['nextCursor'])
    m.sql(f'DELETE FROM answer WHERE answer_id={deleted_cursor};')
    next_page = m.request(base, f'/api/answer/{question_id}?before={deleted_cursor}', token=token)
    expected_after_cursor = sorted((answer_id for answer_id in ids if answer_id < deleted_cursor), reverse=True)[:20]
    m.check([int(a['answerId']) for a in next_page['content']] == expected_after_cursor, 'deleted cursor remains a numeric boundary')
    m.sql(f"INSERT INTO answer(answer_id,content,recommend,dislike,member_id,question_id) VALUES(-2,'음수 본문',0,0,{member_id},{other_id}),(-1,'음수 본문',0,0,{member_id},{other_id});")
    negative = m.request(base, f'/api/answer/{other_id}?before=-1', token=token)
    m.check([int(a['answerId']) for a in negative['content']] == [-2], 'negative cursor respects question scope')
    for exact_id in ['9007199254740992', '9007199254740993', '9223372036854775807']:
        m.sql(f"INSERT INTO answer(answer_id,content,recommend,dislike,member_id,question_id) VALUES({exact_id},'정밀도 본문',0,0,{member_id},{other_id});")
    exact = m.request(base, f'/api/answer/{other_id}', token=token)
    m.check([a['answerId'] for a in exact['content']] == ['9223372036854775807', '9007199254740993', '9007199254740992', '-1', '-2'], 'real MySQL HTTP preserves distinct Long IDs as exact strings')
    after_large = m.request(base, f'/api/answer/{other_id}?before=9007199254740993', token=token)
    m.check([a['answerId'] for a in after_large['content']] == ['9007199254740992', '-1', '-2'], 'large cursor reads the adjacent exact lower ID')
    m.request(base, '/api/answer/9007199254740992/reaction', 'PUT', {'reaction': 'DISLIKE'}, token=token)
    m.check(m.sql('SELECT reaction FROM content_reaction WHERE answer_id=9007199254740992;').stdout.strip() == 'DISLIKE', 'exact large answer reaction targets the intended MySQL row')
    for value in ['bad', '1.5', '9223372036854775808', '']:
        m.request(base, f'/api/answer/{question_id}?before={value}', token=token, expected=400)
    m.request(base, '/api/answer/999999', token=token, expected=404)
    m.request(base, f'/api/answer/{question_id}', expected=401)
    m.check(True, 'actual MySQL HTTP rejects malformed cursors, missing question and unauthenticated read')
    m.request(base, f'/api/answer/{ids[-1]}/reaction', 'PUT', {'reaction': 'DISLIKE'}, token=token)
    reacted = m.request(base, f'/api/answer/{question_id}', token=token)['content'][0]
    m.check(reacted['dislike'] == 1 and reacted['myReaction'] == 'DISLIKE', 'returned-page reaction is independently re-read after commit')
    m.check(m.sql(f'SELECT reaction FROM content_reaction WHERE answer_id={ids[-1]};').stdout.strip() == 'DISLIKE', 'reaction is persisted in independent SQL read')
    # Additional rows are synthetic query-plan fixtures, outside the API write timing.
    m.sql(f"INSERT INTO answer(answer_id,content,recommend,dislike,member_id,question_id) WITH RECURSIVE n AS (SELECT 1 AS v UNION ALL SELECT v+1 FROM n WHERE v<1000) SELECT 20000+v,'plan fixture',0,0,{member_id},{other_id} FROM n;")
    plans['indexes_before'] = m.sql('SHOW INDEX FROM answer;').stdout
    query = f'SELECT a.answer_id,m.username,a.content,a.recommend,a.dislike FROM answer a JOIN member m ON m.member_id=a.member_id WHERE a.question_id={question_id} ORDER BY a.answer_id DESC LIMIT 21'
    plans['explain_before'] = m.sql('EXPLAIN FORMAT=JSON ' + query).stdout
    plans['analyze_before'] = m.sql('EXPLAIN ANALYZE ' + query).stdout
    # Apply a reviewed manual index only when the actual legacy schema lacks one.
    has_question_index = m.sql("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='answer' AND seq_in_index=1 AND column_name='question_id';").stdout.strip() != '0'
    measurements['question_index_present'] = has_question_index
    if not has_question_index:
        print('OBSERVED: legacy answer table has no question-leading index; manual index review needed.', flush=True)
        index_path = ROOT / 'src/main/resources/db/manual/answer-cursor-index.sql'
        hashes[index_path.name] = hashlib.sha256(index_path.read_bytes()).hexdigest()
        m.sql(index_path.read_text())
        has_question_index = m.sql("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='answer' AND seq_in_index=1 AND column_name='question_id';").stdout.strip() != '0'
    else:
        m.check(True, 'existing question-leading index is available')
    m.check(has_question_index, 'legacy answer cursor query has a question-leading index')
    plans['indexes_after'] = m.sql('SHOW INDEX FROM answer;').stdout
    plans['explain_after'] = m.sql('EXPLAIN FORMAT=JSON ' + query).stdout
    plans['analyze_after'] = m.sql('EXPLAIN ANALYZE ' + query).stdout
    m.check('Table scan on a ' not in plans['analyze_after'] and 'idx_answer_question_cursor' in plans['analyze_after'], 'manual index removes the observed whole-answer-table scan')
    m.request(base, '/api/question/delete?questionId=' + str(question_id), 'DELETE', token=token)
    m.check(m.sql(f'SELECT COUNT(*) FROM answer WHERE question_id={question_id};').stdout.strip() == '0', 'question deletion cascades all remaining answers')
    m.check(m.sql(f'SELECT COUNT(*) FROM content_reaction WHERE answer_id={ids[-1]};').stdout.strip() == '0', 'question deletion removes answer reactions')
    m.check(m.sql('SELECT COUNT(*) FROM answer WHERE question_id=900;').stdout.strip() == '2', 'deletion preserves unrelated legacy answers')
    retained = process.pid
    status = 'passed'
finally:
    for process, log in m.SERVERS:
        if process.pid != retained:
            m.stop_server(process)
        log.close()
    result = {'status': status, 'checks': m.CHECKS, 'database': m.DATABASE, 'production_database_verified': False,
              'measurements': measurements, 'query_plans': plans, 'migrations': hashes,
              'jar_sha256': hashlib.sha256((ROOT / 'build/libs/demp-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest(),
              'retained_pid': retained}
    (OUT / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(f'MYSQL ANSWER REHEARSAL {status}; result: {OUT / "result.json"}', flush=True)
