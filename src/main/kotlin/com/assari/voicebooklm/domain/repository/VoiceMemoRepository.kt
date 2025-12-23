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

    /**
     * ユーザーが所有するメモのタグ一覧を取得する（使用回数を含む、削除済みメモを除く）
     *
     * @param userId ユーザーID
     * @return タグ名と使用回数のペアリスト（使用回数降順→タグ名昇順でソート済み）
     */
    suspend fun findTagsWithCountByUserId(userId: UUID): List<Pair<String, Int>>
}
