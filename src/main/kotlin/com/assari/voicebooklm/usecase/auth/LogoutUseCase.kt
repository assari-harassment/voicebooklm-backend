package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
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
interface LogoutUseCase {
    fun execute(command: LogoutCommand)
}

open class LogoutInteractor(
    private val refreshTokenRepository: RefreshTokenRepository,
) : LogoutUseCase {
    /**
     * ログアウトを実行する
     *
     * @param command ログアウトコマンド（リフレッシュトークン）
     */
    @Transactional
    override fun execute(command: LogoutCommand) {
        refreshTokenRepository.revokeByToken(command.refreshToken)
    }
}
