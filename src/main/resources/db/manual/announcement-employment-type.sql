-- Keep existing employment arrangements unknown; apply once before the new JAR.
ALTER TABLE announcement ADD COLUMN employment_type VARCHAR(255) NULL;
