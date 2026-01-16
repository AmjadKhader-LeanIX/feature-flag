-- Add composite indexes for workspace and feature flag query optimization

-- Index for region filtering with pagination on workspaces table
-- Optimizes queries like: SELECT * FROM workspaces WHERE region = ? ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_workspaces_region_created
ON workspaces(region, created_at DESC);

-- Index for workspace lookups with enabled flag filtering
-- Optimizes queries like: SELECT * FROM workspace_feature_flag WHERE workspace_id = ? AND enabled = true
CREATE INDEX IF NOT EXISTS idx_workspace_feature_flag_workspace_enabled
ON workspace_feature_flag(workspace_id, enabled);

-- Index for feature flag lookups with enabled flag filtering
-- Optimizes queries like: SELECT * FROM workspace_feature_flag WHERE feature_flag_id = ? AND enabled = true
CREATE INDEX IF NOT EXISTS idx_workspace_feature_flag_feature_enabled
ON workspace_feature_flag(feature_flag_id, enabled);
