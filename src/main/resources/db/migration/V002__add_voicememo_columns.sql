-- V002: VoiceMemo 集約対応のカラム追加
--
-- 文字起こしとAI整形の詳細情報を永続化するためのカラムを追加

-- 言語コード（文字起こし言語）
ALTER TABLE memos ADD COLUMN IF NOT EXISTS language_code VARCHAR(10) DEFAULT 'ja-JP';

-- フォールバック使用フラグ
ALTER TABLE memos ADD COLUMN IF NOT EXISTS transcription_fallback_used BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE memos ADD COLUMN IF NOT EXISTS formatting_fallback_used BOOLEAN NOT NULL DEFAULT FALSE;

-- コメント追加
COMMENT ON COLUMN memos.language_code IS '文字起こし言語コード (例: ja-JP)';
COMMENT ON COLUMN memos.transcription_fallback_used IS '文字起こしでフォールバックが使用されたか';
COMMENT ON COLUMN memos.formatting_fallback_used IS 'AI整形でフォールバックが使用されたか';
