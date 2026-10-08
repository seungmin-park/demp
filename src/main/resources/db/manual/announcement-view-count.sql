-- Apply once before deploying a JAR with announcement view counts.
-- Existing totals are unknown; start collecting from zero without estimating history.
ALTER TABLE announcement ADD COLUMN hits BIGINT NOT NULL DEFAULT 0;
