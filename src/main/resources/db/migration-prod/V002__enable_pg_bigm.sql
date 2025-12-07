-- V002: pg_bigm 拡張を有効化（本番環境 AWS RDS 専用）
--
-- このマイグレーションは AWS RDS for PostgreSQL でのみ実行されます。
-- ローカル開発環境では pg_bigm がインストールされていないため実行しないでください。
--
-- 適用方法:
--   1. AWS RDS に直接接続
--   2. Flyway の --target オプションを使用して V001 までを開発環境で適用
--   3. 本番環境でのみ V002 を適用

-- pg_bigm 拡張を有効化（AWS RDS には標準インストール済み）
CREATE EXTENSION IF NOT EXISTS pg_bigm;

-- 既存の簡易インデックスを削除
DROP INDEX IF EXISTS idx_memos_title;

-- pg_bigm 全文検索用 GIN インデックス（日本語検索高速化）
CREATE INDEX idx_memos_bigm_title ON memos USING gin (title gin_bigm_ops);
CREATE INDEX idx_memos_bigm_content ON memos USING gin (content gin_bigm_ops);

-- COMMENT 追加
COMMENT ON INDEX idx_memos_bigm_title IS 'pg_bigm 日本語全文検索インデックス (title)';
COMMENT ON INDEX idx_memos_bigm_content IS 'pg_bigm 日本語全文検索インデックス (content)';
