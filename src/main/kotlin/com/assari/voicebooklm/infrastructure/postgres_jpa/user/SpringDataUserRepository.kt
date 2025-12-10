package com.assari.voicebooklm.infrastructure.postgres_jpa.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA ユーザーリポジトリインターフェース
 *
 * 内部使用専用。外部からは UserRepository インターフェースを使用する。
 */
@Repository
interface SpringDataUserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findByGoogleSub(googleSub: String): UserEntity?
}
