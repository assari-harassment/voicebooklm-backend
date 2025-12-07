package com.assari.voicebooklm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfigurationSource

/**
 * セキュリティ設定
 *
 * 開発環境・本番環境で共通の認証方式を使用:
 * - JWT トークン認証（API アクセス用）
 * - Google OAuth2 認証（ソーシャルログイン）
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val corsConfigurationSource: CorsConfigurationSource
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CORS 有効化（React Native からのアクセスを許可）
            .cors { it.configurationSource(corsConfigurationSource) }
            // CSRF 無効化（REST API のため）
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    // 公開エンドポイント
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/api/auth/**",           // 認証エンドポイント
                        "/oauth2/**",             // OAuth2 認証フロー
                        "/login/oauth2/**",       // OAuth2 コールバック
                        "/swagger-ui.html",       // Swagger UI
                        "/swagger-ui/**",         // Swagger UI リソース
                        "/v3/api-docs/**",        // OpenAPI 仕様
                        "/api-docs/**"            // OpenAPI 仕様（代替パス）
                    ).permitAll()
                    // その他は認証が必要
                    .anyRequest().authenticated()
            }
            .sessionManagement { session ->
                // JWT 使用のためステートレスに設定
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            // OAuth2 ログイン設定（Google 認証）
            .oauth2Login { oauth2 ->
                oauth2
                    .loginPage("/api/auth/login")
                    .defaultSuccessUrl("/api/auth/oauth2/success", true)
                    .failureUrl("/api/auth/oauth2/failure")
            }
            // TODO: 自前 JWT 認証フィルターを後で追加
            // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
