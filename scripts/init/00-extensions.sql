-- 安装向量检索：扩展名是 "vector"（不是 "pgvector"）
CREATE EXTENSION IF NOT EXISTS vector;         -- pgvector
-- 安装 ParadeDB 的全文检索
CREATE EXTENSION IF NOT EXISTS pg_search;      -- ParadeDB/pg_search
