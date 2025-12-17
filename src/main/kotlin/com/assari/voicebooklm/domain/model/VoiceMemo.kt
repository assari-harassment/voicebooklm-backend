package com.assari.voicebooklm.domain.model

import java.time.Instant
import java.util.UUID

/**
 * 音声メモ集約ルート
 *
 * 音声データから生成されるメモの全ライフサイクルを管理する。
 * 文字起こし（Transcription）とAI整形（Formatting）の2段階処理を経て完成する。
 *
 * 不変オブジェクトとして設計されており、状態変更時は新しいインスタンスを返す。
 */
data class VoiceMemo(
    val id: UUID,
    val userId: UUID,
    val transcription: Transcription,
    val formatting: Formatting,
    val deleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    /**
     * 処理が完全に完了しているかどうか
     */
    val isFullyCompleted: Boolean
        get() = transcription.isCompleted && formatting.isCompleted

    /**
     * 処理中かどうか
     */
    val isProcessing: Boolean
        get() = transcription.status == TranscriptionStatus.PROCESSING ||
                formatting.status == FormattingStatus.PROCESSING

    /**
     * いずれかの処理が失敗したかどうか
     */
    val hasFailed: Boolean
        get() = transcription.isFailed || formatting.isFailed

    // ============================================================
    // 文字起こし関連の操作
    // ============================================================

    /**
     * 文字起こしを開始する
     */
    fun startTranscription(): VoiceMemo = copy(
        transcription = Transcription.processing(transcription.languageCode),
        updatedAt = Instant.now(),
    )

    /**
     * 文字起こしを完了する
     */
    fun completeTranscription(text: String, fallbackUsed: Boolean = false): VoiceMemo = copy(
        transcription = Transcription.completed(
            text = text,
            languageCode = transcription.languageCode,
            fallbackUsed = fallbackUsed,
        ),
        updatedAt = Instant.now(),
    )

    /**
     * 文字起こしを失敗としてマークする
     */
    fun failTranscription(): VoiceMemo = copy(
        transcription = Transcription.failed(transcription.languageCode),
        updatedAt = Instant.now(),
    )

    // ============================================================
    // AI整形関連の操作
    // ============================================================

    /**
     * AI整形を開始する
     */
    fun startFormatting(): VoiceMemo = copy(
        formatting = Formatting.processing(),
        updatedAt = Instant.now(),
    )

    /**
     * AI整形を完了する
     */
    fun completeFormatting(
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        fallbackUsed: Boolean = false,
    ): VoiceMemo = copy(
        formatting = Formatting.completed(
            title = title,
            content = content,
            tags = tags,
            fallbackUsed = fallbackUsed,
        ),
        updatedAt = Instant.now(),
    )

    /**
     * AI整形を失敗としてマークする
     */
    fun failFormatting(): VoiceMemo = copy(
        formatting = Formatting.failed(),
        updatedAt = Instant.now(),
    )

    // ============================================================
    // ユーザー編集操作（整形完了後のみ可能）
    // ============================================================

    /**
     * タイトルを変更する（整形完了後のみ）
     */
    fun changeTitle(newTitle: String): VoiceMemo {
        require(formatting.isCompleted) { "Cannot change title before formatting is completed" }
        val normalizedTitle = newTitle.trim()
        require(normalizedTitle.isNotBlank()) { "Title must not be blank" }
        return copy(
            formatting = formatting.copy(title = normalizedTitle),
            updatedAt = Instant.now(),
        )
    }

    /**
     * 本文を変更する（整形完了後のみ）
     */
    fun changeContent(newContent: String): VoiceMemo {
        require(formatting.isCompleted) { "Cannot change content before formatting is completed" }
        require(newContent.isNotBlank()) { "Content must not be blank" }
        return copy(
            formatting = formatting.copy(content = newContent),
            updatedAt = Instant.now(),
        )
    }

    /**
     * タグを変更する（整形完了後のみ）
     */
    fun changeTags(newTags: List<String>): VoiceMemo {
        require(formatting.isCompleted) { "Cannot change tags before formatting is completed" }
        val sanitizedTags = newTags.map { it.trim() }.filter { it.isNotEmpty() }
        return copy(
            formatting = formatting.copy(tags = sanitizedTags),
            updatedAt = Instant.now(),
        )
    }

    /**
     * 文字起こしテキストを編集する（文字起こし完了後のみ）
     */
    fun editTranscription(newText: String): VoiceMemo {
        require(transcription.isCompleted) { "Cannot edit transcription before it is completed" }
        require(newText.isNotBlank()) { "Transcription text must not be blank" }
        return copy(
            transcription = transcription.copy(text = newText),
            updatedAt = Instant.now(),
        )
    }

    /**
     * 論理削除する
     */
    fun markAsDeleted(): VoiceMemo = if (deleted) this else copy(
        deleted = true,
        updatedAt = Instant.now(),
    )

    // ============================================================
    // 便利なアクセサ（整形完了後のみ有効）
    // ============================================================

    /**
     * タイトル（整形完了後のみ、それ以外は null）
     */
    val title: String?
        get() = formatting.title

    /**
     * 本文（整形完了後のみ、それ以外は null）
     */
    val content: String?
        get() = formatting.content

    /**
     * タグ（整形完了後のみ、それ以外は空リスト）
     */
    val tags: List<String>
        get() = formatting.tags

    /**
     * 文字起こしテキスト（文字起こし完了後のみ、それ以外は null）
     */
    val transcriptionText: String?
        get() = transcription.text

    companion object {
        /**
         * 新しい VoiceMemo を作成する（処理待ち状態）
         */
        fun create(
            userId: UUID,
            languageCode: String = "ja-JP",
            id: UUID = UUID.randomUUID(),
        ): VoiceMemo = VoiceMemo(
            id = id,
            userId = userId,
            transcription = Transcription.pending(languageCode),
            formatting = Formatting.pending(),
            deleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

        /**
         * 処理完了済みの VoiceMemo を作成する（既存データの復元用）
         */
        fun restore(
            id: UUID,
            userId: UUID,
            transcription: Transcription,
            formatting: Formatting,
            deleted: Boolean,
            createdAt: Instant,
            updatedAt: Instant,
        ): VoiceMemo = VoiceMemo(
            id = id,
            userId = userId,
            transcription = transcription,
            formatting = formatting,
            deleted = deleted,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
