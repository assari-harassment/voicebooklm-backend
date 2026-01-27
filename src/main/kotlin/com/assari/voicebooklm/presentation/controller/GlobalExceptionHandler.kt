package com.assari.voicebooklm.presentation.controller

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

/**
 * グローバル例外ハンドラ
 *
 * アプリケーション全体のエラーをキャッチし、統一されたエラーレスポンスを返す。
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * ドメイン例外（すべてのビジネスエラーを統一的に処理）
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(e: DomainException): ResponseEntity<ErrorResponse> {
        logger.warn("Domain error: code={} message={}", e.code, e.message)
        return ResponseEntity
            .status(e.code.httpStatus)
            .body(ErrorResponse(
                error = e.message,
                code = e.code.name
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
     * ResponseStatusException（コントローラーで明示的にスローされた HTTP ステータス例外）
     */
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
        logger.warn("Request failed: status={} message={}", e.statusCode.value(), e.reason ?: e.message)
        return ResponseEntity
            .status(e.statusCode)
            .body(ErrorResponse(
                error = e.reason ?: "Bad Request",
                code = "REQUEST_FAILED"
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
