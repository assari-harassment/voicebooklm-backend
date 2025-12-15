package com.assari.voicebooklm.infrastructure.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider

    // 256bit 以上のシークレットキー（テスト用）
    private val testSecret = "test-secret-key-for-jwt-token-generation-must-be-at-least-256-bits-long"
    private val accessTokenExpiration = 900000L  // 15分テスト用
    private val refreshTokenExpiration = 604800000L  // 7日テスト用

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = JwtTokenProvider(
            secret = testSecret,
            accessTokenExpiration = accessTokenExpiration,
            refreshTokenExpiration = refreshTokenExpiration
        )
    }

    @Test
    fun `should generate access token with user claims`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"

        val token = jwtTokenProvider.generateAccessToken(userId, email)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `should validate valid access token`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"

        val token = jwtTokenProvider.generateAccessToken(userId, email)

        assertTrue(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun `should return false for invalid token`() {
        val invalidToken = "invalid.token.value"

        assertFalse(jwtTokenProvider.validateToken(invalidToken))
    }

    @Test
    fun `should extract userId from valid token`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"

        val token = jwtTokenProvider.generateAccessToken(userId, email)
        val extractedUserId = jwtTokenProvider.getUserIdFromToken(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `should extract email from valid token`() {
        val userId = UUID.randomUUID()
        val email = "test@example.com"

        val token = jwtTokenProvider.generateAccessToken(userId, email)
        val extractedEmail = jwtTokenProvider.getEmailFromToken(token)

        assertEquals(email, extractedEmail)
    }

    @Test
    fun `should generate refresh token`() {
        val userId = UUID.randomUUID()

        val token = jwtTokenProvider.generateRefreshToken(userId)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `should validate valid refresh token`() {
        val userId = UUID.randomUUID()

        val token = jwtTokenProvider.generateRefreshToken(userId)

        assertTrue(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun `should extract userId from refresh token`() {
        val userId = UUID.randomUUID()

        val token = jwtTokenProvider.generateRefreshToken(userId)
        val extractedUserId = jwtTokenProvider.getUserIdFromToken(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `should return null for malformed token when extracting userId`() {
        val malformedToken = "malformed.token"

        val result = jwtTokenProvider.getUserIdFromToken(malformedToken)

        assertNull(result)
    }

    @Test
    fun `should return null for malformed token when extracting email`() {
        val malformedToken = "malformed.token"

        val result = jwtTokenProvider.getEmailFromToken(malformedToken)

        assertNull(result)
    }
}
