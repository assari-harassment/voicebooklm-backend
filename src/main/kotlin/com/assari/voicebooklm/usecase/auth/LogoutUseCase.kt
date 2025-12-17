package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import org.springframework.transaction.annotation.Transactional

/**
 * ログアウトユースケース
 *
 * リフレッシュトークンを無効化してログアウトする。
 */
open class LogoutUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    /**
     * ログアウトを実行する
     *
     * @param input ログアウトInput（リフレッシュトークン）
     */
    @Transactional
    open fun execute(input: LogoutInput) {
        refreshTokenRepository.revokeByToken(input.refreshToken)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Input
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ログアウトInput
 */
data class LogoutInput(
    val refreshToken: String
)
