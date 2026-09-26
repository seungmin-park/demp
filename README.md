# DEMP

## CORS 설정 (Phase 4 T41)

`APP_CORS_ALLOWED_ORIGINS`에 브라우저가 직접 호출하는 프런트엔드 Origin을 쉼표로 구분해 지정한다. 기본값은 로컬 개발용 `http://localhost:5050`이다. 예: `APP_CORS_ALLOWED_ORIGINS=https://demp.example,https://admin.demp.example`. 요청 Origin은 목록과 정확히 일치해야 하며, 허용되지 않은 사전 요청은 403이다. 같은 출처의 `/api` reverse proxy를 사용하면 브라우저 CORS 설정이 필요하지 않다.

## 공고 이미지 공개 주소 (Phase 5 T49)

`S3_PUBLIC_BASE_URL`은 공고 상세·목록·스크롤 응답의 `image` URL 접두부다. 기본값은 기존 `https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/`이며, CDN을 사용할 때 해당 공개 경로로 바꾼다. 저장 키는 그대로 유지되고 응답 조립에서만 공개 URL을 만든다.

## 실행·검증 기반 (2026-09-14)

Java 11에서 `./gradlew test`를 실행한다. 테스트는 고유 메모리 H2와 테스트 전용 JWT/S3 설정을 사용하며 운영 환경변수를 요구하지 않는다. 전체 컨텍스트 테스트는 InitDb를 그대로 실행한다. 각 테스트 본문에서 추가 데이터를 만들고 자신이 만든 데이터만 정리한다. InitDb 삭제와 seed 관련 기대값 변경은 T10에서 함께 진행한다.

API 문서는 `./gradlew asciidoctor`로 `build/docs/asciidoc/index.html`에 생성된다. Controller 테스트와 REST Docs 테스트는 분리되어 있다. 추적 중인 정적 문서는 빌드가 삭제하거나 덮어쓰지 않는다.

IntelliJ AsciiDoc 미리보기에서는 `src/docs/asciidoc/index.adoc`을 연다. 최초 실행 또는 `clean` 후에는 `./gradlew test`로 snippet을 생성해야 한다. 각 API 문서는 표준 `include::`와 소스 기준 상대 경로로 snippet을 읽으므로 REST Docs 전용 `operation::` 매크로 없이 미리보기할 수 있다. Controller와 REST Docs 테스트 모두 `@WebMvcTest`로 Spring MVC를 사용하며 Service와 직접 의존하는 하위 계층을 mock으로 교체한다.

프런트는 Node 18.18.2/npm 9.8.1에서 `npm ci`, `npm test -- --runInBand`, `npm run lint -- --no-fix`, `npm run build`를 사용한다. 이번 전체 테스트 1개와 lint/build가 통과했다.

[Phase 0 검증 결과·실행 명령·제한](docs/verification/build-and-test-foundation/README.md)을 참고한다. 정상 공고 등록·상세 계약은 T20에서 정리하며, 현재 multipart 실패를 성공 API로 안내하지 않는다.

개발자가 되고 싶은 취준생들에게 여러 정보를 주고받는 커뮤니티 사이트

취업공고 및 부트 캠프 등의 정보를 수집하고 꿀팁들을 공유하며 개발자가 되길 기원하며

### DB ERD

![demp_db_erd](https://user-images.githubusercontent.com/78605779/169659654-2a48cf56-80cd-476b-bdff-2fc247234a4a.PNG)

### Api docs

![demp_api_docs_announce](https://user-images.githubusercontent.com/78605779/169678360-fd8a9029-1e37-407b-91d0-8c1e54fd2d5e.png)
![demp_api_docs_answer](https://user-images.githubusercontent.com/78605779/169678359-73a4029b-959e-4d12-83f3-66f9808d3b10.png)
![demp_api_docs_member_question](https://user-images.githubusercontent.com/78605779/169678358-68c0421f-9889-42e1-a9a1-5b71db5939d0.png)

최종 백엔드 검증: 14 suites / 95 tests, 실패·오류·skip 0. `clean test asciidoctor bootJar` 성공. 문서 snippet 참조 79개 누락 0, multipart 실패 절의 생성 HTML 포함 확인.
