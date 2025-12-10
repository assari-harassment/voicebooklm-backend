package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 現在のユーザー取得コマンド
 */
data class GetCurrentUserCommand(
    val userId: UUID
)

/**
 * ユーザー情報結果
 */
data class UserInfo(
    val id: UUID,
    val email: String,
    val name: String
)

/**
 * 現在のユーザー取得ユースケース
 *
 * JWT トークンから取得したユーザー ID でユーザー情報を取得する。
 */
@Service
class GetCurrentUserUseCase(
    private val userRepository: UserRepository
) {
    /**
     * 現在のユーザー情報を取得する
     *
     * @param command 現在のユーザー取得コマンド（ユーザー ID）
     * @return ユーザー情報
     * @throws UserNotFoundException ユーザーが見つからない場合
     */
    fun execute(command: GetCurrentUserCommand): UserInfo {
        val user = userRepository.findById(command.userId)
            ?: throw UserNotFoundException("ユーザーが見つかりません")

        return UserInfo(
            id = user.id,
            email = user.email,
            name = user.name
        )
    }
}
