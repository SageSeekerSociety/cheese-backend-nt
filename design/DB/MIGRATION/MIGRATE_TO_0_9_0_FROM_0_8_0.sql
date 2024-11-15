-- PostgreSQL Migration Script for Cheese Backend NT (0.8.0 -> 0.9.0)
-- Backup Before Executing This in Production Database

ALTER TABLE task
ADD COLUMN participant_limit INTEGER DEFAULT 2;

ALTER TABLE task_membership
ADD COLUMN apply_reason VARCHAR(255) DEFAULT '',
ADD COLUMN class_name VARCHAR(255) DEFAULT '',
ADD COLUMN email VARCHAR(255) DEFAULT '',
ADD COLUMN grade VARCHAR(255) DEFAULT '',
ADD COLUMN major VARCHAR(255) DEFAULT '',
ADD COLUMN personal_advantage VARCHAR(255) DEFAULT '',
ADD COLUMN phone VARCHAR(255) DEFAULT '',
ADD COLUMN real_name VARCHAR(255) DEFAULT '',
ADD COLUMN remark VARCHAR(255) DEFAULT '',
ADD COLUMN student_id VARCHAR(255) DEFAULT '';
