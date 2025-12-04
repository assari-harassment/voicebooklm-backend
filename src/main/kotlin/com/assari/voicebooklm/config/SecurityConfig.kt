package com.assari.voicebooklm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }  // API のため CSRF 無効化（JWT 使用時）
            .authorizeHttpRequests { auth ->
                auth
                    // 公開エンドポイント
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/h2-console/**",  // 開発環境用
                        "/api/auth/**"     // 認証エンドポイント（将来実装）
                    ).permitAll()
                    // その他は認証が必要
                    .anyRequest().authenticated()
            }
            .sessionManagement { session ->
                // JWT 使用のためステートレスに設定
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .httpBasic { }  // 開発時は Basic 認証を使用

        // H2 Console のため（開発環境のみ）
        http.headers { headers ->
            headers.frameOptions { it.sameOrigin() }
        }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
