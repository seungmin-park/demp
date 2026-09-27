-- MySQL production: apply once before deployment; preserves existing amounts.
ALTER TABLE announcement MODIFY COLUMN payment INTEGER NULL;
ALTER TABLE announcement ADD COLUMN salary_status VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN salary_max INTEGER NULL;
