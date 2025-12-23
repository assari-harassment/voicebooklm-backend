package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ユーザーのメモ一覧を取得するユースケース
 */
@Service
open class ListMemosUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    // 読み取り専用の一覧取得処理
    @Transactional(readOnly = true)
    open suspend fun execute(input: ListMemosInput): ListMemosOutput {
        val memos = voiceMemoRepository.findByUserId(input.userId)
        return ListMemosOutput(memos)
    }
}

/**
 * メモ一覧取得 Input
 */
data class ListMemosInput(
    val userId: UUID,
)

/**
 * メモ一覧取得 Output
 */
data class ListMemosOutput(
    val memos: List<VoiceMemo>,
)
