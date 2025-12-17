package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.gateway.OAuthClient
import com.assari.voicebooklm.domain.gateway.TokenProvider
import com.assari.voicebooklm.domain.model.OAuthUserInfo
import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class LoginUseCaseTest {

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var oAuthClient: OAuthClient
    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var tokenProvider: TokenProvider

    @BeforeEach
    fun setUp() {
        oAuthClient = mockk()
        userRepository = mockk()
        refreshTokenRepository = mockk()
        tokenProvider = mockk()

        every { tokenProvider.refreshTokenExpiration } returns 15552000000L

        loginUseCase = LoginInteractor(
            oAuthClient = oAuthClient,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            tokenProvider = tokenProvider,
        )
    }

    @Test
    fun `should login existing user successfully`() = runTest {
        // Given
        val idToken = "valid-id-token"
        val oAuthUserInfo = OAuthUserInfo(
            providerId = "provider-id-123",
            email = "test@example.com",
            name = "Test User",
            picture = null
        )
        val existingUser = User(
            id = UUID.randomUUID(),
            googleSub = oAuthUserInfo.providerId,
            email = oAuthUserInfo.email,
            name = oAuthUserInfo.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { oAuthClient.verifyIdTokenAndGetUserInfo(idToken) } returns oAuthUserInfo
        every { userRepository.findByGoogleSub(oAuthUserInfo.providerId) } returns existingUser
        every { tokenProvider.generateAccessToken(existingUser.id, existingUser.email) } returns "access-token"
        every { tokenProvider.generateRefreshToken(existingUser.id) } returns "refresh-token"
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        // When
        val result = loginUseCase.execute(LoginCommand(idToken))

        // Then
        assertNotNull(result)
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals(existingUser.id, result.userId)

        verify { userRepository.findByGoogleSub(oAuthUserInfo.providerId) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should create new user on first login`() = runTest {
        // Given
        val idToken = "valid-id-token"
        val oAuthUserInfo = OAuthUserInfo(
            providerId = "provider-id-new",
            email = "newuser@example.com",
            name = "New User",
            picture = null
        )

        coEvery { oAuthClient.verifyIdTokenAndGetUserInfo(idToken) } returns oAuthUserInfo
        every { userRepository.findByGoogleSub(oAuthUserInfo.providerId) } returns null
        every { userRepository.save(any()) } answers { firstArg() }
        every { tokenProvider.generateAccessToken(any(), any()) } returns "access-token"
        every { tokenProvider.generateRefreshToken(any()) } returns "refresh-token"
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        // When
        val result = loginUseCase.execute(LoginCommand(idToken))

        // Then
        assertNotNull(result)
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)

        verify { userRepository.save(any()) }
    }

    @Test
    fun `should throw exception when ID token is invalid`() = runTest {
        // Given
        val invalidIdToken = "invalid-token"

        coEvery { oAuthClient.verifyIdTokenAndGetUserInfo(invalidIdToken) } returns null

        // When & Then
        val exception = assertThrows(InvalidIdTokenException::class.java) {
            kotlinx.coroutines.runBlocking {
                loginUseCase.execute(LoginCommand(invalidIdToken))
            }
        }

        assertEquals("ID トークンの検証に失敗しました", exception.message)
    }

    @Test
    fun `should save refresh token to repository`() = runTest {
        // Given
        val idToken = "valid-id-token"
        val oAuthUserInfo = OAuthUserInfo(
            providerId = "provider-id-123",
            email = "test@example.com",
            name = "Test User",
            picture = null
        )
        val existingUser = User(
            id = UUID.randomUUID(),
            googleSub = oAuthUserInfo.providerId,
            email = oAuthUserInfo.email,
            name = oAuthUserInfo.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { oAuthClient.verifyIdTokenAndGetUserInfo(idToken) } returns oAuthUserInfo
        every { userRepository.findByGoogleSub(oAuthUserInfo.providerId) } returns existingUser
        every { tokenProvider.generateAccessToken(existingUser.id, existingUser.email) } returns "access-token"
        every { tokenProvider.generateRefreshToken(existingUser.id) } returns "refresh-token"

        val savedTokenSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(savedTokenSlot)) } answers { firstArg() }

        // When
        loginUseCase.execute(LoginCommand(idToken))

        // Then
        val savedToken = savedTokenSlot.captured
        assertEquals("refresh-token", savedToken.token)
        assertEquals(existingUser.id, savedToken.userId)
        assertFalse(savedToken.revoked)
    }

    @Test
    fun `InvalidGoogleTokenException should be subtype of InvalidIdTokenException for backward compatibility`() {
        // Given
        val exception = InvalidGoogleTokenException("test message")

        // Then
        assertTrue(exception is InvalidIdTokenException)
        assertEquals("test message", exception.message)
    }
}
