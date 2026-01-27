-- V005: users と refresh_tokens テーブルに version カラム追加
--
-- Spring Data JDBC の楽観的ロック対応のため、
-- @Version フィールド用の version カラムを追加します。
-- version が NULL の場合は新規エンティティとして扱われます。

-- ==============================================
-- 1. users テーブルに version カラム追加
-- ==============================================
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0;

-- 既存レコードに初期値を設定
UPDATE users SET version = 0 WHERE version IS NULL;

-- NOT NULL 制約を追加
ALTER TABLE users ALTER COLUMN version SET NOT NULL;

-- ==============================================
-- 2. refresh_tokens テーブルに version カラム追加
-- ==============================================
ALTER TABLE refresh_tokens ADD COLUMN version BIGINT DEFAULT 0;

-- 既存レコードに初期値を設定
UPDATE refresh_tokens SET version = 0 WHERE version IS NULL;

-- NOT NULL 制約を追加
ALTER TABLE refresh_tokens ALTER COLUMN version SET NOT NULL;

-- ==============================================
-- コメント追加
-- ==============================================
COMMENT ON COLUMN users.version IS '楽観的ロック用バージョン番号';
COMMENT ON COLUMN refresh_tokens.version IS '楽観的ロック用バージョン番号';
