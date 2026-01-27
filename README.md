# voicebooklm-backend

AI ボイスメモアプリケーションのバックエンド（Kotlin Spring Boot）

## Getting Started

開発環境のセットアップ手順については、[Getting Started ガイド](GETTING_STARTED.md)を参照してください。

**クイックスタート:**
```bash
# リポジトリをクローン（--recursive でドキュメントも取得）
git clone --recursive https://github.com/assari-harassment/voicebooklm-backend.git

# 既存のcloneでドキュメントが空の場合
git submodule update --init

# JDK 21 のインストール確認
java -version

# PostgreSQL の起動
docker compose up -d

# ビルド & 実行
./gradlew bootRun
```

### ドキュメントの更新

`docs/` は voicebooklm-docs リポジトリの submodule です。最新のドキュメントを取得するには：

```bash
git submodule update --remote
```
詳しくは`git submodule`で検索

詳細な手順やトラブルシューティングは [GETTING_STARTED.md](GETTING_STARTED.md) をご覧ください。

## 技術スタック

- Kotlin 2.0.21
- Spring Boot 3.4.12
- Gradle 8.5 (Kotlin DSL)
- JDK 21 (LTS)
- PostgreSQL 16（開発・本番・テスト環境）
- Docker Compose（開発環境用）
- Testcontainers（テスト用 PostgreSQL）

## チーム開発について

### 言語設定

- アプリケーションのロケール: 日本語 (ja_JP)
- タイムゾーン: Asia/Tokyo
- 文字エンコーディング: UTF-8

## テストについて

このプロジェクトでは **Testcontainers** を使用して、テストも PostgreSQL で実行します。

### テストの実行

```bash
# 全テスト実行
./gradlew test

# 特定のテストクラスを実行
./gradlew test --tests VoiceBookLmBackendApplicationTests

# テストレポートの確認
open build/reports/tests/test/index.html
```

## React Native 連携

### Swagger UI で API を確認

1. アプリケーションを起動
2. http://localhost:8080/swagger-ui.html にアクセス
3. API エンドポイントを確認・テスト

### TypeScript 型定義の生成

OpenAPI 仕様から TypeScript の型定義を自動生成できます：

### CORS 設定

React Native からのアクセスは自動的に許可されます：

- **開発環境**: `localhost:*`, `192.168.*.*:*`（実機テスト用）
- **本番環境**: `https://*.example.com`（実際のドメインに変更）

### ドキュメント参照

`docs/` に仕様書・設計書があります。git submodule管理しています。

### 各自のユーザーidのテストデータの追加

[src/main/resources/dev/README.md](src/main/resources/dev/README.md)

### リアルタイム文字起こしユーザーテスト用htmlファイル

```bash
cd tools                                                                                               
python3 -m http.server 3000
```
