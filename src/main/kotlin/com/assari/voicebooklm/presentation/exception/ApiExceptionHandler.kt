package com.assari.voicebooklm.presentation.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

/**
 * API 全体の共通エラーレスポンス。
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        logger.warn("Request failed: status={} message={}", ex.statusCode.value(), ex.reason ?: ex.message)
        return ResponseEntity
            .status(ex.statusCode)
            .body(
                ErrorResponse(
                    status = ex.statusCode.value(),
                    message = ex.reason ?: "Bad Request",
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleOther(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    message = "Internal Server Error",
                ),
            )
    }
}

data class ErrorResponse(
    val status: Int,
    val message: String,
)
