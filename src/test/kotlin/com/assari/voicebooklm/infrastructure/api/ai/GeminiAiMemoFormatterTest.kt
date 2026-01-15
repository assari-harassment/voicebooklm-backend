package com.assari.voicebooklm.infrastructure.api.ai

import com.assari.voicebooklm.config.GeminiProperties
import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Gemini クライアントの正常系・フォールバックをモックサーバで検証。
 */
class GeminiAiMemoFormatterTest {

    private val server = MockWebServer()

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun createGeminiProperties(
        apiKey: String = "dummy",
        model: String = "gemini-2.0-flash",
        timeoutSeconds: Long = 5,
        baseUrl: String,
    ): GeminiProperties = GeminiProperties(
        apiKey = apiKey,
        model = model,
        timeoutSeconds = timeoutSeconds,
        baseUrl = baseUrl,
    )

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
                .setHeader("GeminiResponseContent-Type", "application/json")
                .setBody(responseJson),
        )
        server.start()

        val formatter = GeminiAiMemoFormatter(
            geminiProperties = createGeminiProperties(
                baseUrl = server.url("/").toString().removeSuffix("/"),
            ),
        )

        val draft = runBlocking {
            formatter.format(
                MemoFormatCommand(
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
                .setHeader("GeminiResponseContent-Type", "application/json"),
        )
        server.start()

        val formatter = GeminiAiMemoFormatter(
            geminiProperties = createGeminiProperties(
                timeoutSeconds = 1,
                baseUrl = server.url("/").toString().removeSuffix("/"),
            ),
        )

        val transcript = "議事録 テスト"
        val draft = runBlocking {
            formatter.format(
                MemoFormatCommand(
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
                .setHeader("GeminiResponseContent-Type", "application/json")
                .setBody("{}")
                .setBodyDelay(2, java.util.concurrent.TimeUnit.SECONDS),
        )
        server.start()

        // timeoutSeconds は Long（秒単位）なので、200ms は 1 秒未満として設定
        val formatter = GeminiAiMemoFormatter(
            geminiProperties = createGeminiProperties(
                timeoutSeconds = 1, // 最小1秒（元は200msだったがtimeoutSecondsはLongなので）
                baseUrl = server.url("/").toString().removeSuffix("/"),
            ),
        )

        val draft = runBlocking {
            formatter.format(
                MemoFormatCommand(
                    userId = UUID.randomUUID(),
                    transcript = "遅延テスト",
                ),
            )
        }

        assertEquals("遅延テスト", draft.content)
        assertEquals("遅延テスト", draft.title)
    }
}
