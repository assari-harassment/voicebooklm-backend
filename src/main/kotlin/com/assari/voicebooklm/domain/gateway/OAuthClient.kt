package com.assari.voicebooklm.domain.gateway

import com.assari.voicebooklm.domain.model.OAuthUserInfo

/**
 * OAuth クライアントインターフェース
 *
 * OAuth 認証プロバイダーとの通信を抽象化するゲートウェイインターフェース。
 * Domain Layer で定義し、実装は Infrastructure Layer で行う。
 *
 * 現在の実装:
 * - GoogleOAuthClient: Google OAuth 認証
 *
 * 将来の拡張:
 * - AppleOAuthClient: Sign in with Apple
 */
interface OAuthClient {
    /**
     * ID トークンを検証し、ユーザー情報を取得する
     *
     * @param idToken OAuth プロバイダーから発行された ID トークン
     * @return ユーザー情報（検証失敗時は null）
     */
    fun verifyIdTokenAndGetUserInfo(idToken: String): OAuthUserInfo?
}
