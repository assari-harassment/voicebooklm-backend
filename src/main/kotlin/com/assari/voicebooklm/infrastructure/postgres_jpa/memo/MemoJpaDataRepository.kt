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
    fun findActiveMemoById(id: UUID): MemoJpaEntity?

    // ユーザーの未削除メモを作成日時の新しい順で取得する
    @Query(
        """
        SELECT m FROM MemoJpaEntity m
        WHERE m.userId = :userId AND m.deleted = false
        ORDER BY m.createdAt DESC
        """,
    )
    fun findActiveMemosByUser(@Param("userId") userId: UUID): List<MemoJpaEntity>

    @Modifying
    fun deleteByUserId(userId: UUID)
}
