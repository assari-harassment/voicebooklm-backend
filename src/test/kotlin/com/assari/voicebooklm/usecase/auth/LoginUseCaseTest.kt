package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.infrastructure.api.GoogleOAuthClient
import com.assari.voicebooklm.infrastructure.api.GoogleUserInfo
import com.assari.voicebooklm.infrastructure.security.JwtTokenProvider
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class LoginUseCaseTest {

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var googleOAuthClient: GoogleOAuthClient
    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        googleOAuthClient = mockk()
        userRepository = mockk()
        refreshTokenRepository = mockk()
        jwtTokenProvider = mockk()

        every { jwtTokenProvider.refreshTokenExpiration } returns 15552000000L

        loginUseCase = LoginUseCase(
            googleOAuthClient = googleOAuthClient,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            jwtTokenProvider = jwtTokenProvider
        )
    }

    @Test
    fun `should login existing user successfully`() {
        // Given
        val idToken = "valid-google-id-token"
        val googleUserInfo = GoogleUserInfo(
            googleSub = "google-sub-123",
            email = "test@example.com",
            name = "Test User",
            picture = null
        )
        val existingUser = User(
            id = UUID.randomUUID(),
            googleSub = googleUserInfo.googleSub,
            email = googleUserInfo.email,
            name = googleUserInfo.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { googleOAuthClient.verifyIdTokenAndGetUserInfo(idToken) } returns googleUserInfo
        every { userRepository.findByGoogleSub(googleUserInfo.googleSub) } returns existingUser
        every { jwtTokenProvider.generateAccessToken(existingUser.id, existingUser.email) } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(existingUser.id) } returns "refresh-token"
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        // When
        val result = loginUseCase.execute(LoginCommand(idToken))

        // Then
        assertNotNull(result)
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals(existingUser.id, result.userId)

        verify { userRepository.findByGoogleSub(googleUserInfo.googleSub) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should create new user on first login`() {
        // Given
        val idToken = "valid-google-id-token"
        val googleUserInfo = GoogleUserInfo(
            googleSub = "google-sub-new",
            email = "newuser@example.com",
            name = "New User",
            picture = null
        )

        every { googleOAuthClient.verifyIdTokenAndGetUserInfo(idToken) } returns googleUserInfo
        every { userRepository.findByGoogleSub(googleUserInfo.googleSub) } returns null
        every { userRepository.save(any()) } answers { firstArg() }
        every { jwtTokenProvider.generateAccessToken(any(), any()) } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(any()) } returns "refresh-token"
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
    fun `should throw exception when Google ID token is invalid`() {
        // Given
        val invalidIdToken = "invalid-token"

        every { googleOAuthClient.verifyIdTokenAndGetUserInfo(invalidIdToken) } returns null

        // When & Then
        val exception = assertThrows(InvalidGoogleTokenException::class.java) {
            loginUseCase.execute(LoginCommand(invalidIdToken))
        }

        assertEquals("Google ID トークンの検証に失敗しました", exception.message)
    }

    @Test
    fun `should save refresh token to repository`() {
        // Given
        val idToken = "valid-google-id-token"
        val googleUserInfo = GoogleUserInfo(
            googleSub = "google-sub-123",
            email = "test@example.com",
            name = "Test User",
            picture = null
        )
        val existingUser = User(
            id = UUID.randomUUID(),
            googleSub = googleUserInfo.googleSub,
            email = googleUserInfo.email,
            name = googleUserInfo.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { googleOAuthClient.verifyIdTokenAndGetUserInfo(idToken) } returns googleUserInfo
        every { userRepository.findByGoogleSub(googleUserInfo.googleSub) } returns existingUser
        every { jwtTokenProvider.generateAccessToken(existingUser.id, existingUser.email) } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(existingUser.id) } returns "refresh-token"

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
}
