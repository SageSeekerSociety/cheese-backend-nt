-- PostgreSQL Migration Script for Cheese Backend NT (0.3.x -> 0.4.0)
-- Backup Before Executing This in Production Database

ALTER TABLE space
    ADD COLUMN announcements TEXT NOT NULL DEFAULT '{}';
ALTER TABLE space
    ADD COLUMN task_templates TEXT NOT NULL DEFAULT '{}';

ALTER TABLE task
    ALTER COLUMN deadline DROP NOT NULL;

ALTER TABLE task_membership
    ADD COLUMN approved BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE task_membership
    ADD COLUMN deadline TIMESTAMP(6);

