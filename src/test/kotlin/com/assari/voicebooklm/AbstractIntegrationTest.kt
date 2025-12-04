package com.assari.voicebooklm

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 統合テスト用のベースクラス
 *
 * このクラスを継承することで、Testcontainers による PostgreSQL コンテナが
 * 自動的に起動し、テスト環境が構築されます。
 *
 * 使用例:
 * ```
 * class UserRepositoryTest : AbstractIntegrationTest() {
 *     @Autowired
 *     lateinit var userRepository: UserRepository
 *
 *     @Test
 *     fun testSaveUser() {
 *         // テストコード
 *     }
 * }
 * ```
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("voicebooklm_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)  // コンテナを再利用して高速化
    }
}
