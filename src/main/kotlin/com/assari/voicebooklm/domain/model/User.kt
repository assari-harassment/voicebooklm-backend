package com.assari.voicebooklm.domain.model

import java.time.Instant
import java.util.UUID

/**
 * ユーザードメインモデル
 *
 * Google OAuth 認証で取得したユーザー情報を表現する純粋な Kotlin クラス。
 * フレームワーク依存なし（Domain Layer）。
 */
class User(
    val id: UUID,
    val googleSub: String,
    val email: String,
    var name: String,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
) {
    /**
     * ユーザー名を更新する
     */
    fun updateName(newName: String) {
        this.name = newName
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "User(id=$id, email=$email, name=$name)"
    }
}
