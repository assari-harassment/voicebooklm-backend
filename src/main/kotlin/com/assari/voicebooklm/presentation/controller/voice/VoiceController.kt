package com.assari.voicebooklm.presentation.controller.voice

import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.gateway.SpeechTranscriber
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.memo.CreateMemoInput
import com.assari.voicebooklm.usecase.memo.CreateMemoOutput
import com.assari.voicebooklm.usecase.memo.CreateMemoUseCase
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.MonotonicExecutionTimer
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * 音声アップロードを受け取り、メモ作成ユースケースを呼び出す
 */
@RestController
@RequestMapping("/api/voice")
@Tag(name = "Voice", description = "音声入力によるメモ生成 API")
class VoiceController(
    // ユースケースは Bean 登録せず、このレイヤーで組み立てる
    voiceMemoRepository: VoiceMemoRepository,
    speechTranscriber: SpeechTranscriber,
    memoFormatter: MemoFormatter,
    executionTimer: ExecutionTimer = MonotonicExecutionTimer(),
    // テストでユースケースの差し替えを可能にするオプション
    createMemoUseCaseOverride: CreateMemoUseCase? = null,
) {

    private val logger = LoggerFactory.getLogger(VoiceController::class.java)
    // createMemoUseCaseOverride が指定されればそれを使用し、通常はここで手動 new。
    // Bean 化しないことでユースケースをフレームワーク非依存に保つ。
    private val createMemoUseCase: CreateMemoUseCase =
        createMemoUseCaseOverride
            ?: CreateMemoUseCase(
                voiceMemoRepository = voiceMemoRepository,
                speechTranscriber = speechTranscriber,
                memoFormatter = memoFormatter,
                executionTimer = executionTimer,
            )

    @PostMapping(
        "/memos",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(
        summary = "音声ファイルからメモを生成する",
        description = "multipart/form-data で音声ファイルをアップロードし、文字起こし→AI整形→保存を行う。60秒以内のレスポンスを想定。",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "メモ生成成功",
                content = [Content(schema = Schema(implementation = CreateMemoResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "入力不正（音声が空、Content-Type 不正など）",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー（JWT 不正）",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "サーバエラー",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun createMemo(
        authentication: Authentication?,
        @RequestPart("file") file: MultipartFile,
        @RequestParam("language", required = false) language: String?,
    ): ResponseEntity<CreateMemoResponse> {
        val userId = extractUserId(authentication)
        validateFile(file)
        val tempPath = writeTempFile(file)
        val audioBytes = readAudio(tempPath)

        val result = try {
            createMemoUseCase.execute(
                CreateMemoInput(
                    userId = userId,
                    audio = audioBytes,
                    audioMimeType = file.contentType ?: "application/octet-stream",
                    language = language,
                ),
            )
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
        } finally {
            Files.deleteIfExists(tempPath)
        }

        val voiceMemo = result.voiceMemo
        logger.info(
            "voice memo created memoId={} userId={} totalMs={} fallbackT={} fallbackF={}",
            voiceMemo.id,
            userId,
            result.processingTime.total.inWholeMilliseconds,
            voiceMemo.transcription.fallbackUsed,
            voiceMemo.formatting.fallbackUsed,
        )
        val response = CreateMemoResponse.from(result)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    private fun extractUserId(authentication: Authentication?): UUID {
        val name = authentication?.name
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        return runCatching { UUID.fromString(name) }
            .getOrElse {
                logger.warn("Authentication name is not a valid UUID: {}", name)
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
            }
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "audio file is empty")
        }
        val mime = file.contentType ?: ""
        if (!mime.startsWith("audio/")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "audio content-type required")
        }
    }

    private fun writeTempFile(file: MultipartFile): Path {
        val suffix = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?.let { ".$it" }
            ?: ".bin"
        val tempPath: Path = Files.createTempFile("voice-upload-", suffix)
        return try {
            file.transferTo(tempPath)
            tempPath
        } catch (ex: Exception) {
            logger.warn("Failed to read multipart audio", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to read audio file")
        }
    }

    private fun readAudio(path: Path): ByteArray {
        return try {
            Files.readAllBytes(path)
        } catch (ex: Exception) {
            logger.warn("Failed to read temp audio file", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to read audio file")
        }
    }
}

/**
 * メモ作成レスポンス
 */
data class CreateMemoResponse(
    val memoId: UUID,
    val title: String,
    val content: String,
    val tags: List<String>,
    val transcription: String?,
    val transcriptionStatus: String,
    val formattingStatus: String,
    val processingTimeMillis: ProcessingTimeResponse,
    val fallback: FallbackUsageResponse,
) {
    companion object {
        fun from(result: CreateMemoOutput): CreateMemoResponse {
            val voiceMemo = result.voiceMemo
            return CreateMemoResponse(
                memoId = voiceMemo.id,
                title = voiceMemo.title ?: "",
                content = voiceMemo.content ?: "",
                tags = voiceMemo.tags,
                transcription = voiceMemo.transcriptionText,
                transcriptionStatus = voiceMemo.transcription.status.name,
                formattingStatus = voiceMemo.formatting.status.name,
                processingTimeMillis = ProcessingTimeResponse(
                    transcription = result.processingTime.transcription.toMillis(),
                    formatting = result.processingTime.formatting.toMillis(),
                    persistence = result.processingTime.persistence.toMillis(),
                    total = result.processingTime.total.toMillis(),
                ),
                fallback = FallbackUsageResponse(
                    transcription = voiceMemo.transcription.fallbackUsed,
                    formatting = voiceMemo.formatting.fallbackUsed,
                ),
            )
        }
    }
}

data class ProcessingTimeResponse(
    val transcription: Long,
    val formatting: Long,
    val persistence: Long,
    val total: Long,
)

data class FallbackUsageResponse(
    val transcription: Boolean,
    val formatting: Boolean,
)

private fun Duration.toMillis(): Long = this.toJavaDuration().toMillis()
