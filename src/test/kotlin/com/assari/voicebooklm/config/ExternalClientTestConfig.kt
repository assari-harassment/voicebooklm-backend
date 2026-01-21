package com.assari.voicebooklm.config

import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.gateway.StreamingSpeechTranscriber
import com.assari.voicebooklm.domain.gateway.StreamingTranscriptionConfig
import com.assari.voicebooklm.domain.gateway.StreamingTranscriptionSession
import com.assari.voicebooklm.domain.model.TranscriptionResult
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * テストプロファイル用の外部クライアント代替実装。
 * 外部 API に依存しないスタブでユースケースを動かす。
 */
@Configuration
@Profile("test")
class ExternalClientTestConfig {

    @Bean
    fun streamingSpeechTranscriber(): StreamingSpeechTranscriber = FakeStreamingSpeechTranscriber()

    @Bean
    fun memoFormatter(): MemoFormatter = object : MemoFormatter {
        override suspend fun format(command: MemoFormatCommand): MemoFormatResult {
            // テストではシンプルにタイトル・本文を組み立てる
            return MemoFormatResult(
                title = "stub title",
                content = command.transcript,
                tags = emptyList(),
            )
        }
    }
}

/**
 * テスト用のフェイクストリーミング文字起こし実装
 */
class FakeStreamingSpeechTranscriber : StreamingSpeechTranscriber {
    override suspend fun startSession(config: StreamingTranscriptionConfig): StreamingTranscriptionSession {
        return FakeStreamingTranscriptionSession()
    }
}

/**
 * テスト用のフェイクストリーミングセッション
 */
class FakeStreamingTranscriptionSession : StreamingTranscriptionSession {
    private val _results = MutableSharedFlow<TranscriptionResult>()

    override val results: Flow<TranscriptionResult> = _results

    override suspend fun awaitReady(timeout: Duration): Boolean {
        // テスト用: 即座に準備完了とする
        return true
    }

    override suspend fun sendAudio(audioData: ByteArray) {
        // テスト用: 固定のテキストを返す
        _results.emit(TranscriptionResult(text = "stub transcription", isFinal = true))
    }

    override suspend fun close() {
        // no-op
    }
}
