package com.assari.voicebooklm.config

import com.assari.voicebooklm.usecase.memo.client.AiMemoDraft
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatCommand
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatter
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscription
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriptionCommand
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriber
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
        override suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscription {
            // テストではアップロード内容に依存せず固定文字列を返す
            return SpeechTranscription(
                text = "stub transcription",
                languageCode = command.languageCode,
            )
        }
    }

    @Bean
    fun aiMemoFormatter(): AiMemoFormatter = object : AiMemoFormatter {
        override suspend fun format(command: AiMemoFormatCommand): AiMemoDraft {
            // テストではシンプルにタイトル・本文を組み立てる
            return AiMemoDraft(
                title = "stub title",
                content = command.transcript,
                tags = emptyList(),
            )
        }
    }
}
