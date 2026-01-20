-- Workspaces table
CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    region VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Feature flags table
CREATE TABLE feature_flag (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    team VARCHAR(255) NOT NULL,
    rollout_percentage INTEGER NOT NULL CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Workspace feature flag table
CREATE TABLE workspace_feature_flag (
    id UUID PRIMARY KEY,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    feature_flag_id UUID NOT NULL REFERENCES feature_flag(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit log table
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

-- Indexes for workspaces
CREATE INDEX idx_workspaces_name ON workspaces(name);
CREATE INDEX idx_workspaces_region ON workspaces(region);
CREATE INDEX idx_workspaces_region_created ON workspaces(region, created_at DESC);

-- Indexes for feature_flag
CREATE INDEX idx_feature_flag_name ON feature_flag(name);
CREATE INDEX idx_feature_flag_team ON feature_flag(team);

-- Indexes for workspace_feature_flag
CREATE INDEX idx_workspace_feature_flag_feature_flag_id ON workspace_feature_flag(feature_flag_id);
CREATE INDEX idx_workspace_feature_flag_workspace_enabled ON workspace_feature_flag(workspace_id, enabled);
CREATE INDEX idx_workspace_feature_flag_feature_enabled ON workspace_feature_flag(feature_flag_id, enabled);

-- Indexes for audit log
CREATE INDEX idx_audit_log_feature_flag_id ON feature_flag_audit_log(feature_flag_id);
CREATE INDEX idx_audit_log_timestamp ON feature_flag_audit_log(timestamp);
CREATE INDEX idx_audit_log_operation ON feature_flag_audit_log(operation);
CREATE INDEX idx_audit_log_team ON feature_flag_audit_log(team);
