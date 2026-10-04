# 현재 기술 스택과 공식 문서

2026-10-05 CI 보강에서 test scope의 ArchUnit 1.5.1과 [공식 사용자 가이드](https://www.archunit.org/userguide/html/000_Index.html)를 확인했다. 실제 해석된 test dependency는 `gradle.lockfile`에 기록하며 운영 runtime 의존성을 바꾸지 않는다. [적용한 경계와 한계](ci-and-delivery.md)를 따른다.

`official-docs.json`의 버전은 `.tool-versions`, `build.gradle`, Gradle wrapper와 함께 검사한다. 문서를 확인한 날짜는 2026-09-29이다. 외부 문서의 전체 본문은 복사하지 않고 공식 링크와 적용 범위만 남긴다. 최신 문서로 이동하는 링크는 패치 버전을 고정하지 않으므로 버전을 바꿀 때 다시 확인한다.

| 프로젝트 버전 | 공식 문서 | 이 코드에서 확인할 내용 |
|---|---|---|
| Java 25 | [Java SE 25 API](https://docs.oracle.com/en/java/javase/25/docs/api/) | Java toolchain과 컴파일 API |
| Spring Boot 4.1.1 | [Spring Boot reference](https://docs.spring.io/spring-boot/reference/) | MVC·Security·Data JPA·테스트 구성. 확인일에는 4.1.1; 링크는 최신 버전으로 이동 가능 |
| Gradle 9.8.0 | [Gradle Java plugin](https://docs.gradle.org/current/userguide/java_plugin.html) | toolchain·테스트 source set. 확인일에는 9.8.0; 링크는 최신 버전으로 이동 가능 |

Spring Security·JPA·Hibernate 등 BOM 관리 의존성의 실제 패치 버전은 `gradle.lockfile`과 `./gradlew dependencies`로 확인한다. Boot 버전만으로 각각의 패치 번호를 추정하지 않는다. 프런트 도구 문서는 [프런트 문서](https://github.com/seungmin-park/dempfrontend/blob/main/docs/engineering/official-docs.md)에서 해당 `package.json`과 묶어 관리한다. CI의 `python3 scripts/check_agent_contracts.py`가 로컬 버전 불일치를 거절하며, 외부 사이트 갱신 감시는 수행하지 않는다.
