-- V010: tags マスタテーブル追加と memo_tags テーブル変更
--
-- タグを独立した集約（マスタ）として管理するための変更
-- - tags マスタテーブルを新規作成
-- - 既存の memo_tags データを tags マスタに移行
-- - memo_tags テーブルを tag 文字列から tag_id 参照に変更

-- ==============================================
-- 0. UUIDv7 生成用関数を作成
-- ==============================================
-- pgcrypto拡張を有効化（gen_random_bytes関数に必要）
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- PostgreSQLにはビルトインのUUIDv7生成関数がないため、カスタム関数を作成
CREATE OR REPLACE FUNCTION generate_uuidv7()
RETURNS UUID AS $$
DECLARE
    unix_ts_ms BIGINT;
    uuid_bytes BYTEA;
BEGIN
    -- 現在のUnixタイムスタンプ（ミリ秒）を取得
    unix_ts_ms := (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT;

    -- 16バイトのランダムデータを生成
    uuid_bytes := gen_random_bytes(16);

    -- 最初の6バイトをタイムスタンプで上書き（BIGINTでビット演算後にINTへキャスト）
    uuid_bytes := set_byte(uuid_bytes, 0, ((unix_ts_ms >> 40) & 255)::INT);
    uuid_bytes := set_byte(uuid_bytes, 1, ((unix_ts_ms >> 32) & 255)::INT);
    uuid_bytes := set_byte(uuid_bytes, 2, ((unix_ts_ms >> 24) & 255)::INT);
    uuid_bytes := set_byte(uuid_bytes, 3, ((unix_ts_ms >> 16) & 255)::INT);
    uuid_bytes := set_byte(uuid_bytes, 4, ((unix_ts_ms >> 8) & 255)::INT);
    uuid_bytes := set_byte(uuid_bytes, 5, (unix_ts_ms & 255)::INT);

    -- バージョン7を設定（7バイト目の上位4ビット）
    uuid_bytes := set_byte(uuid_bytes, 6, (get_byte(uuid_bytes, 6) & 15) | 112);

    -- バリアント（RFC 4122）を設定（9バイト目の上位2ビット）
    uuid_bytes := set_byte(uuid_bytes, 8, (get_byte(uuid_bytes, 8) & 63) | 128);

    RETURN encode(uuid_bytes, 'hex')::UUID;
END;
$$ LANGUAGE plpgsql;

-- ==============================================
-- 1. tags マスタテーブル作成
-- ==============================================
CREATE TABLE tags (
    id UUID PRIMARY KEY,  -- アプリケーション側で UUIDv7 を生成
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,  -- 楽観的ロック用
    CONSTRAINT fk_tags_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT tags_user_name_unique UNIQUE (user_id, name)
);

-- ユーザーのタグ一覧取得用インデックス
CREATE INDEX idx_tags_user ON tags(user_id);

-- タグ名検索用インデックス
CREATE INDEX idx_tags_user_name ON tags(user_id, name);

-- ==============================================
-- 2. 既存データを tags マスタに移行
-- ==============================================
INSERT INTO tags (id, user_id, name, created_at, updated_at)
SELECT DISTINCT
    generate_uuidv7() AS id,
    m.user_id,
    mt.tag AS name,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM memo_tags mt
INNER JOIN memos m ON mt.memo_id = m.id
ON CONFLICT (user_id, name) DO NOTHING;

-- ==============================================
-- 3. memo_tags テーブル変更（tag 文字列 → tag_id 参照）
-- ==============================================

-- 3.1. 既存の制約を削除
ALTER TABLE memo_tags DROP CONSTRAINT IF EXISTS memo_tags_memo_id_tag_unique;

-- 3.2. tag_id カラムを追加
ALTER TABLE memo_tags ADD COLUMN tag_id UUID;

-- 3.3. 既存データの tag_id を設定
UPDATE memo_tags mt
SET tag_id = t.id
FROM memos m, tags t
WHERE mt.memo_id = m.id
  AND m.user_id = t.user_id
  AND mt.tag = t.name;

-- 3.4. tag カラムを削除
ALTER TABLE memo_tags DROP COLUMN tag;

-- 3.5. tag_id を NOT NULL に設定
ALTER TABLE memo_tags ALTER COLUMN tag_id SET NOT NULL;

-- 3.6. 新しい外部キー制約を追加
ALTER TABLE memo_tags ADD CONSTRAINT fk_memo_tags_tag_id
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE;

-- 3.7. 新しい一意制約を追加（同じメモに同じタグを複数回付けられないように）
ALTER TABLE memo_tags ADD CONSTRAINT memo_tags_memo_id_tag_id_unique
    UNIQUE (memo_id, tag_id);

-- ==============================================
-- コメント追加
-- ==============================================
COMMENT ON TABLE tags IS 'タグマスタ（ユーザーごとに一意）';
COMMENT ON COLUMN tags.id IS 'タグ ID (UUIDv7)';
COMMENT ON COLUMN tags.user_id IS 'ユーザー ID（外部キー）';
COMMENT ON COLUMN tags.name IS 'タグ名（1〜100文字）';
COMMENT ON COLUMN memo_tags.tag_id IS 'タグ ID（外部キー）';
