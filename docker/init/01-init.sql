-- VoiceBook LM データベース初期化スクリプト
-- PostgreSQL 起動時に自動実行されます

-- 拡張機能の有効化
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- UUID生成用

-- タイムゾーンの設定確認
SELECT current_setting('TIMEZONE');

-- データベース情報の表示
SELECT version();
