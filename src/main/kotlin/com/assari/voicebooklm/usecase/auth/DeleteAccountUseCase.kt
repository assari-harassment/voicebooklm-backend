package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * ユーザーが見つからない例外
 */
class UserNotFoundException(message: String) : RuntimeException(message)

/**
 * アカウント削除ユースケース
 *
 * ユーザーのすべてのデータを物理削除する。
 * 削除順序: VoiceMemo → リフレッシュトークン → ユーザー（参照整合性を維持）
 */
open class DeleteAccountUseCase(
    private val userRepository: UserRepository,
    private val voiceMemoRepository: VoiceMemoRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    /**
     * アカウントを削除する
     *
     * @param input アカウント削除Input（ユーザー ID）
     * @throws UserNotFoundException ユーザーが見つからない場合
     */
    @Transactional
    open fun execute(input: DeleteAccountInput) {
        // ユーザーが存在するか確認
        userRepository.findById(input.userId)
            ?: throw UserNotFoundException("ユーザーが見つかりません")

        // 参照整合性を維持するため、順番に削除
        // 1. VoiceMemo を削除
        voiceMemoRepository.deleteByUserId(input.userId)

        // 2. リフレッシュトークンを削除
        refreshTokenRepository.deleteByUserId(input.userId)

        // 3. ユーザーを削除
        userRepository.deleteById(input.userId)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Input
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * アカウント削除Input
 */
data class DeleteAccountInput(
    val userId: UUID
)
