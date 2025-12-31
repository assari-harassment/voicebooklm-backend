package com.assari.voicebooklm.infrastructure.postgres_jdbc.token

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Spring Data JDBC リフレッシュトークンリポジトリインターフェース
 *
 * 内部使用専用。外部からは RefreshTokenRepository インターフェースを使用する。
 */
@Repository
interface RefreshTokenJdbcRepository : CrudRepository<RefreshTokenJdbcEntity, UUID> {

    /**
     * 有効なトークンを取得する（未失効かつ有効期限内）
     */
    @Query("""
        SELECT * FROM refresh_tokens
        WHERE token = :token
        AND revoked = false
        AND expires_at > :now
    """)
    fun findByTokenAndValid(
        @Param("token") token: String,
        @Param("now") now: Instant
    ): RefreshTokenJdbcEntity?

    /**
     * トークン文字列でトークンを無効化する
     */
    @Modifying
    @Query("UPDATE refresh_tokens SET revoked = true WHERE token = :token")
    fun revokeByToken(@Param("token") token: String)

    /**
     * ユーザー ID で全トークンを無効化する（ログアウト時）
     */
    @Modifying
    @Query("UPDATE refresh_tokens SET revoked = true WHERE user_id = :userId")
    fun revokeByUserId(@Param("userId") userId: UUID)

    /**
     * ユーザー ID で全トークンを削除する（アカウント削除時）
     */
    @Modifying
    @Query("DELETE FROM refresh_tokens WHERE user_id = :userId")
    fun deleteByUserId(@Param("userId") userId: UUID)
}
