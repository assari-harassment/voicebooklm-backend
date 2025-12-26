package com.assari.voicebooklm.presentation.controller.voice

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.memo.CreateMemoInput
import com.assari.voicebooklm.usecase.memo.CreateMemoUseCase
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

/**
 * 音声アップロードを受け取り、メモ作成ユースケースを呼び出す
 */
@RestController
@RequestMapping("/api/voice")
@Tag(name = "Voice", description = "音声入力によるメモ生成 API")
class VoiceController(
    private val createMemoUseCase: CreateMemoUseCase,
) {
    private val logger = LoggerFactory.getLogger(VoiceController::class.java)

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
                content = [Content(schema = Schema(implementation = VoiceMemoCreatedResponse::class))],
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
    ): ResponseEntity<VoiceMemoCreatedResponse> {
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
        val response = VoiceMemoCreatedResponse.from(result)
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
        // WAV形式のみ受け付ける（Google Speech-to-Text公式サポート）
        // audio/vnd.wave はiOSが送信する正式なWAV MIMEタイプ
        val allowedWavTypes = setOf("audio/wav", "audio/wave", "audio/x-wav", "audio/vnd.wave")
        if (mime !in allowedWavTypes) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only WAV format is supported. Received: $mime",
            )
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
