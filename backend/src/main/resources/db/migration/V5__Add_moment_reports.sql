CREATE TABLE IF NOT EXISTS moment_reports (
    id BIGSERIAL PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (moment_id) REFERENCES moments(id),
    FOREIGN KEY (reporter_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_moment_reports_moment_id ON moment_reports(moment_id);
CREATE INDEX IF NOT EXISTS idx_moment_reports_reporter_id ON moment_reports(reporter_id);
