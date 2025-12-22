package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.model.RefreshToken
import com.assari.voicebooklm.domain.gateway.TokenProvider
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

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
open class RefreshTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val tokenProvider: TokenProvider,
) {
    /**
     * リフレッシュトークンを使用して新しいトークンペアを発行する
     *
     * @param input リフレッシュトークンInput
     * @return 新しいトークンペア
     * @throws InvalidRefreshTokenException リフレッシュトークンが無効または期限切れの場合
     */
    @Transactional
    open fun execute(input: RefreshTokenInput): RefreshTokenOutput {
        // リフレッシュトークンを検証
        val storedToken = refreshTokenRepository.findByTokenAndValid(
            input.refreshToken,
            Instant.now()
        ) ?: throw InvalidRefreshTokenException("リフレッシュトークンが無効または期限切れです")

        // ユーザーを取得
        val user = userRepository.findById(storedToken.userId)
            ?: throw InvalidRefreshTokenException("ユーザーが見つかりません")

        // 旧トークンを無効化してローテーション（盗難対策）
        refreshTokenRepository.revokeByToken(input.refreshToken)

        // 新しいトークンペアを生成
        val newAccessToken = tokenProvider.generateAccessToken(user.id, user.email)
        val newRefreshTokenValue = tokenProvider.generateRefreshToken(user.id)

        // 新しいリフレッシュトークンを保存
        val newRefreshToken = RefreshToken(
            id = UuidCreator.getTimeOrderedEpoch(),
            token = newRefreshTokenValue,
            userId = user.id,
            expiresAt = Instant.now().plusMillis(tokenProvider.refreshTokenExpiration),
            createdAt = Instant.now(),
            revoked = false
        )
        refreshTokenRepository.save(newRefreshToken)

        return RefreshTokenOutput(
            accessToken = newAccessToken,
            refreshToken = newRefreshTokenValue,
            userId = user.id
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Input / Output
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * リフレッシュトークンInput
 */
data class RefreshTokenInput(
    val refreshToken: String
)

/**
 * リフレッシュトークンOutput
 */
data class RefreshTokenOutput(
    val accessToken: String,
    val refreshToken: String,
    val userId: UUID
)
