package com.assari.voicebooklm.infrastructure.postgres_jpa.token

import com.assari.voicebooklm.domain.model.RefreshToken
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * リフレッシュトークン JPA エンティティ
 *
 * データベースマッピング用のエンティティ。
 * Domain モデルへの変換は toDomain() / fromDomain() で行う（Entity-embedded mapper パターン）。
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,

    @Column(nullable = false, unique = true, length = 500)
    val token: String,

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    val userId: UUID,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var revoked: Boolean = false
) {
    // JPA no-arg constructor
    constructor() : this(
        id = UUID.randomUUID(),
        token = "",
        userId = UUID.randomUUID(),
        expiresAt = Instant.now()
    )

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
