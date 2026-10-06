-- Hibernate Instant uses UTC Calendar for these values; DATETIME avoids MySQL session time_zone conversion.
-- Verify this encoding contract on framework/driver upgrades.
-- Apply once to a backed-up existing database before deploying the new backend.
ALTER TABLE member ADD COLUMN failed_login_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN login_failure_window_started_at DATETIME(6) NULL;
ALTER TABLE member ADD COLUMN login_blocked_until DATETIME(6) NULL;
