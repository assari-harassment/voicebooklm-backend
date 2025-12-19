# 非同期処理パターン分析

現在のプロジェクトで採用している非同期処理パターンと、代替実装の選択肢について解説する。

## 現在の実装パターン

### 採用技術: Kotlin Coroutines + Spring WebFlux

プロジェクトでは **Kotlin Coroutines** を主軸として非同期処理を実装している。

#### 特徴

```kotlin
// ユースケース層: suspend 関数
interface CreateMemoUseCase {
    suspend fun execute(command: CreateMemoCommand): CreateMemoResult
}

// コントローラー層: suspend 関数
@PostMapping("/memos")
suspend fun createMemo(): ResponseEntity<CreateMemoResponse>
```

#### 実装箇所

| レイヤー | ファイル | 非同期パターン |
|---------|----------|---------------|
| Presentation | `VoiceController.kt` | `suspend fun` |
| UseCase | `CreateMemoUseCase.kt` | `suspend fun` |
| UseCase | `CreateMemoInteractor.kt` | `suspend fun` + `runCatching` |
| Infrastructure | `GoogleSpeechTranscriber.kt` | `withTimeout` + `withContext(Dispatchers.IO)` |
| Infrastructure | `GeminiAiMemoFormatter.kt` | `suspend fun` + WebClient (Reactor) + `awaitSingleOrNull` |
| Repository | `MemoRepository.kt` | `suspend fun` |

### 処理フロー

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          VoiceController                                     │
│                         (suspend fun)                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      CreateMemoInteractor                                    │
│                        (suspend fun)                                         │
│                                                                             │
│   ┌────────────────────┐    ┌────────────────────┐    ┌──────────────────┐ │
│   │ speechTranscriber  │ →  │ aiMemoFormatter    │ →  │ memoRepository   │ │
│   │   .transcribe()    │    │     .format()      │    │     .save()      │ │
│   │    (suspend)       │    │    (suspend)       │    │   (suspend)      │ │
│   └────────────────────┘    └────────────────────┘    └──────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 採用されている非同期プリミティブ

#### 1. `suspend fun` (全レイヤー)
```kotlin
suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscription
```

#### 2. `withTimeout` (タイムアウト制御)
```kotlin
withTimeout(timeout) {
    // 60秒以内に完了しなければ TimeoutCancellationException
}
```

#### 3. `withContext(Dispatchers.IO)` (ブロッキング I/O)
```kotlin
withContext(Dispatchers.IO) {
    // Google Speech API 呼び出し（ブロッキング）
}
```

#### 4. WebClient + Reactor Bridge (外部 API 呼び出し)
```kotlin
client.post()
    .body(BodyInserters.fromValue(request))
    .retrieve()
    .bodyToMono(GeminiResponse::class.java)
    .timeout(timeout)
    .awaitSingleOrNull()  // Reactor → Coroutines 変換
```

---

## 代替実装パターン

### 選択肢 1: 完全 Reactor (Project Reactor)

Spring WebFlux のネイティブ非同期モデル。

```kotlin
// Controller
@PostMapping("/memos")
fun createMemo(): Mono<ResponseEntity<CreateMemoResponse>>

// UseCase
interface CreateMemoUseCase {
    fun execute(command: CreateMemoCommand): Mono<CreateMemoResult>
}

// 実装例
fun execute(command: CreateMemoCommand): Mono<CreateMemoResult> {
    return speechTranscriber.transcribe(command.audio)
        .flatMap { transcript -> aiMemoFormatter.format(transcript) }
        .flatMap { draft -> memoRepository.save(draft) }
        .timeout(Duration.ofSeconds(60))
        .onErrorResume { fallbackMono() }
}
```

**メリット:**
- Spring WebFlux とのシームレスな統合
- バックプレッシャー対応
- オペレーター豊富（retry, cache, defer など）

**デメリット:**
- Kotlin との相性がやや悪い（Mono/Flux のラップが必要）
- デバッグが困難（スタックトレースが断片化）
- 学習コストが高い

### 選択肢 2: 完全 Kotlin Coroutines (現在の実装を拡張)

現在の実装を維持しつつ、より Coroutines ネイティブに。

```kotlin
// 並列実行の例
suspend fun execute(command: CreateMemoCommand): CreateMemoResult = coroutineScope {
    val transcriptionDeferred = async { speechTranscriber.transcribe(command) }
    // 他の並列処理があれば追加

    val transcription = transcriptionDeferred.await()
    val draft = aiMemoFormatter.format(transcription)
    memoRepository.save(draft)
}
```

**メリット:**
- Kotlin との自然な統合
- 可読性が高い（同期コードのように書ける）
- 構造化並行性（Structured Concurrency）
- テストが容易

**デメリット:**
- Spring MVC との統合にはコルーチンブリッジが必要
- Reactor ほどオペレーターが豊富でない

### 選択肢 3: CompletableFuture (Java 標準)

Java 標準の非同期 API。

```kotlin
// UseCase
interface CreateMemoUseCase {
    fun execute(command: CreateMemoCommand): CompletableFuture<CreateMemoResult>
}

// 実装例
fun execute(command: CreateMemoCommand): CompletableFuture<CreateMemoResult> {
    return speechTranscriber.transcribe(command.audio)
        .thenCompose { transcript -> aiMemoFormatter.format(transcript) }
        .thenCompose { draft -> memoRepository.save(draft) }
        .orTimeout(60, TimeUnit.SECONDS)
        .exceptionally { fallback() }
}
```

