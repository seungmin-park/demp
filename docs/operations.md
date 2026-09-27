# 운영·배포 및 데이터 호환성

이 문서는 기존 운영 절차를 보존한다. 현재 기능·검증 결과는 [README](../README.md)를 참고한다. 실제 운영 배포 기록은 아니다.

## CI 및 인수 절차

CI는 Zulu Java 25와 Gradle wrapper, `gradle.lockfile`의 고정 의존성을 사용해 `./gradlew clean test` 후 `./gradlew asciidoctor`를 실행한다. 전체 테스트에 REST Docs 생성 테스트가 포함되며, 생성 HTML의 존재, unresolved snippet 표기, 테스트 전용 내부 키 노출을 검사한다. 테스트 설정은 고유 H2 메모리 DB와 테스트 전용 S3 설정을 사용하며 파일 저장은 테스트 대역으로 격리한다. 운영 DB와 S3에 접속하는 테스트 명령은 없다.

실제 Spring API 흐름은 별도 프로세스에서 임시 메모리 H2로 검증할 수 있다. 첫 터미널에서 다음 서버를 시작하고, 둘째 터미널에서 `python3 scripts/verify_local_flow.py`를 실행한 뒤 서버를 종료한다. 이 스크립트는 loopback 주소만 허용하며 공고 이미지를 업로드하지 않는다. 서버 종료 시 임시 DB fixture가 사라진다.

```sh
SPRING_PROFILES_ACTIVE=local SPRING_DATASOURCE_URL='jdbc:h2:mem:t50;MODE=MySQL;DB_CLOSE_DELAY=-1' PORT=18080 AWS_EC2_METADATA_DISABLED=true ./gradlew bootRun
python3 scripts/verify_local_flow.py
```

양쪽 저장소의 같은 API 계약 커밋을 확인한 뒤 백엔드와 프런트 CI를 모두 통과시킨다. 운영 배포는 기존 클라이언트와 호환되는 백엔드를 먼저 배포하고 인증·공고·질문·답변 API를 확인한 뒤 프런트 산출물을 교체한다. 실패 시 프런트 산출물을 직전 버전으로 되돌리고, 이어 백엔드를 직전 호환 버전으로 되돌린다. 스키마 변경이 포함되면 배포 전 백업과 변경별 복구 절차를 준비한다. 이 문서는 배포 수행 기록이 아니다.

## CORS 설정 (Phase 4 T41)

`APP_CORS_ALLOWED_ORIGINS`에 브라우저가 직접 호출하는 프런트엔드 Origin을 쉼표로 구분해 지정한다. 기본값은 로컬 개발용 `http://localhost:5050`이다. 예: `APP_CORS_ALLOWED_ORIGINS=https://demp.example,https://admin.demp.example`. 요청 Origin은 목록과 정확히 일치해야 하며, 허용되지 않은 사전 요청은 403이다. 같은 출처의 `/api` reverse proxy를 사용하면 브라우저 CORS 설정이 필요하지 않다.

## 공고 이미지 공개 주소 (Phase 5 T49)

`S3_PUBLIC_BASE_URL`은 공고 상세·목록·스크롤 응답의 `image` URL 접두부다. 기본값은 기존 `https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/`이며, CDN을 사용할 때 해당 공개 경로로 바꾼다. 저장 키는 그대로 유지되고 응답 조립에서만 공개 URL을 만든다.

## 실행·검증 (Phase 7)

프로젝트 루트에서 `asdf install`로 `.tool-versions`의 **Zulu Java 25 LTS**를 준비한다. Gradle wrapper 9.8.0을 사용하며 전역 Java 선택을 바꿀 필요가 없다. `JAVA_HOME`을 별도로 설정했다면 해당 Zulu 경로와 일치시킨다.

```sh
asdf exec java -version
./gradlew clean test asciidoctor bootJar
```

Spring Boot 4.1.1, Jakarta API, Spring Security 7, Hibernate 7/Jackson 3, OpenFeign Querydsl 7.7, AWS SDK 2를 사용한다. H2는 BOM의 체크 제약 캐시 오류 수정 버전인 2.5.252로 고정한다. 상세 변경·호환성·검증은 [Phase 7 기록](verification/runtime-framework-and-typescript-upgrade/README.md)을 참조한다.

자동 테스트는 고유 메모리 H2와 테스트 전용 JWT/S3 설정을 사용한다. 운영 설정은 `ddl-auto=validate`, local만 SQL seed를 사용한다. REST Docs는 `build/docs/asciidoc/index.html`에 생성되며 IDE 미리보기는 `src/docs/asciidoc/index.adoc`을 연다. `clean` 후 미리보기 전에 `./gradlew test`로 snippet을 생성한다. OpenAPI JSON은 `/v3/api-docs`, UI는 `/swagger-ui.html`이다.

### JWT 키와 비밀번호 호환성

