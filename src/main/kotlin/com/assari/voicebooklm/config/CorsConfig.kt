package com.assari.voicebooklm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * CORS（Cross-Origin Resource Sharing）設定
 *
 * application.yml の cors セクションから設定を読み込む。
 */
@Configuration
@ConfigurationProperties(prefix = "cors")
class CorsConfig {
    lateinit var allowedOriginPatterns: List<String>
    lateinit var allowedMethods: List<String>
    lateinit var allowedHeaders: List<String>
    lateinit var exposedHeaders: List<String>
    var allowCredentials: Boolean = true
    var maxAge: Long = 3600

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = this@CorsConfig.allowedOriginPatterns
            allowedMethods = this@CorsConfig.allowedMethods
            allowedHeaders = this@CorsConfig.allowedHeaders
            exposedHeaders = this@CorsConfig.exposedHeaders
            allowCredentials = this@CorsConfig.allowCredentials
            maxAge = this@CorsConfig.maxAge
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
