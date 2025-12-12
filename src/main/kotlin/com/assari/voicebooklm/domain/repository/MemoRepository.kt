package com.assari.voicebooklm.domain.repository

import com.assari.voicebooklm.domain.model.Memo
import java.util.UUID

/**
 * Memo 集約の永続化ポート。
 */
interface MemoRepository {
    suspend fun save(memo: Memo): Memo

    suspend fun findById(id: UUID): Memo?

    suspend fun findByUserId(userId: UUID): List<Memo>

    fun deleteByUserId(userId: UUID)
}
