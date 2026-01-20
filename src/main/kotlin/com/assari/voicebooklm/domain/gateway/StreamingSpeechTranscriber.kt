package com.assari.voicebooklm.domain.gateway

import com.assari.voicebooklm.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.Flow

/**
 * ストリーミング音声文字起こしゲートウェイ
 *
 * リアルタイムで音声データを受け取り、文字起こし結果をストリームとして返す
 */
interface StreamingSpeechTranscriber {
    /**
     * ストリーミング文字起こしセッションを開始
     *
     * @param config セッション設定
     * @return 音声データを送信し、結果を受信するためのセッション
     */
    suspend fun startSession(config: StreamingTranscriptionConfig): StreamingTranscriptionSession
}

/**
 * ストリーミング文字起こし設定
 */
data class StreamingTranscriptionConfig(
    /** 言語コード（例: ja-JP, en-US） */
    val languageCode: String = "ja-JP",
    /** サンプルレート（Hz） */
    val sampleRateHertz: Int = 16000,
    /** 中間結果を返すかどうか */
    val enableInterimResults: Boolean = true,
)

/**
 * ストリーミング文字起こしセッション
 */
interface StreamingTranscriptionSession {
    /** 文字起こし結果のFlow */
    val results: Flow<TranscriptionResult>

    /**
     * セッションが準備完了するまで待機
     * gRPCストリームが確立され、音声データを受信できる状態になるまで待機します
     *
     * @param timeout タイムアウト時間
     * @return 準備完了した場合はtrue、タイムアウトした場合はfalse
     */
    suspend fun awaitReady(timeout: kotlin.time.Duration = kotlin.time.Duration.parse("PT10S")): Boolean

    /**
     * 音声データを送信
     *
     * @param audioData LINEAR16 (PCM 16bit) 形式の音声データ
     */
    suspend fun sendAudio(audioData: ByteArray)

    /**
     * セッションを終了
     */
    suspend fun close()
}
