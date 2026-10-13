-- Existing amounts remain annual. Unknown facts remain NULL/empty; no guessed data.
ALTER TABLE announcement ADD COLUMN salary_period VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN work_period VARCHAR(1000) NULL;
ALTER TABLE announcement ADD COLUMN conversion_note VARCHAR(1000) NULL;
CREATE TABLE announcement_role (
    announcement_id BIGINT NOT NULL, role_order INTEGER NOT NULL,
    role_title VARCHAR(255), position VARCHAR(255), location VARCHAR(255),
    qualifications VARCHAR(2000), required_skills VARCHAR(2000), preferred_skills VARCHAR(2000), used_technologies VARCHAR(2000),
    PRIMARY KEY (announcement_id, role_order), FOREIGN KEY (announcement_id) REFERENCES announcement(id)
);
CREATE TABLE announcement_reference (
    announcement_id BIGINT NOT NULL, reference_order INTEGER NOT NULL, url VARCHAR(2048),
    PRIMARY KEY (announcement_id, reference_order), FOREIGN KEY (announcement_id) REFERENCES announcement(id)
);
