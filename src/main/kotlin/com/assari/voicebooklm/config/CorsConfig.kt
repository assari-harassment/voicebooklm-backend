package com.assari.voicebooklm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * CORS（Cross-Origin Resource Sharing）設定
 *
 * React Native（iOS/Android）からのAPIアクセスを許可します。
 *
 * 設定内容:
 * - 許可するオリジン: 開発環境（localhost）と本番環境のドメイン
 * - 許可するメソッド: GET, POST, PUT, DELETE, PATCH, OPTIONS
 * - 許可するヘッダー: Authorization, Content-Type など
 * - 認証情報の送信: Cookie、Authorizationヘッダーを許可
 * - Preflight リクエストのキャッシュ: 1時間
 */
@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // 許可するオリジン
            // 開発環境: React Native の Metro bundler
            // 本番環境: 実際のドメインを環境変数で設定
            allowedOriginPatterns = listOf(
                "http://localhost:*",           // Metro bundler
                "http://127.0.0.1:*",           // Metro bundler (IPv4)
                "http://192.168.*.*:*",         // ローカルネットワーク（実機テスト用）
                "https://*.example.com",        // 本番環境（実際のドメインに変更）
            )

            // 許可する HTTP メソッド
            allowedMethods = listOf(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
            )

            // 許可するヘッダー
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "X-CSRF-TOKEN"
            )

            // レスポンスで公開するヘッダー
            exposedHeaders = listOf(
                "Authorization",
                "Content-Disposition"
            )

            // 認証情報（Cookie、Authorizationヘッダー）の送信を許可
            allowCredentials = true

            // Preflight リクエストのキャッシュ時間（秒）
            maxAge = 3600L  // 1時間
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
