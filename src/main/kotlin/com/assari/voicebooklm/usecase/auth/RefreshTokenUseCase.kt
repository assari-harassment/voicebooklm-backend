package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.infrastructure.security.JwtTokenProvider
import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * リフレッシュトークンコマンド
 */
data class RefreshTokenCommand(
    val refreshToken: String
)

/**
 * リフレッシュトークン結果
 */
data class RefreshTokenResult(
    val accessToken: String,
    val refreshToken: String,
    val userId: UUID
)

/**
 * リフレッシュトークン無効例外
 */
class InvalidRefreshTokenException(message: String) : RuntimeException(message)

/**
 * リフレッシュトークンユースケース
 *
 * リフレッシュトークンローテーションを実装。
 * 旧トークンを無効化し、新しいトークンペアを発行する。
 */
@Service
class RefreshTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {
    /**
     * リフレッシュトークンを使用して新しいトークンペアを発行する
     *
     * @param command リフレッシュトークンコマンド
     * @return 新しいトークンペア
     * @throws InvalidRefreshTokenException リフレッシュトークンが無効または期限切れの場合
     */
    @Transactional
    fun execute(command: RefreshTokenCommand): RefreshTokenResult {
        // リフレッシュトークンを検証
        val storedToken = refreshTokenRepository.findByTokenAndValid(
            command.refreshToken,
            Instant.now()
        ) ?: throw InvalidRefreshTokenException("リフレッシュトークンが無効または期限切れです")

        // ユーザーを取得
        val user = userRepository.findById(storedToken.userId)
            ?: throw InvalidRefreshTokenException("ユーザーが見つかりません")

        // 旧トークンを無効化
        refreshTokenRepository.revokeByToken(command.refreshToken)

        // 新しいトークンペアを生成
        val newAccessToken = jwtTokenProvider.generateAccessToken(user.id, user.email)
        val newRefreshTokenValue = jwtTokenProvider.generateRefreshToken(user.id)

        // 新しいリフレッシュトークンを保存
        val newRefreshToken = RefreshToken(
            id = UuidCreator.getTimeOrderedEpoch(),
            token = newRefreshTokenValue,
            userId = user.id,
            expiresAt = Instant.now().plusMillis(jwtTokenProvider.refreshTokenExpiration),
            createdAt = Instant.now(),
            revoked = false
        )
        refreshTokenRepository.save(newRefreshToken)

        return RefreshTokenResult(
            accessToken = newAccessToken,
            refreshToken = newRefreshTokenValue,
            userId = user.id
        )
    }
}
