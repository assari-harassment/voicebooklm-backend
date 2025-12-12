package com.assari.voicebooklm.domain.model

import java.util.UUID

/**
 * 音声文字起こしから生成されるメモ集約。
 */
data class Memo(
    val id: UUID,
    val title: String,
    val content: String,
    val tags: List<String>,
    val userId: UUID,
    val deleted: Boolean = false,
) {
    /**
     * タイトルのみ更新した新しいメモを返す。
     */
    fun changeTitle(newTitle: String): Memo {
        val normalizedTitle = newTitle.trim()
        require(normalizedTitle.isNotBlank()) { "Title must not be blank" }
        return copy(title = normalizedTitle)
    }

    /**
     * 本文のみ更新した新しいメモを返す。
     */
    fun changeContent(newContent: String): Memo {
        require(newContent.isNotBlank()) { "Content must not be blank" }
        return copy(content = newContent)
    }

    /**
     * タグを正規化して更新した新しいメモを返す（trim + 空文字除去）。
     */
    fun changeTags(newTags: List<String>): Memo {
        val sanitizedTags = sanitizeTags(newTags)
        return copy(tags = sanitizedTags)
    }

    /**
     * 論理削除フラグを立てた新しいメモを返す（多重呼び出しは変化なし）。
     */
    fun markAsDeleted(): Memo = if (deleted) this else copy(deleted = true)

    companion object {
        /**
         * 基本的なバリデーションと正規化を行うファクトリ。
         */
        fun create(
            title: String,
            content: String,
            tags: List<String>,
            userId: UUID,
            id: UUID = UUID.randomUUID(),
        ): Memo {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotBlank()) { "Title must not be blank" }
            require(content.isNotBlank()) { "Content must not be blank" }
            val sanitizedTags = sanitizeTags(tags)
            return Memo(
                id = id,
                title = normalizedTitle,
                content = content,
                tags = sanitizedTags,
                userId = userId,
                deleted = false,
            )
        }

        private fun sanitizeTags(tags: List<String>): List<String> =
            tags.map { it.trim() }.filter { it.isNotEmpty() }
    }
}
