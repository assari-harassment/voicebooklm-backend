-- V004: memo_tags テーブルに id カラムを追加
--
-- Spring Data JDBC の標準パターンに従い、memo_tags テーブルに独自IDを追加。
-- これにより、子エンティティの識別が可能になり、差分更新やデバッグが容易になる。
--
-- 注意: IDはアプリケーション側でUUIDv7を生成する。DBのDEFAULTは使用しない。

-- 1. 既存の複合主キーを削除
ALTER TABLE memo_tags DROP CONSTRAINT memo_tags_pkey;

-- 2. id カラムを追加（既存データ用に一時的にDEFAULTを設定）
ALTER TABLE memo_tags ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();

-- 3. 新しい主キーを設定（id のみ）
ALTER TABLE memo_tags ADD PRIMARY KEY (id);

-- 4. DEFAULTを削除（今後はアプリケーション側でUUIDv7を生成）
ALTER TABLE memo_tags ALTER COLUMN id DROP DEFAULT;

-- 5. memo_id + tag の一意制約を追加（重複タグ防止）
ALTER TABLE memo_tags ADD CONSTRAINT memo_tags_memo_id_tag_unique UNIQUE (memo_id, tag);

-- 6. コメント追加
COMMENT ON COLUMN memo_tags.id IS 'タグレコードID (UUIDv7, アプリケーション生成)';
