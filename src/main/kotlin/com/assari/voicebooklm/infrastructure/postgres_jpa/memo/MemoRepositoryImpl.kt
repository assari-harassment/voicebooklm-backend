package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import com.assari.voicebooklm.domain.model.Memo
import com.assari.voicebooklm.domain.repository.MemoRepository
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * MemoRepository の JPA 実装。
 */
@Repository
class MemoRepositoryImpl(
    private val memoJpaRepository: MemoJpaDataRepository,
) : MemoRepository {

    override suspend fun save(memo: Memo): Memo {
        val entity = MemoJpaEntity.fromDomain(memo)
        val saved = memoJpaRepository.save(entity)
        return saved.toDomain()
    }

    override suspend fun findById(id: UUID): Memo? =
        memoJpaRepository.findByIdAndDeletedFalse(id)?.toDomain()

    override suspend fun findByUserId(userId: UUID): List<Memo> =
        memoJpaRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId).map { it.toDomain() }

    override fun deleteByUserId(userId: UUID) {
        memoJpaRepository.deleteByUserId(userId)
    }
}
