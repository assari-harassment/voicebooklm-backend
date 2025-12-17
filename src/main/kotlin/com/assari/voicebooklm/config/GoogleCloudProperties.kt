package com.assari.voicebooklm.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Google Cloud の接続設定（GCS・Speech-to-Text 共通）
 */
@Validated
@ConfigurationProperties(prefix = "google.cloud")
data class GoogleCloudProperties(
    @field:NotBlank
    val credentialsPath: String,

    @field:NotBlank
    val projectId: String,

    @field:Valid
    val storage: StorageProperties,

    @field:Valid
    val speech: SpeechProperties,
) {
    /**
     * Cloud Storage 設定
     */
    data class StorageProperties(
        @field:NotBlank
        val bucketName: String,

        @field:NotBlank
        val location: String = "asia-northeast2",
    )

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
