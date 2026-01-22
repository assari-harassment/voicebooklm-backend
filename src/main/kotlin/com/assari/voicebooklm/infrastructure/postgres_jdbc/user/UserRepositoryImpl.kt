package com.assari.voicebooklm.infrastructure.postgres_jdbc.user

import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * ユーザーリポジトリ実装
 *
 * Domain Layer の UserRepository インターフェースを実装する。
 * Infrastructure Layer に属し、Spring Data JDBC を使用してデータベースアクセスを行う。
 */
@Repository
class UserRepositoryImpl(
    private val userJdbcRepository: UserJdbcRepository
) : UserRepository {

    override fun save(user: User): User {
        // 既存のエンティティがあればversionを引き継ぐ（UPDATE判定のため）
        val existingEntity = userJdbcRepository.findByIdOrNull(user.id)
        val entity = if (existingEntity != null) {
            UserEntity.fromDomainWithVersion(user, existingEntity.version)
        } else {
            UserEntity.fromDomain(user)
        }
        val savedEntity = userJdbcRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: UUID): User? {
        return userJdbcRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun findByEmail(email: String): User? {
        return userJdbcRepository.findByEmail(email)?.toDomain()
    }

    override fun findByGoogleSub(googleSub: String): User? {
        return userJdbcRepository.findByGoogleSub(googleSub)?.toDomain()
    }

    override fun deleteById(id: UUID) {
        userJdbcRepository.deleteById(id)
    }
}
