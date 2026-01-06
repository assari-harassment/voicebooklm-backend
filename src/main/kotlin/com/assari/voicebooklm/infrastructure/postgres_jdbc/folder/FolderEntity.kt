package com.assari.voicebooklm.infrastructure.postgres_jdbc.folder

import com.assari.voicebooklm.domain.model.Folder
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * フォルダー JDBC エンティティ
 *
 * folders テーブルにマッピングされる。
 * Domain モデルへの変換は toDomain() / fromDomain() で行う。
 */
@Table("folders")
data class FolderEntity(
    @Id
    val id: UUID,

    @Column("user_id")
    val userId: UUID,

    val name: String,

    @Column("parent_id")
    val parentId: UUID?,

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
    fun toDomain(): Folder = Folder.restore(
        id = id,
        userId = userId,
        name = name,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        /**
         * Domain -> Entity 変換
         */
        fun fromDomain(folder: Folder): FolderEntity = FolderEntity(
            id = folder.id,
            userId = folder.userId,
            name = folder.name,
            parentId = folder.parentId,
            createdAt = folder.createdAt,
            updatedAt = folder.updatedAt,
        )
    }
}
