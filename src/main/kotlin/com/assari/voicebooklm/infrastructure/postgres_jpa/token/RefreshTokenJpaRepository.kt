package com.assari.voicebooklm.infrastructure.postgres_jpa.token

import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * リフレッシュトークンリポジトリ実装
 *
 * Domain Layer の RefreshTokenRepository インターフェースを実装する。
 * Infrastructure Layer に属し、JPA を使用してデータベースアクセスを行う。
 */
@Repository
class RefreshTokenJpaRepository(
    private val jpaRepo: SpringDataRefreshTokenRepository
) : RefreshTokenRepository {

    override fun save(token: RefreshToken): RefreshToken {
        val entity = RefreshTokenEntity.fromDomain(token)
        val savedEntity = jpaRepo.save(entity)
        return savedEntity.toDomain()
    }

    override fun findByTokenAndValid(token: String, now: Instant): RefreshToken? {
        return jpaRepo.findByTokenAndValid(token, now)?.toDomain()
    }

    @Transactional
    override fun revokeByToken(token: String) {
        jpaRepo.revokeByToken(token)
    }

    @Transactional
    override fun revokeByUserId(userId: UUID) {
        jpaRepo.revokeByUserId(userId)
    }

    @Transactional
    override fun deleteByUserId(userId: UUID) {
        jpaRepo.deleteByUserId(userId)
    }
}
