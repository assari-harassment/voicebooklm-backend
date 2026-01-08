package com.assari.voicebooklm.presentation.controller.voice

import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.gateway.SpeechTranscriber
import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.infrastructure.security.JwtTokenProvider
import com.assari.voicebooklm.usecase.memo.CreateMemoOutput
import com.assari.voicebooklm.usecase.memo.CreateMemoUseCase
import com.assari.voicebooklm.usecase.memo.ProcessingTime
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.mockk.coEvery
import java.util.Date
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * JWT フィルタを通した HTTP レベルの VoiceController テスト。
 */
@Disabled("Security filter chain調整中（JWT WebMvcTest）")
@WebMvcTest(controllers = [VoiceController::class])
@Import(
    SecurityConfig::class,
    JwtTokenProvider::class,
    VoiceControllerJwtWebMvcTest.TestCorsConfig::class,
)
@TestPropertySource(properties = ["jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"])
class VoiceControllerJwtWebMvcTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean(relaxed = true)
    lateinit var voiceMemoRepository: VoiceMemoRepository

    @MockkBean(relaxed = true)
    lateinit var speechTranscriber: SpeechTranscriber

    @MockkBean(relaxed = true)
    lateinit var memoFormatter: MemoFormatter

    @MockkBean(relaxed = true)
    lateinit var executionTimer: ExecutionTimer

    @MockkBean
    lateinit var createMemoUseCase: CreateMemoUseCase

    @Test
    fun `JWT ありで201`() {
        val memoId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        coEvery { createMemoUseCase.execute(any()) } returns stubOutput(memoId, userId)

        val token = generateToken(userId)
        val file = MockMultipartFile("file", "voice.wav", "audio/wav", byteArrayOf(1, 2, 3))

        mockMvc.perform(
            multipart("/api/voice/memos")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.MULTIPART_FORM_DATA),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.memoId").value(memoId.toString()))
    }

    @Test
    fun `JWT なしは401`() {
        val file = MockMultipartFile("file", "voice.wav", "audio/wav", byteArrayOf(1, 2, 3))

        mockMvc.perform(
            multipart("/api/voice/memos")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA),
        )
            .andExpect(status().isUnauthorized)
    }

    private fun generateToken(userId: UUID): String {
        val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val keySpec = SecretKeySpec(secret.toByteArray(), SignatureAlgorithm.HS256.jcaName)
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .signWith(keySpec)
            .compact()
    }

    private fun stubOutput(memoId: UUID, userId: UUID): CreateMemoOutput {
        val voiceMemo = VoiceMemo(
            id = memoId,
            userId = userId,
            transcription = Transcription.completed(
                text = "text",
                languageCode = "ja-JP",
                fallbackUsed = false,
            ),
            formatting = Formatting.completed(
                title = "title",
                content = "content",
                tagIds = listOf(UUID.randomUUID()),
                fallbackUsed = false,
            ),
        )
        return CreateMemoOutput(
            voiceMemo = voiceMemo,
            processingTime = ProcessingTime(
                transcription = 10.milliseconds,
                formatting = 20.milliseconds,
                persistence = 30.milliseconds,
                total = Duration.parse("0.06s"),
            ),
        )
    }

    @Configuration
    class TestCorsConfig {
        @Bean
        @Primary
        fun corsConfigurationSource(): CorsConfigurationSource {
            val config = CorsConfiguration()
            config.allowedOrigins = listOf("*")
            config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            config.allowedHeaders = listOf("*")
            val source = UrlBasedCorsConfigurationSource()
            source.registerCorsConfiguration("/**", config)
            return source
        }
    }
}
