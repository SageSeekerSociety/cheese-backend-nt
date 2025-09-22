-- Adds optional registration start timestamp for tasks so enrollment can be delayed.
ALTER TABLE task
    ADD COLUMN IF NOT EXISTS registration_start_at TIMESTAMP(6);
