-- Apply before deploying the bookmark feature; existing announcements/members are unchanged.
CREATE TABLE IF NOT EXISTS announcement_bookmark (
  id BIGINT NOT NULL AUTO_INCREMENT,
  member_id BIGINT NOT NULL,
  announcement_id BIGINT NOT NULL,
  saved_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_bookmark_member_announcement UNIQUE (member_id, announcement_id),
  INDEX idx_bookmark_member_order (member_id, id),
  CONSTRAINT fk_bookmark_member FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE CASCADE
);
-- No announcement FK: deleted postings remain unavailable saved entries until the owner removes them.
