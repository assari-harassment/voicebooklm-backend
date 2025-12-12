package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.infrastructure.api.GoogleOAuthClient
import com.assari.voicebooklm.infrastructure.security.JwtTokenProvider
import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * ログインコマンド
 */
data class LoginCommand(
    val idToken: String
)

/**
 * ログイン結果
 */
data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val userId: UUID
)

/**
 * Google ID トークン検証失敗例外
 */
class InvalidGoogleTokenException(message: String) : RuntimeException(message)

/**
 * ログインユースケース
 *
 * Google OAuth 認証フローを処理し、JWT トークンペアを発行する。
 * 新規ユーザーの場合はユーザーを作成する。
 */
@Service
class LoginUseCase(
    private val googleOAuthClient: GoogleOAuthClient,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${jwt.refresh-token-expiration:15552000000}")
    private val refreshTokenExpiration: Long = 15552000000L // 180日間
) {
    /**
     * Google ID トークンで認証し、JWT トークンペアを発行する
     *
     * @param command ログインコマンド（Google ID トークン）
     * @return ログイン結果（アクセストークン、リフレッシュトークン、ユーザー ID）
     * @throws InvalidGoogleTokenException Google ID トークンの検証に失敗した場合
     */
    @Transactional
    fun execute(command: LoginCommand): LoginResult {
        // Google ID トークンを検証してユーザー情報を取得
        val googleUserInfo = googleOAuthClient.verifyIdTokenAndGetUserInfo(command.idToken)
            ?: throw InvalidGoogleTokenException("Google ID トークンの検証に失敗しました")

        // ユーザーを取得または作成
        val user = userRepository.findByGoogleSub(googleUserInfo.googleSub)
            ?: createUser(googleUserInfo.googleSub, googleUserInfo.email, googleUserInfo.name)

        // JWT トークンペアを生成
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email)
        val refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.id)

        // リフレッシュトークンを保存
        val refreshToken = RefreshToken(
            id = UuidCreator.getTimeOrderedEpoch(),
            token = refreshTokenValue,
            userId = user.id,
            expiresAt = Instant.now().plusMillis(refreshTokenExpiration),
            createdAt = Instant.now(),
            revoked = false
        )
        refreshTokenRepository.save(refreshToken)

        return LoginResult(
            accessToken = accessToken,
            refreshToken = refreshTokenValue,
            userId = user.id
        )
    }

    private fun createUser(googleSub: String, email: String, name: String): User {
        val user = User(
            id = UuidCreator.getTimeOrderedEpoch(),
            googleSub = googleSub,
            email = email,
            name = name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return userRepository.save(user)
    }
}
