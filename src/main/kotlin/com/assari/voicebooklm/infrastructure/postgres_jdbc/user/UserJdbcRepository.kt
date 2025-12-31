package com.assari.voicebooklm.infrastructure.postgres_jdbc.user

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JDBC ユーザーリポジトリインターフェース
 *
 * 内部使用専用。外部からは UserRepository インターフェースを使用する。
 */
@Repository
interface UserJdbcRepository : CrudRepository<UserEntity, UUID> {

    @Query("SELECT * FROM users WHERE email = :email")
    fun findByEmail(@Param("email") email: String): UserEntity?

    @Query("SELECT * FROM users WHERE google_sub = :googleSub")
    fun findByGoogleSub(@Param("googleSub") googleSub: String): UserEntity?
}
