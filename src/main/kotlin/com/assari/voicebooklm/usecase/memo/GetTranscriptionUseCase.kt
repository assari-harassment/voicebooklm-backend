package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 文字起こしテキスト取得ユースケース
 *
 * 指定されたIDのメモから文字起こしテキストを取得する。
 * 取得前に所有者確認を行い、他ユーザーのメモは取得できない。
 * 文字起こしが完了していない場合はエラーを返す。
 */
@Service
open class GetTranscriptionUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    /**
     * 文字起こしテキストを取得する
     *
     * @param input 文字起こしテキスト取得Input（メモIDとユーザーID）
     * @return 文字起こしテキスト
     * @throws DomainException メモが見つからない、権限がない、または文字起こしが未完了の場合
     */
    @Transactional(readOnly = true)
    open suspend fun execute(input: GetTranscriptionInput): GetTranscriptionOutput {
        // 1. メモを取得（リポジトリで削除済みメモは除外される）
        val memo = voiceMemoRepository.findById(input.memoId)
            ?: throw DomainException(ErrorCode.MEMO_NOT_FOUND)

        // 2. 権限チェック（自分のメモかどうか）
        // メモの存在を推測されないよう、403ではなく404を返す
        if (memo.userId != input.userId) {
            throw DomainException(ErrorCode.MEMO_NOT_FOUND)
        }

        // 3. 文字起こし状態チェック
        if (memo.transcription.isFailed) {
            throw DomainException(ErrorCode.TRANSCRIPTION_FAILED)
        }
        if (!memo.transcription.isCompleted) {
            throw DomainException(ErrorCode.TRANSCRIPTION_NOT_COMPLETED)
        }

        // 4. 結果を返却
        return GetTranscriptionOutput(memo.transcription.text!!)
    }
}

/**
 * 文字起こしテキスト取得Input
 */
data class GetTranscriptionInput(
    /** メモID */
    val memoId: UUID,
    /** リクエストしたユーザーのID */
    val userId: UUID,
)

/**
 * 文字起こしテキスト取得Output
 */
data class GetTranscriptionOutput(
    /** 文字起こしテキスト */
    val transcription: String,
)
