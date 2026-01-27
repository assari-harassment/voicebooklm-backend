package com.assari.voicebooklm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * レート制限設定プロパティ
 *
 * application.yml の rate-limit セクションから設定を読み込む。
 */
@ConfigurationProperties(prefix = "rate-limit")
class RateLimitProperties {
    var enabled: Boolean = true
    var auth: EndpointRateLimitConfig = EndpointRateLimitConfig()

    class EndpointRateLimitConfig {
        var capacity: Long = 0
        var refillTokens: Long = 0
        var refillDurationSeconds: Long = 0

        fun getRefillDuration(): Duration = Duration.ofSeconds(refillDurationSeconds)
    }
}
