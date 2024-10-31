-- PostgreSQL Migration Script for Cheese Backend NT (0.5.0 -> 0.6.0)
-- Backup Before Executing This in Production Database

ALTER TABLE task
    ADD COLUMN reject_reason VARCHAR(255);

ALTER TABLE task_membership
    DROP COLUMN approved;

ALTER TABLE task_membership
    ADD COLUMN approved SMALLINT NOT NULL CHECK(approved BETWEEN 0 AND 2) DEFAULT 2;
