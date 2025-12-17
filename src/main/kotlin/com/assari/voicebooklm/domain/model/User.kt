package com.assari.voicebooklm.domain.model

import java.time.Instant
import java.util.UUID

/**
 * ユーザードメインモデル
 *
 * Google OAuth 認証で取得したユーザー情報を表現する純粋な Kotlin クラス。
 * フレームワーク依存なし（Domain Layer）。
 * イミュータブルな設計で、変更時は新しいインスタンスを返す。
 */
data class User(
    val id: UUID,
    val googleSub: String,
    val email: String,
    val name: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    /**
     * ユーザー名を更新した新しい User インスタンスを返す
     */
    fun updateName(newName: String): User = copy(
        name = newName,
        updatedAt = Instant.now()
    )

    // data class のデフォルト equals/hashCode はすべてのプロパティを比較するが、
    // エンティティとしての等価性は id のみで判断する
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
