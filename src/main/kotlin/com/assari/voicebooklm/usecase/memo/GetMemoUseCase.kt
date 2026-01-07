package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * メモ詳細取得ユースケース
 *
 * 指定されたIDのメモを取得する。
 * 取得前に所有者確認を行い、他ユーザーのメモは取得できない。
 * 削除済みのメモも取得できない。
 */
@Service
open class GetMemoUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    /**
     * メモを取得する
     *
     * @param input メモ詳細取得Input（メモIDとユーザーID）
     * @return メモ詳細
     * @throws DomainException メモが見つからない、削除済み、または権限がない場合
     */
    @Transactional(readOnly = true)
    open suspend fun execute(input: GetMemoInput): GetMemoOutput {
        // 1. メモを取得（リポジトリで削除済みメモは除外される）
        val memo = voiceMemoRepository.findById(input.memoId)
            ?: throw DomainException(ErrorCode.MEMO_NOT_FOUND)

        // 2. 権限チェック（自分のメモかどうか）
        // メモの存在を推測されないよう、403ではなく404を返す
        if (memo.userId != input.userId) {
            throw DomainException(ErrorCode.MEMO_NOT_FOUND)
        }

        // 3. 結果を返却
        return GetMemoOutput(memo)
    }
}

/**
 * メモ詳細取得Input
 */
data class GetMemoInput(
    val memoId: UUID,
    val userId: UUID,
)

/**
 * メモ詳細取得Output
 */
data class GetMemoOutput(
    val memo: VoiceMemo,
)
