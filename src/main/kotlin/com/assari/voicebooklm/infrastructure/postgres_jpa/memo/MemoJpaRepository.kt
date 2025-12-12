package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * MemoEntity 用 Spring Data リポジトリ。
 */
@Repository
interface MemoJpaRepository : JpaRepository<MemoEntity, UUID> {
    fun findByIdAndDeletedFalse(id: UUID): MemoEntity?

    fun findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId: UUID): List<MemoEntity>
}
