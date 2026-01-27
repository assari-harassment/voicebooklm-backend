-- V009: memos テーブルに version カラム追加
--
-- Spring Data JDBC の楽観的ロック対応のため、
-- @Version フィールド用の version カラムを追加します。

ALTER TABLE memos ADD COLUMN version BIGINT DEFAULT 0;

-- 既存レコードに初期値を設定
UPDATE memos SET version = 0 WHERE version IS NULL;

-- NOT NULL 制約を追加
ALTER TABLE memos ALTER COLUMN version SET NOT NULL;

COMMENT ON COLUMN memos.version IS '楽観的ロック用バージョン番号';
