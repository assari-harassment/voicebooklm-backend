package com.assari.voicebooklm.presentation.controller.memo

import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.memo.ListMemosInput
import com.assari.voicebooklm.usecase.memo.ListMemosOutput
import com.assari.voicebooklm.usecase.memo.ListMemosUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * メモ一覧取得 API
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Memo", description = "メモ一覧 API")
class MemoController(
    // ユースケースは Bean 登録せず、このレイヤーで組み立てる
    voiceMemoRepository: VoiceMemoRepository,
) {
    // Bean 化しないことでユースケースをフレームワーク非依存に保つ
    private val listMemosUseCase = ListMemosUseCase(voiceMemoRepository)

    @GetMapping("/memos")
    @Operation(
        summary = "メモ一覧取得",
        description = "検索は未対応。認証ユーザーのメモを新しい順で取得する。",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "取得成功",
                content = [Content(schema = Schema(implementation = ListMemosResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun listMemos(
        @AuthenticationPrincipal userId: UUID?,
    ): ResponseEntity<ListMemosResponse> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val result = listMemosUseCase.execute(ListMemosInput(userId))
        return ResponseEntity.ok(ListMemosResponse.from(result))
    }
}

/**
 * メモ一覧レスポンス
 */
data class ListMemosResponse(
    val memos: List<MemoSummaryResponse>,
) {
    companion object {
        fun from(result: ListMemosOutput): ListMemosResponse {
            // 検索未対応のため、一覧取得結果をそのまま詰める
            val memos = result.memos.map { memo ->
                MemoSummaryResponse(
                    memoId = memo.id,
                    title = memo.title ?: "",
                    content = memo.content ?: "",
                    tags = memo.tags,
                    transcriptionStatus = memo.transcription.status.name,
                    formattingStatus = memo.formatting.status.name,
                    createdAt = memo.createdAt,
                    updatedAt = memo.updatedAt,
                )
            }
            return ListMemosResponse(memos = memos)
        }
    }
}

/**
 * メモ一覧の要素
 */
data class MemoSummaryResponse(
    val memoId: UUID,
    val title: String,
    val content: String,
    val tags: List<String>,
    val transcriptionStatus: String,
    val formattingStatus: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
