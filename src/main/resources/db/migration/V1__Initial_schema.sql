CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE feature_flag (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    team VARCHAR(255) NOT NULL,
    rollout_percentage INTEGER NOT NULL CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100)
);

CREATE TABLE workspace_feature_flag (
    id UUID PRIMARY KEY,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    feature_flag_id UUID NOT NULL REFERENCES feature_flag(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_workspaces_name ON workspaces(name);
CREATE INDEX idx_feature_flag_name ON feature_flag(name);
CREATE INDEX idx_feature_flag_team ON feature_flag(team);

CREATE INDEX idx_workspace_feature_flag_feature_flag_id ON workspace_feature_flag(feature_flag_id);
