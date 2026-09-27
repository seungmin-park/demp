-- MySQL 8+, apply once after backup. Existing aggregate counts remain unchanged.
-- Rollback: deploy previous compatible app first, then DROP TABLE content_reaction.
CREATE TABLE content_reaction (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    question_id BIGINT NULL,
    answer_id BIGINT NULL,
    reaction VARCHAR(255) NOT NULL,
    CONSTRAINT uk_reaction_member_question UNIQUE (member_id, question_id),
    CONSTRAINT uk_reaction_member_answer UNIQUE (member_id, answer_id),
    CONSTRAINT fk_reaction_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT fk_reaction_question FOREIGN KEY (question_id) REFERENCES question(question_id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_answer FOREIGN KEY (answer_id) REFERENCES answer(answer_id) ON DELETE CASCADE,
    CONSTRAINT ck_reaction_target CHECK ((question_id IS NOT NULL AND answer_id IS NULL) OR (question_id IS NULL AND answer_id IS NOT NULL)),
    CONSTRAINT ck_reaction_value CHECK (reaction IN ('NONE', 'RECOMMEND', 'DISLIKE'))
);
