-- Keep URL uniqueness and add stable company/cohort/original-posting identity.
-- Existing rows remain NULL and are checked by the application until reviewed.
-- Do not blindly backfill: mirrors may already exist and require operator review.
ALTER TABLE announcement ADD COLUMN source_identity_key VARCHAR(64) NULL;
ALTER TABLE announcement ADD CONSTRAINT uk_announcement_source_identity UNIQUE (source_identity_key);
