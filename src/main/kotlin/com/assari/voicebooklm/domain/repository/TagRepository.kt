package com.assari.voicebooklm.domain.repository

import com.assari.voicebooklm.domain.model.Tag
import java.util.UUID

/**
 * ソート対象フィールド
 */
enum class TagSortField {
    NAME,
    USAGE;

    companion object {
        fun fromString(value: String): TagSortField = when (value.lowercase()) {
            "name" -> NAME
            "usage" -> USAGE
            else -> NAME
        }
    }
}

/**
 * ソート順
 */
enum class SortOrder {
    ASC,
    DESC;

    companion object {
        fun fromString(value: String): SortOrder = when (value.lowercase()) {
            "asc" -> ASC
            "desc" -> DESC
            else -> ASC
        }
    }
}

/**
 * タグ集約の永続化ポート
 */
interface TagRepository {
    /**
     * タグを保存する
     */
    suspend fun save(tag: Tag): Tag

    /**
     * IDでタグを取得する
     */
    suspend fun findById(id: UUID): Tag?

    /**
     * ユーザーの全タグを取得する（名前昇順）
     */
    suspend fun findByUserId(userId: UUID): List<Tag>

    /**
     * ユーザーのタグをソート条件付きで取得する
     *
     * @param userId ユーザーID
     * @param sortField ソート対象フィールド
     * @param sortOrder ソート順
     * @param limit 取得件数上限（nullの場合は全件取得）
     */
    suspend fun findByUserIdWithSort(
        userId: UUID,
        sortField: TagSortField,
        sortOrder: SortOrder,
        limit: Int? = null,
    ): List<Tag>

    /**
     * ユーザーのタグ名でタグを取得する
     */
    suspend fun findByUserIdAndName(userId: UUID, name: String): Tag?

    /**
     * 複数のタグ名でタグを一括取得する
     */
    suspend fun findByUserIdAndNames(userId: UUID, names: List<String>): List<Tag>

    /**
     * IDリストでタグを一括取得する
     */
    suspend fun findByIds(ids: List<UUID>): List<Tag>

    /**
     * タグを削除する
     */
    suspend fun delete(id: UUID)

    /**
     * ユーザーIDに紐づくすべてのタグを削除する（アカウント削除用）
     */
    fun deleteByUserId(userId: UUID)
}
