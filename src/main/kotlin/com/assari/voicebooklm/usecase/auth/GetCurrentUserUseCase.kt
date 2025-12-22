package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 現在のユーザー取得ユースケース
 *
 * JWT トークンから取得したユーザー ID でユーザー情報を取得する。
 */
@Service
open class GetCurrentUserUseCase(
    private val userRepository: UserRepository,
) {
    /**
     * 現在のユーザー情報を取得する
     *
     * @param input 現在のユーザー取得Input（ユーザー ID）
     * @return ユーザー情報
     * @throws UserNotFoundException ユーザーが見つからない場合
     */
    open fun execute(input: GetCurrentUserInput): GetCurrentUserOutput {
        val user = userRepository.findById(input.userId)
            ?: throw UserNotFoundException("ユーザーが見つかりません")

        return GetCurrentUserOutput(
            id = user.id,
            email = user.email,
            name = user.name
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Input / Output
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * 現在のユーザー取得Input
 */
data class GetCurrentUserInput(
    val userId: UUID
)

/**
 * 現在のユーザー取得Output
 */
data class GetCurrentUserOutput(
    val id: UUID,
    val email: String,
    val name: String
)
