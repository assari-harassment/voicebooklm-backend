package com.assari.voicebooklm.infrastructure.postgres_jdbc.memo

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JDBC メモリポジトリインターフェース
 *
 * 内部使用専用。外部からは VoiceMemoRepository インターフェースを使用する。
 */
@Repository
interface MemoJdbcRepository : CrudRepository<MemoEntity, UUID> {

    /**
     * ID で未削除のメモを取得
     */
    @Query("""
        SELECT * FROM memos
        WHERE id = :id AND deleted = false
    """)
    fun findActiveMemoById(@Param("id") id: UUID): MemoEntity?

    /**
     * ユーザーの未削除メモを作成日時の新しい順で取得
     */
    @Query("""
        SELECT * FROM memos
        WHERE user_id = :userId AND deleted = false
        ORDER BY created_at DESC
    """)
    fun findActiveMemosByUser(@Param("userId") userId: UUID): List<MemoEntity>

    /**
     * ユーザーの未削除メモをキーワード検索して作成日時の新しい順で取得
     * タイトルまたはコンテントにキーワードが含まれるメモを返す（大文字小文字を区別しない）
     */
    @Query("""
        SELECT * FROM memos
        WHERE user_id = :userId AND deleted = false
        AND (title ILIKE '%' || :keyword || '%' ESCAPE E'\\' OR content ILIKE '%' || :keyword || '%' ESCAPE E'\\')
        ORDER BY created_at DESC
    """)
    fun findActiveMemosByUserWithKeyword(
        @Param("userId") userId: UUID,
        @Param("keyword") keyword: String
    ): List<MemoEntity>

    /**
     * ユーザーID に紐づくメモをすべて削除（物理削除）
     */
    @Modifying
    @Query("DELETE FROM memos WHERE user_id = :userId")
    fun deleteByUserId(@Param("userId") userId: UUID)

    /**
     * 指定フォルダー内にメモが存在するかチェック
     *
     * @param userId ユーザーID
     * @param folderId フォルダーID
     * @return メモが存在する場合true
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM memos
            WHERE user_id = :userId AND folder_id = :folderId AND deleted = false
        )
    """)
    fun existsByUserIdAndFolderId(
        @Param("userId") userId: UUID,
        @Param("folderId") folderId: UUID,
    ): Boolean

    /**
     * 複数のフォルダー（子フォルダー含む）のメモ数をカウントする
     *
     * @param userId ユーザーID
     * @param folderIds カウント対象のフォルダーIDリスト（親フォルダーID + 子孫フォルダーIDを含む）
     * @return フォルダーIDをキー、メモ数を値とするマップ（Row型）
     */
    @Query("""
        SELECT folder_id, COUNT(*) as count
        FROM memos
        WHERE user_id = :userId 
          AND folder_id IN (:folderIds)
          AND deleted = false
        GROUP BY folder_id
    """)
    fun countByFolderIds(
        @Param("userId") userId: UUID,
        @Param("folderIds") folderIds: List<UUID>,
    ): List<FolderMemoCount>
}

/**
 * フォルダーごとのメモ数（SQLクエリ結果用）
 */
data class FolderMemoCount(
    @Column("folder_id")
    val folderId: UUID,
    @Column("count")
    val count: Long,
)
