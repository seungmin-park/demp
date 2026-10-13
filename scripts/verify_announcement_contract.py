"""Real HTTP contracts for announcement changes, called by the isolated JAR verifier.

No application services or HTTP responses are mocked. The caller owns the fresh H2
process and destroys it in finally; this flow also deletes its announcement fixtures.
"""
import base64
import copy
import hashlib
import hmac
import json
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


NULL = type(None)
DETAIL = {
    **dict.fromkeys(('recruitmentAudience', 'employmentType', 'cohort', 'stipendNote',
                     'sourceName', 'sourceIdentifier', 'applicationUrl', 'sourceVerifiedAt',
                     'startedDate', 'deadLineDate', 'position', 'workPeriod', 'conversionNote'), (str, NULL)),
    **dict.fromkeys(('stipendAmount', 'payment', 'salaryMax'), (int, NULL)),
    **dict.fromkeys(('image', 'publicationStatus', 'title', 'content', 'accessUrl',
                     'salaryStatus', 'salaryPeriod', 'announcementType'), (str,)),
    **dict.fromkeys(('hits', 'minCareer', 'maxCareer'), (int,)),
    'recruitmentClosed': (bool,), 'company': (dict,), 'education': (dict, NULL),
    'roles': (list,), 'referenceUrls': (list,), 'language': (list,),
}
SUMMARY = {key: DETAIL[key] for key in (
    'recruitmentAudience', 'employmentType', 'title', 'hits', 'language', 'position',
    'recruitmentClosed', 'image', 'publicationStatus', 'education', 'announcementType',
    'minCareer', 'maxCareer', 'payment', 'salaryPeriod', 'startedDate', 'deadLineDate')}
SUMMARY.update(id=(int,), company=(str, NULL))
ROLE = dict.fromkeys(('title', 'position', 'location', 'qualifications', 'requiredSkills',
                     'preferredSkills', 'usedTechnologies'), (str, NULL))
ROLE['title'] = (str,)
ERROR_MESSAGES = {400: 'Invalid request', 401: 'Authentication required',
                  403: 'Access denied', 404: 'Resource not found', 409: 'Resource already exists'}


def shape(value, contract, label):
    assert type(value) is dict, f'{label}: expected object'
    assert set(value) == set(contract), f'{label}: keys differ: {set(value) ^ set(contract)}'
    for key, types in contract.items():
        assert type(value[key]) in types, f'{label}.{key}: unexpected {type(value[key]).__name__}'


def detail_contract(value):
    shape(value, DETAIL, 'detail')
    shape(value['company'], {'name': (str,)}, 'company')
    for role in value['roles']:
        shape(role, ROLE, 'role')
    for key in ('language', 'referenceUrls'):
        assert all(type(item) is str for item in value[key]), key
    # This flow creates employment records. Education contracts have their own suites.
    assert value['education'] is None


def prove_contract_guard(valid_response):
    """Reject representative consumer-breaking drift using the same live-response validator."""
    mutations = []
    missing = copy.deepcopy(valid_response)
    del missing['salaryPeriod']
    mutations.append(('removed salaryPeriod', missing))
    nullable = copy.deepcopy(valid_response)
    nullable['roles'] = None
    mutations.append(('roles unexpectedly nullable', nullable))
    nested = copy.deepcopy(valid_response)
    del nested['roles'][0]['requiredSkills']
    mutations.append(('removed role requiredSkills', nested))
    for label, response in mutations:
        try:
            detail_contract(response)
        except AssertionError:
            print('announcement contract guard rejected: ' + label, flush=True)
        else:
            raise AssertionError('Contract validator accepted drift: ' + label)
    return [label for label, _ in mutations]


