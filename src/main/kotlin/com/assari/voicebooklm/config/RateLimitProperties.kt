package com.assari.voicebooklm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * レート制限設定プロパティ
 *
 * 認証エンドポイントへのリクエスト数を制限するための設定。
 * IP アドレスごとにリクエスト数を管理する。
 */
@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    /**
     * レート制限を有効にするかどうか
     */
    val enabled: Boolean = true,

    /**
     * 認証エンドポイント用の設定
     */
    val auth: EndpointRateLimitConfig = EndpointRateLimitConfig()
) {
    /**
     * エンドポイント別レート制限設定
     */
    data class EndpointRateLimitConfig(
        /**
         * バケット容量（最大リクエスト数）
         */
        val capacity: Long = 10,

        /**
         * リフィル数（補充されるトークン数）
         */
        val refillTokens: Long = 10,

        /**
         * リフィル間隔（秒）
         */
        val refillDurationSeconds: Long = 60
    ) {
        /**
         * リフィル間隔を Duration として取得
         */
        fun getRefillDuration(): Duration = Duration.ofSeconds(refillDurationSeconds)
    }
}
