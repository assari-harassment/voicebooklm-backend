package com.assari.voicebooklm.infrastructure.postgres_jdbc.memo

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
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
            LIMIT 1
        )
    """)
    fun existsByUserIdAndFolderId(
        @Param("userId") userId: UUID,
        @Param("folderId") folderId: UUID,
    ): Boolean
}
