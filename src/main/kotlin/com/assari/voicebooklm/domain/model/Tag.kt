package com.assari.voicebooklm.domain.model

import java.time.Instant
import java.util.UUID

/**
 * タグドメインモデル
 *
 * メモに付与するタグを表現する集約。
 * ユーザーごとにタグ名が一意となる。
 * イミュータブルな設計で、変更時は新しいインスタンスを返す。
 */
data class Tag(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        validate()
    }

    private fun validate() {
        require(name.isNotBlank()) { "Tag name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) {
            "Tag name must not exceed $MAX_NAME_LENGTH characters"
        }
    }

    /**
     * タグ名を変更した新しい Tag インスタンスを返す
     */
    fun rename(newName: String): Tag = copy(
        name = newName.trim(),
        updatedAt = Instant.now(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tag) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val MAX_NAME_LENGTH = 100

        /**
         * 新規タグを作成
         */
        fun create(
            id: UUID,
            userId: UUID,
            name: String,
        ): Tag {
            val normalizedName = name.trim()
            return Tag(
                id = id,
                userId = userId,
                name = normalizedName,
            )
        }

        /**
         * 永続化されたデータから復元
         */
        fun restore(
            id: UUID,
            userId: UUID,
            name: String,
            createdAt: Instant,
            updatedAt: Instant,
        ): Tag = Tag(
            id = id,
            userId = userId,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
