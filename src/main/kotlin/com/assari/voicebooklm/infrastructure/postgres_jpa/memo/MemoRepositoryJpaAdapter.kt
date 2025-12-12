package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import com.assari.voicebooklm.domain.model.memo.Memo
import com.assari.voicebooklm.domain.repository.memo.MemoRepository
import java.util.UUID
import org.springframework.stereotype.Repository

/**
 * MemoRepository ポート実装（JPA 永続化アダプター）。
 */
@Repository
class MemoRepositoryJpaAdapter(
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
}
