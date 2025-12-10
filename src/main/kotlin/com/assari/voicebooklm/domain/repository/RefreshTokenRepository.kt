package com.assari.voicebooklm.domain.repository

import com.assari.voicebooklm.domain.model.RefreshToken
import java.time.Instant
import java.util.UUID

/**
 * リフレッシュトークンリポジトリインターフェース
 *
 * Domain Layer で定義されるリポジトリインターフェース。
 * 実装は Infrastructure Layer で行う。
 */
interface RefreshTokenRepository {
    /**
     * リフレッシュトークンを保存する
     */
    fun save(token: RefreshToken): RefreshToken

    /**
     * 有効なトークンを取得する（未失効かつ有効期限内）
     */
    fun findByTokenAndValid(token: String, now: Instant): RefreshToken?

    /**
     * トークン文字列でトークンを無効化する
     */
    fun revokeByToken(token: String)

    /**
     * ユーザー ID で全トークンを無効化する（ログアウト時）
     */
    fun revokeByUserId(userId: UUID)

    /**
     * ユーザー ID で全トークンを削除する（アカウント削除時）
     */
    fun deleteByUserId(userId: UUID)
}
