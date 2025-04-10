ALTER TABLE task
    ADD team_locking_policy VARCHAR(50) DEFAULT 'NO_LOCK';

ALTER TABLE task
    ALTER COLUMN team_locking_policy SET NOT NULL;