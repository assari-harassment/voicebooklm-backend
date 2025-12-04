# voicebooklm-backend

AI ボイスメモアプリケーションのバックエンド（Kotlin Spring Boot）

## 技術スタック

- Kotlin 2.0.21
- Spring Boot 3.4.12
- Gradle 8.5 (Kotlin DSL)
- JDK 21 (LTS)
- PostgreSQL 16（開発・本番・テスト環境）
- Docker Compose（開発環境用）
- Testcontainers（テスト用 PostgreSQL）

### 主要な依存関係

**Spring Boot**
- Web（REST API）
- WebFlux（AI API との非同期通信）
- Data JPA（データベース）
- Security（認証・認可）
- Actuator（モニタリング）
- DevTools（開発環境でのホットリロード）

**React Native 連携**
- Swagger/OpenAPI（API ドキュメント自動生成）
- CORS 設定（iOS/Android からのアクセス許可）
- TypeScript 型定義の生成が可能

**認証**
- JWT (io.jsonwebtoken:jjwt)

**ファイルストレージ**
- AWS S3（音声ファイル保存）
- S3 Transfer Manager（大容量ファイル対応）

**Kotlin**
- Coroutines（非同期処理）
- Jackson（JSON シリアライゼーション）

**テスト**
- Spring Boot Test
- Spring Security Test
- MockK
- Coroutines Test
- Testcontainers（PostgreSQL を使った統合テスト）

## 必須環境

- **JDK 21** (必須)
- **Docker & Docker Compose** (開発環境・テスト用、必須)
  - Testcontainers でテスト時に PostgreSQL コンテナを自動起動
- Gradle は Wrapper を使用するため、別途インストール不要

## セットアップ手順

### 1. JDK 21 のインストール

#### mise を使用する場合（推奨）

```bash
# mise のインストール（未インストールの場合）
curl https://mise.run | sh

# JDK 21 のインストール
mise install java@temurin-21

# プロジェクトディレクトリに入ると自動的にJDK 21に切り替わります
cd voicebooklm-backend
# .tool-versions ファイルがあるため、自動的にJDK 21が有効化されます
```

#### SDKMAN を使用する場合

```bash
# SDKMAN のインストール（未インストールの場合）
curl -s "https://get.sdkman.io" | bash

# JDK 21 のインストール
sdk install java 21.0.1-tem

# プロジェクトディレクトリで自動的にJDK 21を使用
cd voicebooklm-backend
sdk env install
```

#### Homebrew を使用する場合（macOS）

```bash
brew install openjdk@21
```

#### 手動インストール

[Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) からダウンロード

### 2. JDK バージョンの確認

```bash
java -version
# java version "21.x.x" と表示されることを確認
```

### 3. PostgreSQL のセットアップ

#### Docker Compose を使用する場合（推奨）

```bash
# PostgreSQL を起動
docker compose up -d

# 古いバージョンのDocker Composeを使用している場合
# docker-compose up -d

# ログを確認
docker compose logs -f postgres

# 停止
docker compose down

# データも削除する場合
docker compose down -v
```

> **Note**: Docker Compose V2 を使用している場合は `docker compose`、V1 を使用している場合は `docker-compose` を使用してください。

PostgreSQL が起動すると以下で接続できます：
- Host: `localhost`
- Port: `5432`
- Database: `voicebooklm`
- Username: `postgres`
- Password: `postgres`

#### ローカルに PostgreSQL をインストールする場合

```bash
# PostgreSQL のインストール（macOS）
brew install postgresql@16
brew services start postgresql@16

# データベースの作成
createdb voicebooklm
```

### 4. ビルドと実行

```bash
# PostgreSQL を起動（Docker Compose）
docker compose up -d

# ビルド
./gradlew build

# 実行（開発環境）
./gradlew bootRun

# 実行（本番環境）
./gradlew bootRun --args='--spring.profiles.active=prod'

# テスト（Testcontainers で PostgreSQL を使用）
./gradlew test
```

> **Note**: テストは Testcontainers を使用して PostgreSQL コンテナを自動起動します。初回実行時は Docker イメージのダウンロードに時間がかかります。

### 5. アプリケーションへのアクセス

#### 開発環境
- **アプリケーション**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
  - API ドキュメントをブラウザで確認
  - 直接 API をテスト可能
- **OpenAPI 仕様**: http://localhost:8080/v3/api-docs
  - JSON 形式の API 仕様
  - TypeScript 型定義の生成に使用
- **Actuator**: http://localhost:8080/actuator
- **Health Check**: http://localhost:8080/actuator/health
- **Basic認証**: `dev` / `dev`

#### PostgreSQL（Docker）
- Host: `localhost:5432`
- Database: `voicebooklm`
- Username: `postgres`
- Password: `postgres`

