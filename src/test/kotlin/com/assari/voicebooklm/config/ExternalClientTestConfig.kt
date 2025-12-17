package com.assari.voicebooklm.config

import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.gateway.SpeechTranscriber
import com.assari.voicebooklm.domain.gateway.SpeechTranscriptionCommand
import com.assari.voicebooklm.domain.gateway.SpeechTranscriptionResult
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
    fun speechTranscriber(): SpeechTranscriber = object : SpeechTranscriber {
        override suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscriptionResult {
            // テストではアップロード内容に依存せず固定文字列を返す
            return SpeechTranscriptionResult(
                text = "stub transcription",
                languageCode = command.languageCode,
            )
        }
    }

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
