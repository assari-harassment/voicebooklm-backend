package com.assari.voicebooklm.presentation.controller.folder

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.folder.CreateFolderInput
import com.assari.voicebooklm.usecase.folder.CreateFolderUseCase
import com.assari.voicebooklm.usecase.folder.DeleteFolderInput
import com.assari.voicebooklm.usecase.folder.DeleteFolderUseCase
import com.assari.voicebooklm.usecase.folder.ListFoldersInput
import com.assari.voicebooklm.usecase.folder.ListFoldersUseCase
import com.assari.voicebooklm.usecase.folder.UpdateFolderInput
import com.assari.voicebooklm.usecase.folder.UpdateFolderUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
    private val createFolderUseCase: CreateFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
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

    @PostMapping("/folders")
    @Operation(
        summary = "フォルダー作成",
        description = "新しいフォルダーを作成する。親フォルダーを指定して階層構造を作成可能。",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "作成成功",
                content = [Content(schema = Schema(implementation = FolderResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "親フォルダーが見つからない",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "同名フォルダーが既に存在する",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun createFolder(
        @AuthenticationPrincipal userId: UUID?,
        @Valid @RequestBody request: CreateFolderRequest,
    ): ResponseEntity<FolderResponse> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val result = createFolderUseCase.execute(
            CreateFolderInput(
                userId = userId,
                name = request.name,
                parentId = request.parentId,
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(FolderResponse.from(result.folder, result.path))
    }

    @PatchMapping("/folders/{id}")
    @Operation(
        summary = "フォルダー更新",
        description = "フォルダーのリネームまたは移動を行う。両方同時に指定可能。",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "更新成功",
                content = [Content(schema = Schema(implementation = FolderResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "フォルダーが見つからない",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "同名フォルダーが既に存在する / 循環参照が発生する",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun updateFolder(
        @AuthenticationPrincipal userId: UUID?,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateFolderRequest,
    ): ResponseEntity<FolderResponse> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val result = updateFolderUseCase.execute(
            UpdateFolderInput(
                userId = userId,
                folderId = id,
                newName = request.name,
                newParentId = request.parentId,
                moveToRoot = request.moveToRoot,
            )
        )
        return ResponseEntity.ok(FolderResponse.from(result.folder, result.path))
    }

    @DeleteMapping("/folders/{id}")
    @Operation(
        summary = "フォルダー削除",
        description = "フォルダーを削除する。子フォルダーまたはメモが存在する場合は削除できない。",
        responses = [
            ApiResponse(responseCode = "204", description = "削除成功"),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "フォルダーが見つからない",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "子フォルダーまたはメモが存在するため削除できない",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ],
    )
    suspend fun deleteFolder(
        @AuthenticationPrincipal userId: UUID?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        deleteFolderUseCase.execute(DeleteFolderInput(userId = userId, folderId = id))
        return ResponseEntity.noContent().build()
    }
}
