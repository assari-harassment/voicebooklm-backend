package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ログアウトコマンド
 */
data class LogoutCommand(
    val refreshToken: String
)

/**
 * ログアウトユースケース
 *
 * リフレッシュトークンを無効化してログアウトする。
 */
@Service
class LogoutUseCase(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    /**
     * ログアウトを実行する
     *
     * @param command ログアウトコマンド（リフレッシュトークン）
     */
    @Transactional
    fun execute(command: LogoutCommand) {
        refreshTokenRepository.revokeByToken(command.refreshToken)
    }
}
