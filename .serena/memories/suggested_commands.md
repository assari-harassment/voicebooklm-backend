# 開発コマンド

## ビルド & 実行
```bash
# アプリケーション起動
./gradlew bootRun

# ビルド（テストなし）
./gradlew build -x test

# クリーンビルド
./gradlew clean build
```

## テスト
```bash
# ユニットテスト実行
./gradlew test

# 統合テスト実行（Docker 必須）
./gradlew integrationTest

# 全テスト実行
./gradlew check

# 特定のテストクラス実行
./gradlew test --tests ClassName
```

## Docker
```bash
# PostgreSQL 起動
docker compose up -d

# 停止
docker compose down
```

## その他
```bash
# 依存関係の更新確認
./gradlew dependencies

# テストレポート確認
open build/reports/tests/test/index.html

# Swagger UI
open http://localhost:8080/swagger-ui.html
```
