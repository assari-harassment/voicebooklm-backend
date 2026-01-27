package com.assari.voicebooklm.infrastructure.postgres_jdbc.memo

import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

/**
 * メモタグ JDBC エンティティ
 *
 * memo_tags テーブルにマッピングされる。
 * MemoEntity の子エンティティとして @MappedCollection で管理される。
 */
@Table("memo_tags")
data class MemoTag(
    @Id
    val id: UUID,

    val tag: String
) {
    companion object {
        /**
         * 新規タグを作成（UUIDv7を生成）
         */
        fun create(tag: String): MemoTag = MemoTag(
            id = UuidCreator.getTimeOrderedEpoch(),
            tag = tag
        )
    }
}
