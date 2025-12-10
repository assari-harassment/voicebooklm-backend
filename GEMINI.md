# Repository Guidelines

## プロジェクト構成
- Kotlin/Spring Boot 本体: `src/main/kotlin/com/assari/voicebooklm`。設定類は `config/` 配下（Security/CORS/OpenAPI/WebClient）。
- リソース: `src/main/resources`。Flyway は `db/migration`（本番向けは `db/migration-prod`）。
- テスト: `src/test/kotlin`（共通基底は `AbstractIntegrationTest`）、設定は `src/test/resources`。レポートは `build/reports/tests/test/index.html`。
- 環境関連: `.env.example` をコピーして `.env` を作成。PostgreSQL 用コンテナは `docker-compose.yml`。

## ビルド・実行・テスト
- 依存サービス起動: `docker compose up -d`（PostgreSQL が立ち上がるまで待つ）。
- ビルド: `./gradlew build`（ツールチェーンは自動取得）。
- ローカル起動: `./gradlew bootRun`（API: http://localhost:8080 / Swagger: `/swagger-ui.html`）。
- テスト: `./gradlew test`。特定クラス: `./gradlew test --tests com.assari.voicebooklm.VoiceBookLmBackendApplicationTests`。

## コーディング規約・命名
- Kotlin 公式スタイル（4 スペース、シンプルな関数は式ボディ推奨）。`kotlin.code.style=official` を遵守。
- パッケージは小文字、クラス/オブジェクトは PascalCase、関数/プロパティは lowerCamelCase、テストクラスは `*Tests`。
- Spring はコンストラクタインジェクションを優先。設定値やシークレットは `spring-dotenv` 経由で `.env` から読む。
- コルーチン/`suspend` を活用し、DTO は不変データクラスで定義。

## テスト指針
- JUnit 5 + Spring Boot Test + MockK。Testcontainers が PostgreSQL を自動起動するため Docker 必須。
- DB を触る統合テストは `AbstractIntegrationTest` を継承してコンテナ設定を共有。
- 正常系だけでなくバリデーション・認可エラーもカバー。テスト名はバッククォートで意図を明確に書く。

## コミットと PR
- コミットメッセージは短く動詞で開始し、関連 Issue があれば `Closes #<番号>` を含める（例: `データベーススキーマ変更 #12`）。
- PR では `.github/pull_request_template.md` を使用し、「やったこと」「動作確認」を具体的に記載し `ビルドできた` にチェック。必要に応じてログやスクリーンショットを添付。
- PR 前に最低 `./gradlew test`（依存/マイグレーション変更時は `./gradlew build`）を実行し、失敗を解消してから提出。

## セキュリティ・設定
- `.env` はコミット禁止。`GOOGLE_APPLICATION_CREDENTIALS` は安全なパスを指すように設定。共通値は `.env.example` を更新して共有。
- Colima 利用時は `DOCKER_HOST` と `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` を `GETTING_STARTED.md` の手順で設定し、テストの安定性を確保。
- Flyway マイグレーションはマージ後は書き換えない。IDE や `.gradle/` などの生成物はコミット対象外。

## ルール
・答えは簡潔に
・プログラムにはコメントを書くこと
・https://assari-harassment.github.io/voicebooklm-docs/product.html <- プロジェクトの内容はここのものを参考にしてください。
・使用するアーキテクチャはオニオンアーキテクチャです


