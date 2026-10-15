-- Apply before the new JAR. NULL legacy rows are read as FIXED when a deadline exists, otherwise UNKNOWN.
-- Never convert a missing deadline to UNTIL_FILLED or ALWAYS_OPEN without source confirmation.
ALTER TABLE announcement ADD COLUMN deadline_mode VARCHAR(255) NULL;
