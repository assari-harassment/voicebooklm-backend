# Getting Started

このガイドでは、voicebooklm-backend の開発環境のセットアップ手順を説明します。

## 必須環境

- **JDK 21**
- **Docker & Docker Compose**
- Gradle は Wrapper を使用するため、別途インストール不要

## セットアップ手順

### 1. JDK 21 のインストール

**mise を使用する場合（推奨）:**
```bash
curl https://mise.run | sh
mise install java@temurin-21
```

**その他の方法:**
- SDKMAN: `sdk install java 21.0.1-tem`
- Homebrew (macOS): `brew install openjdk@21`
- 手動: [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21)

**確認:**
```bash
java -version  # java version "21.x.x" と表示されることを確認
```

### 2. PostgreSQL の起動

```bash
docker compose up -d
```

> **Note**: このプロジェクトには Gradle Toolchains が設定されているため、JDK 21 がインストールされていない場合でも、初回ビルド時に自動的にダウンロードされます。

### 3. ビルドと実行

```bash
./gradlew build      # ビルド
./gradlew bootRun    # 実行
./gradlew test       # テスト
```

### 4. アプリケーションへのアクセス

アプリケーションが起動したら、以下にアクセスできます：

- **アプリケーション**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Basic認証**: `dev` / `dev`

## IDE 設定

### IntelliJ IDEA

1. `File` → `Open` → プロジェクトディレクトリを選択
2. `File` → `Project Structure` → `Project` で SDK が `21` になっていることを確認
3. `File` → `Settings` → `Build Tools` → `Gradle` で `Gradle JVM` が `Project SDK (21)` になっていることを確認

**推奨プラグイン:** Kotlin, Spring Boot

## トラブルシューティング

### Testcontainers (Colima)

Colima で Docker を実行している場合、シェルの設定ファイルに以下を追加してください。

**zsh を使用する場合（macOS デフォルト）:**

`~/.zshrc` に以下を追記：
```bash
export DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

設定を反映：
```bash
source ~/.zshrc
```

**bash を使用する場合:**

`~/.bashrc` に以下を追記：
```bash
export DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

設定を反映：
```bash
source ~/.bashrc
```

**mise を使用する場合:**
```bash
mise set DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock
mise set TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

### その他

- Docker Desktop 使用時は通常設定不要
- テストは Testcontainers で PostgreSQL コンテナを自動起動（初回は時間がかかります）
- 詳細は [README.md](README.md) を参照
