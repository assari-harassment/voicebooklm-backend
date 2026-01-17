package com.assari.voicebooklm.domain.exception

import org.springframework.http.HttpStatus

/**
 * ドメイン例外のエラーコード
 *
 * 新しいエラーを追加する場合はここに1行追加するだけ。
 */
enum class ErrorCode(val httpStatus: HttpStatus, val defaultMessage: String) {
    // 認証系 (401)
    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, "IDトークンの検証に失敗しました"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "リフレッシュトークンが無効または期限切れです"),

    // 認可失敗 (403)
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "このメモへのアクセス権限がありません"),

    // リソース未発見 (404)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"),
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "メモが見つかりません"),
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "フォルダーが見つかりません"),

    // 競合 (409)
    FOLDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "同じ名前のフォルダーが既に存在します"),
    FOLDER_HAS_CHILDREN(HttpStatus.CONFLICT, "子フォルダーが存在するため削除できません"),
    FOLDER_HAS_MEMOS(HttpStatus.CONFLICT, "フォルダー内にメモが存在するため削除できません"),
    FOLDER_CIRCULAR_REFERENCE(HttpStatus.CONFLICT, "循環参照が発生するため移動できません"),

    // 処理失敗 (422)
    TRANSCRIPTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "音声の文字起こしに失敗しました"),
    TRANSCRIPTION_NOT_COMPLETED(HttpStatus.UNPROCESSABLE_ENTITY, "文字起こしが完了していないため編集できません"),
    INVALID_TRANSCRIPTION(HttpStatus.UNPROCESSABLE_ENTITY, "文字起こしテキストが空です"),
    FORMATTING_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "メモの整形に失敗しました"),
    MEMO_NOT_COMPLETED(HttpStatus.UNPROCESSABLE_ENTITY, "メモの整形が完了していないため編集できません"),
}

/**
 * ドメイン層の例外クラス
 *
 * すべてのドメインエラーはこのクラスを使用する。
 * GlobalExceptionHandler で統一的に処理される。
 *
 * @property code エラーコード（HTTPステータスとデフォルトメッセージを含む）
 * @property message カスタムメッセージ（省略時はデフォルトメッセージ）
 * @property cause 原因となった例外（オプション）
 */
class DomainException(
    val code: ErrorCode,
    override val message: String = code.defaultMessage,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
