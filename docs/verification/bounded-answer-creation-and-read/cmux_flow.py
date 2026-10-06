"""Visible cmux clicks against the current real Spring/H2 app, no network mocks."""
import importlib.util
import json
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[3]
OUT = Path('/private/tmp/demp-bounded-answers-20261006/live-final')
seed = json.loads((OUT / 'seed.json').read_text())
spec = importlib.util.spec_from_file_location('http_flow', ROOT / 'docs/verification/deployment-runtime-and-data-verification/mysql_rehearsal.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
steps = []
def browser(label, *arguments):
    result = subprocess.run(['cmux', 'browser', '--surface', 'surface:13', *arguments], text=True, capture_output=True, timeout=20)
    if result.returncode:
        raise AssertionError(label + ': ' + result.stderr + result.stdout)
    steps.append({'label': label, 'operation': arguments[0], 'exit_code': result.returncode})
    print('CMUX: ' + label, flush=True)
    return result.stdout.strip()
def observation(label, expression):
    raw = browser(label, 'eval', 'JSON.stringify(' + expression + ')')
    value = json.loads(raw)
    if isinstance(value, str):
        value = json.loads(value)
    return value
def poll_count(expected):
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        count = int(browser('실제 답변 개수 확인', 'get', 'count', '.question-answer'))
        if count == expected:
            return
        time.sleep(0.25)
    raise AssertionError(f'expected {expected} visible answers, last count {count}')
status = 'failed'
measurements = {}
try:
    browser('현재 답변 검증 질문으로 이동', 'goto', seed['browser_url'])
    poll_count(20)
    first = observation('첫 페이지 관찰', "({count:document.querySelectorAll('.question-answer').length,more:!!document.querySelector('[data-test=answer-load-more]')})")
    m.check(first == {'count': 20, 'more': True}, 'visible first page shows twenty and more')
    browser('작성 중 입력', 'fill', '#answer', '작성 유지 확인')
    browser('더 보기 실제 클릭', 'click', '[data-test=answer-load-more]')
    poll_count(26)
    more = observation('더 보기 후 입력과 개수 관찰', "({count:document.querySelectorAll('.question-answer').length,body:document.querySelector('#answer').value,more:!!document.querySelector('[data-test=answer-load-more]')})")
    m.check(more == {'count': 26, 'body': '작성 유지 확인', 'more': False}, 'visible more preserves input and ends at twenty-six')
    browser('페이지 화면 기록', 'screenshot', '--out', str(OUT / 'twenty-six.png'))
    # Observe the real serialized answer request; preserve the original transport.
    browser('실제 답변 요청 크기 관찰 준비', 'eval', "(()=>{const send=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.send=function(body){if(typeof body==='string'){try{const value=JSON.parse(body);if('answerContent' in value)window.__answerRequest={htmlCodePoints:[...value.answerContent].length,htmlUtf8Bytes:new TextEncoder().encode(value.answerContent).length,jsonUtf8Bytes:new TextEncoder().encode(body).length};}catch{}}return send.call(this,body);};return true;})()")
    raw_body = '합성 긴 본문 시작\n' + '한글🙂 **강조**\n' * 1200 + '합성 긴 본문 끝'
    browser('1만 자를 넘는 합성 Markdown 입력', 'fill', '#answer', raw_body)
    measurements['rawCodePoints'] = len(raw_body)
    measurements['rawUtf8Bytes'] = len(raw_body.encode())
    m.check(measurements['rawCodePoints'] > 10000, 'synthetic user input exceeds ten thousand code points')
    browser('단건 저장 실제 클릭', 'click', '.answer-composer button[type=submit]')
    poll_count(27)
    m.check(browser('성공 입력 초기화 관찰', 'get', 'value', '#answer') == '', 'visible save clears the submitted input')
    measurements['request'] = observation('원문과 다른 HTML·JSON 요청 크기 관찰', 'window.__answerRequest')
    m.check(isinstance(measurements['request'], dict), 'actual browser XHR serialized request was observed')
    token = m.request(seed['backend_url'], '/api/member/login', 'POST', form={'username': 'local-member', 'password': 'password'})['jwt']
    page = m.request(seed['backend_url'], f'/api/answer/{seed["question_id"]}', token=token)
    created = page['content'][0]
    created_id = created['answerId']
    m.check(created_id not in seed['seeded_ids'] and created['content'].count('<strong>강조</strong>') == 1200 and created['content'].count('🙂') == 1200 and '합성 긴 본문 끝' in created['content'], 'independent HTTP read proves long Markdown saved without truncation')
    measurements['sanitizedCodePoints'] = len(created['content'])
    measurements['sanitizedUtf8Bytes'] = len(created['content'].encode())
    browser('새 답변 비추천 실제 클릭', 'click', '.question-answer:first-of-type button[aria-label^="비추천"]')
    browser('비추천 확정 대기', 'wait', '--function', "document.querySelector('.question-answer button[aria-label^=비추천]').getAttribute('aria-pressed') === 'true'", '--timeout-ms', '10000')
    browser('실제 페이지 새로고침', 'reload')
    browser('첫 20개와 비추천 복원 대기', 'wait', '--function', "document.querySelectorAll('.question-answer').length === 20 && document.querySelector('.question-answer button[aria-label^=비추천]').getAttribute('aria-pressed') === 'true'", '--timeout-ms', '10000')
    final = m.request(seed['backend_url'], f'/api/answer/{seed["question_id"]}', token=token)
    m.check(final['content'][0]['answerId'] == created_id and final['content'][0]['myReaction'] == 'DISLIKE' and final['content'][0]['dislike'] == 1, 'fresh API query confirms the browser-created answer and reaction')
    second = m.request(seed['backend_url'], f'/api/answer/{seed["question_id"]}?before={final["nextCursor"]}', token=token)
    m.check(len(final['content']) == 20 and len(second['content']) == 7 and len({a['answerId'] for a in final['content'] + second['content']}) == 27, 'separate page reads retain all twenty-six original answers plus new answer')
    browser('최종 화면 기록', 'screenshot', '--out', str(OUT / 'final.png'))
    status = 'passed'
finally:
    (OUT / 'browser-result.json').write_text(json.dumps({'status': status, 'steps': steps, 'checks': m.CHECKS, 'measurements': measurements,
        'target': seed['browser_url'], 'database': 'isolated in-memory H2', 'network_mock': False}, ensure_ascii=False, indent=2) + '\n')
    print('VISIBLE CMUX FLOW ' + status, flush=True)
