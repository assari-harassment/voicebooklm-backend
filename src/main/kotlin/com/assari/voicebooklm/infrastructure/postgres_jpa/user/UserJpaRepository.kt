package com.assari.voicebooklm.infrastructure.postgres_jpa.user

import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.UserRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * ユーザーリポジトリ実装
 *
 * Domain Layer の UserRepository インターフェースを実装する。
 * Infrastructure Layer に属し、JPA を使用してデータベースアクセスを行う。
 */
@Repository
class UserJpaRepository(
    private val jpaRepo: SpringDataUserRepository
) : UserRepository {

    override fun save(user: User): User {
        val entity = UserEntity.fromDomain(user)
        val savedEntity = jpaRepo.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: UUID): User? {
        return jpaRepo.findById(id)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByEmail(email: String): User? {
        return jpaRepo.findByEmail(email)?.toDomain()
    }

    override fun findByGoogleSub(googleSub: String): User? {
        return jpaRepo.findByGoogleSub(googleSub)?.toDomain()
    }

    override fun deleteById(id: UUID) {
        jpaRepo.deleteById(id)
    }
}
