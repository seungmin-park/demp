-- Preserve existing published rows; new HTTP requests default to DRAFT in the application.
ALTER TABLE announcement ADD COLUMN publication_status VARCHAR(255) NOT NULL DEFAULT 'PUBLISHED';
ALTER TABLE announcement ADD COLUMN source_name VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN source_identifier VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN application_url VARCHAR(2048) NULL;
ALTER TABLE announcement ADD COLUMN source_verified_at TIMESTAMP NULL;
CREATE TABLE announcement_revision (
    announcement_id BIGINT NOT NULL,
    revision_order INTEGER NOT NULL,
    actor VARCHAR(255), changed_at TIMESTAMP, status VARCHAR(255), title VARCHAR(255), source_url VARCHAR(2048),
    PRIMARY KEY (announcement_id, revision_order),
    FOREIGN KEY (announcement_id) REFERENCES announcement(id)
);
-- NULL identity keeps legacy duplicates intact. The application checks legacy rows on registration.
ALTER TABLE announcement ADD COLUMN recruitment_audience VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN cohort VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN stipend_amount INTEGER NULL;
ALTER TABLE announcement ADD COLUMN stipend_note VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN duplicate_key VARCHAR(64) NULL;
ALTER TABLE announcement ADD CONSTRAINT uk_announcement_source UNIQUE (duplicate_key);
ALTER TABLE announcement ADD COLUMN recruitment_closed BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE announcement_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, announcement_id BIGINT NOT NULL,
    message VARCHAR(1000) NOT NULL, reporter VARCHAR(255), created_at TIMESTAMP,
    resolution VARCHAR(1000), resolved_by VARCHAR(255), resolved_at TIMESTAMP,
    FOREIGN KEY (announcement_id) REFERENCES announcement(id) ON DELETE CASCADE
);
