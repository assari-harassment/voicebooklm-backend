package com.assari.voicebooklm.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Google Cloud Speech の接続設定。必須値が欠けていれば起動時に失敗させる。
 */
@Validated
@ConfigurationProperties(prefix = "google.cloud.speech")
data class GoogleSpeechProperties(
    @field:NotBlank
    val credentialsPath: String,
    @field:NotBlank
    val projectId: String,
    @field:NotBlank
    val defaultLanguage: String,
    @field:Positive
    val timeoutSeconds: Long,
)
