package com.assari.voicebooklm.usecase.dev

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.f4b6a3.uuid.UuidCreator
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * 開発用シードデータ作成ユースケース
 *
 * seed-data.yml から定義を読み込み、認証済みユーザー用にテストデータを作成する。
 * 冪等性を持ち、既にデータが存在する場合はスキップする。
 */
@Service
@Profile("dev")
class DevSeedUseCase(
    private val folderRepository: FolderRepository,
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

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

        // YAMLファイルからシードデータを読み込み
        val seedData = loadSeedData()

        // フォルダー作成
        val folderMap = createFolders(userId, seedData.folders)

        // メモ作成
        val memoCount = createMemos(userId, seedData.memos, folderMap)

        return Output(
            foldersCreated = folderMap.size,
            memosCreated = memoCount,
            skipped = false,
            message = "テストデータを作成しました（フォルダー: ${folderMap.size}件、メモ: ${memoCount}件）",
        )
    }

    private fun loadSeedData(): SeedData {
        return try {
            val resource = ClassPathResource("dev/seed-data.yml")
            resource.inputStream.use { inputStream ->
                yamlMapper.readValue(inputStream, SeedData::class.java)
            }
        } catch (e: Exception) {
            logger.error("seed-data.yml の読み込みに失敗しました", e)
            throw IllegalStateException("シードデータの読み込みに失敗しました: ${e.message}", e)
        }
    }

    /**
     * フォルダーを再帰的に作成し、パス → Folder のマップを返す
     */
    private suspend fun createFolders(
        userId: UUID,
        seedFolders: List<SeedFolder>,
        parentId: UUID? = null,
        parentPath: String = "",
    ): Map<String, Folder> {
        val folderMap = mutableMapOf<String, Folder>()

        for (seedFolder in seedFolders) {
            val folder = folderRepository.save(
                Folder.create(
                    id = UuidCreator.getTimeOrderedEpoch(),
                    userId = userId,
                    name = seedFolder.name,
                    parentId = parentId,
                )
            )

            val path = if (parentPath.isEmpty()) seedFolder.name else "$parentPath/${seedFolder.name}"
            folderMap[path] = folder

            // 子フォルダーを再帰的に作成
            if (seedFolder.children.isNotEmpty()) {
                val childFolders = createFolders(userId, seedFolder.children, folder.id, path)
                folderMap.putAll(childFolders)
            }
        }

        return folderMap
    }

    /**
     * メモを作成し、作成件数を返す
     */
    private suspend fun createMemos(
        userId: UUID,
        seedMemos: List<SeedMemo>,
        folderMap: Map<String, Folder>,
    ): Int {
        var count = 0

        for (seedMemo in seedMemos) {
            // フォルダーパスからフォルダーIDを解決
            val folderId = seedMemo.folder?.let { path ->
                folderMap[path]?.id.also {
                    if (it == null) {
                        logger.warn("フォルダーが見つかりません: $path（メモ: ${seedMemo.title}）")
                    }
                }
            }

            // 新規作成 → 文字起こし完了 → 整形完了 の流れで作成
            val memo = VoiceMemo.create(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                languageCode = "ja-JP",
            ).completeTranscription(
                text = seedMemo.transcription.trim(),
            ).completeFormatting(
                title = seedMemo.title,
                content = seedMemo.content.trim(),
                tags = seedMemo.tags,
                folderId = folderId,
            )

            voiceMemoRepository.save(memo)
            count++
        }

        return count
    }
}
