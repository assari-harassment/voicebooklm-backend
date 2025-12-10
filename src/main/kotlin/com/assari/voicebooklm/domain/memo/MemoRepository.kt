package com.assari.voicebooklm.domain.memo

import java.util.UUID

/**
 * Memo 集約の永続化ポート。
 */
interface MemoRepository {
    suspend fun save(memo: Memo): Memo

    suspend fun findById(id: UUID): Memo?

    suspend fun findByUserId(userId: UUID): List<Memo>
}
