-- PostgreSQL Migration Script for Cheese Backend NT (0.6.0 -> 0.7.0)
-- Backup Before Executing This in Production Database

ALTER TABLE task
    ALTER COLUMN approved SET DATA TYPE SMALLINT USING
        CASE
            WHEN approved = TRUE THEN 0
            WHEN approved = FALSE THEN 1
            END;

ALTER TABLE task
    ALTER COLUMN approved SET NOT NULL;

ALTER TABLE task
    ADD CONSTRAINT approved_check CHECK (approved BETWEEN 0 AND 2);

UPDATE task
SET reject_reason = ''
WHERE reject_reason IS NULL;

ALTER TABLE task
    ALTER COLUMN reject_reason SET NOT NULL;

UPDATE task
SET approved =
        CASE
            WHEN approved = 0 THEN 0
            WHEN approved = 1 AND reject_reason != '' THEN 1
            ELSE 2
            END;




