package com.assari.voicebooklm.domain.repository

import com.assari.voicebooklm.domain.model.VoiceMemo
import java.util.UUID

/**
 * VoiceMemo 集約の永続化ポート
 */
interface VoiceMemoRepository {
    /**
     * VoiceMemo を保存する
     */
    suspend fun save(voiceMemo: VoiceMemo): VoiceMemo

    /**
     * ID で VoiceMemo を取得する
     */
    suspend fun findById(id: UUID): VoiceMemo?

    /**
     * ユーザーID で VoiceMemo 一覧を取得する（削除済みを除く）
     */
    suspend fun findByUserId(userId: UUID): List<VoiceMemo>

    /**
     * ユーザーID に紐づくすべての VoiceMemo を削除する（アカウント削除用）
     */
    fun deleteByUserId(userId: UUID)
}
