package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/** MemoJpaEntity 用 Spring Data JPA リポジトリ。 */
@Repository
interface MemoJpaDataRepository : JpaRepository<MemoJpaEntity, UUID> {
    /**
     * ID で未削除のメモを取得
     */
    @Query(
        """
        SELECT m FROM MemoJpaEntity m
        WHERE m.id = :id AND m.deleted = false
        """,
    )
    fun findActiveMemoById(@Param("id") id: UUID): MemoJpaEntity?

    /**
     * ユーザーの未削除メモを作成日時の新しい順で取得
     */
    @Query(
        """
        SELECT m FROM MemoJpaEntity m
        WHERE m.userId = :userId AND m.deleted = false
        ORDER BY m.createdAt DESC
        """,
    )
    fun findActiveMemosByUser(@Param("userId") userId: UUID): List<MemoJpaEntity>

    /**
     * ユーザーID に紐づくメモをすべて削除（物理削除）
     */
    @Modifying
    fun deleteByUserId(userId: UUID)

    /**
     * ユーザーの未削除メモからタグ一覧を取得（使用回数を含む）
     *
     * @param userId ユーザーID
     * @return タグ名と使用回数のペアリスト（使用回数降順→タグ名昇順でソート済み）
     */
    @Query(
        value = """
            SELECT mt.tag AS name, COUNT(*) AS count
            FROM memo_tags mt
            INNER JOIN memos m ON mt.memo_id = m.id
            WHERE m.user_id = :userId AND m.deleted = false
            GROUP BY mt.tag
            ORDER BY count DESC, mt.tag ASC
        """,
        nativeQuery = true
    )
    fun findTagsWithCountByUserId(@Param("userId") userId: UUID): List<Array<Any>>
}
