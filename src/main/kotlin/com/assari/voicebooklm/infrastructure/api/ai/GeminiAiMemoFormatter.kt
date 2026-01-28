package com.assari.voicebooklm.infrastructure.api.ai

import com.assari.voicebooklm.config.GeminiProperties
import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import java.time.Duration
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import org.slf4j.LoggerFactory

/**
 * Gemini（Flash）を用いてメモを整形するクライアント実装。
 * 失敗時はフォールバックとしてプレーンなメモを生成する。
 */
@Component
@Profile("!test")
class GeminiAiMemoFormatter(
    geminiProperties: GeminiProperties,
) : MemoFormatter {
    private val logger = LoggerFactory.getLogger(GeminiAiMemoFormatter::class.java)
    private val apiKey: String = geminiProperties.apiKey
    private val model: String = geminiProperties.model
    private val timeout: Duration = Duration.ofSeconds(geminiProperties.timeoutSeconds)
    private val baseUrl: String = geminiProperties.baseUrl

    // WebClient を Bean にせず、必要なタイムアウト付きでここに閉じ込める
    private val client = WebClient.builder()
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(timeout)
            ),
        )
        .baseUrl(baseUrl)
        .build()

    override suspend fun format(command: MemoFormatCommand): MemoFormatResult {
        val request = GeminiRequest.fromTranscript(
            transcript = command.transcript,
            existingFolderPaths = command.existingFolderPaths,
            folderSpecifiedByUser = command.folderSpecifiedByUser,
            userSpecifiedTags = command.userSpecifiedTags,
        )

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
                .onErrorResume { error ->
                    logger.error("Gemini API call failed", error)
                    Mono.empty()
                }
                .awaitSingleOrNull()
                ?.toResult(command.transcript)
                ?: run {
                    logger.warn("Gemini API returned empty response, using fallback")
                    fallbackResult(command.transcript)
                }
        }.getOrElse { error ->
            logger.error("Unexpected error during Gemini API call", error)
            fallbackResult(command.transcript)
        }
    }

    private fun fallbackResult(transcript: String): MemoFormatResult {
        val normalizedTranscript = transcript.trim()
        val title = normalizedTranscript.lines().firstOrNull().orEmpty().take(40).ifBlank { "ボイスメモ" }
        val content = normalizedTranscript.ifBlank { "音声内容を取得できませんでした。" }
        return MemoFormatResult(
            title = title,
            content = content,
            tags = emptyList(),
            folderPath = null,
        )
    }
}

data class GeminiRequest(
    val contents: List<GeminiRequestContent>,
) {
    companion object {
        fun fromTranscript(
            transcript: String,
            existingFolderPaths: List<String> = emptyList(),
            folderSpecifiedByUser: Boolean = false,
            userSpecifiedTags: List<String> = emptyList(),
        ): GeminiRequest {
            val folderSection = when {
                folderSpecifiedByUser -> """
                    保存先フォルダーはユーザーが指定済みです。
                    フォルダー行には「未分類」と出力してください。
                """.trimIndent()
                existingFolderPaths.isNotEmpty() -> {
                    val folderList = existingFolderPaths.joinToString("\n") { "- $it" }
                    """
                    【既存フォルダー】
                    $folderList

                    上記から内容に最も合うフォルダーを1つ選択してください。
                    適切なものがなければ新規作成してください（最大3階層）。
                    """.trimIndent()
                }
                else -> """
                    フォルダー名を1〜3階層で作成してください（例: "仕事/会議"）。
                    分類が難しい場合は「未分類」にしてください。
                """.trimIndent()
            }

            val tagSection = if (userSpecifiedTags.isNotEmpty()) {
                val tagList = userSpecifiedTags.joinToString(", ")
                """
                【ユーザー指定タグ】
                以下のタグはユーザーが事前に指定したものです。タグ行にはこれらを必ず含めてください。
                - $tagList
                上記に加えて、内容に合うタグを必要な分だけ追加し、タグ行は全体で2〜4個（カンマ区切り）にしてください。
                """.trimIndent()
            } else {
                ""
            }

            val prompt = """
                あなたは音声メモの文字起こしを「読みやすい文章」に整えるアシスタントです。
                以下の文字起こしテキストをもとに、内容の流れを保ちながら、
                人が書いたような自然な文章のMarkdownメモを作成してください。

                【出力形式（厳守）】
                - 出力は必ず次の順で行う（順番変更禁止）
                  タイトル: <30文字以内>
                  フォルダー: <1つだけ>
                  タグ: <2〜4個をカンマ区切り（例: 会議, メモ）>
                  ---
                  <本文（Markdown）>
                - 1〜4行目（タイトル/フォルダー/タグ/---）は必ず出力する
                - タイトル/フォルダー/タグの行はそれぞれ1行のみ（候補・補足・説明を書かない）
                - 本文内にメタ情報（タイトル/フォルダー/タグ/---）を書かない
                - 前置き・説明・注意書きは一切書かない

                【フォルダー選定ルール】
                - 既存フォルダーがある場合は、内容に最も合うものを1つ選ぶ
                - 適切なものがなければ新規作成（最大3階層）
                - 判断が難しい場合は「未分類」

                $folderSection

                $tagSection

                【基本方針】
                - 箇条書きよりも文章を優先する
                - 会話調・口語表現は自然な書き言葉に直す
                - フィラー（えー、あの等）、言い直し、重複は削除する
                - 文脈が分かるよう主語や接続語を補う（推測はしない）

                【Markdownのルール】
                - 内容のまとまりごとに見出し(##, ###)を使う
                - 各見出しの直下は短い段落の文章にする
                - 1段落は2〜4文程度
                - 行は適度に改行する

                【強調と整理】
                - **結論・重要な判断・期限・数値・重要語**は太字
                - 並列関係が明確な場合のみ箇条書きを使用
                - TODOがあれば最後にチェックリストでまとめる

                【禁止事項】
                - 元の発言の単なる書き写し
                - 箇条書きだけの本文
                - 過度な要約
                - 事実の推測や補完

                【文字起こしテキスト】
                $transcript
            """.trimIndent()

            val part = GeminiRequestPart(text = prompt)
            val content = GeminiRequestContent(parts = listOf(part))
            return GeminiRequest(contents = listOf(content))
        }
    }
}

