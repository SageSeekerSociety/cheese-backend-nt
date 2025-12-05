-- V20251205173538_Add_Trgm_Index_To_Topic.sql

-- 1. 开启 pg_trgm 扩展 (如果还没开启)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. 给 Topic 的 name 字段创建 GIN 索引
--    gin_trgm_ops 专门用于优化 LIKE '%...%' 和正则表达式查询
CREATE INDEX IF NOT EXISTS idx_topic_name_trgm
    ON public.topic
    USING gin (name gin_trgm_ops);
