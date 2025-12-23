package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * VoiceMemoRepository の JPA 実装
 */
@Repository
class VoiceMemoRepositoryImpl(
    private val memoJpaRepository: MemoJpaDataRepository,
) : VoiceMemoRepository {

    override suspend fun save(voiceMemo: VoiceMemo): VoiceMemo {
        val entity = MemoJpaEntity.fromDomain(voiceMemo)
        val saved = memoJpaRepository.save(entity)
        return saved.toDomain()
    }

    override suspend fun findById(id: UUID): VoiceMemo? =
        memoJpaRepository.findActiveMemoById(id)?.toDomain()

    override suspend fun findByUserId(userId: UUID): List<VoiceMemo> =
        memoJpaRepository.findActiveMemosByUser(userId).map { it.toDomain() }

    override fun deleteByUserId(userId: UUID) {
        memoJpaRepository.deleteByUserId(userId)
    }

    override suspend fun findTagsWithCountByUserId(userId: UUID): List<Pair<String, Int>> {
        val results = memoJpaRepository.findTagsWithCountByUserId(userId)
        return results.map { row ->
            // ネイティブクエリの結果: [tag名(String), count(Long)]
            val tagName = row[0] as String
            val count = (row[1] as Number).toInt()
            Pair(tagName, count)
        }
    }
}
