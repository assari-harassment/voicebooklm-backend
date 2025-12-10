package com.assari.voicebooklm.domain.model

import java.time.Instant
import java.util.UUID

/**
 * リフレッシュトークンドメインモデル
 *
 * JWT リフレッシュトークンの情報を表現する純粋な Kotlin クラス。
 * トークンローテーション（使用後に無効化して新しいトークンを発行）をサポート。
 * フレームワーク依存なし（Domain Layer）。
 */
class RefreshToken(
    val id: UUID,
    val token: String,
    val userId: UUID,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
    var revoked: Boolean = false
) {
    /**
     * トークンが有効期限切れかどうかを判定する
     */
    fun isExpired(): Boolean {
        return Instant.now().isAfter(expiresAt)
    }

    /**
     * トークンが有効かどうかを判定する
     * 無効化されておらず、有効期限内である場合に true を返す
     */
    fun isValid(): Boolean {
        return !revoked && !isExpired()
    }

    /**
     * トークンを無効化する
     */
    fun revoke() {
        this.revoked = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RefreshToken) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "RefreshToken(id=$id, userId=$userId, expiresAt=$expiresAt, revoked=$revoked)"
    }
}
