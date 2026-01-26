package com.assari.voicebooklm.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Google Cloud の接続設定（Speech-to-Text）
 */
@Validated
@ConfigurationProperties(prefix = "google.cloud")
data class GoogleCloudProperties(
    @field:NotBlank
    val credentialsPath: String,

    @field:NotBlank
    val projectId: String,

    @field:Valid
    val speech: SpeechProperties,
) {
    /**
     * Speech-to-Text 設定
     */
    data class SpeechProperties(
        @field:NotBlank
        val defaultLanguage: String = "ja-JP",

        @field:Positive
        val timeoutSeconds: Long = 300,
    )
}
