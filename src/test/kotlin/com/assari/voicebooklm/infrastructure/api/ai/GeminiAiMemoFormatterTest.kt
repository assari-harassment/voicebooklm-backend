package com.assari.voicebooklm.infrastructure.api.ai

import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatCommand
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

/**
 * Gemini クライアントの正常系・フォールバックをモックサーバで検証。
 */
class GeminiAiMemoFormatterTest {

    private val server = MockWebServer()

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `200 応答で生成結果を返す`() {
        val responseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "# 会議メモ\n\n- 議題: テスト\n\nTags: voice, memo" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson),
        )
        server.start()

        val formatter = GeminiAiMemoFormatter(
            webClient = WebClient.builder().build(),
            apiKey = "dummy",
            model = "gemini-1.5-flash",
            timeout = Duration.ofSeconds(5),
            baseUrl = server.url("/").toString().removeSuffix("/"),
        )

        val draft = runBlocking {
            formatter.format(
                AiMemoFormatCommand(
                    userId = UUID.randomUUID(),
                    transcript = "会議内容",
                ),
            )
        }

        assertEquals("# 会議メモ", draft.title)
        assertEquals(listOf("voice", "memo"), draft.tags)
        org.junit.jupiter.api.Assertions.assertTrue(draft.content.contains("# 会議メモ"))
        org.junit.jupiter.api.Assertions.assertTrue(draft.content.contains("Tags: voice, memo"))
    }

    @Test
    fun `500 応答でフォールバック`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json"),
        )
        server.start()

        val formatter = GeminiAiMemoFormatter(
            webClient = WebClient.builder().build(),
            apiKey = "dummy",
            timeout = Duration.ofSeconds(1),
            baseUrl = server.url("/").toString().removeSuffix("/"),
        )

        val transcript = "議事録 テスト"
        val draft = runBlocking {
            formatter.format(
                AiMemoFormatCommand(
                    userId = UUID.randomUUID(),
                    transcript = transcript,
                ),
            )
        }

        assertEquals(transcript, draft.content)
        assertEquals("議事録 テスト", draft.title)
        assertEquals(emptyList<String>(), draft.tags)
    }

    @Test
    fun `レスポンス遅延でタイムアウトしフォールバック`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
                .setBodyDelay(2, java.util.concurrent.TimeUnit.SECONDS),
        )
        server.start()

        val formatter = GeminiAiMemoFormatter(
            webClient = WebClient.builder().build(),
            apiKey = "dummy",
            timeout = Duration.ofMillis(200),
            baseUrl = server.url("/").toString().removeSuffix("/"),
        )

        val draft = runBlocking {
            formatter.format(
                AiMemoFormatCommand(
                    userId = UUID.randomUUID(),
                    transcript = "遅延テスト",
                ),
            )
        }

        assertEquals("遅延テスト", draft.content)
        assertEquals("遅延テスト", draft.title)
    }
}
