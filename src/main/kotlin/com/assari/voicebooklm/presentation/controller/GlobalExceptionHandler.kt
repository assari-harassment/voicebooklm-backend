package com.assari.voicebooklm.presentation.controller

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.auth.InvalidGoogleTokenException
import com.assari.voicebooklm.usecase.auth.InvalidRefreshTokenException
import com.assari.voicebooklm.usecase.auth.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * グローバル例外ハンドラ
 *
 * アプリケーション全体のエラーをキャッチし、統一されたエラーレスポンスを返す。
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Google ID トークン検証失敗
     */
    @ExceptionHandler(InvalidGoogleTokenException::class)
    fun handleInvalidGoogleToken(e: InvalidGoogleTokenException): ResponseEntity<ErrorResponse> {
        logger.warn("Google token validation failed: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(
                error = e.message ?: "Google ID トークンの検証に失敗しました",
                code = "INVALID_GOOGLE_TOKEN"
            ))
    }

    /**
     * リフレッシュトークン無効
     */
    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(e: InvalidRefreshTokenException): ResponseEntity<ErrorResponse> {
        logger.warn("Refresh token validation failed: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(
                error = e.message ?: "リフレッシュトークンが無効または期限切れです",
                code = "INVALID_REFRESH_TOKEN"
            ))
    }

    /**
     * ユーザーが見つからない
     */
    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("User not found: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                error = e.message ?: "ユーザーが見つかりません",
                code = "USER_NOT_FOUND"
            ))
    }

    /**
     * 必須ヘッダー欠如（Authorization ヘッダーなしなど）
     */
    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(e: MissingRequestHeaderException): ResponseEntity<ErrorResponse> {
        logger.warn("Missing request header: ${e.headerName}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(
                error = "認証情報がありません",
                code = "UNAUTHORIZED"
            ))
    }

    /**
     * バリデーションエラー
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorMessage = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        logger.warn("Validation error: $errorMessage")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = errorMessage.ifEmpty { "リクエスト形式が不正です" },
                code = "INVALID_REQUEST"
            ))
    }

    /**
     * 予期しないエラー
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericError(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                error = "サーバーエラーが発生しました",
                code = "INTERNAL_ERROR"
            ))
    }
}
