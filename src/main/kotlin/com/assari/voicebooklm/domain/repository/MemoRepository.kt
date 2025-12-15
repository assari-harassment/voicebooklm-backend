package com.assari.voicebooklm.domain.repository

import java.util.UUID

/**
 * メモリポジトリインターフェース
 *
 * Domain Layer で定義されるリポジトリインターフェース。
 * 実装は Infrastructure Layer で行う。
 *
 * NOTE: アカウント削除に必要なメソッドのみ定義。
 * 他のメソッドは Phase 3 で追加予定。
 */
interface MemoRepository {
    /**
     * ユーザー ID でメモを物理削除する（アカウント削除時）
     */
    fun deleteByUserId(userId: UUID)
}
