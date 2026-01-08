package com.assari.voicebooklm.infrastructure.postgres_jdbc.tag

import com.assari.voicebooklm.domain.model.Tag
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * タグマスタ JDBC エンティティ
 *
 * tags テーブルにマッピングされる。
 * Domain モデルへの変換は toDomain() / fromDomain() で行う。
 */
@Table("tags")
data class TagMasterEntity(
    @Id
    val id: UUID,

    @Column("user_id")
    val userId: UUID,

    val name: String,

    @Column("created_at")
    val createdAt: Instant,

    @Column("updated_at")
    val updatedAt: Instant,

    @Version
    val version: Long? = null
) {
    /**
     * Entity -> Domain 変換
     */
    fun toDomain(): Tag = Tag.restore(
        id = id,
        userId = userId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        /**
         * Domain -> Entity 変換
         */
        fun fromDomain(tag: Tag): TagMasterEntity = TagMasterEntity(
            id = tag.id,
            userId = tag.userId,
            name = tag.name,
            createdAt = tag.createdAt,
            updatedAt = tag.updatedAt,
        )
    }
}
