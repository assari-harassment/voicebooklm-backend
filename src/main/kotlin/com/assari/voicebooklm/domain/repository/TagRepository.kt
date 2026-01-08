package com.assari.voicebooklm.domain.repository

import com.assari.voicebooklm.domain.model.Tag
import java.util.UUID

/**
 * タグと使用回数
 */
data class TagWithCount(
    val tag: Tag,
    val count: Int,
)

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
     * ユーザーの全タグを取得する
     */
    suspend fun findByUserId(userId: UUID): List<Tag>

    /**
     * ユーザーのタグ名でタグを取得する
     */
    suspend fun findByUserIdAndName(userId: UUID, name: String): Tag?

    /**
     * 複数のタグ名でタグを一括取得する
     */
    suspend fun findByUserIdAndNames(userId: UUID, names: List<String>): List<Tag>

    /**
     * ユーザーのタグを使用回数順で取得する
     *
     * @param userId ユーザーID
     * @param limit 取得件数上限（nullの場合は全件取得）
     * @return 使用回数降順（同数の場合はタグ名昇順）でソートされたタグ一覧
     */
    suspend fun findTagsWithCountByUserId(userId: UUID, limit: Int? = null): List<TagWithCount>

    /**
     * タグを削除する
     */
    suspend fun delete(id: UUID)

    /**
     * ユーザーIDに紐づくすべてのタグを削除する（アカウント削除用）
     */
    fun deleteByUserId(userId: UUID)
}
