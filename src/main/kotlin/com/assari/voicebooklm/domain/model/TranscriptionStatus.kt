package com.assari.voicebooklm.domain.model

/**
 * 文字起こし処理のステータス
 */
enum class TranscriptionStatus {
    /** 処理待ち */
    PENDING,
    /** 処理中 */
    PROCESSING,
    /** 完了 */
    COMPLETED,
    /** 失敗 */
    FAILED,
}