data class GeminiRequestContent(
    val parts: List<GeminiRequestPart>,
)

data class GeminiRequestPart(
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
            folderPath = null,
        )

        val title = parseTitle(text)
        val tags = parseTags(text)
        val folderPath = parseFolderPath(text)
        val content = parseContent(text)

        return MemoFormatResult(
            title = title,
            content = content,
            tags = tags,
            folderPath = folderPath,
        )
    }

    private fun parseTitle(text: String): String {
        val lines = text.lines()
        val titleLine = lines.firstOrNull { it.contains("タイトル", ignoreCase = true) || it.startsWith("title", ignoreCase = true) }
        val extracted = titleLine
            ?.substringAfter(":")
            ?.trim()
            ?.take(50)
            ?.ifBlank { null }

        return extracted ?: text.lineSequence().firstOrNull().orEmpty().take(50).ifBlank { "ボイスメモ" }
    }

    private fun parseTags(text: String): List<String> {
        val lines = text.lines()
        val tagLine = lines.firstOrNull { it.contains("tags", ignoreCase = true) || it.contains("タグ") }
        val normalized = tagLine
            ?.substringAfter(":")
            ?.ifBlank { tagLine }
            ?: return emptyList()

        return normalized.split(",", "・", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.equals("tags", ignoreCase = true) }
            .take(4)
    }

    private fun parseFolderPath(text: String): String? {
        val lines = text.lines()
        val folderLine = lines.firstOrNull {
            it.contains("フォルダー", ignoreCase = true) || it.contains("folder", ignoreCase = true)
        }
        val folderPath = folderLine
            ?.substringAfter(":")
            ?.trim()
            ?.ifBlank { null }

        // 「未分類」または空の場合は null を返す
        return if (folderPath.isNullOrBlank() || folderPath == "未分類" || folderPath == "null") {
            null
        } else {
            folderPath
        }
    }

    /**
     * レスポンステキストから本文部分だけを抽出する
     * 「---」以降の部分を本文として扱い、メタデータ（タイトル、フォルダー、タグ）は除外する
     */
    private fun parseContent(text: String): String {
        // 「---」で分割して本文部分を取得
        val parts = text.split("---", limit = 2)
        if (parts.size == 2) {
            return parts[1].trim()
        }

        // 「---」がない場合は、メタデータ行を除外して本文を抽出
        val lines = text.lines()
        val contentLines = lines.dropWhile { line ->
            val trimmed = line.trim()
            trimmed.startsWith("タイトル:", ignoreCase = true) ||
            trimmed.startsWith("title:", ignoreCase = true) ||
            trimmed.startsWith("フォルダー:", ignoreCase = true) ||
            trimmed.startsWith("folder:", ignoreCase = true) ||
            trimmed.startsWith("タグ:", ignoreCase = true) ||
            trimmed.startsWith("tags:", ignoreCase = true) ||
            trimmed.isBlank()
        }

        return contentLines.joinToString("\n").trim()
    }
}

data class GeminiResponseContent(
    val parts: List<GeminiResponsePart> = emptyList(),
)

data class GeminiResponsePart(
    val text: String? = null,
)

data class Candidate(
    val content: GeminiResponseContent? = null,
)
