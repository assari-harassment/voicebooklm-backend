package com.assari.voicebooklm.infrastructure.postgres_jpa.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

/**
 * Spring Data JPA ユーザーリポジトリインターフェース
 *
 * 内部使用専用。外部からは UserRepository インターフェースを使用する。
 */
@Repository
interface UserJpaRepository : JpaRepository<UserEntity, UUID> {

    /**
     * ユーザーを保存する（JpaRepository から継承）
     */
    override fun <S : UserEntity> save(entity: S): S

    /**
     * ID でユーザーを検索する（JpaRepository から継承）
     */
    override fun findById(id: UUID): Optional<UserEntity>

    /**
     * ID でユーザーを削除する（JpaRepository から継承）
     */
    override fun deleteById(id: UUID)

    fun findByEmail(email: String): UserEntity?
    fun findByGoogleSub(googleSub: String): UserEntity?
}
