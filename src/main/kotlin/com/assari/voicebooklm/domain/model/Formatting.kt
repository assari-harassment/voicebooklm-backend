package com.assari.voicebooklm.domain.model

import java.util.UUID

/**
 * AI整形結果を表す値オブジェクト
 *
 * 文字起こしテキストからAIが生成したタイトル、本文、タグIDを保持する。
 */
data class Formatting(
    /** 処理ステータス */
    val status: FormattingStatus,
    /** AI生成タイトル（処理完了前は null） */
    val title: String?,
    /** AI整形済み本文（Markdown形式、処理完了前は null） */
    val content: String?,
    /** タグIDリスト（マスタへの参照） */
    val tagIds: List<UUID>,
    /** フォールバックが使用されたかどうか */
    val fallbackUsed: Boolean = false,
    /** フォルダーID（未分類の場合は null） */
    val folderId: UUID? = null,
) {
    init {
        validate()
    }

    private fun validate() {
        if (status == FormattingStatus.COMPLETED) {
            require(!title.isNullOrBlank()) { "title must not be blank when status is COMPLETED" }
            require(!content.isNullOrBlank()) { "content must not be blank when status is COMPLETED" }
        }
    }

    /**
     * AI整形が完了しているかどうか
     */
    val isCompleted: Boolean
        get() = status == FormattingStatus.COMPLETED

    /**
     * AI整形が失敗したかどうか
     */
    val isFailed: Boolean
        get() = status == FormattingStatus.FAILED

    companion object {
        /**
         * 処理待ち状態の Formatting を作成
         */
        fun pending(): Formatting = Formatting(
            status = FormattingStatus.PENDING,
            title = null,
            content = null,
            tagIds = emptyList(),
            fallbackUsed = false,
        )

        /**
         * 処理中状態の Formatting を作成
         */
        fun processing(): Formatting = Formatting(
            status = FormattingStatus.PROCESSING,
            title = null,
            content = null,
            tagIds = emptyList(),
            fallbackUsed = false,
        )

        /**
         * 完了状態の Formatting を作成
         */
        fun completed(
            title: String,
            content: String,
            tagIds: List<UUID> = emptyList(),
            fallbackUsed: Boolean = false,
            folderId: UUID? = null,
        ): Formatting {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotBlank()) { "Formatting title must not be blank when completed" }
            require(content.isNotBlank()) { "Formatting content must not be blank when completed" }
            return Formatting(
                status = FormattingStatus.COMPLETED,
                title = normalizedTitle,
                content = content,
                tagIds = tagIds,
                fallbackUsed = fallbackUsed,
                folderId = folderId,
            )
        }

        /**
         * 失敗状態の Formatting を作成
         */
        fun failed(): Formatting = Formatting(
            status = FormattingStatus.FAILED,
            title = null,
            content = null,
            tagIds = emptyList(),
            fallbackUsed = false,
        )
    }
}
