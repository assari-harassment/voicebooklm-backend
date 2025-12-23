package com.assari.voicebooklm.infrastructure.api.storage

import com.assari.voicebooklm.config.GoogleCloudProperties
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Google Cloud Storage を使った音声ファイルの一時保存サービス
 *
 * 音声ファイルを GCS にアップロードし、Speech-to-Text API で利用可能な
 * gs:// URI を返す。処理完了後は削除することを想定。
 */
@Component
@Profile("!test")
class GcsStorageService(
    private val storage: Storage,
    cloudProperties: GoogleCloudProperties,
) {
    private val bucketName: String = cloudProperties.storage.bucketName
    private val logger = LoggerFactory.getLogger(GcsStorageService::class.java)

    companion object {
        private const val AUDIO_PREFIX = "audio/"
    }

    /**
     * 音声データを GCS にアップロードし、gs:// URI を返す
     *
     * @param userId ユーザーID（パス構成用）
     * @param audioData 音声バイナリデータ
     * @param mimeType 音声の MIME タイプ（例: audio/wav）
     * @return GCS URI（例: gs://bucket-name/audio/userId/uuid.wav）
     */
    suspend fun uploadAudio(
        userId: UUID,
        audioData: ByteArray,
        mimeType: String,
    ): GcsUploadResult = withContext(Dispatchers.IO) {
        val extension = mimeTypeToExtension(mimeType)
        val objectName = "$AUDIO_PREFIX$userId/${UUID.randomUUID()}$extension"

        val blobId = BlobId.of(bucketName, objectName)
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(mimeType)
            .build()

        logger.info("Uploading audio to GCS: bucket={}, object={}, size={} bytes", bucketName, objectName, audioData.size)

        storage.create(blobInfo, audioData)

        val gcsUri = "gs://$bucketName/$objectName"
        logger.info("Audio uploaded successfully: {}", gcsUri)

        GcsUploadResult(
            gcsUri = gcsUri,
            bucketName = bucketName,
            objectName = objectName,
        )
    }

    /**
     * GCS から音声ファイルを削除する
     *
     * @param objectName オブジェクト名（gs:// を除いたパス）
     */
    suspend fun deleteAudio(objectName: String): Boolean = withContext(Dispatchers.IO) {
        val blobId = BlobId.of(bucketName, objectName)
        val deleted = storage.delete(blobId)

        if (deleted) {
            logger.info("Audio deleted from GCS: {}", objectName)
        } else {
            logger.warn("Audio not found in GCS (may already be deleted): {}", objectName)
        }

        deleted
    }

    /**
     * GCS URI からオブジェクト名を抽出する
     */
    fun extractObjectName(gcsUri: String): String {
        // gs://bucket-name/path/to/file → path/to/file
        val prefix = "gs://$bucketName/"
        return if (gcsUri.startsWith(prefix)) {
            gcsUri.removePrefix(prefix)
        } else {
            throw IllegalArgumentException("Invalid GCS URI: $gcsUri")
        }
    }

    private fun mimeTypeToExtension(mimeType: String): String = when {
        mimeType.contains("wav") -> ".wav"
        mimeType.contains("mp3") || mimeType.contains("mpeg") -> ".mp3"
        mimeType.contains("ogg") -> ".ogg"
        mimeType.contains("webm") -> ".webm"
        mimeType.contains("flac") -> ".flac"
        mimeType.contains("m4a") || mimeType.contains("mp4") -> ".m4a"
        else -> ".audio"
    }
}

/**
 * GCS アップロード結果
 */
data class GcsUploadResult(
    val gcsUri: String,
    val bucketName: String,
    val objectName: String,
)
