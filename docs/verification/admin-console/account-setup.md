# 관리자 계정 준비

공개 회원가입은 항상 ROLE_USER로 저장한다. 관리자 생성 API와 기본 운영 비밀번호는 없다. 로그인 토큰의 roles나 localStorage는 권한 근거가 아니다. 매 API 요청은 회원 ID로 DB 역할을 다시 읽는다.

운영 DB 담당자가 본인이 확인한 기존 회원 ID에만 아래 작업을 트랜잭션으로 수행한다. 백업·접속 대상·회원 ID 확인 후 실행하고 변경 기록을 남긴다. 이 문서는 SQL을 실행하지 않는다.

```sql
-- :member_id는 확인한 기존 회원 ID로 바인딩한다.
SELECT member_id, username FROM member WHERE member_id = :member_id;
INSERT INTO member_roles (member_member_id, roles)
SELECT :member_id, 'ROLE_ADMIN'
WHERE EXISTS (SELECT 1 FROM member WHERE member_id = :member_id)
  AND NOT EXISTS (SELECT 1 FROM member_roles WHERE member_member_id = :member_id AND roles = 'ROLE_ADMIN');
-- 회수
DELETE FROM member_roles WHERE member_member_id = :member_id AND roles = 'ROLE_ADMIN';
```

실제 배포 스키마의 컬럼명을 확인한다. 현재 Hibernate 생성 스키마는 `member_roles.member_member_id`다. 기존 ROLE_USER는 유지하여 일반 사용자 기능도 사용할 수 있게 한다. `/admin` 접속 후 `GET /api/admin/me` 성공을 확인한다. 회수는 토큰 재발급 없이 다음 요청부터 403, 계정 삭제/만료는 401이다.

로컬 E2E는 격리된 H2와 별도 SQL fixture를 사용한다. 운영 계정·운영 DB에 역할을 부여하지 않는다.
