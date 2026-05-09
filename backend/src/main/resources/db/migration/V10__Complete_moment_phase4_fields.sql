ALTER TABLE moments ADD COLUMN tags VARCHAR(512);
ALTER TABLE moments ADD COLUMN mood VARCHAR(32);
ALTER TABLE moments ADD COLUMN activity VARCHAR(32);
ALTER TABLE moments ADD COLUMN audit_status VARCHAR(16) DEFAULT 'approved';
ALTER TABLE moments ADD COLUMN audit_reason VARCHAR(255);

CREATE INDEX idx_moments_audit_status ON moments(audit_status);
