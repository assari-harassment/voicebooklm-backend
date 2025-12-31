package com.assari.voicebooklm.infrastructure.postgres_jdbc.token

import com.assari.voicebooklm.domain.model.RefreshToken
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * リフレッシュトークン JDBC エンティティ
 *
 * データベースマッピング用のエンティティ。
 * Domain モデルへの変換は toDomain() / fromDomain() で行う（Entity-embedded mapper パターン）。
 */
@Table("refresh_tokens")
data class RefreshTokenEntity(
    @Id
    val id: UUID,

    val token: String,

    @Column("user_id")
    val userId: UUID,

    @Column("expires_at")
    val expiresAt: Instant,

    @Column("created_at")
    val createdAt: Instant,

    val revoked: Boolean
) {
    /**
     * Entity -> Domain 変換
     */
    fun toDomain(): RefreshToken = RefreshToken(
        id = id,
        token = token,
        userId = userId,
        expiresAt = expiresAt,
        createdAt = createdAt,
        revoked = revoked
    )

    companion object {
        /**
         * Domain -> Entity 変換
         */
        fun fromDomain(refreshToken: RefreshToken): RefreshTokenEntity = RefreshTokenEntity(
            id = refreshToken.id,
            token = refreshToken.token,
            userId = refreshToken.userId,
            expiresAt = refreshToken.expiresAt,
            createdAt = refreshToken.createdAt,
            revoked = refreshToken.revoked
        )
    }
}
