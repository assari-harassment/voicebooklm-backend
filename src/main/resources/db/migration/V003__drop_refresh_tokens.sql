-- Firebase Auth 完全移行に伴い、独自リフレッシュトークンテーブルを削除
-- クライアント側で Firebase SDK がトークンリフレッシュを自動処理するため、
-- サーバー側でのリフレッシュトークン管理は不要

DROP INDEX IF EXISTS idx_refresh_tokens_user;
DROP INDEX IF EXISTS idx_refresh_tokens_valid;
DROP TABLE IF EXISTS refresh_tokens;
