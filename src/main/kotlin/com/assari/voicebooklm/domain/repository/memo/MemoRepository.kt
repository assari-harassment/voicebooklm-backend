package com.assari.voicebooklm.domain.repository.memo

import com.assari.voicebooklm.domain.model.memo.Memo
import java.util.UUID

/**
 * Memo 集約の永続化ポート。
 */
interface MemoRepository {
    suspend fun save(memo: Memo): Memo

    suspend fun findById(id: UUID): Memo?

    suspend fun findByUserId(userId: UUID): List<Memo>
}
