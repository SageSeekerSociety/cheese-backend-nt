-- Flyway migration script
-- Author: HuanCheng65
-- Date: 2025-03-29
-- Description: Add minimum and maximum team size limits to the task table.
-- These columns are nullable and only relevant when task.submitter_type = 'TEAM'.

-- Add min_team_size column to the task table
-- This column stores the minimum number of members required for a team.
-- It's nullable because it's only applicable for team tasks.
ALTER TABLE task
    ADD COLUMN min_team_size INTEGER NULL;

-- Add max_team_size column to the task table
-- This column stores the maximum number of members allowed for a team.
-- It's nullable because it's only applicable for team tasks.
ALTER TABLE task
    ADD COLUMN max_team_size INTEGER NULL;