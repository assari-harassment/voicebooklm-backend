package com.assari.voicebooklm.infrastructure.postgres_jdbc.memo

import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * VoiceMemoRepository の JDBC 実装
 *
 * Domain Layer の VoiceMemoRepository インターフェースを実装する。
 * Infrastructure Layer に属し、Spring Data JDBC を使用してデータベースアクセスを行う。
 */
@Repository
class VoiceMemoRepositoryImpl(
    private val memoJdbcRepository: MemoJdbcRepository,
) : VoiceMemoRepository {

    override suspend fun save(voiceMemo: VoiceMemo): VoiceMemo {
        val existing = memoJdbcRepository.findActiveMemoById(voiceMemo.id)
        val entity = MemoEntity.fromDomain(voiceMemo, version = existing?.version)
        val saved = memoJdbcRepository.save(entity)
        return saved.toDomain()
    }

    override suspend fun findById(id: UUID): VoiceMemo? =
        memoJdbcRepository.findActiveMemoById(id)?.toDomain()

    override suspend fun findByUserId(userId: UUID): List<VoiceMemo> =
        memoJdbcRepository.findActiveMemosByUser(userId).map { it.toDomain() }

    @Transactional
    override fun deleteByUserId(userId: UUID) {
        memoJdbcRepository.deleteByUserId(userId)
    }

    override suspend fun existsByUserIdAndFolderId(userId: UUID, folderId: UUID): Boolean =
        memoJdbcRepository.existsByUserIdAndFolderId(userId, folderId)
}
