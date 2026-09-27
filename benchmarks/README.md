# 최초 리팩터링 이전 / 현재 조회 성능 비교

이 벤치마크는 **실제 Spring MVC·인증·서비스·JPA·JSON 직렬화를 통과하는 MockMvc 요청**을 측정한다. 네트워크 왕복·브라우저 렌더링·실제 MySQL 서버의 처리량을 뜻하지 않는다. 운영 DB를 받는 옵션은 없으며 `jdbc:h2:mem:benchmark`가 아니면 데이터 생성을 거절한다.

## 비교군

| 이름 | 코드/환경 | 목적 |
|---|---|---|
| baseline | `a8b057b`, Java11 / Boot2.5 / Hibernate5 / H2 1.4 | 최초 개선 계획 당시 제품 코드 |
| current | `2f4d749`, Java25 / Boot4.1 / Hibernate7 / H2 2.5 | 반응 기능까지 완료한 현재 코드 |
| current-control | current와 같은 JVM·DB·DTO + 과거 fetch join/전체 목록 조회 | 조회 전략의 비용을 분리한 대조군 |
| current-optimized | 측정 후 발견한 병목을 수정한 코드 | 추가 개선의 전후 비교 |

baseline은 원래 compileQuerydsl 오류 4개로 빌드되지 않는다. `735e768`에서 **InitDb, AnnouncementController 두 파일의 타입·생성자 정합성만** 복원한다. 쿼리·서비스·테스트를 당시 이후 코드로 교체하지 않는다. 복원 diff는 검증 자료에 보존한다. 역사적 커밋을 수정하거나 덮어쓰지 않는다.

## 데이터와 측정 계약

- 공고/질문 각각 1천·1만·10만 건. 각 공고 기술 2개, 각 질문 태그 2개, 회원 1천 명. 상세 질문의 답변 50개는 작성자가 모두 다르다.
- 공고 EMP/EDU 50:50, 전체 공개. 날짜·문자열·ID·분포는 seed 함수로 결정되어 난수나 실행 날짜에 의존하지 않는다.
- 크기별 워밍업 5회 후 15회 측정, 독립 JVM 3라운드. 각 라운드의 버전 실행 순서를 회전한다. 프로세스는 동시에 측정하지 않는다.
- heap 1GiB 고정, ActiveProcessorCount=4. DB 생성/데이터 적재는 측정에서 제외한다. 각 HTTP 요청마다 새 persistence context, JVM/DB 캐시는 warm 상태다.
- 개별 표본: 지연(ms), SQL 준비 실행 수, 로딩 엔티티 수, 응답 건수·바이트, Java 버전. 잘못된 HTTP 상태·반환 건수·공고 hasNext는 assertion 실패로 중단한다.
- 공고는 두 버전 모두 20개를 반환하지만 과거는 정렬 계약이 없어 동일 ID 집합을 보장하지 않는다. 현재는 ID 내림차순이다.
- **질문 목록은 계약이 다르다**: 과거는 전체 N개, 현재는 20개 Slice. 이 수치는 같은 결과의 쿼리만 빨라진 비율이 아니라 페이지 단위 응답으로 변경한 효과다.
- current에는 게시 상태·교육 메타데이터와 내 반응 조회가 추가되어 응답 크기/SQL이 늘어난 경우도 있다. current-control은 과거 조회 알고리즘을 현재 모델에 적용한 대조군이며 과거 프로그램 그 자체라고 부르지 않는다.
- 초당 처리 요청은 순차 표본의 총 요청수/측정 시간이다. 최대 동시 처리 용량이나 운영 SLA로 해석하지 않는다. H2 결과를 MySQL 인덱스 효과로 일반화하지 않는다.

## 재현

먼저 `.tool-versions`의 Java25와 별도의 Java11을 준비한다. 아래 `BENCH_JAVA11`, `BENCH_JAVA25`는 각각 JDK home 절대경로다.

```sh
git worktree add --detach .worktrees/benchmark-baseline a8b057b
python3 benchmarks/install_harness.py .worktrees/benchmark-baseline --legacy
python3 benchmarks/install_harness.py .
python3 benchmarks/run_comparison.py \
  --baseline .worktrees/benchmark-baseline --current . \
  --java11 "$BENCH_JAVA11" --java25 "$BENCH_JAVA25" \
  --output build/benchmark-comparison
```

`build/benchmark-src`만 생성하며 일반 애플리케이션/테스트 소스셋에 벤치마크용 controller를 넣지 않는다. 일반 빌드 산출물에는 `/bench/**`가 존재하지 않는다. `benchmark.init.gradle`을 명시한 `queryBenchmark` 작업에서만 사용한다. 이미 결과 파일이 있는 디렉터리에는 덧붙이지 않고 중단한다. 결과 비교는 같은 측정 조건으로 수행한다.

원본 current를 재현하려면 그 커밋의 별도 worktree에 현재 `benchmarks/install_harness.py`로 하네스를 설치하고 `--current`로 지정한다. 과거 시점의 성능 숫자는 최신 코드에서 자동 재현되는 숫자로 오해하지 않는다.
