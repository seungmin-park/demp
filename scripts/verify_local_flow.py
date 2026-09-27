"""Exercise the real local Spring API against an isolated in-memory H2 server.

Start the backend with the command in README, then run this script. It accepts
only loopback URLs so it cannot create fixtures in a remote environment.
"""

import json
import os
import urllib.error
import urllib.request
import uuid
from urllib.parse import urlparse


BASE_URL = os.environ.get("DEMP_LOCAL_FLOW_BASE_URL", "http://127.0.0.1:18080")
if urlparse(BASE_URL).hostname not in ("localhost", "127.0.0.1"):
    raise SystemExit("Only a loopback Spring server is allowed")


def request(path, method="GET", payload=None, form=None, token=None):
    headers = {}
    body = None
    if form is not None:
        boundary = "----demp-local-flow"
        body = "".join(
            f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'
            for key, value in form.items()
        ).encode() + f"--{boundary}--\r\n".encode()
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    elif payload is not None:
        body = json.dumps(payload).encode()
        headers["Content-Type"] = "application/json"
    if token:
        headers["X-AUTH-TOKEN"] = token
    operation = urllib.request.Request(BASE_URL + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(operation, timeout=10) as response:
            raw = response.read().decode()
            try:
                return response.status, json.loads(raw)
            except json.JSONDecodeError:
                return response.status, raw
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode()


def require_status(label, result, expected):
    status, body = result
    if status != expected:
        raise AssertionError(f"{label}: expected {expected}, received {status}: {body}")
    print(f"{label}: {status}")
    return body


def create_member():
    name = f"t50-{uuid.uuid4().hex[:8]}"
    require_status("register", request("/api/member/save", "POST", form={"username": name, "password": "secret"}), 200)
    session = require_status("login", request("/api/member/login", "POST", form={"username": name, "password": "secret"}), 200)
    assert session["username"] == name
    return name, session["jwt"]


announcements = require_status("announcement list", request("/api/announce?page=0&size=8"), 200)
assert any(item["id"] == -1 for item in announcements["content"])
writer, writer_token = create_member()
announcement = require_status("announcement detail", request("/api/announce/detail/-1", token=writer_token), 200)
assert announcement["title"] == "로컬 개발자 모집 예제"
assert announcement["image"].endswith("noimg.jpg")

title = f"T50 {uuid.uuid4().hex[:8]}"
require_status("question create", request("/api/question/add", "POST", {
    "title": title, "content": '<p>실제 본문</p><img src=x onerror="alert(1)">',
    "username": "ignored", "hashtags": [],
}, token=writer_token), 200)
listing = require_status("question list", request("/api/question?orderBy=createdDate&page=0&size=20"), 200)
matches = [item for item in listing["content"] if item["title"] == title]
assert len(matches) == 1
question_id = matches[0]["id"]
question = require_status("question detail", request(f"/api/question/detail/{question_id}", token=writer_token), 200)
assert question["username"] == writer
assert "onerror" not in question["content"]

answers = require_status("answer create", request("/api/answer/save", "POST", {
    "username": "ignored", "questionId": question_id, "answerContent": "별도 조회 답변",
}, token=writer_token), 200)
assert len(answers) == 1
answers = require_status("answer requery", request(f"/api/answer/{question_id}", token=writer_token), 200)
assert len(answers) == 1 and answers[0]["content"] == "별도 조회 답변"

_, other_token = create_member()
require_status("other member update", request("/api/question/update", "PATCH", {
    "questionId": question_id, "title": "변조", "content": "변조", "hashtags": [],
}, token=other_token), 403)
require_status("expired token", request(f"/api/question/detail/{question_id}", token="expired-token"), 401)
question = require_status("question requery", request(f"/api/question/detail/{question_id}", token=writer_token), 200)
assert question["title"] == title
missing = json.loads(require_status("unmapped API", request("/api/member/missing/path"), 404))
assert missing == {"errorMessage": "Resource not found", "errorCode": 404, "instance": "/api/member/missing/path"}
invalid = json.loads(require_status("overlong registration password", request("/api/member/save", "POST", form={
    "username": f"long-{uuid.uuid4().hex[:8]}", "password": "가" * 25,
}), 400))
assert invalid["errorCode"] == 400 and invalid["errorMessage"] == "Invalid request"
print("local Spring flow: passed")
