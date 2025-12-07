-- V001: 初期スキーマ作成
--
-- このマイグレーションは以下を実装します:
-- - users テーブル作成
-- - memos テーブル作成
-- - memo_tags テーブル作成
-- - refresh_tokens テーブル作成
-- - 必要なインデックスの作成
--
-- NOTE: pg_bigm 日本語全文検索は本番環境 (AWS RDS) のみ有効化
-- ローカル環境では LIKE 検索を使用し、本番では V002__enable_pg_bigm.sql で有効化

-- ==============================================
-- pg_bigm 拡張は本番環境 (AWS RDS) のみ有効化
-- ローカルでは LIKE 検索を使用
-- ==============================================
-- CREATE EXTENSION IF NOT EXISTS pg_bigm;  -- 本番のみ

-- ==============================================
-- 2. users テーブル作成
-- ==============================================
CREATE TABLE users (
    id UUID PRIMARY KEY,  -- アプリケーション側で UUIDv7 を生成
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    picture_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_unique UNIQUE (email)
);

-- email カラムにユニークインデックス（高速検索用）
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- ==============================================
-- 3. memos テーブル作成
-- ==============================================
CREATE TABLE memos (
    id UUID PRIMARY KEY,  -- アプリケーション側で UUIDv7 を生成
    user_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    recording_start_time TIMESTAMPTZ NOT NULL,
    recording_end_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_memos_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ユーザー別メモ取得用インデックス（deleted = FALSE フィルタ、作成日降順）
CREATE INDEX idx_memos_user_created ON memos(user_id, created_at DESC) WHERE deleted = FALSE;

-- タイトル検索用インデックス（MVP: LIKE 検索用）
CREATE INDEX idx_memos_title ON memos(title);

-- 本番環境では pg_bigm GIN インデックスを作成 (V002__enable_pg_bigm.sql)
-- CREATE INDEX idx_memos_bigm_title ON memos USING gin (title gin_bigm_ops);
-- CREATE INDEX idx_memos_bigm_content ON memos USING gin (content gin_bigm_ops);

-- ==============================================
-- 4. memo_tags テーブル作成
-- ==============================================
CREATE TABLE memo_tags (
    memo_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (memo_id, tag),
    CONSTRAINT fk_memo_tags_memo_id FOREIGN KEY (memo_id) REFERENCES memos(id) ON DELETE CASCADE
);

-- タグ検索用インデックス
CREATE INDEX idx_memo_tags_tag ON memo_tags(tag);

-- ==============================================
-- 5. refresh_tokens テーブル作成
-- ==============================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,  -- アプリケーション側で UUIDv7 を生成
    token VARCHAR(500) NOT NULL,
    family_id UUID NOT NULL,
    user_id UUID NOT NULL,
    device_name VARCHAR(255),
    user_agent VARCHAR(500),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT refresh_tokens_token_unique UNIQUE (token)
);

-- リフレッシュトークン検証用インデックス（token + revoked + expires_at）
CREATE INDEX idx_refresh_tokens_valid ON refresh_tokens(token, revoked, expires_at) WHERE revoked = FALSE;

-- Token Family 検索用インデックス（リユース検出用）
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id) WHERE revoked = FALSE;

-- ==============================================
-- コメント追加（テーブル・カラムの説明）
-- ==============================================
COMMENT ON TABLE users IS 'ユーザーアカウント情報（Google OAuth 認証）';
COMMENT ON COLUMN users.id IS 'ユーザー ID (UUID v4)';
COMMENT ON COLUMN users.email IS 'メールアドレス（Google アカウント）';
COMMENT ON COLUMN users.picture_url IS 'プロフィール画像 URL（Google アカウント）';

COMMENT ON TABLE memos IS 'AI 整形済みボイスメモ';
COMMENT ON COLUMN memos.id IS 'メモ ID (UUID v4)';
COMMENT ON COLUMN memos.user_id IS 'ユーザー ID（外部キー）';
COMMENT ON COLUMN memos.title IS 'AI 生成タイトル';
COMMENT ON COLUMN memos.content IS 'AI 整形済み本文（Markdown 形式）';
COMMENT ON COLUMN memos.recording_start_time IS '録音開始時刻';
COMMENT ON COLUMN memos.recording_end_time IS '録音終了時刻';
COMMENT ON COLUMN memos.deleted IS '論理削除フラグ';
COMMENT ON COLUMN memos.deleted_at IS '論理削除日時';

COMMENT ON TABLE memo_tags IS 'メモタグ（AI 生成またはユーザー編集）';

COMMENT ON TABLE refresh_tokens IS 'JWT リフレッシュトークン（トークンローテーション対応）';
COMMENT ON COLUMN refresh_tokens.family_id IS 'Token Family ID（リユース検出用）';
COMMENT ON COLUMN refresh_tokens.device_name IS 'デバイス名（オプション）';
COMMENT ON COLUMN refresh_tokens.user_agent IS 'User-Agent（オプション）';
COMMENT ON COLUMN refresh_tokens.revoked IS '無効化フラグ';
