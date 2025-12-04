package com.assari.voicebooklm

import org.junit.jupiter.api.Test

/**
 * アプリケーション起動テスト
 *
 * PostgreSQL Testcontainer を使用して、アプリケーションコンテキストが
 * 正常にロードされることを確認します。
 */
class VoiceBookLmBackendApplicationTests : AbstractIntegrationTest() {

    @Test
    fun contextLoads() {
        // PostgreSQL Testcontainer が起動し、Spring コンテキストが正常にロードされることを確認
    }
}
