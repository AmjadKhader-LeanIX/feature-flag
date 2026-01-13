CREATE TABLE feature_flag_audit_log (
    id UUID PRIMARY KEY,
    feature_flag_id UUID,
    feature_flag_name VARCHAR(255) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    team VARCHAR(255) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    changed_by VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_feature_flag FOREIGN KEY (feature_flag_id) REFERENCES feature_flag(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_feature_flag_id ON feature_flag_audit_log(feature_flag_id);
CREATE INDEX idx_audit_log_timestamp ON feature_flag_audit_log(timestamp);
CREATE INDEX idx_audit_log_operation ON feature_flag_audit_log(operation);
CREATE INDEX idx_audit_log_team ON feature_flag_audit_log(team);
