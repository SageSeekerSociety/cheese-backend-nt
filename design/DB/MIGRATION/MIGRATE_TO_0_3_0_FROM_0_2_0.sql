-- PostgreSQL Migration Script for Cheese Backend NT (0.2.0 -> 0.3.0)
-- Backup Before Excuting This in Production Database

ALTER TABLE TASK ADD COLUMN approved BOOLEAN NOT NULL DEFAULT FALSE;
