-- V20250925132923__Add_description_json.sql
-- =========================================================
-- Migration: Add generated JSONB column for rich text
-- Purpose : Make JSON content indexable by pg_search (json_fields)
-- =========================================================

ALTER TABLE task
    ADD COLUMN description_json JSONB
        GENERATED ALWAYS AS (description::jsonb) STORED;
