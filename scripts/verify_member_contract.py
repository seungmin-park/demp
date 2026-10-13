"""Account contracts against the verifier's isolated real Spring application."""
import json
import time
import urllib.request
import urllib.error
import urllib.parse
import uuid


def run(base):
    assert urllib.parse.urlparse(base).hostname in ('127.0.0.1', 'localhost')
    observations = []

    def call(path, expected=200, method='GET', body=None, token=None, form=None):
        headers = {}
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers['Content-Type'] = 'application/json'
        if form is not None:
            data = urllib.parse.urlencode(form).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded'
        if token:
            headers['X-AUTH-TOKEN'] = token
        started = time.monotonic()
        try:
            response = urllib.request.urlopen(urllib.request.Request(base + path, data, headers, method=method), timeout=10)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            content = response.read()
            assert response.status == expected, (path, expected, response.status)
            assert 'no-store' in response.headers['Cache-Control']
            assert response.headers['X-Content-Type-Options'] == 'nosniff'
            if expected != 204:
                assert 'application/json' in response.headers['Content-Type']
                value = json.loads(content)
            else:
                assert content == b''
                value = None
            if expected >= 400:
                assert set(value) == {'errorCode', 'errorMessage', 'instance'}
                assert value['errorCode'] == expected
            observations.append({'path': path, 'method': method, 'status': response.status,
                                 'milliseconds': round((time.monotonic() - started) * 1000, 2)})
            return value

    me = '/api/member/me'
    call(me, 401)
    call(me, 401, 'PATCH', {'nickname': 'forbidden'})
    call(me + '/password', 401, 'PUT', {'currentPassword': 'password', 'newPassword': 'new-password'})
    username = 'account-' + uuid.uuid4().hex
    call('/api/member/save', method='POST', form={'username': username, 'password': 'password'})
    session = call('/api/member/login', method='POST', form={'username': username, 'password': 'password'})
    token = session['jwt']
    before = call(me, token=token)
    assert set(before) == {'id', 'username', 'nickname', 'admin'}
    assert type(before['id']) is int and before['username'] == username and before['nickname'] is None and before['admin'] is False
    updated = call(me, method='PATCH', body={'nickname': '  새로운 이름  ', 'id': -1, 'roles': ['ROLE_ADMIN'], 'username': 'stolen'}, token=token)
    assert updated == dict(before, nickname='새로운 이름')
    assert call(me, token=token) == updated
    call('/api/admin/me', 403, token=token)
    for nickname in ['', ' ', '가' * 31]:
        call(me, 400, 'PATCH', {'nickname': nickname}, token)
        assert call(me, token=token) == updated
    call(me + '/password', 400, 'PUT', {'currentPassword': 'wrong', 'newPassword': 'new-password'}, token)
    for password in ['short', '가' * 25]:
        call(me + '/password', 400, 'PUT', {'currentPassword': 'password', 'newPassword': password}, token)
    call(me + '/password', 204, 'PUT', {'currentPassword': 'password', 'newPassword': 'new-password'}, token)
    call(me, 401, token=token)
    call('/api/member/login', 401, 'POST', form={'username': username, 'password': 'password'})
    new_session = call('/api/member/login', method='POST', form={'username': username, 'password': 'new-password'})
    assert call(me, token=new_session['jwt']) == updated
    return {'status': 'passed', 'requests': len(observations), 'observations': observations}


if __name__ == '__main__':
    import sys
    print(json.dumps(run(sys.argv[1]), ensure_ascii=False, indent=2))