배포 전에 `JWT_SECRET`이 **최소 32 UTF-8 바이트**인지 확인한다. JJWT 0.13은 이보다 짧은 HS256 키를 거절하며 앱은 값 자체를 출력하지 않고 설정 이름과 최소 길이만 안내한다. 임의의 짧은 문구를 반복하거나 공백으로 채우지 말고, 비밀 관리 시스템에서 충분한 난수로 만든 값을 사용한다. 설정값은 기존과 같이 raw UTF-8로 사용하며 자동 Base64 디코딩하지 않는다.

**기존 토큰 보존은 길이 조건을 만족하는 동일한 키를 계속 사용할 때 적용된다.** 이전 키가 짧으면 배포 전에 새 키를 준비하고 백엔드 인스턴스가 함께 같은 키로 전환되도록 계획한다. 키를 바꾸면 기존 토큰은 401이 되어 사용자가 다시 로그인해야 한다. 새 키와 이전 키를 동시에 검증하는 기능은 제공하지 않는다. 키를 로그·명령 이력·저장소에 남기지 않는다. 롤백에도 사용할 키를 배포 산출물과 별도로 안전하게 관리하고, 이미 회전한 키를 되돌리면 새 키로 발급한 토큰도 무효화됨을 고려한다. 운영 키는 이번 작업에서 조회하거나 변경하지 않았다.

신규 가입 비밀번호는 BCrypt의 **최대 72 UTF-8 바이트**를 넘으면 안전한 400으로 거절하고 프런트에서도 안내한다. 영문·숫자는 최대 72자, 한글은 최대 24자이며 혼합 입력은 바이트 수로 계산한다. 입력을 잘라내어 저장하지 않는다. 기존 해시를 가진 긴 비밀번호는 로그인 시 길이를 제한하거나 바꾸지 않으며 기존 BCrypt 검증 경로를 유지한다.

### 저장 데이터와 롤백

운영 DB를 연결하거나 수정하지 않았다. 자동화된 기본 프로필 시작·재시작 검증은 격리된 H2로 실행한다. Hibernate 5에서 사용한 공유 `hibernate_sequence`와 `member_sequence`/`que_sequence`, 문자열 enum 매핑을 유지하며 기존 스키마 fixture에 `validate`와 신규 저장을 검증한다. 실제 MySQL 스키마는 배포 전 복제본에 같은 검증을 실행해야 한다.

기존 H2 1.4 로컬 파일을 H2 2.x로 바로 열지 않는다. 서버를 종료하고 `.local`을 백업한 뒤, 이전 H2 버전의 SCRIPT와 새 버전의 RUNSCRIPT로 별도 파일에 이관한다. 예제 데이터만 있다면 백업을 보관하고 새 worktree의 빈 `.local` DB로 시작할 수 있다. 이전 JAR/Java 11과 원본 DB 백업은 함께 보관하며 새 DB 파일을 이전 H2로 다시 열지 않는다. 운영은 배포 전 백업과 복제본 검증을 통과한 뒤 양쪽 호환 산출물을 순서대로 교체한다.


## 운영 환경변수와 프로필

운영에서는 `local` 프로필을 사용하지 않는다. local은 예제 계정·개발 JWT·H2 자동 스키마 갱신과 로컬 저장을 위한 설정이다. 기본 운영 프로필은 MySQL 스키마를 `validate`하고 seed SQL을 실행하지 않는다.

| 변수 | 의미 |
|---|---|
| `JDBC_DATABASE_URL` | 대상 MySQL JDBC URL |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | DB 접속 계정·비밀번호 |
| `JWT_SECRET` | 최소32 UTF-8바이트 HS256 키. 위 전환 절차 참고 |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY` | S3 접근 자격 증명 |
| `S3_STATIC` | S3 리전(예: ap-northeast-2) |
| `S3_BUCKET` | 이미지 저장 버킷 |
| `S3_PUBLIC_BASE_URL` | 공개 이미지 주소/CDN 접두부. 대상 버킷과 일치시킨다 |
| `APP_CORS_ALLOWED_ORIGINS` | 직접 API 호출을 허용할 프런트 Origin 목록 |
| `PORT` | HTTP 포트, 기본8080 |

값은 배포 환경의 비밀 관리 경계에서 주입하며 저장소·README에 실제 키를 쓰지 않는다. S3 IAM 권한·버킷 공개 정책/배포 CDN은 실제 환경에서 검증해야 한다. 이번 작업은 운영 S3에 접속하거나 정책을 변경하지 않았다.

대표 이미지와 본문 이미지의 저장·삭제는 FileStorage 경계가 담당한다. local만 `.local/uploads`(또는 `APP_LOCAL_UPLOAD_DIR`)를 사용하며 운영은 S3다. [게시·이미지 정책](plans/curated-publication-policy.md)과 [관리자 계정 준비](verification/admin-console/account-setup.md)를 함께 확인한다.