**メリット:**
- Java 標準ライブラリのみで実装可能
- Spring MVC との親和性が高い
- Java 開発者にとって馴染みやすい

**デメリット:**
- Kotlin との統合が不自然
- コールバック地獄になりやすい
- キャンセレーション制御が弱い

### 選択肢 4: Virtual Threads (Java 21+)

JDK 21 の仮想スレッド（Project Loom）。

```kotlin
// 通常の同期コードとして記述
@PostMapping("/memos")
fun createMemo(): ResponseEntity<CreateMemoResponse> {
    // 仮想スレッド上で実行される
    val transcription = speechTranscriber.transcribe(command)
    val draft = aiMemoFormatter.format(transcription)
    val memo = memoRepository.save(draft)
    return ResponseEntity.ok(memo)
}
```

**メリット:**
- 同期コードのように書ける（最も可読性が高い）
- 既存の同期ライブラリがそのまま使える
- コンテキストスイッチのオーバーヘッドが極小

**デメリット:**
- Spring Boot 3.2+ と Tomcat/Jetty 最新版が必要
- Kotlin Coroutines との統合がまだ発展途上
- 一部のライブラリ（synchronized ブロック内のブロッキング）で問題

### 選択肢 5: メッセージキュー (非同期ジョブ)

長時間処理を完全に非同期化。

```kotlin
// Controller: ジョブ投入のみ
@PostMapping("/memos")
fun createMemo(): ResponseEntity<JobResponse> {
    val jobId = messageQueue.submit(CreateMemoJob(command))
    return ResponseEntity.accepted().body(JobResponse(jobId))
}

// 別プロセスで処理
@Component
class CreateMemoJobProcessor {
    @JmsListener(destination = "memo-creation")
    fun process(job: CreateMemoJob) {
        // 文字起こし → AI整形 → 保存
    }
}
```

**メリット:**
- 長時間処理に最適
- スケーラビリティが高い
- 障害に強い（リトライ機構）

**デメリット:**
- アーキテクチャが複雑になる
- リアルタイム性が低い
- 追加のインフラ（RabbitMQ, SQS など）が必要

---

## 比較表

| パターン | 可読性 | Spring 統合 | Kotlin 親和性 | 学習コスト | スケーラビリティ |
|---------|-------|------------|--------------|-----------|----------------|
| **Kotlin Coroutines (現在)** | ★★★★☆ | ★★★★☆ | ★★★★★ | ★★★☆☆ | ★★★★☆ |
| Reactor | ★★☆☆☆ | ★★★★★ | ★★☆☆☆ | ★★☆☆☆ | ★★★★★ |
| CompletableFuture | ★★★☆☆ | ★★★★☆ | ★★☆☆☆ | ★★★★☆ | ★★★☆☆ |
| Virtual Threads | ★★★★★ | ★★★☆☆ | ★★★☆☆ | ★★★★★ | ★★★★☆ |
| メッセージキュー | ★★★☆☆ | ★★★☆☆ | ★★★★☆ | ★★☆☆☆ | ★★★★★ |

---

## 推奨

### 現状維持を推奨: Kotlin Coroutines

**理由:**

1. **Kotlin との自然な統合**: `suspend` キーワードにより、非同期コードを同期的に記述できる
2. **構造化並行性**: `coroutineScope` による安全なスコープ管理
3. **テスト容易性**: `runTest` で簡単にテスト可能
4. **Spring WebFlux 対応**: Spring Boot 3.x で完全サポート
5. **適切なタイムアウト制御**: `withTimeout` による明示的な制御

### 改善提案

現在の実装は十分に良いが、以下の改善を検討できる:

#### 1. 並列処理の導入（将来的に）

文字起こしと AI 整形を並列化することは現在の要件では不可能だが、複数音声の同時処理などで活用可能:

```kotlin
coroutineScope {
    val results = files.map { file ->
        async { processFile(file) }
    }.awaitAll()
}
```

#### 2. Flow の活用（ストリーミング対応時）

長い音声のリアルタイム文字起こしを実装する場合:

```kotlin
fun transcribeStream(audio: Flow<ByteArray>): Flow<String> = flow {
    audio.collect { chunk ->
        val text = speechClient.streamingRecognize(chunk)
        emit(text)
    }
}
```

#### 3. Virtual Threads への移行（将来検討）

JDK 21+ と Spring Boot 3.2+ の組み合わせが安定したら、Virtual Threads への移行も選択肢:

```properties
# application.properties
spring.threads.virtual.enabled=true
```

### 移行しない方が良いケース

- **Reactor への移行**: Kotlin プロジェクトでは Coroutines の方が自然
- **CompletableFuture への移行**: Kotlin では明らかに劣る選択
- **メッセージキュー**: 現在の 60 秒以内の処理では過剰設計

---

## まとめ

現在の **Kotlin Coroutines** ベースの実装は、このプロジェクトにとって最適な選択である。

- リアルタイム性が求められる音声処理に適している
- Kotlin との親和性が高く、コードの可読性が維持される
- Spring WebFlux との統合も問題なく動作している
- 将来的な拡張（並列処理、ストリーミング）にも対応可能

大きなアーキテクチャ変更は不要であり、現状の実装を維持することを推奨する。
