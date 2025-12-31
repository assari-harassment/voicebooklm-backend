package com.assari.voicebooklm.infrastructure.postgres_jdbc.user

import com.assari.voicebooklm.domain.model.User
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * ユーザー JDBC エンティティ
 *
 * データベースマッピング用のエンティティ。
 * Domain モデルへの変換は toDomain() / fromDomain() で行う（Entity-embedded mapper パターン）。
 */
@Table("users")
data class UserEntity(
    @Id
    val id: UUID,

    @Column("google_sub")
    val googleSub: String,

    val email: String,

    val name: String,

    @Column("created_at")
    val createdAt: Instant,

    @Column("updated_at")
    val updatedAt: Instant
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
