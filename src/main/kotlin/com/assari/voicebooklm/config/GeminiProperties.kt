package com.assari.voicebooklm.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Gemini API への接続設定。必須値が欠けていれば起動時に失敗させる。
 */
@Validated
@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    @field:NotBlank
    val apiKey: String,
    @field:NotBlank
    val model: String,
    @field:Positive
    val timeoutSeconds: Long,
    @field:NotBlank
    val baseUrl: String,
)
