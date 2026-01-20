package com.assari.voicebooklm.config

import com.assari.voicebooklm.infrastructure.websocket.TranscriptionWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

/**
 * WebSocket 設定
 *
 * リアルタイム文字起こし用の WebSocket エンドポイントを設定する。
 */
@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val transcriptionWebSocketHandler: TranscriptionWebSocketHandler,
) : WebSocketConfigurer {

    companion object {
        /** バイナリメッセージの最大サイズ (64KB) */
        private const val MAX_BINARY_MESSAGE_SIZE = 64 * 1024

        /** テキストメッセージの最大サイズ (64KB) */
        private const val MAX_TEXT_MESSAGE_SIZE = 64 * 1024
    }

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(transcriptionWebSocketHandler, "/ws/transcription")
            .setAllowedOrigins("*") // 本番環境では適切なオリジンに制限する
    }

    /**
     * WebSocket コンテナ設定
     *
     * バッファサイズを増やして大きな音声データを受信できるようにする。
     */
    @Bean
    fun createWebSocketContainer(): ServletServerContainerFactoryBean {
        val container = ServletServerContainerFactoryBean()
        container.setMaxBinaryMessageBufferSize(MAX_BINARY_MESSAGE_SIZE)
        container.setMaxTextMessageBufferSize(MAX_TEXT_MESSAGE_SIZE)
        return container
    }
}
