package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * メモ削除ユースケース
 *
 * 指定されたメモを論理削除する。
 * 削除前に所有者確認を行い、他ユーザーのメモは削除できない。
 */
@Service
open class DeleteMemoUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    /**
     * メモを削除する
     *
     * @param input メモ削除Input（メモIDとユーザーID）
     * @throws DomainException メモが見つからない、または権限がない場合
     */
    @Transactional
    open suspend fun execute(input: DeleteMemoInput) {
        // 1. メモを取得（deleted=false のもののみ）
        val memo = voiceMemoRepository.findById(input.memoId)
            ?: throw DomainException(ErrorCode.MEMO_NOT_FOUND)

        // 2. 権限チェック（自分のメモかどうか）
        if (memo.userId != input.userId) {
            throw DomainException(ErrorCode.UNAUTHORIZED_ACCESS)
        }

        // 3. 論理削除
        val deletedMemo = memo.markAsDeleted()
        voiceMemoRepository.save(deletedMemo)
    }
}

/**
 * メモ削除Input
 */
data class DeleteMemoInput(
    val memoId: UUID,
    val userId: UUID,
)
