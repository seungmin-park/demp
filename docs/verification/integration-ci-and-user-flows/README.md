# Phase 6 T50 검증 기록 (2026-09-26)

## 요구사항과 실행 경계

두 저장소 CI는 각자의 lockfile/Gradle wrapper로 설치·테스트·문서를 재현한다. 브라우저 흐름은 프런트 API fixture를 매 테스트마다 새로 만들어 실행한다. 실제 Spring API 흐름은 별도 프로세스의 임시 메모리 H2에서 `scripts/verify_local_flow.py`로 실행했다. 브라우저 검증과 Spring API 검증은 각각 실행한 것이며 브라우저가 실제 Spring에 연결된 단일 실행으로 표현하지 않는다. 이미지 업로드를 호출하지 않아 S3 저장을 수행하지 않는다. REST Docs는 별도 Gradle 작업으로 HTML을 생성한다.

```text
Chromium → Vue 개발 서버 → Playwright API fixture (테스트별 새 상태)
Python API 점검 ────────→ Spring local → 임시 메모리 H2
                                         └→ S3 저장 호출 없음
```

## Red → Green → Refactor

| 실제 실패 | 원인 | 최소 변경 및 재검증 |
| --- | --- | --- |
| 브라우저 가입 뒤 `/account`에 남고 가입 POST 없음 | 재확인 규칙이 같은 비밀번호를 불일치로 판정; 중복 검사 버튼도 submit으로 동작 | `AccountForm`은 폼의 현재 `password`와 비교하고 중복 검사 버튼을 일반 버튼으로 지정. 가입 POST와 `/login` 이동 확인 |
| 가입·로그인 API가 200인데 `/login`에 남음 | redirect 쿼리가 없을 때 `undefined`를 경로로 전달 | `LoginForm`에서 빈 redirect를 홈 경로로 처리. 가입→로그인→공고 흐름 확인 |
| `npm ci` 후 17 suite 실행 전 실패 | lockfile의 `@babel/plugin-syntax-import-attributes`가 Babel core 7.22 이상을 요구하는데 직접 의존성은 7.17.8 | `@babel/core` 7.22.20 고정 후 `npm ci`, 17 suite·45 test 통과 |
| 임시 H2의 실제 공고 목록·상세 500 | 기존 local seed 공고의 `image` 저장 키가 없어 URL 조립 시 null 참조 | 새 seed에 `noimg.jpg` 키 삽입, 기존 local seed도 갱신. 새 메모리 DB에서 목록·상세 200 확인 |

테스트 대역은 회원·토큰·질문·답변 상태를 브라우저 테스트마다 새로 만든다. 실제 Spring 점검은 고유 회원·질문 이름을 사용하고 서버 종료로 임시 H2를 폐기한다. `AccountForm`은 입력 검증, `LoginForm`은 인증 후 목적지 이동, API 모듈은 HTTP 요청, Vuex는 인증 상태 보존을 맡는다. 두 컴포넌트의 이름과 책임은 그대로 맞으며 호출부의 라우트·JSON 키를 변경하지 않았다. 독립 TypeScript 객체는 없다.

## API fixture와 `spec.md` 대조

| 계약 | 브라우저 fixture | 실제 Spring 확인 |
| --- | --- | --- |
| 회원 가입·로그인 | multipart `username/password`, 로그인 `username/jwt` | 가입·로그인 각각 200 |
| 공고 목록·상세 | Slice `content/last`, 상세 `company.name`, `announcementType`, `language`, `image` | 로컬 공고 목록·상세 200; 이미지 URL이 `noimg.jpg`로 끝남 |
| 질문 작성·목록·상세 | JSON `title/content/username/hashtags`, 목록 `content/number/last`, 상세 작성자 | 생성·별도 목록/상세 200, HTML 이벤트 속성 제거, 작성자는 인증 주체 |
| 답변 작성·별도 재조회 | JSON `username/questionId/answerContent`, 답변 배열 | 생성·별도 GET 200, 내용 1개 일치 |
| 소유권·만료 인증 | 타인 PATCH 403, 만료 GET 401 | 실제 다른 회원 PATCH 403, 잘못된 토큰 GET 401 |

`spec.md`의 초기 관찰 표에는 이전 결함이 보존되어 있다. 위 행은 현재 DTO/Controller와 실행 결과를 대조한 T50 상태다. 브라우저의 검색 응답 역전과 새로고침 후 상태는 Playwright 4개 테스트에서 확인했다. 브라우저의 타인 PATCH는 API fixture 응답이며 실제 403은 별도 Spring 실행으로 검증했다.

## 명령과 결과

최종 수치는 아래 명령의 종료 코드와 XML/러너 집계를 기준으로 기입한다. GitHub Actions 서버에서 워크플로를 실행한 기록은 없다. workflow의 각 `run` 단계는 기본 실패 종료 코드에서 작업을 멈추도록 구성했다.

| 저장소 | 명령 | 결과 |
| --- | --- | --- |
| B | `./gradlew clean test` | 33 suite, 195 test, 실패·오류·skip 0 |
| B | `./gradlew asciidoctor` | `build/docs/asciidoc/index.html` 생성; 누락 include/테스트 전용 키 검사 통과 |
| B | `python3 scripts/verify_local_flow.py` (임시 H2 서버) | 14개 요청 확인, 마지막 결과 `local Spring flow: passed` |
| F | `npm ci` | 1,506개 설치, 종료 0; 기존 의존성 audit 96건 출력 |
| F | `npm test -- --runInBand` | 17 suite, 45 test 통과 |
| F | `npm run lint -- --no-fix` | 오류 0 |
| F | `npm run build` | 종료 0; 기존 app entrypoint 크기 권고 경고 |
| F | `npx playwright test` | Chromium 4개 테스트 통과, 기본 4 worker |

`npm audit` 출력의 96건은 해결된 것으로 취급하지 않는다. 런타임·프레임워크 갱신은 `tasks.md` Phase 7에서 별도로 다룬다. 양쪽 동시 배포·복구 순서는 각 README에 기록했으며 배포 자체는 실행하지 않았다.
