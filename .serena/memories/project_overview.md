# VoiceBookLM Backend - プロジェクト概要

## 目的
AI ボイスメモアプリケーションのバックエンドサービス。音声を録音してテキストに変換し、AIで整形したメモを生成する。

## 技術スタック
- **言語**: Kotlin 2.0.21
- **フレームワーク**: Spring Boot 3.4.12
- **ビルドツール**: Gradle 8.5 (Kotlin DSL)
- **JDK**: 21 (LTS)
- **データベース**: PostgreSQL 16
- **DB マイグレーション**: Flyway
- **テスト**: JUnit 5, MockK, Testcontainers
- **API ドキュメント**: SpringDoc OpenAPI (Swagger)
- **レート制限**: Bucket4j

## 主要機能
- Google OAuth 認証
- JWT トークン認証
- Google Cloud Speech-to-Text による音声文字起こし
- Google Cloud Storage による音声ファイル一時保存
- Gemini AI によるメモ整形
- メモの CRUD 操作
- レート制限 (Bucket4j)

## アーキテクチャ
クリーンアーキテクチャに基づいた階層構造:
- `domain/` - ドメインモデル、リポジトリインターフェース、ゲートウェイインターフェース
  - `model/` - VoiceMemo, User, RefreshToken 等のドメインモデル
  - `repository/` - データ永続化インターフェース
  - `gateway/` - 外部サービス連携インターフェース（OAuthClient, TokenProvider, SpeechTranscriber, MemoFormatter）
  - `exception/` - ドメイン例外（DomainException, ErrorCode）
- `usecase/` - ユースケース（ビジネスロジック）
  - `auth/` - 認証関連（Login, Logout, RefreshToken, GetCurrentUser, DeleteAccount）
  - `memo/` - メモ関連（CreateMemo, ListMemos）
  - `support/` - ユーティリティ（ExecutionTimer）
- `infrastructure/` - 外部サービス実装
  - `api/` - 外部API クライアント
    - `speech/` - Google Speech-to-Text (GoogleSpeechTranscriber)
    - `storage/` - Google Cloud Storage (GcsStorageService)
    - `ai/` - Gemini AI (GeminiAiMemoFormatter)
    - GoogleOAuthClient
  - `postgres_jdbc/` - PostgreSQL リポジトリ実装
    - `user/`, `memo/`, `token/`
  - `security/` - JWT 認証（JwtTokenProvider, JwtAuthenticationFilter）
  - `ratelimit/` - レート制限（RateLimitFilter, RateLimitService）
- `presentation/` - REST コントローラー
  - `controller/auth/` - 認証エンドポイント
  - `controller/voice/` - 音声メモエンドポイント
  - `controller/memo/` - メモエンドポイント
  - GlobalExceptionHandler
- `config/` - Spring 設定

## ロケール
- 言語: 日本語 (ja_JP)
- タイムゾーン: Asia/Tokyo
- 文字エンコーディング: UTF-8
