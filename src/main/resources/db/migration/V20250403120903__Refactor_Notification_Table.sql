-- Flyway migration script V20250403120903__Refactor_Notification_Table.sql
-- Merges previous notification refactoring steps (V20250403021827, V202500403022600, V20250403120903).
-- Implements schema changes for event-driven system, aggregation, and frontend rendering.
-- Changes receiver_id to BIGINT, adds metadata, aggregation fields, versioning, indexes,
-- updates type constraint, and removes the original 'content' column.
-- NOTE: This script assumes the original table had 'content' JSONB and 'receiver_id' INT.
-- IMPORTANT: Run this merged script INSTEAD OF the individual scripts it replaces.

-- 1. Change receiver_id type from INT to BIGINT
-- Assumes the referenced column in the users table is already BIGINT or will be migrated first.
ALTER TABLE public.notification
    ALTER COLUMN receiver_id TYPE BIGINT;

-- 2. Rename the original 'content' column to 'metadata'
-- This preserves the original payload data needed for context.
ALTER TABLE public.notification
    RENAME COLUMN content TO metadata;

-- Update comment for the renamed column
COMMENT ON COLUMN public.notification.metadata IS 'Stores the raw payload/context (previously named ''content'') as JSONB for context, resolving IDs, aggregation, and client-side logic.';

-- 3. Add New Columns required for aggregation and locking
ALTER TABLE public.notification
    ADD COLUMN is_aggregatable BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN aggregation_key VARCHAR(255) NULL,
    ADD COLUMN aggregate_until TIMESTAMPTZ  NULL,
    ADD COLUMN finalized       BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN version         BIGINT       NOT NULL DEFAULT 0;

-- 4. Add New Indexes for improved query performance
-- Index for querying user notifications (covers filtering by read, sorting by date)
CREATE INDEX IF NOT EXISTS idx_notification_receiver_read_created -- Use IF NOT EXISTS for safety if rerunning locally
    ON public.notification (receiver_id, read, created_at DESC);

-- Index to efficiently find active aggregation records
-- Note: Dropping and recreating if structure/condition changes is safer than complex ALTER INDEX
DROP INDEX IF EXISTS idx_notification_aggregation;
CREATE INDEX idx_notification_aggregation
    ON public.notification (receiver_id, aggregation_key, aggregate_until)
    WHERE finalized = false AND is_aggregatable = true;
-- Partial index for active aggregations

-- 5. Update the CHECK constraint on the 'type' column for new enum values
-- Drop the existing constraint first (ensure name 'notification_type_check' is correct)
ALTER TABLE public.notification
    DROP CONSTRAINT IF EXISTS notification_type_check;

-- Add the new constraint with the full list of types
ALTER TABLE public.notification
    ADD CONSTRAINT notification_type_check CHECK (
        type::text = ANY (ARRAY [
            -- Existing types
            'MENTION',
            'REPLY',
            'REACTION',
            'PROJECT_INVITE',
            'DEADLINE_REMIND',
            -- New Team-related types
            'TEAM_JOIN_REQUEST',
            'TEAM_INVITATION',
            'TEAM_REQUEST_APPROVED',
            'TEAM_REQUEST_REJECTED',
            'TEAM_INVITATION_ACCEPTED',
            'TEAM_INVITATION_DECLINED',
            'TEAM_INVITATION_CANCELED',
            'TEAM_REQUEST_CANCELED'
            ]::text[])
        );
