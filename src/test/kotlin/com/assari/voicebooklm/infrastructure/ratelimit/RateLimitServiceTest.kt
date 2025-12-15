package com.assari.voicebooklm.infrastructure.ratelimit

import com.assari.voicebooklm.config.RateLimitProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimitServiceTest {

    private lateinit var rateLimitService: RateLimitService

    @BeforeEach
    fun setUp() {
        val properties = createProperties(enabled = true, capacity = 5)
        rateLimitService = RateLimitService(properties)
        rateLimitService.clearCache()
    }

    @Test
    fun `should allow requests within limit`() {
        val clientIp = "192.168.1.1"

        // 5回のリクエストは許可される
        repeat(5) { index ->
            assertTrue(rateLimitService.tryConsumeForAuth(clientIp)) {
                "Request ${index + 1} should be allowed"
            }
        }
    }

    @Test
    fun `should block requests exceeding limit`() {
        val clientIp = "192.168.1.2"

        // 5回のリクエストは許可される
        repeat(5) {
            assertTrue(rateLimitService.tryConsumeForAuth(clientIp))
        }

        // 6回目以降は拒否される
        assertFalse(rateLimitService.tryConsumeForAuth(clientIp)) {
            "Request exceeding limit should be blocked"
        }
    }

    @Test
    fun `should track different IPs independently`() {
        val clientIp1 = "192.168.1.3"
        val clientIp2 = "192.168.1.4"

        // IP1 の上限を消費
        repeat(5) {
            assertTrue(rateLimitService.tryConsumeForAuth(clientIp1))
        }

        // IP1 は拒否される
        assertFalse(rateLimitService.tryConsumeForAuth(clientIp1))

        // IP2 はまだ許可される
        assertTrue(rateLimitService.tryConsumeForAuth(clientIp2)) {
            "Different IP should have its own limit"
        }
    }

    @Test
    fun `should return available tokens correctly`() {
        val clientIp = "192.168.1.5"

        // 初期状態は5トークン
        assertEquals(5, rateLimitService.getAvailableTokens(clientIp))

        // 1回消費後は4トークン
        rateLimitService.tryConsumeForAuth(clientIp)
        assertEquals(4, rateLimitService.getAvailableTokens(clientIp))

        // 4回消費後は0トークン
        repeat(4) {
            rateLimitService.tryConsumeForAuth(clientIp)
        }
        assertEquals(0, rateLimitService.getAvailableTokens(clientIp))
    }

    @Test
    fun `should allow all requests when disabled`() {
        val disabledProperties = createProperties(enabled = false, capacity = 1)
        val disabledService = RateLimitService(disabledProperties)
        val clientIp = "192.168.1.6"

        // 無効の場合、何回でも許可される
        repeat(100) {
            assertTrue(disabledService.tryConsumeForAuth(clientIp)) {
                "All requests should be allowed when rate limiting is disabled"
            }
        }
    }

    private fun createProperties(enabled: Boolean, capacity: Long): RateLimitProperties {
        return RateLimitProperties().apply {
            this.enabled = enabled
            this.auth = RateLimitProperties.EndpointRateLimitConfig().apply {
                this.capacity = capacity
                this.refillTokens = capacity
                this.refillDurationSeconds = 60
            }
        }
    }
}