class ContractClient:
    def __init__(self, base):
        assert urllib.parse.urlparse(base).hostname in ('127.0.0.1', 'localhost')
        self.base = base
        self.observations = []

    def call(self, path, status=200, method='GET', form=None, token=None, kind='json'):
        headers, body = {}, None
        if form is not None:
            boundary = 'demp-contract-' + uuid.uuid4().hex
            body = ''.join(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'
                           for key, value in form.items()).encode() + f'--{boundary}--\r\n'.encode()
            headers['Content-Type'] = 'multipart/form-data; boundary=' + boundary
        if token:
            headers['X-AUTH-TOKEN'] = token
        req = urllib.request.Request(self.base + path, body, headers, method=method)
        started = time.monotonic()
        try:
            response = urllib.request.urlopen(req, timeout=10)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            raw = response.read()
            elapsed = round((time.monotonic() - started) * 1000, 2)
            assert response.status == status, f'{method} {path}: expected {status}, got {response.status}: {raw[:300]!r}'
            # Spring Security sets these for all relevant responses, including errors.
            assert 'no-store' in response.headers.get('Cache-Control', ''), f'{path}: cache contract'
            assert response.headers.get('X-Content-Type-Options') == 'nosniff', f'{path}: content sniffing'
            if kind == 'json':
                assert 'application/json' in response.headers.get('Content-Type', ''), f'{path}: JSON content type'
                result = json.loads(raw)
            elif kind == 'empty':
                assert raw == b'', f'{path}: expected documented empty body'
                result = None
            else:
                assert 'text/plain' in response.headers.get('Content-Type', ''), f'{path}: text content type'
                result = raw.decode()
        self.observations.append({'method': method, 'path': path, 'status': status,
                                  'milliseconds': elapsed, 'bytes': len(raw)})
        print(f'announcement contract: {method} {path}: {status}', flush=True)
        if status >= 400 and kind == 'json':
            assert result == {'errorCode': status, 'errorMessage': ERROR_MESSAGES[status],
                              'instance': path.split('?')[0]}, f'{path}: error body contract'
        return result

    def page(self, path, token=None):
        page = self.call(path, token=token)
        for field, expected in {'content': list, 'number': int, 'size': int,
                                'numberOfElements': int, 'first': bool, 'last': bool, 'empty': bool}.items():
            assert field in page and type(page[field]) is expected, f'page.{field}'
        assert page['numberOfElements'] == len(page['content'])
        assert page['empty'] == (not page['content'])
        for item in page['content']:
            shape(item, SUMMARY, 'summary')
            assert all(type(tag) is str for tag in item['language'])
        return page


def expired_token(session_token, signing_secret):
    def encode(value):
        return base64.urlsafe_b64encode(json.dumps(value, separators=(',', ':')).encode()).rstrip(b'=')
    claims = json.loads(base64.urlsafe_b64decode(session_token.split('.')[1] + '==='))
    claims.update(iat=1, exp=2)
    content = encode({'alg': 'HS256'}) + b'.' + encode(claims)
    signature = base64.urlsafe_b64encode(hmac.new(signing_secret.encode(), content, hashlib.sha256).digest()).rstrip(b'=')
    return (content + b'.' + signature).decode()


