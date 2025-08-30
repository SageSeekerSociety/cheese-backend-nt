-- Flyway migration script
-- Version: V20250328233531
-- Purpose: Create space_categories, establish default categories, link spaces,
--          remove obsolete team link from tasks, delete ALL tasks without spaces,
--          assign default categories to ALL remaining tasks, and enforce mandatory space/category links.

-- 1. Create Sequence for SpaceCategory IDs
CREATE SEQUENCE space_categories_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 2. Create the space_categories table
CREATE TABLE space_categories
(
    id            BIGINT                         NOT NULL,
    created_at    TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    deleted_at    TIMESTAMP(6) WITHOUT TIME ZONE,
    name          VARCHAR(255)                   NOT NULL,
    description   TEXT,
    display_order INTEGER                        NOT NULL DEFAULT 0,
    space_id      BIGINT                         NOT NULL,
    archived_at   TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_space_categories PRIMARY KEY (id)
);

-- 3. Add Foreign Key from space_categories to space table
ALTER TABLE space_categories
    ADD CONSTRAINT fk_spacecategories_on_space
        FOREIGN KEY (space_id) REFERENCES space (id);

-- 4. Add Unique Constraint for active category name within a space
CREATE UNIQUE INDEX idx_uq_spacecategories_space_name_active
    ON space_categories (space_id, name)
    WHERE deleted_at IS NULL AND archived_at IS NULL;

-- 5. Add index on space_id and archived_at for efficient querying
CREATE INDEX idx_spacecategories_spaceid ON space_categories (space_id);
CREATE INDEX idx_spacecategories_archived ON space_categories (archived_at);

-- 6. Remove the obsolete team_id column from the task table
ALTER TABLE task
    DROP COLUMN IF EXISTS team_id;

-- 7. Add default_category_id column to the 'space' table (initially nullable)
ALTER TABLE space
    ADD COLUMN default_category_id BIGINT;

-- 8. Create a default category ('General') for each existing *active* space
INSERT INTO space_categories (id, name, space_id, display_order, created_at, updated_at, description)
SELECT nextval('space_categories_seq'),
       'General',
       s.id,
       0,
       NOW(),
       NOW(),
       'Default category for general items.'
FROM space s
WHERE s.deleted_at IS NULL;

-- 9. Update the 'space' table to link to the newly created default category
UPDATE space s
SET default_category_id = (SELECT sc.id
                           FROM space_categories sc
                           WHERE sc.space_id = s.id
                             AND sc.name = 'General'
                             AND sc.deleted_at IS NULL
                           ORDER BY sc.created_at DESC
                           LIMIT 1)
WHERE s.deleted_at IS NULL
  AND s.default_category_id IS NULL;

-- 10. Add Foreign Key from space to its default category (ON DELETE RESTRICT)
ALTER TABLE space
    ADD CONSTRAINT fk_space_on_defaultcategory
        FOREIGN KEY (default_category_id) REFERENCES space_categories (id)
            ON DELETE RESTRICT;

-- 11. Make default_category_id mandatory in 'space' table *after* backfilling
-- ALTER TABLE space
--     ALTER COLUMN default_category_id SET NOT NULL;

-- 12. Add category_id column to the 'task' table (initially nullable)
ALTER TABLE task
    ADD COLUMN category_id BIGINT;

-- 13. !! CRITICAL DATA MODIFICATION !! Delete ALL tasks (active AND soft-deleted) that are not linked to any space.
-- This is necessary to allow making space_id NOT NULL for the entire table.
-- BACK UP YOUR DATABASE BEFORE APPLYING.
DELETE
FROM task t
WHERE t.space_id IS NULL;

-- 14. Backfill category_id for ALL remaining tasks (active AND soft-deleted)
-- Assign the default category of their space. This ensures category_id can become NOT NULL.
UPDATE task t
SET category_id = (SELECT s.default_category_id
                   FROM space s
                   WHERE s.id = t.space_id)
WHERE t.category_id IS NULL;
-- Update all rows where category_id is currently null (regardless of deleted_at)
-- AND t.space_id IS NOT NULL; -- This condition is implicitly true now due to step 13

-- 15. Make space_id mandatory in tasks table
-- This should now succeed as all rows with space_id IS NULL were deleted in step 13.
ALTER TABLE task
    ALTER COLUMN space_id SET NOT NULL;

-- 16. Make category_id mandatory in tasks table *after* successful backfilling
-- This should now succeed as all remaining rows had their category_id backfilled in step 14.
ALTER TABLE task
    ALTER COLUMN category_id SET NOT NULL;

-- 17. Add Foreign Key from tasks to space_categories table (ON DELETE RESTRICT)
ALTER TABLE task
    ADD CONSTRAINT fk_tasks_on_category
        FOREIGN KEY (category_id) REFERENCES space_categories (id)
            ON DELETE RESTRICT;

-- 18. Add index on tasks.category_id for faster task lookup per category
CREATE INDEX idx_tasks_categoryid ON task (category_id);

-- End of corrected migration script