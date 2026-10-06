"""Seed and verify the current isolated H2 app before visible browser actions."""
import importlib.util
import json
from pathlib import Path
import time

ROOT = Path(__file__).resolve().parents[3]
OUT = Path('/private/tmp/demp-bounded-answers-20261006/live-final')
spec = importlib.util.spec_from_file_location('http_flow', ROOT / 'docs/verification/deployment-runtime-and-data-verification/mysql_rehearsal.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
ready = json.loads((OUT / 'ready.json').read_text())
base = ready['backend_url']
token = m.request(base, '/api/member/login', 'POST', form={'username': 'local-member', 'password': 'password'})['jwt']
title = '답변 페이지 검증 ' + time.strftime('%H:%M:%S')
m.request(base, '/api/question/add', 'POST', {'title': title, 'content': '<p>실제 Spring/H2 답변 페이지 검증</p>', 'username': 'forged', 'hashtags': []}, token=token)
question_id = next(q['id'] for q in m.request(base, '/api/question?orderBy=createdDate&page=0&size=20')['content'] if q['title'] == title)
ids = []
started = time.monotonic()
for i in range(26):
    created = m.request(base, '/api/answer/save', 'POST', {'username': 'forged', 'questionId': question_id, 'answerContent': f'<p>기존 검증 답변 {i + 1}</p>'}, token=token)
    m.check(isinstance(created, dict) and isinstance(created['answerId'], str) and created['username'] == 'local-member', f'actual H2 single-object write {i + 1}')
    ids.append(created['answerId'])
first = m.request(base, f'/api/answer/{question_id}', token=token)
second = m.request(base, f'/api/answer/{question_id}?before={first["nextCursor"]}', token=token)
m.check(len(first['content']) == 20 and first['hasNext'], 'actual first page returns twenty and cursor')
m.check(len(second['content']) == 6 and second['nextCursor'] is None and not second['hasNext'], 'actual final page returns remaining six')
m.check([a['answerId'] for a in first['content'] + second['content']] == list(reversed(ids)), 'independent HTTP page reads preserve all seeded IDs')
result = {**ready, 'question_id': question_id, 'seeded_ids': ids, '26_write_seconds': time.monotonic() - started,
          'checks': m.CHECKS, 'browser_url': ready['frontend_url'] + '/questions/' + str(question_id)}
(OUT / 'seed.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
print('Visible browser target: ' + result['browser_url'], flush=True)
