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
     * キーワードでユーザーID の VoiceMemo 一覧を検索する（削除済みを除く）
     * タイトルまたはコンテントにキーワードが含まれるメモを返す
     */
    suspend fun findByUserIdWithKeyword(userId: UUID, keyword: String): List<VoiceMemo>

    /**
     * ユーザーID に紐づくすべての VoiceMemo を削除する（アカウント削除用）
     */
    fun deleteByUserId(userId: UUID)

    /**
     * 指定フォルダー内のメモが存在するかチェックする
     *
     * @param userId ユーザーID
     * @param folderId フォルダーID
     * @return メモが存在する場合true
     */
    suspend fun existsByUserIdAndFolderId(userId: UUID, folderId: UUID): Boolean
}
