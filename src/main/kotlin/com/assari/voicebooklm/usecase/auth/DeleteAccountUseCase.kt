package com.assari.voicebooklm.usecase.auth

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
 * 削除順序: VoiceMemo → ユーザー（参照整合性を維持）
 *
 * 注: Firebase 側のユーザー削除は呼び出し元（AuthController）で実行する。
 */
open class DeleteAccountUseCase(
    private val userRepository: UserRepository,
    private val voiceMemoRepository: VoiceMemoRepository,
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

        // 2. ユーザーを削除
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
