package com.assari.voicebooklm.config

import com.assari.voicebooklm.infrastructure.ratelimit.RateLimitFilter
import com.assari.voicebooklm.infrastructure.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

/**
 * セキュリティ設定
 * - JWTトークン認証
 * - レート制限
 */

@Configuration
@EnableWebSecurity
class SecurityConfig(
        private val corsConfigurationSource: CorsConfigurationSource,
        private val jwtAuthenticationFilter: JwtAuthenticationFilter,
        private val rateLimitFilter: RateLimitFilter
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
                                    "/api/auth/**", // 認証エンドポイント（ログイン、リフレッシュ、ログアウト）
                                    "/swagger-ui.html", // Swagger UI
                                    "/swagger-ui/**", // Swagger UI リソース
                                    "/v3/api-docs/**", // OpenAPI 仕様
                                    "/api-docs/**" // OpenAPI 仕様（代替パス）
                            )
                            .permitAll()
                            // その他は認証が必要
                            .anyRequest()
                            .authenticated()
                }
                .sessionManagement { session ->
                    // JWT 使用のためステートレスに設定
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                }
                // レート制限フィルターを追加（認証フィルターの前）
                .addFilterBefore(
                        rateLimitFilter,
                        UsernamePasswordAuthenticationFilter::class.java
                )
                // JWT 認証フィルターを追加
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter::class.java
                )
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
                        "/api/auth/**", // 認証エンドポイント（ログイン、リフレッシュ、ログアウト）
                        "/swagger-ui.html", // Swagger UI
                        "/swagger-ui/**", // Swagger UI リソース
                        "/v3/api-docs/**", // OpenAPI 仕様
                        "/api-docs/**", // OpenAPI 仕様（代替パス）
                    )
                    .permitAll()
                    // その他は認証が必要
                    .anyRequest()
                    .authenticated()
            }
            .sessionManagement { session ->
                // JWT 使用のためステートレスに設定
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            // JWT 認証フィルターを追加
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
