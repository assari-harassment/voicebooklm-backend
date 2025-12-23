tepackage com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.AbstractIntegrationTest
import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.FormattingStatus
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.TranscriptionStatus
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.infrastructure.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * TagController 統合テスト
 *
 * Testcontainers を使用して、実際の PostgreSQL データベースに対して
 * タグ一覧取得 API のエンドツーエンドテストを実行する。
 */
@AutoConfigureMockMvc
@Transactional
class TagControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var voiceMemoRepository: VoiceMemoRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var testAccessToken: String
    private lateinit var otherUser: User

    @BeforeEach
    fun setUp() {
        // テストユーザーを作成
        testUser = User(
            id = UUID.randomUUID(),
            googleSub = "google-sub-${UUID.randomUUID()}",
            email = "test-${UUID.randomUUID()}@example.com",
            name = "Test User",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        userRepository.save(testUser)

        // 他のユーザーを作成（ユーザー間のデータ分離をテストするため）
        otherUser = User(
            id = UUID.randomUUID(),
            googleSub = "google-sub-other-${UUID.randomUUID()}",
            email = "other-${UUID.randomUUID()}@example.com",
            name = "Other User",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        userRepository.save(otherUser)

        // アクセストークンを生成
        testAccessToken = jwtTokenProvider.generateAccessToken(testUser.id, testUser.email)
    }

    // =====================================================================
    // GET /api/tags - タグ一覧取得
    // =====================================================================

    @Test
    fun `GET tags - should return tags sorted by count DESC when authenticated`() {
        // Given: テストユーザーのメモを複数作成（異なるタグと使用回数）
        createVoiceMemo(
            userId = testUser.id,
            tags = listOf("仕事", "重要")  // 2つ
        )
        createVoiceMemo(
            userId = testUser.id,
            tags = listOf("仕事")  // 1つ（"仕事"は合計2回）
        )
        createVoiceMemo(
            userId = testUser.id,
            tags = listOf("学習", "読書", "重要")  // 3つ（"重要"は合計2回）
        )
        // 他のユーザーのメモは含まれないことを確認するため
        createVoiceMemo(
            userId = otherUser.id,
            tags = listOf("プライベート")
        )

        // When & Then
        mockMvc.perform(
            get("/api/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tags").isArray)
            .andExpect(jsonPath("$.tags.length()").value(4))  // 仕事, 重要, 学習, 読書
            // 使用回数降順でソートされていることを確認
            .andExpect(jsonPath("$.tags[0].name").value("重要"))
            .andExpect(jsonPath("$.tags[0].count").value(2))
            .andExpect(jsonPath("$.tags[1].name").value("仕事"))
            .andExpect(jsonPath("$.tags[1].count").value(2))
            // 使用回数が同じ場合はタグ名昇順（学習 < 読書）
            .andExpect(jsonPath("$.tags[2].name").value("学習"))
            .andExpect(jsonPath("$.tags[2].count").value(1))
            .andExpect(jsonPath("$.tags[3].name").value("読書"))
            .andExpect(jsonPath("$.tags[3].count").value(1))
    }

    @Test
    fun `GET tags - should return empty list when user has no memos`() {
        // Given: メモを作成しない

        // When & Then
        mockMvc.perform(
            get("/api/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tags").isArray)
            .andExpect(jsonPath("$.tags.length()").value(0))
    }

    @Test
    fun `GET tags - should exclude tags from deleted memos`() {
        // Given: 通常のメモと削除済みメモを作成
        val normalMemo = createVoiceMemo(
            userId = testUser.id,
            tags = listOf("仕事")
        )
        val deletedMemo = createVoiceMemo(
            userId = testUser.id,
            tags = listOf("削除済みタグ")
        )
        // メモを削除
        val deletedVoiceMemo = voiceMemoRepository.findById(deletedMemo.id)!!
        val deleted = deletedVoiceMemo.markAsDeleted()
        voiceMemoRepository.save(deleted)

        // When & Then: 削除済みメモのタグは含まれない
        mockMvc.perform(
            get("/api/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tags.length()").value(1))
            .andExpect(jsonPath("$.tags[0].name").value("仕事"))
            .andExpect(jsonPath("$.tags[0].count").value(1))
    }

    @Test
    fun `GET tags - should return 401 when not authenticated`() {
        // When & Then: JWTトークンなし
        mockMvc.perform(get("/api/tags"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET tags - should return 401 when token is invalid`() {
        // When & Then: 無効なJWTトークン
        mockMvc.perform(
            get("/api/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET tags - should return only tags from current user`() {
        // Given: テストユーザーと他のユーザーそれぞれにメモを作成
        createVoiceMemo(
            userId = testUser.id,
            tags = listOf("自分のタグ")
        )
        createVoiceMemo(
            userId = otherUser.id,
            tags = listOf("他人のタグ")
        )

        // When & Then: 自分のタグのみが返される
        mockMvc.perform(
            get("/api/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tags.length()").value(1))
            .andExpect(jsonPath("$.tags[0].name").value("自分のタグ"))
            .andExpect(jsonPath("$.tags[0].count").value(1))
    }

    // =====================================================================
    // ヘルパー関数
    // =====================================================================

    /**
     * テスト用のVoiceMemoを作成して保存する
     */
    private fun createVoiceMemo(
        userId: UUID,
        tags: List<String> = emptyList()
    ): VoiceMemo {
        val voiceMemo = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed(
                text = "テスト用の文字起こしテキスト",
                languageCode = "ja-JP"
            ),
            formatting = Formatting.completed(
                title = "テスト用タイトル",
                content = "テスト用の本文",
                tags = tags
            ),
            deleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return voiceMemoRepository.save(voiceMemo)
    }
}

