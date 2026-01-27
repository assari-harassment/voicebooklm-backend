# 開発用 API

開発環境で使用するテストデータの追加・利用方法です。

## アクセストークンを取得する

2回目以降のOAuth 不要で、メールアドレスからアクセストークンを取得できます。

### 手順

1. アプリを起動（`./gradlew bootRun`）
2. Swagger UI を開く: http://localhost:8080/swagger-ui.html
3. 「Dev」セクションの `POST /api/dev/token` を実行
   - `email` に自分のメールアドレスを入力
4. レスポンスの `accessToken` をコピー
5. 「Authorize」ボタンをクリック → `Bearer {accessToken}` を入力

これで他の API をテストできます。

---

## テストデータを自分のアカウントに作成する

### 手順

1. 上記の方法でアクセストークンを取得・設定
2. 「Dev」セクションの `POST /api/dev/seed` を実行

### 注意事項

- **dev 環境でのみ有効**（本番環境では無効）
- **冪等性あり**: 既にフォルダーが存在する場合はスキップされる
- 再作成したい場合は、DBから既存のデータを削除もしくはdocker compose down -vして![img.png](img.png)から再実行
