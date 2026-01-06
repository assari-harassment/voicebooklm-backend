package com.assari.voicebooklm.usecase.dev

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 開発用シードデータ作成ユースケース
 *
 * 認証済みユーザー用にテスト用のフォルダーとメモを作成する。
 * 冪等性を持ち、既にデータが存在する場合はスキップする。
 */
@Service
@Profile("dev")
class DevSeedUseCase(
    private val folderRepository: FolderRepository,
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    data class Output(
        val foldersCreated: Int,
        val memosCreated: Int,
        val skipped: Boolean,
        val message: String,
    )

    suspend fun execute(userId: UUID): Output {
        // 既存のフォルダーをチェック（冪等性確保）
        val existingFolders = folderRepository.findByUserId(userId)
        if (existingFolders.isNotEmpty()) {
            return Output(
                foldersCreated = 0,
                memosCreated = 0,
                skipped = true,
                message = "既にフォルダーが存在するためスキップしました",
            )
        }

        // フォルダー作成
        val folders = createFolders(userId)

        // メモ作成
        val memos = createMemos(userId, folders)

        return Output(
            foldersCreated = folders.size,
            memosCreated = memos.size,
            skipped = false,
            message = "テストデータを作成しました",
        )
    }

    private suspend fun createFolders(userId: UUID): Map<String, Folder> {
        val folders = mutableMapOf<String, Folder>()

        // ルートフォルダー作成
        val workFolder = folderRepository.save(
            Folder.create(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                name = "仕事",
                parentId = null,
            )
        )
        folders["仕事"] = workFolder

        val studyFolder = folderRepository.save(
            Folder.create(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                name = "学習",
                parentId = null,
            )
        )
        folders["学習"] = studyFolder

        val projectFolder = folderRepository.save(
            Folder.create(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                name = "プロジェクト",
                parentId = null,
            )
        )
        folders["プロジェクト"] = projectFolder

        // サブフォルダー作成
        val meetingFolder = folderRepository.save(
            Folder.create(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                name = "会議",
                parentId = workFolder.id,
            )
        )
        folders["仕事/会議"] = meetingFolder

        val readingFolder = folderRepository.save(
            Folder.create(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                name = "読書",
                parentId = studyFolder.id,
            )
        )
        folders["学習/読書"] = readingFolder

        val appDevFolder = folderRepository.save(
            Folder.create(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                name = "アプリ開発",
                parentId = projectFolder.id,
            )
        )
        folders["プロジェクト/アプリ開発"] = appDevFolder

        return folders
    }

    private suspend fun createMemos(userId: UUID, folders: Map<String, Folder>): List<VoiceMemo> {
        val memos = mutableListOf<VoiceMemo>()

        // メモ1: プロジェクト会議の議事録 → 仕事/会議
        val memo1 = VoiceMemo.restore(
            id = UuidCreator.getTimeOrderedEpoch(),
            userId = userId,
            transcription = Transcription.completed(
                text = "今日はプロジェクト会議。新機能のリリーススケジュールを確認。β版は12月15日。フィードバック期間は2週間。UI/UXチームからナビゲーション改善・ボタン配置・レスポンシブ強化の指摘。次回会議は12月20日予定。",
                languageCode = "ja-JP",
            ),
            formatting = Formatting.completed(
                title = "プロジェクト会議の議事録",
                content = """## 会議概要
本日のプロジェクト会議では、新機能のリリーススケジュールについて議論しました。

### 主な決定事項
- **β版リリース日**: 2024年12月15日
- **フィードバック収集期間**: 2週間

### UI/UXチームからの報告
ユーザビリティテストの結果が共有され、以下の改善が必要との指摘がありました：
1. ナビゲーションの改善
2. ボタン配置の最適化
3. レスポンシブデザインの強化

> 次回会議は12月20日を予定しています。""",
                tags = listOf("仕事", "ミーティング", "重要"),
                folderId = folders["仕事/会議"]?.id,
            ),
            deleted = false,
            createdAt = java.time.Instant.now().minusSeconds(86400 * 3),
            updatedAt = java.time.Instant.now().minusSeconds(86400 * 3),
        )
        memos.add(voiceMemoRepository.save(memo1))

        // メモ2: 読書メモ：デザイン思考 → 学習/読書
        val memo2 = VoiceMemo.restore(
            id = UuidCreator.getTimeOrderedEpoch(),
            userId = userId,
            transcription = Transcription.completed(
                text = "デザイン思考について読書メモ。5ステップは共感、問題定義、アイデア創出、プロトタイプ、テスト。共感フェーズでユーザーの潜在ニーズを理解するのが重要。観察してインタビューして分析する流れ。",
                languageCode = "ja-JP",
            ),
            formatting = Formatting.completed(
                title = "読書メモ：デザイン思考",
                content = """# デザイン思考の5つのステップ

デザイン思考のプロセスを通じて、**ユーザー中心の解決策**を見つけることができます。

## プロセス
1. **共感** - ユーザーの潜在的なニーズを理解する
2. **問題定義** - 本質的な課題を明確にする
3. **アイデア創出** - 多様な解決策を考える
4. **プロトタイプ** - 具体的な形にする
5. **テスト** - フィードバックを得る

### ポイント
特に*共感のフェーズ*では、ユーザーの潜在的なニーズを理解することが重要です。

```
観察 → インタビュー → 分析
```""",
                tags = listOf("学習", "読書", "デザイン"),
                folderId = folders["学習/読書"]?.id,
            ),
            deleted = false,
            createdAt = java.time.Instant.now().minusSeconds(86400 * 2),
            updatedAt = java.time.Instant.now().minusSeconds(86400 * 2),
        )
        memos.add(voiceMemoRepository.save(memo2))

        // メモ3: アプリアイデア：習慣トラッカー → プロジェクト/アプリ開発
        val memo3 = VoiceMemo.restore(
            id = UuidCreator.getTimeOrderedEpoch(),
            userId = userId,
            transcription = Transcription.completed(
                text = "習慣トラッカーのアプリアイデア。毎日の習慣をチェックリスト管理、連続記録を可視化、モチベ維持の仕組み。NotionっぽいシンプルUIを想定。必要なUIはカレンダー、ストリーク表示、統計グラフ。",
                languageCode = "ja-JP",
            ),
            formatting = Formatting.completed(
                title = "アプリアイデア：習慣トラッカー",
                content = """## コンセプト
習慣を継続するためのシンプルなアプリ

### 主な機能
- 毎日の習慣をチェックリストで管理
- 連続記録をビジュアル化
- モチベーション維持のための仕組み

### デザイン方針
**Notion**のようなシンプルなインターフェース

#### UI要素
- カレンダービュー
- ストリーク表示
- 統計グラフ

> シンプルで継続しやすいデザインを目指す""",
                tags = listOf("アイデア", "開発", "アプリ"),
                folderId = folders["プロジェクト/アプリ開発"]?.id,
            ),
            deleted = false,
            createdAt = java.time.Instant.now().minusSeconds(86400),
            updatedAt = java.time.Instant.now().minusSeconds(86400),
        )
        memos.add(voiceMemoRepository.save(memo3))

        return memos
    }
}
