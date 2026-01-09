-- V011: tags テーブルに usage_count カラムを追加
--
-- タグの使用回数を非正規化して保持することで、
-- 人気タグ取得時のJOINを不要にする

-- ==============================================
-- 1. usage_count カラムを追加
-- ==============================================
ALTER TABLE tags ADD COLUMN usage_count INT NOT NULL DEFAULT 0;

-- ==============================================
-- 2. 既存データの usage_count を初期化
-- ==============================================
-- memo_tags から削除されていないメモに紐づくタグの使用回数を集計
UPDATE tags t
SET usage_count = (
    SELECT COUNT(*)
    FROM memo_tags mt
    INNER JOIN memos m ON mt.memo_id = m.id
    WHERE mt.tag_id = t.id
      AND m.deleted = false
);

-- ==============================================
-- 3. usage_count 更新用トリガー関数を作成
-- ==============================================
CREATE OR REPLACE FUNCTION update_tag_usage_count()
RETURNS TRIGGER AS $$
DECLARE
    memo_deleted BOOLEAN;
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- 挿入されたメモが削除済みでないか確認
        SELECT deleted INTO memo_deleted FROM memos WHERE id = NEW.memo_id;
        IF NOT memo_deleted THEN
            UPDATE tags SET usage_count = usage_count + 1 WHERE id = NEW.tag_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        -- 削除されるメモが削除済みでないか確認
        SELECT deleted INTO memo_deleted FROM memos WHERE id = OLD.memo_id;
        IF NOT memo_deleted THEN
            UPDATE tags SET usage_count = usage_count - 1 WHERE id = OLD.tag_id;
        END IF;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- ==============================================
-- 4. memo_tags テーブルにトリガーを設定
-- ==============================================
CREATE TRIGGER memo_tags_usage_count_trigger
AFTER INSERT OR DELETE ON memo_tags
FOR EACH ROW EXECUTE FUNCTION update_tag_usage_count();

-- ==============================================
-- 5. メモの deleted フラグ変更時に usage_count を更新するトリガー
-- ==============================================
CREATE OR REPLACE FUNCTION update_tag_usage_count_on_memo_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted = false AND NEW.deleted = true THEN
        -- メモが削除された場合、関連タグの usage_count を減らす
        UPDATE tags t
        SET usage_count = usage_count - 1
        FROM memo_tags mt
        WHERE mt.memo_id = NEW.id AND mt.tag_id = t.id;
    ELSIF OLD.deleted = true AND NEW.deleted = false THEN
        -- メモが復元された場合、関連タグの usage_count を増やす
        UPDATE tags t
        SET usage_count = usage_count + 1
        FROM memo_tags mt
        WHERE mt.memo_id = NEW.id AND mt.tag_id = t.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER memos_deleted_usage_count_trigger
AFTER UPDATE OF deleted ON memos
FOR EACH ROW EXECUTE FUNCTION update_tag_usage_count_on_memo_delete();

-- ==============================================
-- 6. usage_count での並び替え用インデックス
-- ==============================================
CREATE INDEX idx_tags_user_usage_count ON tags(user_id, usage_count DESC);

-- ==============================================
-- コメント追加
-- ==============================================
COMMENT ON COLUMN tags.usage_count IS 'タグの使用回数（削除されていないメモでの使用数）';
