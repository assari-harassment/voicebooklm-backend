package com.assari.voicebooklm.infrastructure.websocket

import com.assari.voicebooklm.domain.gateway.StreamingSpeechTranscriber
import com.assari.voicebooklm.domain.gateway.StreamingTranscriptionConfig
import com.assari.voicebooklm.domain.gateway.StreamingTranscriptionSession
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 文字起こしセッション管理
 *
 * ユーザーあたり1セッションの制限を管理する。
 */
@Component
class TranscriptionSessionManager(
    private val streamingSpeechTranscriber: StreamingSpeechTranscriber,
) {
    private val logger = LoggerFactory.getLogger(TranscriptionSessionManager::class.java)

    // userId -> WebSocketセッションID のマッピング
    private val userSessions = ConcurrentHashMap<UUID, String>()
    // WebSocketセッションID -> 文字起こしセッション のマッピング
    private val transcriptionSessions = ConcurrentHashMap<String, StreamingTranscriptionSession>()

    /**
     * 新しいセッションを作成
     *
     * @param userId ユーザーID
     * @param webSocketSessionId WebSocketセッションID
     * @param config 文字起こし設定
     * @return 既存セッションがある場合は null、それ以外は新しいセッション
     */
    suspend fun createSession(
        userId: UUID,
        webSocketSessionId: String,
        config: StreamingTranscriptionConfig = StreamingTranscriptionConfig(),
    ): StreamingTranscriptionSession? {
        // アトミックにセッションIDを登録（既に存在する場合は以前の値が返る）
        val existingSessionId = userSessions.putIfAbsent(userId, webSocketSessionId)
        if (existingSessionId != null && existingSessionId != webSocketSessionId) {
            logger.warn("User {} already has an active session: {}", userId, existingSessionId)
            return null
        }

        // 新しいセッションを作成（失敗時はロールバック）
        val session = try {
            streamingSpeechTranscriber.startSession(config)
        } catch (e: Exception) {
            userSessions.remove(userId)
            throw e
        }
        transcriptionSessions[webSocketSessionId] = session

        logger.info("Created transcription session for user {}: webSocketSession={}",
            userId, webSocketSessionId)

        return session
    }

    /**
     * セッションを取得
     */
    fun getSession(webSocketSessionId: String): StreamingTranscriptionSession? {
        return transcriptionSessions[webSocketSessionId]
    }

    /**
     * セッションを削除
     *
     * 削除順序: transcriptionSessions → session.close() → userSessions
     * userSessions を最後に削除することで、古いセッションが終了する前に
     * 新しいセッションが作成されることを防ぐ。
     */
    suspend fun removeSession(userId: UUID, webSocketSessionId: String) {
        val session = transcriptionSessions.remove(webSocketSessionId)
        session?.close()
        userSessions.remove(userId)

        logger.info("Removed transcription session: user={}, webSocketSession={}", userId, webSocketSessionId)
    }

    /**
     * ユーザーがアクティブなセッションを持っているか確認
     */
    fun hasActiveSession(userId: UUID): Boolean {
        return userSessions.containsKey(userId)
    }

    /**
     * ユーザーのアクティブセッションID を取得
     */
    fun getActiveSessionId(userId: UUID): String? {
        return userSessions[userId]
    }
}
