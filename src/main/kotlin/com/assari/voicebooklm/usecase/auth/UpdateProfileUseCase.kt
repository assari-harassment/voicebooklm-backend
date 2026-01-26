package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * プロフィール更新ユースケース
 *
 * ユーザーのプロフィール情報（名前）を更新する。
 */
@Service
open class UpdateProfileUseCase(
    private val userRepository: UserRepository,
) {
    /**
     * プロフィールを更新する
     *
     * @param input プロフィール更新Input
     * @return 更新後のプロフィール情報
     * @throws DomainException ユーザーが見つからない場合
     */
    @Transactional
    open fun execute(input: UpdateProfileInput): UpdateProfileOutput {
        // 1. ユーザーを取得
        val user = userRepository.findById(input.userId)
            ?: throw DomainException(ErrorCode.USER_NOT_FOUND)

        // 2. ドメインメソッドで名前を更新
        val updatedUser = user.updateName(input.name)

        // 3. 永続化
        val savedUser = userRepository.save(updatedUser)

        // 4. 結果を返却
        return UpdateProfileOutput(
            name = savedUser.name,
            email = savedUser.email
        )
    }
}

/**
 * プロフィール更新Input
 */
data class UpdateProfileInput(
    val userId: UUID,
    val name: String
)

/**
 * プロフィール更新Output
 */
data class UpdateProfileOutput(
    val name: String,
    val email: String
)
