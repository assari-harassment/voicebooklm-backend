package com.assari.voicebooklm.domain.model

/**
 * 文字起こし結果を表す値オブジェクト
 *
 * 音声データから生成されたテキストとその処理状態を保持する。
 */
data class Transcription(
    /** 処理ステータス */
    val status: TranscriptionStatus,
    /** 文字起こしテキスト（処理完了前は null） */
    val text: String?,
    /** 言語コード（例: ja-JP） */
    val languageCode: String,
    /** フォールバックが使用されたかどうか */
    val fallbackUsed: Boolean = false,
) {
    init {
        validate()
    }

    private fun validate() {
        require(languageCode.isNotBlank()) { "languageCode must not be blank" }
        if (status == TranscriptionStatus.COMPLETED) {
            require(!text.isNullOrBlank()) { "text must not be blank when status is COMPLETED" }
        }
    }

    /**
     * 文字起こしが完了しているかどうか
     */
    val isCompleted: Boolean
        get() = status == TranscriptionStatus.COMPLETED

    /**
     * 文字起こしが失敗したかどうか
     */
    val isFailed: Boolean
        get() = status == TranscriptionStatus.FAILED

    companion object {
        /**
         * 処理待ち状態の Transcription を作成
         */
        fun pending(languageCode: String = "ja-JP"): Transcription = Transcription(
            status = TranscriptionStatus.PENDING,
            text = null,
            languageCode = languageCode,
            fallbackUsed = false,
        )

        /**
         * 処理中状態の Transcription を作成
         */
        fun processing(languageCode: String = "ja-JP"): Transcription = Transcription(
            status = TranscriptionStatus.PROCESSING,
            text = null,
            languageCode = languageCode,
            fallbackUsed = false,
        )

        /**
         * 完了状態の Transcription を作成
         */
        fun completed(
            text: String,
            languageCode: String = "ja-JP",
            fallbackUsed: Boolean = false,
        ): Transcription {
            require(text.isNotBlank()) { "Transcription text must not be blank when completed" }
            return Transcription(
                status = TranscriptionStatus.COMPLETED,
                text = text,
                languageCode = languageCode,
                fallbackUsed = fallbackUsed,
            )
        }

        /**
         * 失敗状態の Transcription を作成
         */
        fun failed(languageCode: String = "ja-JP"): Transcription = Transcription(
            status = TranscriptionStatus.FAILED,
            text = null,
            languageCode = languageCode,
            fallbackUsed = false,
        )
    }
}
