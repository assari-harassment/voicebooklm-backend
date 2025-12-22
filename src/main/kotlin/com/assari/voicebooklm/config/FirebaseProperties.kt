package com.assari.voicebooklm.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Firebase の接続設定
 */
@Validated
@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(
    /**
     * Firebase サービスアカウント JSON ファイルのパス
     * 空の場合は Application Default Credentials を使用
     */
    val credentialsPath: String = "",

    /**
     * Firebase プロジェクト ID
     */
    @field:NotBlank
    val projectId: String,
)
