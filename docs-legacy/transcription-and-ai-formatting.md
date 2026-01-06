
# 文字起こしと AI 整形

音声データをテキストに変換し、AI でメモとして整形する機能の技術ドキュメント。

## 概要

VoiceBookLM では、ユーザーが録音した音声を以下の 2 段階で処理する:

1. **文字起こし (Speech-to-Text)**: Google Cloud Speech-to-Text API を使用して音声をテキストに変換
2. **AI 整形 (Memo Formatting)**: Gemini AI を使用してテキストを構造化されたメモに整形

## アーキテクチャ

```
┌─────────────────────────────────────────────────────────────────┐
│                      CreateMemoService                          │
│                     (ユースケース層)                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────────┐      ┌─────────────────────┐         │
│   │  SpeechTranscriber  │      │  AiMemoFormatter    │         │
│   │   (インターフェース)  │      │   (インターフェース)  │         │
│   └──────────┬──────────┘      └──────────┬──────────┘         │
│              │                            │                     │
└──────────────┼────────────────────────────┼─────────────────────┘
               │                            │
┌──────────────▼──────────────┐  ┌──────────▼──────────────┐
│  GoogleSpeechTranscriber    │  │  GeminiAiMemoFormatter  │
│  (インフラストラクチャ層)     │  │  (インフラストラクチャ層)  │
│                             │  │                         │
│  Google Cloud Speech-to-Text│  │  Gemini API             │
└─────────────────────────────┘  └─────────────────────────┘
```

## コンポーネント詳細

### 1. SpeechTranscriber インターフェース

音声をテキストに変換するクライアントのインターフェース。

**ファイル**: `src/main/kotlin/com/assari/voicebooklm/usecase/memo/client/SpeechTranscriber.kt`

```kotlin
interface SpeechTranscriber {
    suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscription
}
```

#### データクラス

| クラス | 説明 |
|--------|------|
| `SpeechTranscriptionCommand` | 文字起こし要求（userId, audio, mimeType, languageCode） |
| `SpeechTranscription` | 文字起こし結果（text, languageCode） |

### 2. GoogleSpeechTranscriber 実装

Google Cloud Speech-to-Text API を使用した実装。

**ファイル**: `src/main/kotlin/com/assari/voicebooklm/infrastructure/api/speech/GoogleSpeechTranscriber.kt`

#### 主要パラメータ

| パラメータ | デフォルト値 | 説明 |
|-----------|-------------|------|
| `defaultLanguageCode` | `"ja-JP"` | デフォルト言語コード |
| `fallbackText` | `"[transcription unavailable]"` | エラー時のフォールバックテキスト |
| `timeout` | `60秒` | API タイムアウト |

#### 処理フロー

1. `RecognitionConfig` を構築（言語コード、LINEAR16 エンコーディング）
2. `RecognitionAudio` に音声バイナリをセット
3. `speechClient.recognize()` で同期的に文字起こし実行
4. 結果を `SpeechTranscription` に変換
5. 失敗時はフォールバックテキストを返却

#### エラーハンドリング

- タイムアウト (`TimeoutCancellationException`): フォールバックテキストを返却
- その他の例外: フォールバックテキストを返却
- `CancellationException`（コルーチンキャンセル）: 再スロー

### 3. AiMemoFormatter インターフェース

文字起こしテキストを構造化されたメモに整形する AI クライアントのインターフェース。

**ファイル**: `src/main/kotlin/com/assari/voicebooklm/usecase/memo/client/AiMemoFormatter.kt`

```kotlin
interface AiMemoFormatter {
    suspend fun format(command: AiMemoFormatCommand): AiMemoDraft
}
```

#### データクラス

| クラス | 説明 |
|--------|------|
| `AiMemoFormatCommand` | 整形要求（userId, transcript） |
| `AiMemoDraft` | AI 生成メモ下書き（title, content, tags） |

### 4. GeminiAiMemoFormatter 実装

Gemini API を使用したメモ整形の実装。

**ファイル**: `src/main/kotlin/com/assari/voicebooklm/infrastructure/api/ai/GeminiAiMemoFormatter.kt`

#### 主要パラメータ

| パラメータ | デフォルト値 | 説明 |
|-----------|-------------|------|
| `model` | `"gemini-2.0-flash"` | 使用する Gemini モデル |
| `timeout` | `60秒` | API タイムアウト |
| `baseUrl` | `"https://generativelanguage.googleapis.com"` | API ベース URL |

