-- Additive migration; back up and restore-rehearse before production application.
ALTER TABLE member ADD COLUMN nickname VARCHAR(30) NULL;
ALTER TABLE member ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0;
