-- V006: folders テーブル追加と memos.folder_id カラム追加
--
-- メモにフォルダー属性を追加し、AI整形時に自動的にフォルダー分類を行う機能の基盤
-- #44: memosにfolder属性追加

-- ==============================================
-- 1. folders テーブル作成
-- ==============================================
CREATE TABLE folders (
    id UUID PRIMARY KEY,  -- アプリケーション側で UUIDv7 を生成
    user_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    parent_id UUID,  -- NULL許可（ルートフォルダーの場合）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_folders_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_folders_parent_id FOREIGN KEY (parent_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT folders_unique_name UNIQUE (user_id, parent_id, name)
);

-- ユーザーのフォルダー一覧取得用インデックス
CREATE INDEX idx_folders_user ON folders(user_id);

-- 子フォルダー取得用インデックス
CREATE INDEX idx_folders_parent ON folders(parent_id);

-- ==============================================
-- 2. memos テーブルに folder_id カラム追加
-- ==============================================
ALTER TABLE memos ADD COLUMN IF NOT EXISTS folder_id UUID;

-- 外部キー制約（フォルダー削除時は未分類に変更）
ALTER TABLE memos ADD CONSTRAINT fk_memos_folder_id
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL;

-- フォルダー別メモ取得用インデックス
CREATE INDEX idx_memos_folder ON memos(folder_id) WHERE folder_id IS NOT NULL;

-- ==============================================
-- コメント追加
-- ==============================================
COMMENT ON TABLE folders IS 'メモ分類用フォルダー（階層構造対応）';
COMMENT ON COLUMN folders.id IS 'フォルダー ID (UUIDv7)';
COMMENT ON COLUMN folders.user_id IS 'ユーザー ID（外部キー）';
COMMENT ON COLUMN folders.name IS 'フォルダー名（1〜50文字）';
COMMENT ON COLUMN folders.parent_id IS '親フォルダー ID（NULL = ルートフォルダー）';

COMMENT ON COLUMN memos.folder_id IS '所属フォルダー ID（NULL = 未分類）';
