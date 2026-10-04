# CI와 보호된 PR 전달

날짜: 2026-10-05 (Asia/Seoul)
Phase: CI 검증과 보호된 전달. 브랜치: `refactor/ci-and-protected-delivery`.
작업 공간: `/Users/seungmin/.codex/worktrees/ci-and-protected-delivery/demp`.
기준 main: `b5871ae2e26050e0bddbcb19a4b025eb73676aa8`.

## 목표와 설계

기존 DEMP 동작 테스트와 반응 쓰기 소유권 검사에 구조 검사, 실제 XML의 필수 suite 확인, 실행 JAR의 Spring/H2 HTTP 흐름을 연결한다. 도메인 규칙의 정확성은 동작 assertion이 맡고, 의존성의 위치는 구조 검사와 책임 리뷰가 맡는다. JaCoCo와 고정 총 테스트 건수는 도입하지 않는다.

```text
검사기 회귀 → 기존 소유권/버전 검사 → 전체 테스트/구조 검사/REST Docs
 → 필수 XML·문서·JAR 확인 → 실제 Spring/H2 HTTP
 → PR 필수 DEMP verify → 최신 main 조건 → 자동 squash merge
```

DEMP의 현재 Service는 구체 클래스이므로 PlugPass의 인터페이스·Default 이름 규칙을 복사하지 않는다. Controller 저장소 직접 의존, Domain의 HTTP/Service/저장소 의존, 엔티티 public setter, Service 테스트의 테스트 트랜잭션, MVC slice의 실제 저장소 의존을 검사한다. 기존 외부 파일/S3 대체와 동시성 실험의 spy는 이 작업에서 금지하지 않는다. 기존 반응 writer 스캔은 유지한다.

## 구현과 실행 체크리스트

- [x] 기준선 전체 테스트와 기존 원격 CI 확인.
- [x] Red: 필수 XML 누락/실패/skip·문서 누출/불일치의 검사기 테스트와 금지 구조 예제 작성 후 의도한 assertion 실패 확인.
- [x] Green: 검사 정책과 공용 `scripts/verify.sh`, 실행 JAR 문서 패키징, 실제 로컬 HTTP 검증 연결.
- [x] 실제 구조 위반과 필수 보고서 누락을 임시로 넣어 거부 확인 후 복원.
- [x] Refactor: 검사 정책·입력·실행 조정을 분리하고 이름/호출부/공개 계약 검토.
- [x] 전체 공용 검증 및 문서/JSON/diff 정합성 확인.
- [ ] main 보호·native auto-merge 설정, 서명 commit/push와 PR 생성·첨부.
- [ ] 정확한 PR head CI 성공 → 자동 squash 머지 → 머지된 main CI 성공 확인.

## 검증 환경과 한계

`CMUX_WORKSPACE_ID`와 `CMUX_SURFACE_ID`가 없으며 `cmux identify --json`, ping, capabilities가 모두 live socket 없음으로 실패했다. 로컬 화면 E2E를 수행했다고 보고하지 않는다. 터미널 runner와 loopback Spring/H2 HTTP 검증 및 원격 CI를 구분한다. 프런트는 별도 저장소이며 이번에는 변경하지 않는다. 운영 DB/S3나 실제 사용자 데이터에 접근하지 않는다.

실행 명령·Red/Green·건수·종료 코드·리뷰와 원격 결과는 작업 완료 시 이 문서에 추가한다. 출력은 `build/verification/`, 테스트 XML은 `build/test-results/test/`에 보관한다.

## 실행과 검토 근거

