package com.assari.voicebooklm.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * レート制限設定
 *
 * RateLimitProperties を有効化し、設定値をバインドする。
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties::class)
class RateLimitConfig
