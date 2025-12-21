package com.assari.voicebooklm.config

import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.infrastructure.api.speech.GoogleSpeechTranscriber
import com.assari.voicebooklm.infrastructure.api.speech.SpeechTranscriptionCommand
import com.assari.voicebooklm.infrastructure.api.speech.SpeechTranscriptionResult
import io.mockk.coEvery
import io.mockk.mockk
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
    fun speechTranscriber(): GoogleSpeechTranscriber = mockk<GoogleSpeechTranscriber>().also { mock ->
        coEvery { mock.transcribe(any()) } answers {
            val command = firstArg<SpeechTranscriptionCommand>()
            SpeechTranscriptionResult(
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
