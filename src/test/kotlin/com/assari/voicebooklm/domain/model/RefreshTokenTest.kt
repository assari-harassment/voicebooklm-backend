package com.assari.voicebooklm.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RefreshTokenTest {

    @Test
    fun `should create refresh token with valid data`() {
        val id = UUID.randomUUID()
        val token = "refresh-token-value"
        val userId = UUID.randomUUID()
        val expiresAt = Instant.now().plusSeconds(604800) // 7日後テスト用
        val createdAt = Instant.now()

        val refreshToken = RefreshToken(
            id = id,
            token = token,
            userId = userId,
            expiresAt = expiresAt,
            createdAt = createdAt,
            revoked = false
        )

        assertEquals(id, refreshToken.id)
        assertEquals(token, refreshToken.token)
        assertEquals(userId, refreshToken.userId)
        assertEquals(expiresAt, refreshToken.expiresAt)
        assertEquals(createdAt, refreshToken.createdAt)
        assertFalse(refreshToken.revoked)
    }

    @Test
    fun `should return false for isExpired when token is not expired`() {
        val refreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = "refresh-token",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(3600), // 1時間後
            createdAt = Instant.now(),
            revoked = false
        )

        assertFalse(refreshToken.isExpired())
    }

    @Test
    fun `should return true for isExpired when token is expired`() {
        val refreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = "refresh-token",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().minusSeconds(3600), // 1時間前
            createdAt = Instant.now().minusSeconds(7200),
            revoked = false
        )

        assertTrue(refreshToken.isExpired())
    }

    @Test
    fun `should return false for isValid when token is revoked`() {
        val refreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = "refresh-token",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
            revoked = true
        )

        assertFalse(refreshToken.isValid())
    }

    @Test
    fun `should return false for isValid when token is expired`() {
        val refreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = "refresh-token",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().minusSeconds(3600),
            createdAt = Instant.now().minusSeconds(7200),
            revoked = false
        )

        assertFalse(refreshToken.isValid())
    }

    @Test
    fun `should return true for isValid when token is not revoked and not expired`() {
        val refreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = "refresh-token",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
            revoked = false
        )

        assertTrue(refreshToken.isValid())
    }

    @Test
    fun `should revoke token`() {
        val refreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = "refresh-token",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
            revoked = false
        )

        refreshToken.revoke()

        assertTrue(refreshToken.revoked)
        assertFalse(refreshToken.isValid())
    }

    @Test
    fun `should have equals based on id`() {
        val id = UUID.randomUUID()

        val token1 = RefreshToken(
            id = id,
            token = "token-1",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
            revoked = false
        )

        val token2 = RefreshToken(
            id = id,
            token = "token-2",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(7200),
            createdAt = Instant.now(),
            revoked = true
        )

        assertEquals(token1, token2)
    }

    @Test
    fun `should have consistent hashCode based on id`() {
        val id = UUID.randomUUID()

        val token1 = RefreshToken(
            id = id,
            token = "token-1",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
            revoked = false
        )

        val token2 = RefreshToken(
            id = id,
            token = "token-2",
            userId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(7200),
            createdAt = Instant.now(),
            revoked = true
        )

        assertEquals(token1.hashCode(), token2.hashCode())
    }
}
