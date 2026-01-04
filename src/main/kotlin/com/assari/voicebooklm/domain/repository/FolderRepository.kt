package com.assari.voicebooklm.domain.repository

import com.assari.voicebooklm.domain.model.Folder
import java.util.UUID

/**
 * Folder 永続化ポート
 */
interface FolderRepository {
    /**
     * フォルダーを保存する
     */
    suspend fun save(folder: Folder): Folder

    /**
     * ID でフォルダーを取得する
     */
    suspend fun findById(id: UUID): Folder?

    /**
     * ユーザーの全フォルダーを取得する
     */
    suspend fun findByUserId(userId: UUID): List<Folder>

    /**
     * ユーザーの特定親フォルダー直下のフォルダーを取得する
     *
     * @param userId ユーザーID
     * @param parentId 親フォルダーID（null の場合はルートフォルダー直下）
     */
    suspend fun findByUserIdAndParentId(userId: UUID, parentId: UUID?): List<Folder>

    /**
     * パス（例: "仕事/プロジェクトA"）からフォルダーを検索する
     *
     * @param userId ユーザーID
     * @param path スラッシュ区切りのフォルダーパス
     */
    suspend fun findByUserIdAndPath(userId: UUID, path: String): Folder?

    /**
     * 指定フォルダーの全子孫フォルダーIDを取得する
     *
     * @param folderId 基点となるフォルダーID
     */
    suspend fun findDescendantIds(folderId: UUID): List<UUID>

    /**
     * フォルダーを削除する
     */
    suspend fun delete(id: UUID)

    /**
     * 同一親フォルダー内に同名フォルダーが存在するかチェックする
     *
     * @param userId ユーザーID
     * @param parentId 親フォルダーID（null の場合はルート直下）
     * @param name フォルダー名
     * @param excludeId 除外するフォルダーID（自分自身を除外する場合に使用）
     */
    suspend fun existsByUserIdAndParentIdAndName(
        userId: UUID,
        parentId: UUID?,
        name: String,
        excludeId: UUID? = null,
    ): Boolean

    /**
     * ユーザーIDに紐づくすべてのフォルダーを削除する（アカウント削除用）
     */
    fun deleteByUserId(userId: UUID)
}
