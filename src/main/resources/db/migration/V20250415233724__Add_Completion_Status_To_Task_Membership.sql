-- V20250415233724__Add_Completion_Status_To_Task_Membership.sql

-- Add the completion_status column to the task_membership table
-- Assuming TaskCompletionStatus enum values like 'NOT_SUBMITTED', 'PENDING_REVIEW', etc.
-- VARCHAR(50) should be sufficient for storing enum names.
-- Setting a NOT NULL constraint and a DEFAULT value is crucial for existing rows and future inserts.
ALTER TABLE task_membership
    ADD COLUMN completion_status VARCHAR(50) NOT NULL DEFAULT 'NOT_SUBMITTED';

-- Create an index on the new column if frequent querying by status is expected
CREATE INDEX idx_task_membership_completion_status ON task_membership (completion_status);

-- Optional: Add comments for clarity in database tools
-- Adjust syntax based on your specific database (e.g., PostgreSQL, MySQL)
-- COMMENT ON COLUMN task_membership.completion_status IS 'Tracks the final completion state of the task participation (e.g., SUCCESS, FAILED, PENDING_REVIEW).';