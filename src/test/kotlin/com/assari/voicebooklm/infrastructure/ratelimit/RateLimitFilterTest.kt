package com.assari.voicebooklm.infrastructure.ratelimit

import com.assari.voicebooklm.config.RateLimitProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RateLimitFilterTest {

    private lateinit var rateLimitFilter: RateLimitFilter
    private lateinit var rateLimitService: RateLimitService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        val properties = createProperties(capacity = 5)
        rateLimitService = RateLimitService(properties)
        rateLimitService.clearCache()
        objectMapper = ObjectMapper()
        rateLimitFilter = RateLimitFilter(rateLimitService, objectMapper)
        filterChain = mockk(relaxed = true)
    }

    @Test
    fun `should skip non-auth endpoints`() {
        val request = MockHttpServletRequest().apply {
            requestURI = "/api/users/me"
            remoteAddr = "192.168.1.1"
        }
        val response = MockHttpServletResponse()

        rateLimitFilter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        assertEquals(HttpStatus.OK.value(), response.status)
    }

    @Test
    fun `should apply rate limit to auth endpoints`() {
        val clientIp = "192.168.1.2"

        // 5回のリクエストは成功
        repeat(5) {
            val request = MockHttpServletRequest().apply {
                requestURI = "/api/auth/google"
                remoteAddr = clientIp
            }
            val response = MockHttpServletResponse()

            rateLimitFilter.doFilter(request, response, filterChain)
            assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.status)
        }

        // 6回目は 429 エラー
        val request = MockHttpServletRequest().apply {
            requestURI = "/api/auth/google"
            remoteAddr = clientIp
        }
        val response = MockHttpServletResponse()

        rateLimitFilter.doFilter(request, response, filterChain)

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.status)
        assertTrue(response.contentType?.contains("application/json") == true)
        assertTrue(response.contentAsString.contains("RATE_LIMIT_EXCEEDED"))
    }

    @Test
    fun `should use X-Forwarded-For header when present`() {
        val realClientIp = "10.0.0.1"
        val proxyIp = "192.168.1.100"

        // X-Forwarded-For ヘッダーを設定
        val request = MockHttpServletRequest().apply {
            requestURI = "/api/auth/google"
            remoteAddr = proxyIp
            addHeader("X-Forwarded-For", realClientIp)
        }
        val response = MockHttpServletResponse()

        rateLimitFilter.doFilter(request, response, filterChain)

        // 残りトークン数が正しく設定されていることを確認
        assertEquals("4", response.getHeader("X-RateLimit-Remaining"))
    }

    @Test
    fun `should handle multiple IPs in X-Forwarded-For`() {
        val realClientIp = "10.0.0.2"
        val proxyChain = "$realClientIp, 172.16.0.1, 192.168.1.1"

        val request = MockHttpServletRequest().apply {
            requestURI = "/api/auth/google"
            remoteAddr = "192.168.1.100"
            addHeader("X-Forwarded-For", proxyChain)
        }
        val response = MockHttpServletResponse()

        rateLimitFilter.doFilter(request, response, filterChain)

        // 最初の IP（realClientIp）が使用される
        assertEquals("4", response.getHeader("X-RateLimit-Remaining"))
    }

    @Test
    fun `should add rate limit remaining header`() {
        val request = MockHttpServletRequest().apply {
            requestURI = "/api/auth/refresh"
            remoteAddr = "192.168.1.3"
        }
        val response = MockHttpServletResponse()

        rateLimitFilter.doFilter(request, response, filterChain)

        assertNotNull(response.getHeader("X-RateLimit-Remaining"))
        assertEquals("4", response.getHeader("X-RateLimit-Remaining"))
    }

    @Test
    fun `should return Japanese error message when rate limited`() {
        val clientIp = "192.168.1.4"

        // 上限まで消費
        repeat(5) {
            val request = MockHttpServletRequest().apply {
                requestURI = "/api/auth/google"
                remoteAddr = clientIp
            }
            rateLimitFilter.doFilter(request, MockHttpServletResponse(), filterChain)
        }

        // 6回目のリクエスト
        val request = MockHttpServletRequest().apply {
            requestURI = "/api/auth/google"
            remoteAddr = clientIp
        }
        val response = MockHttpServletResponse()

        rateLimitFilter.doFilter(request, response, filterChain)

        assertTrue(response.contentAsString.contains("リクエスト数の上限に達しました"))
    }

    private fun createProperties(capacity: Long): RateLimitProperties {
        return RateLimitProperties().apply {
            this.enabled = true
            this.auth = RateLimitProperties.EndpointRateLimitConfig().apply {
                this.capacity = capacity
                this.refillTokens = capacity
                this.refillDurationSeconds = 60
            }
        }
    }
}
