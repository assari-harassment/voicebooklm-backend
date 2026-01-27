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
     * ISO 8601形式の文字列をInstantに変換する
     * nullまたは不正な形式の場合は現在時刻を返す
     */
    private fun parseInstant(isoString: String?): Instant {
        if (isoString == null) {
            logger.debug("createdAt/updatedAt is null, using current time")
            return Instant.now()
        }

        return try {
            val instant = Instant.parse(isoString)
            logger.debug("Parsed timestamp: $isoString -> $instant")
            instant
        } catch (e: Exception) {
            logger.warn("Invalid ISO 8601 format: '$isoString'. Using current time instead.", e)
            Instant.now()
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
            logger.debug("Creating folder: ${seedFolder.name}, createdAt from YAML: ${seedFolder.createdAt}, updatedAt from YAML: ${seedFolder.updatedAt}")
            val createdAt = parseInstant(seedFolder.createdAt)
            val updatedAt = parseInstant(seedFolder.updatedAt)

            val folder = folderRepository.save(
                Folder.create(
                    id = UuidCreator.getTimeOrderedEpoch(),
                    userId = userId,
                    name = seedFolder.name,
                    parentId = parentId,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
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

            logger.debug("Creating memo: ${seedMemo.title}, createdAt from YAML: ${seedMemo.createdAt}, updatedAt from YAML: ${seedMemo.updatedAt}")
            val createdAt = parseInstant(seedMemo.createdAt)
            val updatedAt = parseInstant(seedMemo.updatedAt)

            // 新規作成 → 文字起こし完了 → 整形完了 の流れで作成
            // 注: completeTranscription()とcompleteFormatting()はupdatedAtを現在時刻に更新するが、
            // 開発用シードデータとしてはYAMLで指定された時刻を保持したいため、
            // 後続の処理でupdatedAtを復元する（createdAtはcreate()で設定後、変更されないため復元不要）
            var memo = VoiceMemo.create(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                languageCode = "ja-JP",
                createdAt = createdAt,
                updatedAt = updatedAt,
            ).completeTranscription(
                text = seedMemo.transcription.trim(),
            ).completeFormatting(
                title = seedMemo.title,
                content = seedMemo.content.trim(),
                tags = seedMemo.tags,
                folderId = folderId,
            )

            // YAMLで指定されたupdatedAtを復元（completeTranscription/completeFormattingで上書きされた値を元に戻す）
            if (seedMemo.updatedAt != null) {
                memo = memo.copy(updatedAt = updatedAt)
            }

            voiceMemoRepository.save(memo)
            count++
        }

        return count
    }
}
