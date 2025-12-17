package com.assari.voicebooklm.usecase.auth

import java.util.UUID
import com.assari.voicebooklm.domain.repository.UserRepository

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
interface GetCurrentUserUseCase {
    fun execute(command: GetCurrentUserCommand): UserInfo
}

open class GetCurrentUserInteractor(
    private val userRepository: UserRepository,
) : GetCurrentUserUseCase {
    /**
     * 現在のユーザー情報を取得する
     *
     * @param command 現在のユーザー取得コマンド（ユーザー ID）
     * @return ユーザー情報
     * @throws UserNotFoundException ユーザーが見つからない場合
     */
    override fun execute(command: GetCurrentUserCommand): UserInfo {
        val user = userRepository.findById(command.userId)
            ?: throw UserNotFoundException("ユーザーが見つかりません")

        return UserInfo(
            id = user.id,
            email = user.email,
            name = user.name
        )
    }
}
