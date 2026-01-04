# UUID生成ルール

## 基本方針
- **UUIDv7**を使用する（タイムスタンプベースでソート可能、DBインデックスに優しい）
- ライブラリ: `com.github.f4b6a3:uuid-creator`

## 生成方法
```kotlin
import com.github.f4b6a3.uuid.UuidCreator

val id = UuidCreator.getTimeOrderedEpoch() // UUIDv7
```

## 責務の分離
- **ドメイン層**: ID生成方法に依存しない。IDは外部から渡される必須引数とする
- **ユースケース層**: `UuidCreator.getTimeOrderedEpoch()`でUUIDv7を生成し、ドメインモデルに渡す
- **インフラ層**: データ永続化に専念。ID生成は行わない

## 例外: インフラ層エンティティ
- `MemoTag` のようにドメインモデルではなく、インフラ層のみで使うエンティティのIDは、インフラ層で生成してよい
- 理由: ユースケース層がインフラ層の詳細に依存するのを避けるため

## 注意事項
- `UUID.randomUUID()`（UUIDv4）は使用しない
