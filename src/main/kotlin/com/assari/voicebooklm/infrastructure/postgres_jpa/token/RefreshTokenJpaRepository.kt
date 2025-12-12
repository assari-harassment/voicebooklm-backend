package com.assari.voicebooklm.infrastructure.postgres_jpa.token

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Spring Data JPA リフレッシュトークンリポジトリインターフェース
 *
 * 内部使用専用。外部からは RefreshTokenRepository インターフェースを使用する。
 */
@Repository
interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, UUID> {

    /**
     * リフレッシュトークンを保存する（JpaRepository から継承）
     */
    override fun <S : RefreshTokenEntity> save(entity: S): S

    /**
     * 有効なトークンを取得する（未失効かつ有効期限内）
     */
    @Query("""
        SELECT t FROM RefreshTokenEntity t
        WHERE t.token = :token
        AND t.revoked = false
        AND t.expiresAt > :now
    """)
    fun findByTokenAndValid(
        @Param("token") token: String,
        @Param("now") now: Instant
    ): RefreshTokenEntity?

    /**
     * トークン文字列でトークンを無効化する
     */
    @Modifying
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.token = :token")
    fun revokeByToken(@Param("token") token: String)

    /**
     * ユーザー ID で全トークンを無効化する（ログアウト時）
     */
    @Modifying
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.userId = :userId")
    fun revokeByUserId(@Param("userId") userId: UUID)

    /**
     * ユーザー ID で全トークンを削除する（アカウント削除時）
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: UUID)
}
