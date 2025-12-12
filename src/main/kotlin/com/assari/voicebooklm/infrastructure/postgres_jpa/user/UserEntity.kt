package com.assari.voicebooklm.infrastructure.postgres_jpa.user

import com.assari.voicebooklm.domain.model.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * ユーザー JPA エンティティ
 *
 * データベースマッピング用のエンティティ。
 * Domain モデルへの変換は toDomain() / fromDomain() で行う（Entity-embedded mapper パターン）。
 */
@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,

    @Column(name = "google_sub", nullable = false, unique = true, length = 255)
    val googleSub: String,

    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * Entity -> Domain 変換
     */
    fun toDomain(): User = User(
        id = id,
        googleSub = googleSub,
        email = email,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        /**
         * Domain -> Entity 変換
         */
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            googleSub = user.googleSub,
            email = user.email,
            name = user.name,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}
