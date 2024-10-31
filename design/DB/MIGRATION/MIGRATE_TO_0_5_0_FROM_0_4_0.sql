-- PostgreSQL Migration Script for Cheese Backend NT (0.4.0 -> 0.5.0)
-- Backup Before Executing This in Production Database

ALTER TABLE task
    ADD COLUMN default_deadline BIGINT;

