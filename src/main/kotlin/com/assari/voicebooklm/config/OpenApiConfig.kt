package com.assari.voicebooklm.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI（Swagger）設定
 *
 * API ドキュメントを自動生成します。
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI 仕様: http://localhost:8080/v3/api-docs
 * - TypeScript型定義の生成も可能
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("VoiceBook LM API")
                    .description(
                        """
                        AI ボイスメモアプリケーションのバックエンド API

                        ## 機能
                        - ユーザー認証・認可（JWT）
                        - 音声ファイルのアップロード・管理
                        - AI による音声認識・文字起こし
                        - 音声メモの検索・フィルタ

                        ## React Native との連携
                        - CORS 設定済み
                        - JWT トークン認証
                        - TypeScript 型定義の生成が可能
                        """.trimIndent()
                    )
                    .version("0.0.1-SNAPSHOT")
                    .contact(
                        Contact()
                            .name("VoiceBook LM Team")
                            .email("support@voicebooklm.example.com")
                    )
                    .license(
                        License()
                            .name("Private")
                            .url("https://voicebooklm.example.com/license")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("開発環境"),
                    Server()
                        .url("https://api.voicebooklm.example.com")
                        .description("本番環境")
                )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT トークン認証")
                    )
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList("bearerAuth")
            )
    }
}
