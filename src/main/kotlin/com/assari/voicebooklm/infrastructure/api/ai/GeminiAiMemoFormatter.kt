package com.assari.voicebooklm.infrastructure.api.ai

import com.assari.voicebooklm.config.GeminiProperties
import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import java.time.Duration
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient

/**
 * Gemini（Flash）を用いてメモを整形するクライアント実装。
 * 失敗時はフォールバックとしてプレーンなメモを生成する。
 */
@Component
@Profile("!test")
class GeminiAiMemoFormatter(
    geminiProperties: GeminiProperties,
) : MemoFormatter {
    private val apiKey: String = geminiProperties.apiKey
    private val model: String = geminiProperties.model
    private val timeout: Duration = Duration.ofSeconds(geminiProperties.timeoutSeconds)
    private val baseUrl: String = geminiProperties.baseUrl

    // WebClient を Bean にせず、必要なタイムアウト付きでここに閉じ込める
    private val client = WebClient.builder()
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(timeout),
            ),
        )
        .baseUrl(baseUrl)
        .build()

    override suspend fun format(command: MemoFormatCommand): MemoFormatResult {
        val request = GeminiRequest.fromTranscript(command.transcript)

        return runCatching {
            client.post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v1beta/models/${model}:generateContent")
                        .queryParam("key", apiKey)
                        .build()
                }
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(GeminiResponse::class.java)
                .timeout(timeout)
                .onErrorResume { Mono.empty() } // フォールバックへ
                .awaitSingleOrNull()
                ?.toResult(command.transcript)
                ?: fallbackResult(command.transcript)
        }.getOrElse { fallbackResult(command.transcript) }
    }

    private fun fallbackResult(transcript: String): MemoFormatResult {
        val normalizedTranscript = transcript.trim()
        val title = normalizedTranscript.lines().firstOrNull().orEmpty().take(40).ifBlank { "ボイスメモ" }
        val content = normalizedTranscript.ifBlank { "音声内容を取得できませんでした。" }
        return MemoFormatResult(
            title = title,
            content = content,
            tags = emptyList(),
        )
    }
}

data class GeminiRequest(
    val contents: List<Content>,
) {
    companion object {
        fun fromTranscript(transcript: String): GeminiRequest {
            val prompt = """
                次の文字起こしを要約し、Markdown のメモを生成してください。
                - 30 文字以内のタイトル
                - 文字起こしを構造化したMarkdown本文
                - 2-4 個の日本語単語タグ

                Transcript:
                $transcript
            """.trimIndent()
            val part = Part(text = prompt)
            val content = Content(parts = listOf(part))
            return GeminiRequest(contents = listOf(content))
        }
    }
}

data class Content(
    val parts: List<Part>,
)

data class Part(
    val text: String,
)

data class GeminiResponse(
    val candidates: List<Candidate> = emptyList(),
) {
    fun toResult(fallbackTranscript: String): MemoFormatResult {
        val text = candidates.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            .orEmpty()
        if (text.isBlank()) return MemoFormatResult(
            title = "ボイスメモ",
            content = fallbackTranscript.ifBlank { "音声内容を取得できませんでした。" },
            tags = emptyList(),
        )

        val title = text.lineSequence().firstOrNull().orEmpty().take(50).ifBlank { "ボイスメモ" }
        val tags = parseTags(text)

        return MemoFormatResult(
            title = title,
            content = text,
            tags = tags,
        )
    }

    private fun parseTags(text: String): List<String> {
        val lines = text.lines()
        val tagLine = lines.firstOrNull { it.contains("tags", ignoreCase = true) || it.contains("タグ") }
        val normalized = tagLine
            ?.substringAfter(":")
            ?.ifBlank { tagLine }
            ?: return emptyList()

        return normalized.split(",", "・", " ")
            .map { it.trim().trimStart('#') }
            .filter { it.isNotBlank() }
            .filterNot { it.equals("tags", ignoreCase = true) }
            .take(4)
    }
}

data class Candidate(
    val content: Content? = null,
)
