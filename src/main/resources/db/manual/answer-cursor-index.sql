-- MySQL 8+, inspect SHOW INDEX FROM answer and EXPLAIN before applying.
-- Apply once after backup only when no suitable question-leading index exists.
-- InnoDB secondary indexes already include the primary key; an existing
-- question_id index may support this ordering without another index.
-- ddl-auto=validate does not verify indexes. This file is never auto-applied.
CREATE INDEX idx_answer_question_cursor ON answer (question_id, answer_id);
