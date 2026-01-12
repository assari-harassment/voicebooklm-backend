package com.assari.voicebooklm.domain.repository

import java.util.UUID

/**
 * タグ検索・集計ポート
 *
 * memo_tags テーブルから、ユーザーが使用しているタグの一覧や
 * 使用回数を集計して取得するためのリポジトリ。
 */
interface TagRepository {
    /**
     * ユーザーが使用している全タグを取得する
     *
     * @param userId ユーザーID
     * @param sort ソート基準（NAME: 名前順, USAGE_COUNT: 使用回数順）
     * @param order ソート順（ASC: 昇順, DESC: 降順）
     * @param limit 取得件数上限（null の場合は無制限）
     * @return タグ名のリスト
     */
    suspend fun findByUserId(
        userId: UUID,
        sort: TagSortField = TagSortField.NAME,
        order: SortOrder = SortOrder.ASC,
        limit: Int? = null,
    ): List<String>
}

/**
 * タグのソート基準
 */
enum class TagSortField {
    /** タグ名でソート */
    NAME,

    /** 使用回数でソート */
    USAGE_COUNT,
}

/**
 * ソート順
 */
enum class SortOrder {
    /** 昇順 */
    ASC,

    /** 降順 */
    DESC,
}
