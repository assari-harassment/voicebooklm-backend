package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** MemoJpaEntity 用 Spring Data JPA リポジトリ。 */
@Repository
interface MemoJpaDataRepository : JpaRepository<MemoJpaEntity, UUID> {
    fun findByIdAndDeletedFalse(id: UUID): MemoJpaEntity?

    fun findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId: UUID): List<MemoJpaEntity>
}
