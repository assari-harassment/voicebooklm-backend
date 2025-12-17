package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.gateway.TokenProvider
import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RefreshTokenUseCaseTest {

    private lateinit var refreshTokenUseCase: RefreshTokenUseCase
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var userRepository: UserRepository
    private lateinit var tokenProvider: TokenProvider

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mockk()
        userRepository = mockk()
        tokenProvider = mockk()

        every { tokenProvider.refreshTokenExpiration } returns 15552000000L

        refreshTokenUseCase = RefreshTokenInteractor(
            refreshTokenRepository = refreshTokenRepository,
            userRepository = userRepository,
            tokenProvider = tokenProvider,
        )
    }

    @Test
    fun `should refresh token successfully`() {
        // Given
        val oldRefreshToken = "old-refresh-token"
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            googleSub = "google-sub",
            email = "test@example.com",
            name = "Test User",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val storedToken = RefreshToken(
            id = UUID.randomUUID(),
            token = oldRefreshToken,
            userId = userId,
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
            revoked = false
        )

        every { refreshTokenRepository.findByTokenAndValid(oldRefreshToken, any()) } returns storedToken
        every { userRepository.findById(userId) } returns user
        every { refreshTokenRepository.revokeByToken(oldRefreshToken) } just Runs
        every { tokenProvider.generateAccessToken(userId, user.email) } returns "new-access-token"
        every { tokenProvider.generateRefreshToken(userId) } returns "new-refresh-token"
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        // When
        val result = refreshTokenUseCase.execute(RefreshTokenCommand(oldRefreshToken))

        // Then
        assertNotNull(result)
        assertEquals("new-access-token", result.accessToken)
        assertEquals("new-refresh-token", result.refreshToken)

        verify { refreshTokenRepository.revokeByToken(oldRefreshToken) }
        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `should throw exception when refresh token is invalid`() {
        // Given
        val invalidToken = "invalid-refresh-token"

        every { refreshTokenRepository.findByTokenAndValid(invalidToken, any()) } returns null

        // When & Then
        val exception = assertThrows(InvalidRefreshTokenException::class.java) {
            refreshTokenUseCase.execute(RefreshTokenCommand(invalidToken))
        }

        assertEquals("リフレッシュトークンが無効または期限切れです", exception.message)
    }

    @Test
    fun `should throw exception when user not found`() {
        // Given
        val refreshToken = "valid-refresh-token"
        val userId = UUID.randomUUID()
        val storedToken = RefreshToken(
            id = UUID.randomUUID(),
            token = refreshToken,
            userId = userId,
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
            revoked = false
        )

        every { refreshTokenRepository.findByTokenAndValid(refreshToken, any()) } returns storedToken
        every { userRepository.findById(userId) } returns null

        // When & Then
        val exception = assertThrows(InvalidRefreshTokenException::class.java) {
            refreshTokenUseCase.execute(RefreshTokenCommand(refreshToken))
        }

        assertEquals("ユーザーが見つかりません", exception.message)
    }

    @Test
    fun `should revoke old token and save new token`() {
        // Given
        val oldRefreshToken = "old-refresh-token"
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            googleSub = "google-sub",
            email = "test@example.com",
            name = "Test User",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val storedToken = RefreshToken(
            id = UUID.randomUUID(),
            token = oldRefreshToken,
            userId = userId,
            expiresAt = Instant.now().plusSeconds(3600),
            createdAt = Instant.now(),
            revoked = false
        )

        every { refreshTokenRepository.findByTokenAndValid(oldRefreshToken, any()) } returns storedToken
        every { userRepository.findById(userId) } returns user
        every { refreshTokenRepository.revokeByToken(oldRefreshToken) } just Runs
        every { tokenProvider.generateAccessToken(userId, user.email) } returns "new-access-token"
        every { tokenProvider.generateRefreshToken(userId) } returns "new-refresh-token"

        val savedTokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(savedTokenSlot)) } answers { firstArg() }

        // When
        refreshTokenUseCase.execute(RefreshTokenCommand(oldRefreshToken))

        // Then
        verify(exactly = 1) { refreshTokenRepository.revokeByToken(oldRefreshToken) }

        val savedToken = savedTokenSlot.captured
        assertEquals("new-refresh-token", savedToken.token)
        assertEquals(userId, savedToken.userId)
        assertFalse(savedToken.revoked)
    }
}
