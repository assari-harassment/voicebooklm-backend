package com.assari.voicebooklm.infrastructure.websocket

import com.assari.voicebooklm.domain.gateway.StreamingTranscriptionConfig
import com.assari.voicebooklm.infrastructure.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * リアルタイム文字起こし WebSocket ハンドラー
 *
 * クライアントから音声データ（バイナリ）を受け取り、
 * 文字起こし結果（JSON）を返す。
 *
 * 接続時は token クエリパラメータで JWT 認証を行う。
 */
@Component
class TranscriptionWebSocketHandler(
    private val sessionManager: TranscriptionSessionManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper,
) : BinaryWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(TranscriptionWebSocketHandler::class.java)

    // WebSocketセッションID -> ユーザーID のマッピング
    private val sessionUsers = ConcurrentHashMap<String, UUID>()

    // WebSocketセッションID -> CoroutineScope のマッピング
    private val sessionScopes = ConcurrentHashMap<String, CoroutineScope>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = authenticateSession(session)
        if (userId == null) {
            logger.warn("Authentication failed for WebSocket session: {}", session.id)
            session.close(CloseStatus.POLICY_VIOLATION.withReason("認証に失敗しました"))
            return
        }

        // 同時セッション数チェック
        if (sessionManager.hasActiveSession(userId)) {
            val existingSessionId = sessionManager.getActiveSessionId(userId)
            if (existingSessionId != session.id) {
                logger.warn("User {} already has an active session: {}", userId, existingSessionId)
                session.close(CloseStatus.POLICY_VIOLATION.withReason("既にアクティブなセッションが存在します"))
                return
            }
        }

        sessionUsers[session.id] = userId
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        sessionScopes[session.id] = scope

        // READY メッセージを送信（クライアントは START を送信可能になる）
        val readyMessage = objectMapper.writeValueAsString(
            mapOf(
                "type" to "READY",
                "message" to "接続が確立されました。START メッセージを送信してください。",
            )
        )
        session.sendMessage(TextMessage(readyMessage))

        logger.info("WebSocket connection established: session={}, user={}", session.id, userId)
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val userId = sessionUsers[session.id] ?: return
        val scope = sessionScopes[session.id] ?: return

        scope.launch {
            try {
                val transcriptionSession = sessionManager.getSession(session.id)
                if (transcriptionSession != null) {
                    val audioData = message.payload.array()
                    transcriptionSession.sendAudio(audioData)
                }
            } catch (e: Exception) {
                logger.error("Error processing audio data for session {}", session.id, e)
            }
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val userId = sessionUsers[session.id] ?: return
        val scope = sessionScopes[session.id] ?: return

        try {
            val payload = message.payload
            logger.debug("Received text message from session {}: {}", session.id, payload)

            // ping/pong 対応
            if (payload == "ping") {
                session.sendMessage(TextMessage("pong"))
                return
            }

            // JSON メッセージをパース
            val jsonNode = objectMapper.readTree(payload)
            val type = jsonNode.get("type")?.asText()

            when (type) {
                "START" -> {
                    val language = jsonNode.get("language")?.asText() ?: "ja-JP"
                    logger.info("Starting transcription for session {}, language: {}", session.id, language)

                    scope.launch {
                        try {
                            startTranscriptionSession(session, userId, language)
                        } catch (e: Exception) {
                            logger.error("Failed to start transcription session for user {}", userId, e)
                            val errorMessage = objectMapper.writeValueAsString(
                                mapOf(
                                    "type" to "error",
                                    "message" to "セッションの開始に失敗しました: ${e.message}",
                                )
                            )
                            session.sendMessage(TextMessage(errorMessage))
                        }
                    }
                }

                "STOP" -> {
                    logger.info("Stopping transcription for session {}", session.id)
                    scope.launch {
                        try {
                            sessionManager.removeSession(userId, session.id)
                            val stoppedMessage = objectMapper.writeValueAsString(
                                mapOf(
                                    "type" to "STOPPED",
                                    "message" to "文字起こしセッションが停止されました",
                                )
                            )
                            session.sendMessage(TextMessage(stoppedMessage))
                        } catch (e: Exception) {
                            logger.error("Failed to stop transcription session for session {}", session.id, e)
                        }
                    }
                }

                else -> {
                    logger.debug("Unknown message type: {}", type)
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling text message for session {}", session.id, e)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = sessionUsers.remove(session.id)
        val scope = sessionScopes.remove(session.id)

        if (scope != null) {
            scope.launch {
                if (userId != null) {
                    sessionManager.removeSession(userId, session.id)
                }
            }.invokeOnCompletion {
                // クリーンアップ完了後にスコープをキャンセル（他のコルーチンも停止）
                scope.cancel()
            }
        }

        logger.info(
            "WebSocket connection closed: session={}, user={}, status={}",
            session.id, userId, status
        )
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        logger.error("WebSocket transport error for session {}: {}", session.id, exception.message)
        try {
            session.close(CloseStatus.SERVER_ERROR)
        } catch (e: Exception) {
            logger.warn("Error closing session after transport error", e)
        }
    }

    private fun authenticateSession(session: WebSocketSession): UUID? {
        // クエリパラメータから token を取得
        val uri = session.uri ?: return null
        val query = uri.query ?: return null

        val encodedToken = query.split("&")
            .map { it.split("=", limit = 2) }
            .filter { it.size == 2 }
            .firstOrNull { it[0] == "token" }
            ?.get(1)
            ?: return null

        // URLデコード（トークンに特殊文字が含まれる場合に対応）
        val token = URLDecoder.decode(encodedToken, StandardCharsets.UTF_8)

        // トークン検証
        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
            return null
        }

        return jwtTokenProvider.getUserIdFromToken(token)
    }

    private suspend fun startTranscriptionSession(session: WebSocketSession, userId: UUID, languageCode: String) {
        val config = StreamingTranscriptionConfig(
            languageCode = languageCode,
            sampleRateHertz = 16000,
            enableInterimResults = true,
        )

        val transcriptionSession = sessionManager.createSession(userId, session.id, config)
        if (transcriptionSession == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("既にアクティブなセッションが存在します"))
            return
        }

        // 文字起こし結果をクライアントに送信
        val scope = sessionScopes[session.id] ?: return
        scope.launch {
            try {
                transcriptionSession.results.collect { result ->
                    if (session.isOpen) {
                        val response = TranscriptionResultMessage(
                            text = result.text,
                            isFinal = result.isFinal,
                        )
                        val json = objectMapper.writeValueAsString(response)
                        session.sendMessage(TextMessage(json))
                    }
                }
            } catch (e: Exception) {
                if (session.isOpen) {
                    logger.error("Error sending transcription results for session {}", session.id, e)
                }
            }
        }

        // 開始成功メッセージを送信
        val startedMessage = objectMapper.writeValueAsString(
            mapOf(
                "type" to "STARTED",
                "message" to "文字起こしセッションが開始されました",
                "language" to languageCode,
            )
        )
        session.sendMessage(TextMessage(startedMessage))
    }
}

data class TranscriptionResultMessage(
    val type: String = "transcription",
    val text: String,
    val isFinal: Boolean,
)
