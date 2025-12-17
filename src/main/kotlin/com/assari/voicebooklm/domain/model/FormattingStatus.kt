package com.assari.voicebooklm.domain.model

/**
 * AI整形処理のステータス
 */
enum class FormattingStatus {
    /** 処理待ち */
    PENDING,
    /** 処理中 */
    PROCESSING,
    /** 完了 */
    COMPLETED,
    /** 失敗 */
    FAILED,
}
