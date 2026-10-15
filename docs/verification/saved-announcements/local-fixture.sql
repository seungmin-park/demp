INSERT INTO member_roles (member_member_id,roles) SELECT -1,'ROLE_ADMIN' WHERE NOT EXISTS (SELECT 1 FROM member_roles WHERE member_member_id=-1 AND roles='ROLE_ADMIN');
INSERT INTO announcement (id,title,announcement_type,min_career,max_career,name,content,payment,job_position,started_date,dead_line_date,deadline_mode,publication_status,recruitment_closed) VALUES
(501,'관심 공고 검증 · 정확한 마감','EMP',0,3,'검증 회사','<p>저장·재조회 검증을 위한 로컬 공고입니다.</p>',0,'BACKEND','2026-01-01 09:00:00','2030-10-26 17:00:00','FIXED','PUBLISHED',false),
(502,'관심 공고 검증 · 채용 시 마감','EMP',0,3,'검증 회사','<p>채용 종료 확인 전까지 모집합니다.</p>',0,'BACKEND','2026-01-01 09:00:00',null,'UNTIL_FILLED','PUBLISHED',false),
(503,'관심 공고 검증 · 상시 모집','EMP',0,3,'검증 회사','<p>상시 모집하는 로컬 검증 공고입니다.</p>',0,'BACKEND','2026-01-01 09:00:00',null,'ALWAYS_OPEN','PUBLISHED',false);

UPDATE announcement SET access_url=CONCAT('https://example.test/posting/',id) WHERE id IN (501,502,503);
