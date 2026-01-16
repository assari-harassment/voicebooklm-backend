-- V010: タグに「#」プレフィックスを追加
-- 既存のタグで「#」が付いていないものに「#」を追加する

UPDATE memo_tags
SET tag = '#' || tag
WHERE tag NOT LIKE '#%';
