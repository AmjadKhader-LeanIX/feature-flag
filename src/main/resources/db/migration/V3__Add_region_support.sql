ALTER TABLE feature_flag
ADD COLUMN region VARCHAR(50) NOT NULL DEFAULT 'ALL';

ALTER TABLE workspaces
ADD COLUMN region VARCHAR(50);

CREATE INDEX idx_feature_flag_region ON feature_flag(region);
CREATE INDEX idx_workspaces_region ON workspaces(region);
