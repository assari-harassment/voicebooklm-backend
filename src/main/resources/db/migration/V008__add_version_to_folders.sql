-- V008: folders テーブルに version カラム追加
--
-- Spring Data JDBC の楽観的ロック対応のため、
-- @Version フィールド用の version カラムを追加します。

ALTER TABLE folders ADD COLUMN version BIGINT DEFAULT 0;

-- 既存レコードに初期値を設定
UPDATE folders SET version = 0 WHERE version IS NULL;

-- NOT NULL 制約を追加
ALTER TABLE folders ALTER COLUMN version SET NOT NULL;

COMMENT ON COLUMN folders.version IS '楽観的ロック用バージョン番号';
