-- Expand the existing MySQL job_position CHECK without changing stored values.
-- Hibernate numbered its name by column order, so find the constraint by its clause.
SET @legacy_check = (SELECT tc.CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
        ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
    WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = 'announcement'
        AND tc.CONSTRAINT_TYPE = 'CHECK' AND cc.CHECK_CLAUSE LIKE '%`job_position`%');
SET @drop_legacy = IF(@legacy_check IS NULL, 'DO 0', CONCAT('ALTER TABLE announcement DROP CHECK `', @legacy_check, '`'));
PREPARE drop_legacy FROM @drop_legacy;
EXECUTE drop_legacy;
DEALLOCATE PREPARE drop_legacy;
ALTER TABLE announcement ADD CONSTRAINT announcement_job_position_chk CHECK (job_position IN ('BACKEND', 'FRONTEND', 'FULLSTACK', 'ANDROID', 'IOS', 'MACHINE_LEARNING', 'AI', 'DATA_ENGINEER', 'MOBILE_GAME', 'GAME_CLIENT', 'GAME_SERVER', 'SYSTEM_NETWORK', 'SYSTEM_SOFTWARE', 'DEVOPS', 'INTERNET_SECURITY', 'EMBEDDED_SOFTWARE', 'ROBOTIC_MIDDLEWARE', 'QA', 'IOT', 'APPLICATION_PROGRAM', 'BLOCK_CHAIN', 'DATA_SCIENTIST', 'DATA_ANALYST', 'DATABASE_ADMIN', 'CLOUD_ENGINEER', 'PLATFORM_ENGINEER', 'SRE', 'MLOPS', 'WEB_PUBLISHER', 'GAME_ENGINE', 'AI_RESEARCH'));
