-- V20251015121500__Add_BM25_Index_To_Team.sql
-- =========================================================
-- Migration: Create BM25 index for team search (ParadeDB)
-- Why      : Move team search off Elasticsearch and enable pg_search usage
-- Fields   : name/intro/description for text relevance; updated_at/deleted_at for filtering
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_team_search
    ON public.team
    USING bm25 (
        id,
        name,
        intro,
        updated_at,
        deleted_at
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
