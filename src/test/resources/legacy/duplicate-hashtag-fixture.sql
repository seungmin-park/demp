-- Legacy rows written before T31: the same tag name stored twice and linked to one question.
INSERT INTO member (member_id, password, username) VALUES (1200, 'legacy-hash', 'legacy-writer');
INSERT INTO question (question_id, content, created_date, dislike, hits, recommend, title, member_id)
VALUES (1300, 'legacy content', CURRENT_TIMESTAMP, 0, 0, 0, 'legacy question', 1200);
INSERT INTO hashtag (hashtag_id, tag_name) VALUES (1100, 'legacy-duplicate');
INSERT INTO hashtag (hashtag_id, tag_name) VALUES (1101, 'legacy-duplicate');
INSERT INTO question_hashtag (id, hashtag_id, question_id) VALUES (1400, 1100, 1300);
INSERT INTO question_hashtag (id, hashtag_id, question_id) VALUES (1401, 1101, 1300);
