package com.assari.voicebooklm.domain.model

/**
 * OAuth ユーザー情報
 *
 * OAuth プロバイダーから取得したユーザー情報を表現する値オブジェクト。
 * プロバイダーに依存しない汎用的なデータ構造（Domain Layer）。
 */
data class OAuthUserInfo(
    /**
     * プロバイダー固有のユーザー識別子
     * - Google: sub claim
     * - Apple: sub claim
     */
    val providerId: String,

    /**
     * メールアドレス
     */
    val email: String,

    /**
     * 表示名
     */
    val name: String,

    /**
     * プロフィール画像 URL（オプション）
     */
    val picture: String? = null
)
