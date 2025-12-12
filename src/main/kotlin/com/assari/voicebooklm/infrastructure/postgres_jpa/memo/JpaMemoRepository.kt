package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import com.assari.voicebooklm.domain.model.memo.Memo
import com.assari.voicebooklm.domain.repository.memo.MemoRepository
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * MemoRepository ポート実装（JPA）。
 */
@Repository
class JpaMemoRepository(
    private val memoJpaRepository: MemoJpaRepository,
) : MemoRepository {

    @Transactional
    override suspend fun save(memo: Memo): Memo {
        val entity = MemoEntity.fromDomain(memo)
        val saved = memoJpaRepository.save(entity)
        return saved.toDomain()
    }

    @Transactional(readOnly = true)
    override suspend fun findById(id: UUID): Memo? =
        memoJpaRepository.findByIdAndDeletedFalse(id)?.toDomain()

    @Transactional(readOnly = true)
    override suspend fun findByUserId(userId: UUID): List<Memo> =
        memoJpaRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId).map { it.toDomain() }
}
