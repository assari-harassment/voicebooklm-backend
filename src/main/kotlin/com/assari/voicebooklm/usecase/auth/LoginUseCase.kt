package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.gateway.OAuthClient
import com.assari.voicebooklm.domain.gateway.TokenProvider
import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * ID トークン検証失敗例外
 */
open class InvalidIdTokenException(message: String) : RuntimeException(message)

/**
 * Google ID トークン検証失敗例外（後方互換性のため維持）
 */
class InvalidGoogleTokenException(message: String) : InvalidIdTokenException(message)

/**
 * ログインユースケース
 *
 * OAuth 認証フローを処理し、JWT トークンペアを発行する。
 * 新規ユーザーの場合はユーザーを作成する。
 *
 * OAuthClient インターフェースを使用し、プロバイダーに依存しない設計。
 */
@Service
open class LoginUseCase(
    private val oAuthClient: OAuthClient,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenProvider: TokenProvider,
) {
    /**
     * ID トークンで認証し、JWT トークンペアを発行する
     *
     * @param input ログインInput（ID トークン）
     * @return ログインOutput（アクセストークン、リフレッシュトークン、ユーザー ID）
     * @throws InvalidIdTokenException ID トークンの検証に失敗した場合
     */
    @Transactional
    open suspend fun execute(input: LoginInput): LoginOutput {
        // ID トークンを検証してユーザー情報を取得
        val oAuthUserInfo = oAuthClient.verifyIdTokenAndGetUserInfo(input.idToken)
            ?: throw InvalidIdTokenException("ID トークンの検証に失敗しました")

        // ユーザーを取得または作成
        val user = userRepository.findByGoogleSub(oAuthUserInfo.providerId)
            ?: createUser(oAuthUserInfo.providerId, oAuthUserInfo.email, oAuthUserInfo.name)

        // JWT トークンペアを生成
        val accessToken = tokenProvider.generateAccessToken(user.id, user.email)
        val refreshTokenValue = tokenProvider.generateRefreshToken(user.id)

        // リフレッシュトークンを保存
        val refreshToken = RefreshToken(
            id = UuidCreator.getTimeOrderedEpoch(),
            token = refreshTokenValue,
            userId = user.id,
            expiresAt = Instant.now().plusMillis(tokenProvider.refreshTokenExpiration),
            createdAt = Instant.now(),
            revoked = false
        )
        refreshTokenRepository.save(refreshToken)

        return LoginOutput(
            accessToken = accessToken,
            refreshToken = refreshTokenValue,
            userId = user.id
        )
    }

    private fun createUser(providerId: String, email: String, name: String): User {
        val user = User(
            id = UuidCreator.getTimeOrderedEpoch(),
            googleSub = providerId,
            email = email,
            name = name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return userRepository.save(user)
    }
}

/**
 * ログインInput
 */
data class LoginInput(
    val idToken: String
)

/**
 * ログインOutput
 */
data class LoginOutput(
    val accessToken: String,
    val refreshToken: String,
    val userId: UUID
)
