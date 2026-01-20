-- Drop regions column from feature_flag table
ALTER TABLE feature_flag DROP COLUMN IF EXISTS regions;
