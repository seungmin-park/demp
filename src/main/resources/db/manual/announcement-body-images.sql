-- Apply once before deploying body-image support. Existing announcement rows are unchanged.
CREATE TABLE announcement_body_image (
    announcement_id BIGINT NOT NULL,
    upload_file_name VARCHAR(255),
    save_file_name VARCHAR(255),
    CONSTRAINT fk_announcement_body_image FOREIGN KEY (announcement_id) REFERENCES announcement(id)
);
CREATE INDEX idx_announcement_body_image_owner ON announcement_body_image(announcement_id);
