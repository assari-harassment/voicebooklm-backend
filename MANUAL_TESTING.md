# 手動APIテストガイド

このドキュメントでは、`bootRun`で起動したアプリケーションに対して手動でAPIをテストする方法を説明します。

## 前提条件

- アプリケーションが `./gradlew bootRun` で起動している
- Docker ComposeでPostgreSQLが起動している
- サーバーポート: `8080`（デフォルト）

## 方法1: Swagger UIを使う（推奨・最も簡単）

### 手順

1. **ブラウザでSwagger UIにアクセス**
   ```
   http://localhost:8080/swagger-ui.html
   ```

2. **認証を行う**
   - `/api/auth/google` エンドポイントを使用してGoogle OAuthでログイン
   - または、既存のアクセストークンを持っている場合は次のステップへ

3. **JWTトークンを設定**
   - ページ右上の「**Authorize**」ボタンをクリック
   - 「Value」欄に `Bearer {アクセストークン}` の形式で入力
   - 例: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

4. **APIをテスト**
   - `/api/tags` エンドポイントを展開
   - 「Try it out」ボタンをクリック
   - 「Execute」ボタンをクリック
   - レスポンスを確認

### メリット

- GUIで簡単にテストできる
- リクエスト/レスポンスのフォーマットが見やすい
- 認証の設定が簡単

---

## 方法2: curlコマンドを使う

### ステップ1: JWTトークンを取得

**注意**: 実際のGoogle OAuth認証が必要です。テスト環境では以下の方法を検討してください：

#### オプションA: Google OAuthを使う（実際の認証）

```bash
# 1. モバイルアプリやフロントエンドからGoogle OAuthでログイン
# 2. 取得したIDトークンを使ってログインAPIを呼び出す
curl -X POST http://localhost:8080/api/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'

# レスポンスから accessToken を取得
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
# }
```

#### オプションB: データベースにテストユーザーを作成してJWTトークンを生成（開発用）

1. **PostgreSQLに接続**
   ```bash
   docker-compose exec postgres psql -U postgres -d voicebooklm
   ```

2. **テストユーザーを作成（既存の場合はスキップ）**
   ```sql
   INSERT INTO users (id, google_sub, email, name, created_at, updated_at)
   VALUES (
     'aaaaaaaa-0000-0000-0000-000000000001',
     'test-sub-voice-1',
     'test_voice_1@example.com',
     'Test User',
     NOW(),
     NOW()
   )
   ON CONFLICT (id) DO NOTHING;
   ```

3. **JWTトークンを生成**
   - アプリケーションコードでJWTトークンを生成する必要があります
   - または、統合テストのコードを参考に一時的なエンドポイントを作成する

### ステップ2: タグ一覧APIを呼び出す

```bash
# JWTトークンを変数に保存
ACCESS_TOKEN="your-access-token-here"

# タグ一覧を取得
curl -X GET http://localhost:8080/api/tags \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json"
```

### 期待されるレスポンス

```json
{
  "tags": [
    {
      "name": "仕事",
      "count": 5
    },
    {
      "name": "学習",
      "count": 3
    },
    {
      "name": "重要",
      "count": 2
    }
  ]
}
```

### エラーケースのテスト

#### 認証なし（401 Unauthorized）

```bash
curl -X GET http://localhost:8080/api/tags \
  -H "Content-Type: application/json" \
  -v
```

期待されるレスポンス: `401 Unauthorized`

#### 無効なトークン（401 Unauthorized）

```bash
curl -X GET http://localhost:8080/api/tags \
  -H "Authorization: Bearer invalid-token" \
  -H "Content-Type: application/json" \
  -v
```

期待されるレスポンス: `401 Unauthorized`

---

## 方法3: HTTPieを使う（curlの代替）

```bash
# インストール（未インストールの場合）
# Arch Linux: sudo pacman -S httpie

# タグ一覧を取得
http GET http://localhost:8080/api/tags \
  Authorization:"Bearer $ACCESS_TOKEN"
```

---

## テストデータの準備

### タグ付きメモを作成する

テスト用のシードデータ（`V003__seed_memos_for_tests.sql`）を使用する場合：

```bash
# マイグレーションを実行（既に実行済みの場合はスキップ）
./gradlew flywayMigrate
```

このシードデータには、既にタグが付いたメモが含まれています。

### 手動でメモを作成する

Swagger UIの `/api/voice` エンドポイントを使用して、音声メモを作成することで、タグが自動的に生成されます。

---

## トラブルシューティング

### 401 Unauthorized が返される

- JWTトークンが正しく設定されているか確認
- トークンの有効期限が切れていないか確認
- `Authorization` ヘッダーの形式が `Bearer {token}` になっているか確認

### 500 Internal Server Error が返される

- アプリケーションのログを確認
- データベース接続が正常か確認
- Docker Composeが起動しているか確認

### 空のタグリストが返される

- データベースにメモが存在するか確認
- メモにタグが付いているか確認
- ユーザーIDが正しいか確認（他のユーザーのメモは取得できない）

---

## 参考リンク

- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs (OpenAPI JSON): http://localhost:8080/v3/api-docs
- ヘルスチェック: http://localhost:8080/actuator/health

