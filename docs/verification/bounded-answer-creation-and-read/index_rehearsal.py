"""Compare answer cursor indexes using the current JAR and a new synthetic DB.

Uses only the existing owned loopback rehearsal container. No production access,
global measurement changes, index hints, or application query changes.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import shutil
import statistics
import subprocess
import time

ROOT = Path(__file__).resolve().parents[3]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--interleaved', action='store_true', help='Mix target question answers throughout the global ID order')
args = parser.parse_args()
OUT = Path('/private/tmp/demp-bounded-answers-20261006/index-rehearsal') / time.strftime('%Y%m%d-%H%M%S')
OUT.mkdir(parents=True)
spec = importlib.util.spec_from_file_location('rehearsal', ROOT / 'docs/verification/deployment-runtime-and-data-verification/mysql_rehearsal.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def unescape_batch(value):
    # mysql --batch escapes transport newlines and backslashes, preserving UTF-8.
    escapes = {'0': '\0', 'b': '\b', 't': '\t', 'n': '\n', 'r': '\r', 'Z': '\x1a', '\\': '\\'}
    return re.sub(r'\\([0btnrZ\\])', lambda match: escapes[match[1]], value)

def sql_json(statement):
    return [json.loads(unescape_batch(line)) for line in m.sql(statement).stdout.splitlines()]

def history():
    rows = sql_json("SELECT JSON_OBJECT('thread',THREAD_ID,'event',EVENT_ID,'sql',SQL_TEXT,"
                    "'rows_examined',ROWS_EXAMINED,'rows_sent',ROWS_SENT,'sort_rows',SORT_ROWS,"
                    "'tmp_tables',CREATED_TMP_TABLES,'sql_ms',TIMER_WAIT/1000000000) "
                    "FROM performance_schema.events_statements_history "
                    f"WHERE CURRENT_SCHEMA='{m.DATABASE}' AND LOWER(SQL_TEXT) LIKE 'select %' "
                    "AND LOWER(SQL_TEXT) LIKE '%from answer %' AND LOWER(SQL_TEXT) LIKE '%order by%' "
                    "AND LOWER(SQL_TEXT) LIKE '%limit%';")
    return {(row['thread'], row['event']): row for row in rows}

def observe_page(base, token, question_id, cursor):
    previous = history()
    started = time.monotonic()
    page = m.request(base, f'/api/answer/{question_id}' + ('' if cursor is None else '?before=' + str(cursor)), token=token)
    http_ms = (time.monotonic() - started) * 1000
    added = [row for key, row in history().items() if key not in previous]
    if len(added) != 1:
        raise AssertionError(f'Expected one actual answer projection, observed {len(added)}')
    return page, {**added[0], 'http_ms': http_ms}

def validate_page(page, question_id, cursor):
    ids = fixture_ids[question_id]
    expected = [str(value) for value in reversed(ids) if cursor is None or value < cursor]
    assert [answer['answerId'] for answer in page['content']] == expected[:20]
    assert page['hasNext'] == (len(expected) > 20)
    assert page['nextCursor'] == (expected[19] if len(expected) > 20 else None)
    assert all(answer['myReaction'] == 'NONE' for answer in page['content'])

fixture_ids = {3000: list(range(1000001, 1000042)), 3001: list(range(1000042, 1020042)), 5000: []}
if args.interleaved:
    fixture_ids = {
        3000: [1000000 + number for number in range(1, 200001) if number % 4877 == 0],
        3001: [1000000 + number for number in range(1, 200001) if number % 4877 != 0 and number % 10 == 0],
        5000: [],
    }
scenarios = [('sparse-first', 3000, None), ('sparse-next', 3000, 1000022), ('sparse-last', 3000, 1000006),
             ('hot-first', 3001, None), ('hot-middle', 3001, 1010042), ('hot-near-end', 3001, 1000063),
             ('hot-last', 3001, 1000047), ('empty', 5000, None)]
scenarios = [(name, question_id, None if cursor is None else (
    fixture_ids[question_id][-20] if name == 'sparse-next' else
    fixture_ids[question_id][len(fixture_ids[question_id]) // 2] if name == 'hot-middle' else
    fixture_ids[question_id][21] if name == 'hot-near-end' else fixture_ids[question_id][5]))
    for name, question_id, cursor in scenarios]
report = {'status': 'failed', 'production_database_verified': False, 'checks': m.CHECKS, 'candidates': [],
          'distribution': 'interleaved' if args.interleaved else 'clustered',
          'samples': {'warmup_per_scenario': 2, 'measured_per_scenario': 5, 'cache': 'warm; no global cache flush'},
          'jar_sha256': hashlib.sha256((ROOT / 'build/libs/demp-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest(),
          'script_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}

try:
    label = subprocess.check_output(['docker', 'inspect', '--format', '{{index .Config.Labels "demp.verification"}}', m.CONTAINER], text=True).strip()
    port = subprocess.check_output(['docker', 'port', m.CONTAINER, '3306/tcp'], text=True).strip()
    m.check(label == 'release-20261005' and re.fullmatch(r'127\.0\.0\.1:\d+', port), 'owned MySQL container and loopback scope')
    shutil.copyfile('/private/tmp/demp-bounded-answers-20261006/mysql/mysql.env', OUT / 'mysql.env')
    (OUT / 'mysql.env').chmod(0o600)
    (OUT / 'mysql-port.txt').write_text(port)
    m.EVIDENCE = OUT
    m.DATABASE = 'demp_release_' + time.strftime('%Y%m%d_%H%M%S')
    m.DATABASES = {m.DATABASE}
    report['database'] = m.DATABASE
    m.create_database(m.DATABASE)
    m.sql(m.legacy_fixture())
    for name in ['announcement-body-images.sql', 'announcement-compensation.sql', 'announcement-education.sql',
                 'announcement-publication.sql', 'content-reaction.sql', 'member-login-protection.sql']:
        m.sql((ROOT / 'src/main/resources/db/manual' / name).read_text())
    report['mysql'] = sql_json("SELECT JSON_OBJECT('version',VERSION(),'engine',@@default_storage_engine,"
                               "'optimizer_switch',@@optimizer_switch,'page_bytes',@@innodb_page_size);")[0]
    print('MySQL ' + report['mysql']['version'] + ' / ' + report['mysql']['engine'], flush=True)
    m.check(m.sql("SELECT ENABLED FROM performance_schema.setup_consumers WHERE NAME='events_statements_history';").stdout.strip() == 'YES', 'existing statement history enabled; global configuration untouched')
    base, process = m.start_server(m.DATABASE, 'index-api')
    actor_id, token = m.member(base, 'index-qa')
    digits = 'SELECT 0 AS d ' + ' '.join(f'UNION ALL SELECT {number}' for number in range(1, 10))
    m.sql('CREATE TABLE rehearsal_numbers(n INT PRIMARY KEY); '
          f'INSERT INTO rehearsal_numbers WITH digits AS ({digits}) '
          'SELECT 1+a.d+10*b.d+100*c.d+1000*d.d+10000*e.d+100000*f.d '
          'FROM digits a CROSS JOIN digits b CROSS JOIN digits c CROSS JOIN digits d CROSS JOIN digits e CROSS JOIN digits f '
          'WHERE 1+a.d+10*b.d+100*c.d+1000*d.d+10000*e.d+100000*f.d<=200000;')
    m.sql("INSERT INTO member(member_id,username) SELECT 4999+n,CONCAT('synthetic-author-',n) FROM rehearsal_numbers WHERE n<=1000;")
    m.sql(f"INSERT INTO question(question_id,title,content,hits,recommend,dislike,member_id) "
          f"SELECT 2999+n,CONCAT('synthetic-question-',n),'검증',0,0,0,{actor_id} FROM rehearsal_numbers WHERE n<=1001; "
          f"INSERT INTO question(question_id,title,content,hits,recommend,dislike,member_id) VALUES(5000,'empty','',0,0,0,{actor_id});")
    distribution_sql = ("CASE WHEN MOD(n,4877)=0 THEN 3000 WHEN MOD(n,10)=0 THEN 3001 ELSE 3002+MOD(n,999) END"
                        if args.interleaved else "CASE WHEN n<=41 THEN 3000 WHEN n<=20041 THEN 3001 ELSE 3002+MOD(n,999) END")
    m.sql("INSERT INTO answer(answer_id,content,recommend,dislike,member_id,question_id) "
          "SELECT 1000000+n,CONCAT('합성 본문 ',n,REPEAT('한글',32)),0,0,5000+MOD(n,1000),"
          + distribution_sql + " FROM rehearsal_numbers;")
    report['fixture'] = {'answers': 200000, 'authors': 1000, 'questions': 1002,
                         'sparse_answers': len(fixture_ids[3000]), 'hot_answers': len(fixture_ids[3001])}
    m.check(m.sql('SELECT COUNT(*),SUM(answer_id) FROM answer;').stdout.strip() == '200000\t220000100000', 'all synthetic IDs seeded; 200000 answers')
    baseline_checksum = m.sql('CHECKSUM TABLE answer;').stdout
    changes = [
        ('none', None),
        ('question-only', 'CREATE INDEX idx_answer_question ON answer(question_id);'),
        ('explicit-composite', 'DROP INDEX idx_answer_question ON answer; ' + (ROOT / 'src/main/resources/db/manual/answer-cursor-index.sql').read_text()),
        ('foreign-key-auto', 'DROP INDEX idx_answer_question_cursor ON answer; ALTER TABLE answer ADD CONSTRAINT fk_rehearsal_answer_question FOREIGN KEY(question_id) REFERENCES question(question_id);'),
    ]
    for name, ddl in changes:
        print('\nCANDIDATE: ' + name, flush=True)
        if ddl:
            m.sql(ddl)
        m.sql('ANALYZE TABLE answer,member,question;')
        candidate = {'name': name, 'indexes': m.sql('SHOW INDEX FROM answer;').stdout,
                     'create_table': m.sql('SHOW CREATE TABLE answer;').stdout, 'scenarios': []}
        report['candidates'].append(candidate)
        for scenario, question_id, cursor in scenarios:
            path = f'/api/answer/{question_id}' + ('' if cursor is None else '?before=' + str(cursor))
            for _ in range(2):
                validate_page(m.request(base, path, token=token), question_id, cursor)
            samples = []
            for _ in range(5):
                page, sample = observe_page(base, token, question_id, cursor)
                validate_page(page, question_id, cursor)
                assert sample['rows_sent'] == min(21, len([value for value in fixture_ids[question_id] if cursor is None or value < cursor]))
                samples.append(sample)
            actual_sql = samples[-1]['sql']
            assert '?' not in actual_sql and re.search(r'limit\s+21\s*$', actual_sql, re.I), actual_sql
            plan_json = json.loads(unescape_batch(m.sql('EXPLAIN FORMAT=JSON ' + actual_sql).stdout))
            plan_tree = unescape_batch(m.sql('EXPLAIN ANALYZE ' + actual_sql).stdout)
            result = {'name': scenario, 'question_id': question_id, 'cursor': cursor, 'actual_sql': actual_sql,
                      'samples': samples, 'explain_json': plan_json, 'explain_analyze': plan_tree,
                      'rows_examined': samples[-1]['rows_examined'], 'sort_rows': samples[-1]['sort_rows'],
                      'sql_ms_median': statistics.median(float(sample['sql_ms']) for sample in samples),
                      'sql_ms_range': [min(float(sample['sql_ms']) for sample in samples), max(float(sample['sql_ms']) for sample in samples)],
                      'http_ms_median': statistics.median(sample['http_ms'] for sample in samples)}
            candidate['scenarios'].append(result)
            print(f"{scenario:16} examined={result['rows_examined']:7} sort={result['sort_rows']:5} SQL median={result['sql_ms_median']:.3f}ms", flush=True)
            m.check(True, name + '/' + scenario + ': exact HTTP page and captured current-JAR SQL; 5 measurements')
        candidate['index_pages'] = sql_json("SELECT JSON_OBJECT('index',index_name,'pages',stat_value) FROM mysql.innodb_index_stats "
                                           f"WHERE database_name='{m.DATABASE}' AND table_name='answer' AND stat_name='size';")
        m.check(m.sql('CHECKSUM TABLE answer;').stdout == baseline_checksum, name + ': index changes preserve all answer data')
    report['status'] = 'passed'
finally:
    for owned_process, log in m.SERVERS:
        m.stop_server(owned_process)
        log.close()
    (OUT / 'result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    latest = OUT.parent / 'latest.json'
    latest.write_text(json.dumps({'result': str(OUT / 'result.json'), 'status': report['status']}, indent=2) + '\n')
    print('INDEX REHEARSAL ' + report['status'] + '; ' + str(OUT / 'result.json'), flush=True)
