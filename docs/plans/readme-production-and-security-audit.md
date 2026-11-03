# README: 운영 배포 사실과 보안 감사 결과

- Phase: 보안 감사 후속 4 (문서)
- 브랜치: `refactor/readme-production-and-security-audit`
- worktree: `.worktrees/readme-production-and-security-audit`

## 변경

- "운영 배포는 아직"이라는 오래된 문장을 실제 전달 방식(성공한 CI 산출물을 재빌드 없이 GCP VM에 배포, 수동 스키마 변경은 운영자가 별도 적용)으로 바꿨다. 실사용 트래픽·동시 부하 측정은 여전히 하지 않았다고 남겼다.
- `기술적 문제 해결`에 운영 DB CHECK 제약 차이 대응(2026-10-13 기록)과 보안 감사 발견·수정(#23~#25)을 추가했다.
- 아직 운영자 조치가 남은 위험(이력에 남은 자격 증명, 배포 권한·관리 포트 설정)은 공개 README에 구체적으로 적지 않았다.

## 검증 (문서 전용)

- README의 상대 링크가 모두 존재하는 파일을 가리키는지 스크립트로 확인했다(깨진 링크 0).
- 수치는 원본 기록과 대조했다: CHECK 6→66개·해시 일치(`docs/verification/production-postings-20261013/README.md`), 재빌드 없는 CD(`docs/engineering/ci-and-delivery.md`).
- 애플리케이션 코드 변경이 없으므로 애플리케이션 테스트를 실행한 것으로 표현하지 않는다. #24·#25가 머지된 뒤 이 PR을 머지해야 표의 "수정" 설명이 main과 일치한다.