#### 本番環境
- アプリケーション: http://localhost:8080
- Actuator Health: http://localhost:8080/actuator/health
- Basic認証: 環境変数 `ADMIN_PASSWORD` で設定

### 6. Docker Compose コマンド

```bash
# 起動
docker compose up -d

# ログ確認
docker compose logs -f

# 停止
docker compose stop

# 停止してコンテナ削除
docker compose down

# 停止してコンテナ・ボリューム削除（データも削除）
docker compose down -v

# PostgreSQL に接続
docker compose exec postgres psql -U postgres -d voicebooklm
```

## チーム開発について

### Gradle Toolchains による自動JDK管理

このプロジェクトでは **Gradle Toolchains** を使用しています。

- Gradle が自動的に JDK 21 をダウンロード・使用
- チームメンバー全員が同じJDKバージョンで開発可能
- 手動でJDKを切り替える必要なし

初回ビルド時に自動的に JDK 21 がダウンロードされます：

```bash
./gradlew build
```

### 言語設定

- アプリケーションのロケール: 日本語 (ja_JP)
- タイムゾーン: Asia/Tokyo
- 文字エンコーディング: UTF-8

## IDE 設定

### IntelliJ IDEA

プロジェクトには IntelliJ IDEA 用の設定ファイルが含まれています：

1. **プロジェクトを開く**: `File` → `Open` → `voicebooklm-backend` ディレクトリを選択
2. **JDK 設定の確認**:
   - `File` → `Project Structure` → `Project`
   - Project SDK が `21` になっていることを確認
   - なっていない場合は、`Add SDK` → `Download JDK` → `Version: 21` → `Vendor: Eclipse Temurin` を選択
3. **Gradle 設定**:
   - `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
   - `Gradle JVM` が `Project SDK (21)` になっていることを確認

### 推奨プラグイン
- Kotlin
- Spring Boot
- .ignore

## テストについて

このプロジェクトでは **Testcontainers** を使用して、テストも PostgreSQL で実行します。

### Testcontainers の利点

- **環境の一貫性**: 開発・テスト・本番すべて PostgreSQL
- **本番と同じ挙動**: SQL、型、制約が完全に一致
- **早期バグ発見**: 環境差異によるバグを防止

### テストの実行

```bash
# 全テスト実行
./gradlew test

# 特定のテストクラスを実行
./gradlew test --tests VoiceBookLmBackendApplicationTests

# テストレポートの確認
open build/reports/tests/test/index.html
```

### テストの書き方

`AbstractIntegrationTest` を継承することで、Testcontainers が自動的に設定されます：

```kotlin
class UserRepositoryTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun testSaveUser() {
        // PostgreSQL コンテナが自動的に起動してテストが実行される
    }
}
```

### トラブルシューティング

#### Colima を使用している場合

Colima で Docker を実行している場合、`~/.testcontainers.properties` に以下を追加：

```properties
docker.client.strategy=org.testcontainers.dockerclient.UnixSocketClientProviderStrategy
docker.host=unix:///Users/YOUR_USERNAME/.colima/default/docker.sock
testcontainers.reuse.enable=true
```

#### Docker Desktop を使用している場合

通常は設定不要ですが、問題がある場合は Docker Desktop が起動していることを確認してください。

#### CI/CD環境

GitHub Actions などの CI 環境では Docker が利用可能であることを確認してください。

## React Native 連携

### Swagger UI で API を確認

1. アプリケーションを起動
2. http://localhost:8080/swagger-ui.html にアクセス
3. API エンドポイントを確認・テスト

### TypeScript 型定義の生成

OpenAPI 仕様から TypeScript の型定義を自動生成できます：

```bash
# openapi-typescript を使用
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.ts

# または openapi-generator を使用
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-axios \
  -o src/api
```

### CORS 設定

React Native からのアクセスは自動的に許可されます：

- **開発環境**: `localhost:*`, `192.168.*.*:*`（実機テスト用）
- **本番環境**: `https://*.example.com`（実際のドメインに変更）

## AWS S3 設定

音声ファイルは AWS S3 に保存されます。

### 環境変数の設定

```bash
# ローカル開発環境
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=ap-northeast-1
export AWS_S3_BUCKET_NAME=voicebooklm-audio-files
```

### application.yml での設定（非推奨）

```yaml
aws:
  access-key-id: your_access_key
  secret-access-key: your_secret_key
  region: ap-northeast-1
  s3:
    bucket-name: voicebooklm-audio-files
```

> **Note**: 本番環境では環境変数または IAM Role を使用してください。

### S3 バケットの作成

```bash
# AWS CLI でバケット作成
aws s3 mb s3://voicebooklm-audio-files --region ap-northeast-1

# CORS 設定
aws s3api put-bucket-cors --bucket voicebooklm-audio-files --cors-configuration file://s3-cors.json
```

## 開発ガイドライン

チーム開発プロジェクトです。コーディング規約とコントリビューションガイドラインに従ってください。
