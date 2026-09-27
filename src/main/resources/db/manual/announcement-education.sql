-- Apply once before education discovery deployment. Unknown education data remains NULL.
ALTER TABLE announcement ADD COLUMN delivery_mode VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN region VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN commitment VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN funding_type VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN selection_process VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN learning_level VARCHAR(255) NULL;
ALTER TABLE announcement ADD COLUMN learning_start_date DATE NULL;
ALTER TABLE announcement ADD COLUMN learning_end_date DATE NULL;
ALTER TABLE announcement ADD COLUMN duration_days INTEGER NULL;
