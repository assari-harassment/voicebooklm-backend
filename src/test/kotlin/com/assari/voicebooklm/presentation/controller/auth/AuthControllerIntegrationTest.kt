package com.assari.voicebooklm.presentation.controller.auth

import com.assari.voicebooklm.AbstractIntegrationTest
import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.infrastructure.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

/**
 * AuthController 統合テスト
 *
 * Testcontainers を使用して、実際の PostgreSQL データベースに対して
 * 認証 API のエンドツーエンドテストを実行する。
 *
 * Requirements: 9.4, 10.5, 12.3, 12.6
 */
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var testAccessToken: String
    private lateinit var testRefreshToken: RefreshToken

    @BeforeEach
    fun setUp() {
        // テストユーザーを作成
        testUser = User(
            id = UUID.randomUUID(),
            googleSub = "google-sub-${UUID.randomUUID()}",
            email = "test-${UUID.randomUUID()}@example.com",
            name = "Test User",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        userRepository.save(testUser)

        // アクセストークンを生成
        testAccessToken = jwtTokenProvider.generateAccessToken(testUser.id, testUser.email)

        // リフレッシュトークンを生成・保存
        val refreshTokenString = jwtTokenProvider.generateRefreshToken(testUser.id)
        testRefreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = refreshTokenString,
            userId = testUser.id,
            expiresAt = Instant.now().plusSeconds(604800), // 7日後テスト用
            createdAt = Instant.now(),
            revoked = false
        )
        refreshTokenRepository.save(testRefreshToken)
    }

    // =====================================================================
    // POST /api/auth/refresh - トークンリフレッシュ
    // =====================================================================

    @Test
    fun `POST refresh - should return new tokens when refresh token is valid`() {
        // Given
        val request = RefreshTokenRequest(refreshToken = testRefreshToken.token)

        // When & Then
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
    }

    @Test
    fun `POST refresh - should return 401 when refresh token is invalid`() {
        // Given
        val request = RefreshTokenRequest(refreshToken = "invalid-refresh-token")

        // When & Then
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST refresh - should issue new valid tokens`() {
        // Given
        val request = RefreshTokenRequest(refreshToken = testRefreshToken.token)

        // When - refresh token
        val response = mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andReturn()

        // Then - verify new tokens are valid JWTs
        val responseContent = response.response.contentAsString
        val tokenResponse = objectMapper.readValue(responseContent, TokenResponse::class.java)

        // New access token should be valid
        assert(jwtTokenProvider.validateToken(tokenResponse.accessToken)) {
            "New access token should be valid"
        }

        // New access token should contain correct user ID
        assert(jwtTokenProvider.getUserIdFromToken(tokenResponse.accessToken) == testUser.id) {
            "New access token should contain correct user ID"
        }
    }

    // =====================================================================
    // POST /api/auth/logout - ログアウト
    // =====================================================================

    @Test
    fun `POST logout - should return 204 and revoke refresh token`() {
        // Given
        val request = LogoutRequest(refreshToken = testRefreshToken.token)

        // When & Then
        mockMvc.perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNoContent)

        // Verify token is revoked (refresh should fail)
        val refreshRequest = RefreshTokenRequest(refreshToken = testRefreshToken.token)
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    // =====================================================================
    // GET /api/auth/me - 現在のユーザー情報取得
    // =====================================================================

    @Test
    fun `GET me - should return user info when authenticated`() {
        // When & Then
        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $testAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testUser.id.toString()))
            .andExpect(jsonPath("$.email").value(testUser.email))
            .andExpect(jsonPath("$.name").value(testUser.name))
    }

    @Test
    fun `GET me - should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(
            get("/api/auth/me")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET me - should return 401 when token is invalid`() {
        // When & Then
        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer invalid-token")
        )
            .andExpect(status().isUnauthorized)
    }

    // =====================================================================
    // DELETE /api/auth/account - アカウント削除
    // =====================================================================

    @Test
    fun `DELETE account - should delete user and all related data`() {
        // When & Then
        mockMvc.perform(
            delete("/api/auth/account")
                .header("Authorization", "Bearer $testAccessToken")
        )
            .andExpect(status().isNoContent)

        // Verify user is deleted (get me should fail)
        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $testAccessToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE account - should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(
            delete("/api/auth/account")
        )
            .andExpect(status().isUnauthorized)
    }
}
