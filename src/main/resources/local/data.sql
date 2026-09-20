-- Negative IDs reserve a separate seed namespace; generated application IDs start at 1.
-- This BCrypt hash is for local-only password "password".
INSERT INTO member (member_id, username, password)
SELECT -1, 'local-member', '$2a$10$oU7dWyYnUSLfcCU0.7Zffep0oc0YcId/ILnao2ahXC0wwD8Iphb4.'
WHERE NOT EXISTS (SELECT 1 FROM member WHERE member_id = -1);
INSERT INTO member_roles (member_member_id, roles)
SELECT -1, 'ROLE_USER' WHERE NOT EXISTS (SELECT 1 FROM member_roles WHERE member_member_id = -1 AND roles = 'ROLE_USER');
INSERT INTO announcement (id, title, announcement_type, min_career, max_career, name, content, payment, job_position, started_date, dead_line_date)
SELECT -1, '로컬 개발자 모집 예제', 'EMP', 0, 3, 'Local Company', '로컬 화면 확인용 공고입니다.', 0, 'BACKEND', '2026-01-01 00:00:00', '2030-12-31 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE id = -1);
INSERT INTO language (announcement_id, languages)
SELECT -1, 'JAVA' WHERE NOT EXISTS (SELECT 1 FROM language WHERE announcement_id = -1 AND languages = 'JAVA');
INSERT INTO question (question_id, title, content, created_date, hits, recommend, dislike, member_id)
SELECT -1, '로컬 질문 예제', '자바 학습 질문입니다.', '2026-01-01 00:00:00', 0, 0, 0, -1
WHERE NOT EXISTS (SELECT 1 FROM question WHERE question_id = -1);
INSERT INTO hashtag (hashtag_id, tag_name)
SELECT -1, 'java' WHERE NOT EXISTS (SELECT 1 FROM hashtag WHERE hashtag_id = -1);
INSERT INTO question_hashtag (id, hashtag_id, question_id)
SELECT -1, -1, -1 WHERE NOT EXISTS (SELECT 1 FROM question_hashtag WHERE id = -1);
INSERT INTO answer (answer_id, content, recommend, dislike, member_id, question_id)
SELECT -1, '로컬 답변 예제입니다.', 0, 0, -1, -1 WHERE NOT EXISTS (SELECT 1 FROM answer WHERE answer_id = -1);
