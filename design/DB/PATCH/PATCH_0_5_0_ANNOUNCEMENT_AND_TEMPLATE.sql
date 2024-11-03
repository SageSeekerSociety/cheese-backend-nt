-- PostgreSQL Patch Script for Cheese Backend NT (0.5.0)
-- Backup Before Executing This in Production Database

-- Description:
-- Replace all invalid announcements and templates with "[]"

-- This script can be executed several times safely without any side effects.

UPDATE SPACE
    SET announcements = '[]'
    WHERE announcements NOT LIKE '[%]';

UPDATE SPACE
    SET task_templates = '[]'
    WHERE task_templates NOT LIKE '[%]';
