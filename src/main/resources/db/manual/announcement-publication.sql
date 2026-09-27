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