#### プロンプト

```
次の文字起こしを要約し、Markdown のメモを生成してください。
- 50 文字以内のタイトル
- Markdown本文
- 2-4 個の英単語タグ

Transcript:
{transcript}
```

#### 処理フロー

1. `GeminiRequest` を構築（プロンプト + 文字起こしテキスト）
2. WebClient で POST リクエスト送信
3. `GeminiResponse` をパース
4. タイトル、コンテンツ、タグを抽出して `AiMemoDraft` を生成
5. 失敗時はフォールバックメモを生成

#### タグ抽出ロジック

1. `tags` または `タグ` を含む行を検索
2. `:` 以降の文字列を取得
3. `,`、`・`、スペースで分割
4. `#` プレフィックスを除去
5. 最大 4 つのタグを返却

#### フォールバック処理

```kotlin
private fun fallbackDraft(transcript: String): AiMemoDraft {
    val title = transcript.lines().firstOrNull().orEmpty().take(40).ifBlank { "ボイスメモ" }
    val content = transcript.ifBlank { "音声内容を取得できませんでした。" }
    return AiMemoDraft(title = title, content = content, tags = emptyList())
}
```

### 5. CreateMemoService（統合ユースケース）

文字起こしと AI 整形を統合し、メモを作成するサービス。

**ファイル**: `src/main/kotlin/com/assari/voicebooklm/usecase/memo/CreateMemoService.kt`

#### 処理フロー

```
1. 音声データのバリデーション
2. 文字起こし実行（タイムアウト・エラー時はフォールバック）
3. AI 整形実行（タイムアウト・エラー時はフォールバック）
4. メモをデータベースに保存
5. 処理結果を返却（処理時間、フォールバック使用状況を含む）
```

#### レスポンス構造

```kotlin
data class CreateMemoResult(
    val memo: Memo,                    // 保存されたメモ
    val transcription: SpeechTranscription,  // 文字起こし結果
    val processingTime: ProcessingTime,      // 各工程の処理時間
    val fallbackUsage: FallbackUsage,        // フォールバック使用状況
)
```

## 設定

### 環境変数

| 変数名 | 説明 |
|--------|------|
| `GOOGLE_APPLICATION_CREDENTIALS` | Google Cloud 認証情報ファイルパス |
| `GOOGLE_SPEECH_LANGUAGE_CODE` | デフォルト言語コード（任意） |
| `GEMINI_API_KEY` | Gemini API キー |
| `GEMINI_MODEL` | Gemini モデル名（任意） |

### Bean 設定

外部クライアントの Bean 登録は `ExternalClientConfig.kt` で実施:

```kotlin
@Bean
fun speechTranscriber(speechClient: SpeechClient, props: GoogleSpeechProperties): SpeechTranscriber

@Bean
fun aiMemoFormatter(webClient: WebClient, props: GeminiProperties): AiMemoFormatter
```

## エラーハンドリング戦略

このシステムは「フェイルソフト」戦略を採用している:

1. **文字起こし失敗時**: `"[transcription unavailable]"` を使用して AI 整形を続行
2. **AI 整形失敗時**: 文字起こしテキストをそのままメモ本文として使用
3. **すべての工程で**: 警告ログを出力し、処理を継続

これにより、外部 API の一時的な障害があってもユーザー体験を損なわない。

## パフォーマンス

### タイムアウト設定

| 工程 | タイムアウト |
|------|------------|
| 文字起こし | 60 秒 |
| AI 整形 | 60 秒 |

### 処理時間の計測

`ExecutionTimer` を使用して各工程の処理時間を計測し、レスポンスに含める。
これにより、パフォーマンスのモニタリングとボトルネックの特定が可能。

## 関連ファイル

| ファイル | 説明 |
|----------|------|
| `usecase/memo/client/SpeechTranscriber.kt` | 文字起こしインターフェース |
| `usecase/memo/client/AiMemoFormatter.kt` | AI 整形インターフェース |
| `infrastructure/api/speech/GoogleSpeechTranscriber.kt` | Google Speech 実装 |
| `infrastructure/api/ai/GeminiAiMemoFormatter.kt` | Gemini 実装 |
| `usecase/memo/CreateMemoService.kt` | 統合ユースケース |
| `config/GoogleSpeechProperties.kt` | Speech-to-Text 設定 |
| `config/GeminiProperties.kt` | Gemini 設定 |
| `config/ExternalClientConfig.kt` | Bean 設定 |
