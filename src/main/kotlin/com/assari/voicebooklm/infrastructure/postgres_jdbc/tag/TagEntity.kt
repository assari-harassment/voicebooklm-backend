package com.assari.voicebooklm.infrastructure.postgres_jdbc.tag

import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

/**
 * メモ-タグ関連 JDBC エンティティ
 *
 * memo_tags テーブルにマッピングされる。
 * MemoEntity の子エンティティとして @MappedCollection で管理される。
 * タグマスタ（tags テーブル）への参照を tag_id で保持する。
 */
@Table("memo_tags")
data class TagEntity(
    @Id
    val id: UUID,

    @Column("tag_id")
    val tagId: UUID
) {
    companion object {
        /**
         * 新規関連を作成（UUIDv7を生成）
         */
        fun create(tagId: UUID): TagEntity = TagEntity(
            id = UuidCreator.getTimeOrderedEpoch(),
            tagId = tagId
        )
    }
}
