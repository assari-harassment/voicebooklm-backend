package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.repository.MemoRepository
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * アカウント削除コマンド
 */
data class DeleteAccountCommand(
    val userId: UUID
)

/**
 * ユーザーが見つからない例外
 */
class UserNotFoundException(message: String) : RuntimeException(message)

/**
 * アカウント削除ユースケース
 *
 * ユーザーのすべてのデータを物理削除する。
 * 削除順序: メモ → リフレッシュトークン → ユーザー（参照整合性を維持）
 */
@Service
class DeleteAccountUseCase(
    private val userRepository: UserRepository,
    private val memoRepository: MemoRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    /**
     * アカウントを削除する
     *
     * @param command アカウント削除コマンド（ユーザー ID）
     * @throws UserNotFoundException ユーザーが見つからない場合
     */
    @Transactional
    fun execute(command: DeleteAccountCommand) {
        // ユーザーが存在するか確認
        userRepository.findById(command.userId)
            ?: throw UserNotFoundException("ユーザーが見つかりません")

        // 参照整合性を維持するため、順番に削除
        // 1. メモを削除
        memoRepository.deleteByUserId(command.userId)

        // 2. リフレッシュトークンを削除
        refreshTokenRepository.deleteByUserId(command.userId)

        // 3. ユーザーを削除
        userRepository.deleteById(command.userId)
    }
}
