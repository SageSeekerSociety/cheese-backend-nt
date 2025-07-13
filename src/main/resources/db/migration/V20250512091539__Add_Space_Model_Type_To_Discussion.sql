-- V20250512091539__Add_Space_Model_Type_To_Discussion.sql

ALTER TABLE public.discussion
    DROP CONSTRAINT IF EXISTS discussion_model_type_check;

ALTER TABLE public.discussion
    ADD CONSTRAINT discussion_model_type_check
        CHECK (((model_type)::text = ANY (ARRAY['PROJECT'::text, 'SPACE'::text])));