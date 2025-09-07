-- Fix project table content column issue
-- Either drop the column if it exists, or make it nullable
DO $$ 
BEGIN
    -- Check if content column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'project' 
        AND column_name = 'content'
        AND table_schema = 'public'
    ) THEN
        -- Make it nullable with empty string default
        ALTER TABLE project ALTER COLUMN content DROP NOT NULL;
        ALTER TABLE project ALTER COLUMN content SET DEFAULT '';
    END IF;
END $$;