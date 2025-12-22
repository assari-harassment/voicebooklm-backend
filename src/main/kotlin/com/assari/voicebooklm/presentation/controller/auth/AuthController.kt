package com.assari.voicebooklm.presentation.controller.auth

import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.usecase.auth.DeleteAccountInput
import com.assari.voicebooklm.usecase.auth.DeleteAccountUseCase
import com.assari.voicebooklm.usecase.auth.GetCurrentUserInput
import com.assari.voicebooklm.usecase.auth.GetCurrentUserUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 認証 REST API コントローラー
 *
 * Firebase Authentication を使用した認証を処理する。
 * ログイン・トークンリフレッシュはクライアント側で Firebase SDK が処理するため、
 * サーバー側ではアカウント削除とユーザー情報取得のみを提供する。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "認証関連 API")
class AuthController(
    private val userRepository: UserRepository,
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    private val deleteAccountUseCase = DeleteAccountUseCase(
        userRepository = userRepository,
        voiceMemoRepository = voiceMemoRepository,
    )
    private val getCurrentUserUseCase = GetCurrentUserUseCase(userRepository)

    /** アカウント削除 */
    @Operation(
        summary = "アカウント削除",
        description = "ユーザーアカウントとすべての関連データを完全に削除します。Firebase 側のアカウントも削除されます。"
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "削除成功"),
        ApiResponse(
            responseCode = "401",
            description = "認証失敗",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "ユーザーが見つからない",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @DeleteMapping("/account")
    fun deleteAccount(@AuthenticationPrincipal userId: UUID?): ResponseEntity<Void> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        // Firebase 側のユーザーも削除
        val user = userRepository.findById(userId)
        if (user != null) {
            try {
                FirebaseAuth.getInstance().deleteUser(user.googleSub)
                logger.info("Deleted Firebase user: ${user.googleSub}")
            } catch (e: FirebaseAuthException) {
                logger.warn("Failed to delete Firebase user: ${e.message}")
                // Firebase 側の削除に失敗しても、ローカルデータは削除を続行
            }
        }

        deleteAccountUseCase.execute(DeleteAccountInput(userId))
        return ResponseEntity.noContent().build()
    }

    /** 現在のユーザー情報取得 */
    @Operation(summary = "現在のユーザー情報取得", description = "認証済みユーザーの情報を取得します。")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "取得成功"),
        ApiResponse(
            responseCode = "401",
            description = "認証失敗",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "ユーザーが見つからない",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal userId: UUID?): ResponseEntity<UserResponse> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val userInfo = getCurrentUserUseCase.execute(GetCurrentUserInput(userId))
        return ResponseEntity.ok(
            UserResponse(id = userInfo.id, email = userInfo.email, name = userInfo.name)
        )
    }
}
