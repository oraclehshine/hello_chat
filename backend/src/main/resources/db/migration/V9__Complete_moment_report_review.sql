ALTER TABLE moment_reports ADD COLUMN status SMALLINT DEFAULT 1;
ALTER TABLE moment_reports ADD COLUMN handled_by BIGINT;
ALTER TABLE moment_reports ADD COLUMN handle_note VARCHAR(255);
ALTER TABLE moment_reports ADD COLUMN handled_at TIMESTAMP;

CREATE INDEX idx_moment_reports_status ON moment_reports(status);
CREATE INDEX idx_moment_reports_created_at ON moment_reports(created_at DESC);
