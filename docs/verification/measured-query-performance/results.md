# 반복 측정 결과

단위: 서버 내부 HTTP 처리 ms. 각 행은 3개 JVM × 15개 표본이며 워밍업 제외. p95는 nearest rank. RPS는 순차 요청 처리량으로 최대 동시 용량을 뜻하지 않는다.

| 비교군 | 데이터 규모 | 경로 | p50 ms | p95 ms | 순차 RPS | SQL | 로딩 엔티티 | 반환 건수 |
|---|---:|---|---:|---:|---:|---:|---:|---:|
| baseline | 1,000 | announcement_deep | 19.446 | 36.915 | 45.8 | 3–3 | 1001–1001 | 20–20 |
| baseline | 1,000 | announcement_filtered | 12.374 | 19.756 | 72.4 | 3–3 | 501–501 | 20–20 |
| baseline | 1,000 | announcement_first | 23.513 | 46.417 | 39.0 | 3–3 | 1001–1001 | 20–20 |
| baseline | 1,000 | answer_list | 3.787 | 9.481 | 199.3 | 52–52 | 101–101 | 50–50 |
| baseline | 1,000 | question_detail | 2.088 | 2.967 | 534.7 | 6–6 | 7–7 | 1–1 |
| baseline | 1,000 | question_list | 2.156 | 5.394 | 327.3 | 2–2 | 1–1 | 1000–1000 |
| baseline | 10,000 | announcement_deep | 192.592 | 220.931 | 5.0 | 3–3 | 10001–10001 | 20–20 |
| baseline | 10,000 | announcement_filtered | 118.317 | 196.842 | 7.8 | 3–3 | 5001–5001 | 20–20 |
| baseline | 10,000 | announcement_first | 189.147 | 597.310 | 3.5 | 3–3 | 10001–10001 | 20–20 |
| baseline | 10,000 | answer_list | 1.627 | 4.129 | 393.0 | 52–52 | 101–101 | 50–50 |
| baseline | 10,000 | question_detail | 0.798 | 1.651 | 1113.6 | 6–6 | 7–7 | 1–1 |
| baseline | 10,000 | question_list | 9.811 | 25.253 | 73.6 | 2–2 | 1–1 | 10000–10000 |
| current | 1,000 | announcement_deep | 3.809 | 5.642 | 241.0 | 3–3 | 21–21 | 20–20 |
| current | 1,000 | announcement_filtered | 4.139 | 5.120 | 243.2 | 3–3 | 21–21 | 20–20 |
| current | 1,000 | announcement_first | 5.298 | 8.653 | 186.3 | 3–3 | 21–21 | 20–20 |
| current | 1,000 | answer_list | 4.927 | 8.164 | 189.2 | 53–53 | 101–101 | 50–50 |
| current | 1,000 | question_detail | 2.759 | 3.559 | 384.6 | 7–7 | 7–7 | 1–1 |
| current | 1,000 | question_list | 1.772 | 2.699 | 519.9 | 2–2 | 1–1 | 20–20 |
| current | 10,000 | announcement_deep | 2.782 | 3.371 | 354.3 | 3–3 | 21–21 | 20–20 |
| current | 10,000 | announcement_filtered | 2.815 | 3.711 | 347.9 | 3–3 | 21–21 | 20–20 |
| current | 10,000 | announcement_first | 2.833 | 3.406 | 345.8 | 3–3 | 21–21 | 20–20 |
| current | 10,000 | answer_list | 3.140 | 3.841 | 314.0 | 53–53 | 101–101 | 50–50 |
| current | 10,000 | question_detail | 1.313 | 1.743 | 744.9 | 7–7 | 7–7 | 1–1 |
| current | 10,000 | question_list | 1.198 | 1.555 | 812.8 | 2–2 | 1–1 | 20–20 |
| current | 100,000 | announcement_deep | 1.874 | 2.773 | 505.6 | 3–3 | 21–21 | 20–20 |
| current | 100,000 | announcement_filtered | 1.927 | 2.507 | 504.1 | 3–3 | 21–21 | 20–20 |
| current | 100,000 | announcement_first | 1.909 | 2.598 | 506.2 | 3–3 | 21–21 | 20–20 |
| current | 100,000 | answer_list | 1.976 | 2.741 | 493.5 | 53–53 | 101–101 | 50–50 |
| current | 100,000 | question_detail | 0.805 | 1.231 | 1167.0 | 7–7 | 7–7 | 1–1 |
| current | 100,000 | question_list | 0.768 | 1.149 | 1164.7 | 2–2 | 1–1 | 20–20 |
| current-control | 1,000 | announcement_deep | 12.851 | 29.900 | 62.7 | 3–3 | 1001–1001 | 20–20 |
| current-control | 1,000 | announcement_filtered | 5.747 | 6.622 | 172.3 | 3–3 | 501–501 | 20–20 |
| current-control | 1,000 | announcement_first | 20.364 | 30.442 | 50.5 | 3–3 | 1001–1001 | 20–20 |
| current-control | 1,000 | question_list | 5.568 | 7.085 | 183.6 | 2–2 | 1–1 | 1000–1000 |
| current-control | 10,000 | announcement_deep | 56.802 | 76.870 | 16.2 | 3–3 | 10001–10001 | 20–20 |
| current-control | 10,000 | announcement_filtered | 28.356 | 46.534 | 32.7 | 3–3 | 5001–5001 | 20–20 |
| current-control | 10,000 | announcement_first | 58.024 | 79.851 | 15.9 | 3–3 | 10001–10001 | 20–20 |
| current-control | 10,000 | question_list | 15.578 | 21.234 | 61.5 | 2–2 | 1–1 | 10000–10000 |
| current-optimized | 1,000 | announcement_deep | 7.048 | 9.332 | 149.9 | 3–3 | 21–21 | 20–20 |
| current-optimized | 1,000 | announcement_filtered | 6.133 | 8.556 | 160.7 | 3–3 | 21–21 | 20–20 |
| current-optimized | 1,000 | announcement_first | 9.472 | 15.776 | 103.0 | 3–3 | 21–21 | 20–20 |
| current-optimized | 1,000 | answer_list | 5.092 | 8.547 | 185.9 | 3–3 | 101–101 | 50–50 |
| current-optimized | 1,000 | question_detail | 4.719 | 6.002 | 230.0 | 3–3 | 7–7 | 1–1 |
| current-optimized | 1,000 | question_list | 2.481 | 4.637 | 345.1 | 2–2 | 1–1 | 20–20 |
| current-optimized | 10,000 | announcement_deep | 4.277 | 5.627 | 238.4 | 3–3 | 21–21 | 20–20 |
| current-optimized | 10,000 | announcement_filtered | 4.095 | 6.467 | 233.1 | 3–3 | 21–21 | 20–20 |
| current-optimized | 10,000 | announcement_first | 3.809 | 5.337 | 241.5 | 3–3 | 21–21 | 20–20 |
| current-optimized | 10,000 | answer_list | 3.666 | 5.486 | 269.3 | 3–3 | 101–101 | 50–50 |
| current-optimized | 10,000 | question_detail | 2.908 | 4.349 | 340.2 | 3–3 | 7–7 | 1–1 |
| current-optimized | 10,000 | question_list | 1.494 | 2.933 | 543.9 | 2–2 | 1–1 | 20–20 |
| current-optimized | 100,000 | announcement_deep | 2.522 | 3.078 | 391.0 | 3–3 | 21–21 | 20–20 |
| current-optimized | 100,000 | announcement_filtered | 2.783 | 5.164 | 325.3 | 3–3 | 21–21 | 20–20 |
| current-optimized | 100,000 | announcement_first | 2.624 | 3.414 | 368.7 | 3–3 | 21–21 | 20–20 |
| current-optimized | 100,000 | answer_list | 2.416 | 3.686 | 387.5 | 3–3 | 101–101 | 50–50 |
| current-optimized | 100,000 | question_detail | 1.864 | 2.525 | 530.1 | 3–3 | 7–7 | 1–1 |
| current-optimized | 100,000 | question_list | 1.099 | 1.967 | 832.5 | 2–2 | 1–1 | 20–20 |