def run(base, signing_secret):
    client = ContractClient(base)
    session = client.call('/api/member/login', method='POST', form={'username': 'local-member', 'password': 'password'})
    shape(session, {'username': (str,), 'jwt': (str,)}, 'login')
    assert session['username'] == 'local-member' and session['jwt']
    admin = session['jwt']
    client.call('/api/member/login', 401, 'POST', {'username': 'local-member', 'password': 'incorrect'})
    username = 'contract-' + uuid.uuid4().hex[:12]
    client.call('/api/member/save', method='POST', form={'username': username, 'password': 'local-test-password'})
    member = client.call('/api/member/login', method='POST', form={'username': username, 'password': 'local-test-password'})['jwt']
    root = '/api/admin/announcements'
    client.call(root, 401)
    client.call(root, 401, token=expired_token(admin, signing_secret))
    client.call(root, 403, token=member)
    client.call(root + '?page=-1', 400, token=admin)
    client.call('/api/announce?recruitmentStatus=INVALID', 400)
    suffix = uuid.uuid4().hex
    draft = {'title': 'contract-' + suffix, 'company': '계약 검증 회사', 'type': 'EMP',
             'content': '미확인 일정과 기술은 초안에 보존합니다.', 'publicationStatus': 'DRAFT',
             'accessUrl': 'https://example.test/jobs/' + suffix, 'sourceIdentifier': suffix}
    ids = []
    query = '?title=' + urllib.parse.quote(draft['title'])
    try:
        for token, status in ((None, 401), (member, 403)):
            client.call(root, status, 'POST', draft, token)
        client.call(root, 400, 'POST', dict(draft, title=''), admin)
        created = client.call(root, 201, 'POST', draft, admin)
        assert created == {'cleanupPending': False}
        page = client.page(root + query, admin)
        ids.extend(item['id'] for item in page['content'])
        assert len(ids) == 1 and page['last'] and page['number'] == 0
        path = root + '/' + str(ids[0])
        public = '/api/announce/detail/' + str(ids[0]) + '?recordView=false'
        detail = client.call(path, token=admin)
        detail_contract(detail)
        assert detail['position'] is None and detail['language'] == []
        assert detail['startedDate'] is None and detail['deadLineDate'] is None
        assert detail['roles'] == [] and detail['referenceUrls'] == []
        client.call(public, 404, token=member, kind='empty')
        assert client.page('/api/announce' + query)['content'] == []
        client.call(root, 409, 'POST', dict(draft, accessUrl='https://mirror.test/' + suffix), admin)
        client.call(path, 400, 'PATCH', dict(draft, publicationStatus='PUBLISHED'), admin)
        client.call(path, 400, 'PATCH', dict(draft, **{'referenceUrls[0]': 'javascript:alert(1)'}), admin)
        client.call(path, 400, 'PATCH', dict(draft, **{'roles[0].title': ''}), admin)
        assert client.call(path, token=admin) == detail, 'Rejected changes must preserve complete prior response'
        for method in ('GET', 'PATCH', 'DELETE'):
            client.call(path, 401, method, draft if method == 'PATCH' else None)
            client.call(path, 403, method, draft if method == 'PATCH' else None, member)
        published = dict(draft, publicationStatus='PUBLISHED', position='BACKEND', language='JAVA',
                         startedDate='2026-01-01T00:00:01', deadLineDate='2037-12-31T16:59:59',
                         salaryPeriod='MONTH', salaryStatus='DISCLOSED', payment='230',
                         workPeriod='약 5개월', conversionNote='정규직 전환 불가')
        published.update({'roles[0].title': 'AI-OCR', 'roles[0].position': 'AI',
                          'roles[0].requiredSkills': 'Python 또는 PyTorch',
                          'roles[0].preferredSkills': '문서 인식 경험', 'roles[0].usedTechnologies': '팀 기술',
                          'referenceUrls[0]': 'https://example.test/description.pdf'})
        assert client.call(path, method='PATCH', form=published, token=admin) == {'cleanupPending': False}
        updated = client.call(public, token=member)
        detail_contract(updated)
        assert updated['salaryPeriod'] == 'MONTH' and updated['payment'] == 230
        assert updated['workPeriod'] == '약 5개월' and updated['conversionNote'] == '정규직 전환 불가'
        assert updated['deadLineDate'] == '2037-12-31T16:59:59'
        assert updated['roles'] == [{'title': 'AI-OCR', 'position': 'AI', 'location': None,
            'qualifications': None, 'requiredSkills': 'Python 또는 PyTorch',
            'preferredSkills': '문서 인식 경험', 'usedTechnologies': '팀 기술'}]
        assert updated['referenceUrls'] == ['https://example.test/description.pdf']
        rejected_mutations = prove_contract_guard(updated)
        assert client.call(path, token=admin) == updated
        assert [item['id'] for item in client.page('/api/announce' + query + '&positions=AI')['content']] == ids
        assert client.page('/api/announce' + query + '&payment=200')['content'] == [], 'Monthly amount is not annual salary'
        assert client.page('/api/announce' + query + '&page=99')['content'] == []
        assert client.page(root + query + '&page=99', admin)['content'] == []
        assert client.call(path, method='PATCH', form=published, token=admin) == {'cleanupPending': False}
        assert client.call(public, token=member) == updated, 'Repeated PATCH must preserve intended state'
        for closed, state in (('true', 'CLOSED'), ('false', 'OPEN')):
            client.call(path, method='PATCH', form=dict(published, recruitmentClosed=closed), token=admin)
            assert [item['id'] for item in client.page('/api/announce' + query + '&recruitmentStatus=' + state)['content']] == ids
        assert client.call(path, method='DELETE', token=admin) == {'cleanupPending': False}
        client.call(path, 404, token=admin)
        client.call(path, 404, 'PATCH', published, admin)
        client.call(path, 404, 'DELETE', token=admin)
        client.call(public, 404, token=member, kind='empty')
        assert client.page('/api/announce' + query)['content'] == []
        ids.clear()
        # Legacy write route shares the changed input contract and must retain its text response.
        legacy = dict(draft, sourceIdentifier=suffix + '-legacy', accessUrl=draft['accessUrl'] + '-legacy')
        client.call('/api/announce/add', 403, 'POST', legacy, member)
        client.call('/api/announce/add', 400, 'POST', dict(legacy, title=''), admin)
        assert client.call('/api/announce/add', method='POST', form=legacy, token=admin, kind='text') == 'ok'
        ids.extend(item['id'] for item in client.page(root + query, admin)['content'])
        assert len(ids) == 1
        detail_contract(client.call(root + '/' + str(ids[0]), token=admin))
    finally:
        # Query again covers assertion failures after a create response but before ID capture.
        remaining = client.page(root + query, admin)['content']
        for item in remaining:
            client.call(root + '/' + str(item['id']), method='DELETE', token=admin)
        assert client.page(root + query, admin)['content'] == []
    return {'status': 'passed', 'requests': len(client.observations),
            'rejectedContractMutations': rejected_mutations,
            'observations': client.observations, 'fixtureAnnouncementsRemaining': 0,
            'authentication': 'real login, invalid credentials, correctly signed expired JWT, user/admin boundaries',
            'isolation': 'real Spring/H2; no external calls; account removed with owned in-memory DB',
            'timing': 'observations only; no unapproved latency budget asserted'}
