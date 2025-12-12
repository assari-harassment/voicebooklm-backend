package com.assari.voicebooklm.infrastructure.api.ai

import com.assari.voicebooklm.usecase.memo.port.AiMemoDraft
import com.assari.voicebooklm.usecase.memo.port.AiMemoFormatCommand
import com.assari.voicebooklm.usecase.memo.port.AiMemoFormatter
import java.time.Duration
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Gemini（Flash）を用いてメモを整形するクライアント実装。
 * 失敗時はフォールバックとしてプレーンなメモを生成する。
 */
class GeminiAiMemoFormatter(
    private val webClient: WebClient,
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash",
    private val timeout: Duration = Duration.ofSeconds(60),
    baseUrl: String = "https://generativelanguage.googleapis.com",
) : AiMemoFormatter {

    private val client = webClient.mutate()
        .baseUrl(baseUrl)
        .build()

    override suspend fun format(command: AiMemoFormatCommand): AiMemoDraft {
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
                ?.toDraft(command.transcript)
                ?: fallbackDraft(command.transcript)
        }.getOrElse { fallbackDraft(command.transcript) }
    }

    private fun fallbackDraft(transcript: String): AiMemoDraft {
        val normalizedTranscript = transcript.trim()
        val title = normalizedTranscript.lines().firstOrNull().orEmpty().take(40).ifBlank { "ボイスメモ" }
        val content = normalizedTranscript.ifBlank { "音声内容を取得できませんでした。" }
        return AiMemoDraft(
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
        fun fromTranscript(transcript: String): GeminiRequest =
            GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(
                                text = """
                                    次の文字起こしを要約し、Markdown のメモを生成してください。
                                    - 50 文字以内のタイトル
                                    - Markdown本文
                                    - 2-4 個の英単語タグ
                                    
                                    Transcript:
                                    $transcript
                                """.trimIndent(),
                            ),
                        ),
                    ),
                ),
            )
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
    fun toDraft(fallbackTranscript: String): AiMemoDraft {
        val text = candidates.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            .orEmpty()
        if (text.isBlank()) return AiMemoDraft(
            title = "ボイスメモ",
            content = fallbackTranscript.ifBlank { "音声内容を取得できませんでした。" },
            tags = emptyList(),
        )

        val title = text.lineSequence().firstOrNull().orEmpty().take(50).ifBlank { "ボイスメモ" }
        val tags = parseTags(text)

        return AiMemoDraft(
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
