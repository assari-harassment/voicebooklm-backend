package com.assari.voicebooklm.presentation.controller.folder

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.folder.ListFoldersInput
import com.assari.voicebooklm.usecase.folder.ListFoldersUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * フォルダー API
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Folder", description = "フォルダー API")
class FolderController(
    private val listFoldersUseCase: ListFoldersUseCase,
) {
    @GetMapping("/folders")
    @Operation(
        summary = "フォルダー一覧取得",
        description = "認証ユーザーのフォルダー一覧をパス情報付きで取得する。",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "取得成功",
                content = [Content(schema = Schema(implementation = ListFoldersResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun listFolders(
        @AuthenticationPrincipal userId: UUID?,
    ): ResponseEntity<ListFoldersResponse> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val result = listFoldersUseCase.execute(ListFoldersInput(userId))
        return ResponseEntity.ok(ListFoldersResponse.from(result))
    }
}