- 기준선: Java 25의 `./gradlew test --console=plain`, 종료 0. main `b5871ae`의 기존 GitHub CI도 success였다.
- Red: no-op 구조 정책에 `./gradlew test --tests '*ArchitectureRulesTest' --write-locks --console=plain`을 실행해 12개 중 금지 예제 8개가 예외 없음으로 assertion 실패했다. 새 의존성은 실패 task에서 lockfile이 저장되지 않아 다음 실행이 lock state 오류로 실패했고, 이 환경 오류는 Red로 세지 않았다. `dependencies --write-locks`로 전체 lock state를 갱신했다.
- Python Red: `python3 -m unittest discover -s scripts -p 'test_*.py' -v`가 15개 실행, subtest를 포함한 실패 14개였다. 누락·0개·빈 suite·실패/오류/skip·문서·JAR 위반을 허용해 `RuntimeError not raised` assertion이 실패했다.
- Green: 금지 경계와 파일 검사를 구현했다. HTTP 최상위 패키지 `org.springframework.http`를 하위 패키지만 찾는 selector가 놓쳐 예제 1개가 계속 실패했고, 실제 패키지 경계를 수정한 뒤 구조 17개와 Python 15개가 통과했다.
- 실제 JAR 검사는 기존 체크인 HTML과 생성 문서의 불일치를 `Packaged documentation mismatch`로 거부했다. 새 생성 문서를 추가하는 과정의 duplicate ZIP entry 실패를 확인하고 과거 체크인 HTML을 제거했다. bootJar가 생성 HTML을 유일한 문서로 넣는다.
- 공용 전체 실행: `JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-25.36.205/Contents/Home bash scripts/verify.sh`, 종료 0. Java 333개(기존 316 + 구조 5 + 검사기 회귀 12), Python 15개, 실패/오류/skip 0. 필수 suite 55개가 실제 XML에 존재했다. 실제 local API, 반응 commit·중복·전환·취소·재조회와 인증 401, 생성/JAR/HTTP 문서 일치가 통과했다.
- 실제 누락 probe: `ContentReactionServiceTest` XML을 임시 이동하고 `python3 scripts/verify_ci.py`를 실행해 종료 1과 정확한 누락 suite 이름을 확인한 뒤 finally 복원했다.
- 실제 구조 probe: `ContentReactionController`에 `ContentReactionRepository` 필드와 import를 임시로 추가하고 `./gradlew test --tests '*ArchitectureTest' --console=plain`을 실행했다. 종료 1, 5개 중 assertion 실패 1개, 해당 Controller와 Repository의 Architecture Violation을 확인했다. finally에서 원본 bytes를 복원했으며 운영 Java diff는 없다.
- Refactor 검토: 정책(`ArchitectureRules`), 실제 클래스 선택(`ArchitectureTest`), 금지/허용 입력(`ArchitectureRulesTest`), XML·문서·JAR 확인(`verify_ci`), 서버 수명과 HTTP(`verify_runtime`)의 책임을 분리했다. 그린 이후 추가 분리는 필요 없어 유지했다. 이름은 대상과 효과를 나타내며 기존 HTTP JSON·DB·Service 계약은 바꾸지 않았다. Vue/TypeScript와 형제 프런트 저장소는 이번 변경 대상이 아니다.
- 기존 `ContentReaction`의 deprecated API, test/JVM CDS, Gradle 10 호환성 deprecation 경고는 남아 있다. 테스트·문서·패키징의 실패로 취급하지 않으며 이번 범위에서 운영 리팩터링을 추가하지 않았다. ArchUnit이 요구한 test scope SLF4J API 2.0.19는 lockfile에 기록했고 production runtime의 2.0.18은 유지했다.
- 필수 목록 JSON 중복과 문서 로컬 참조, `git diff --check`를 확인했다. JaCoCo·coverage 비율·고정 전체 건수는 도입하지 않았다. 독립 병렬 에이전트 리뷰는 실행하지 않았다.

probe와 최초 Red 로그는 로컬 `/tmp/demp-ci-*.log`, 최종 실행 산출물은 `build/verification/`에 남긴다. GitHub의 필수 check와 머지 후 main 실행은 PR/checks의 정확한 SHA에서 별도로 확인한다.

복원 후 최종 공용 검증도 종료 0, Java 333개·Python 15개·실패/오류/skip 0과 동일한 실제 HTTP assertion을 확인했다. 위 GitHub 전달 체크리스트는 PR 생성 전의 소스 기록이며 최종 완료 여부는 PR의 MERGED 상태와 해당 merge SHA의 main CI로 기록한다.
