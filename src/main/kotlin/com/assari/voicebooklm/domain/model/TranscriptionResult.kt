package com.assari.voicebooklm.domain.model

/**
 * リアルタイム文字起こし結果
 */
data class TranscriptionResult(
    /** 文字起こしテキスト */
    val text: String,
    /** 確定フラグ（true: 最終結果、false: 中間結果） */
    val isFinal: Boolean,
)
