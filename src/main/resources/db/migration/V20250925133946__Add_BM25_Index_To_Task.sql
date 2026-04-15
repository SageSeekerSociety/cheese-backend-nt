-- V20250925133946__Add_BM25_Index_To_Task.sql
-- =========================================================
-- Migration: Create BM25 index for full-text & filters on `task`
-- Why      : Replace ES with ParadeDB(pg_search), enable Chinese search
-- Fields   : name/intro for text search;
--            bool/datetime/ids for pushdown filtering
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_task_search
    ON public.task
    USING bm25 (
    id,                 -- key_field (must be first & unique)
    name,
    intro,
    space_id, category_id,
    require_real_name, resubmittable, editable,
    registration_start_at, deadline, approved, deleted_at
    )
WITH (
    key_field='id',
    text_fields='{
            "name":  {"tokenizer": {"type":"jieba"}, "record":"position"},
            "intro": {"tokenizer": {"type":"jieba"}, "record":"position"},
            "name_ngram":  {"column":"name",
                            "tokenizer":{"type":"ngram","min_gram":2,"max_gram":3,"prefix_only":false}},
            "intro_ngram": {"column":"intro",
                            "tokenizer":{"type":"ngram","min_gram":2,"max_gram":3,"prefix_only":false}}
        }'
)
    WHERE deleted_at IS NULL;
