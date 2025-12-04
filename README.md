# voicebooklm-backend

AI ボイスメモアプリケーションのバックエンド（Kotlin Spring Boot）

## 技術スタック

- Kotlin 2.0.21
- Spring Boot 3.4.12
- Gradle 8.5 (Kotlin DSL)
- JDK 21 (LTS)
- PostgreSQL 16（開発・本番環境）
- Docker Compose（開発環境用）
- H2 Database（テストのみ）

### 主要な依存関係

**Spring Boot**
- Web（REST API）
- WebFlux（AI API との非同期通信）
- Data JPA（データベース）
- Security（認証・認可）
- Actuator（モニタリング）
- DevTools（開発環境でのホットリロード）

**認証**
- JWT (io.jsonwebtoken:jjwt)

**Kotlin**
- Coroutines（非同期処理）
- Jackson（JSON シリアライゼーション）

**テスト**
- Spring Boot Test
- Spring Security Test
- MockK
- Coroutines Test

## 必須環境

- **JDK 21** (必須)
- **Docker & Docker Compose** (開発環境用、推奨)
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

# テスト（H2を使用）
./gradlew test
```

### 5. アプリケーションへのアクセス

#### 開発環境
- アプリケーション: http://localhost:8080
- Actuator: http://localhost:8080/actuator
- Health Check: http://localhost:8080/actuator/health
- Basic認証: `dev` / `dev`

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

## 開発ガイドライン

チーム開発プロジェクトです。コーディング規約とコントリビューションガイドラインに従ってください。
