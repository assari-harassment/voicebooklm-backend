package com.assari.voicebooklm.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.transfer.s3.S3TransferManager

/**
 * AWS S3 設定
 *
 * 音声ファイルの保存先として S3 を使用します。
 *
 * 設定方法:
 * 1. 環境変数で設定（推奨）
 *    - AWS_ACCESS_KEY_ID
 *    - AWS_SECRET_ACCESS_KEY
 *    - AWS_REGION（デフォルト: ap-northeast-1）
 *    - AWS_S3_BUCKET_NAME
 *
 * 2. application.yml で設定
 *    aws:
 *      access-key-id: YOUR_ACCESS_KEY
 *      secret-access-key: YOUR_SECRET_KEY
 *      region: ap-northeast-1
 *      s3:
 *        bucket-name: voicebooklm-audio-files
 *
 * 3. IAM Role（EC2/ECS/Lambda で実行時）
 *    - 環境変数を設定しなければ自動的に IAM Role を使用
 *
 * 必要な S3 権限:
 * - s3:PutObject（ファイルアップロード）
 * - s3:GetObject（ファイルダウンロード）
 * - s3:DeleteObject（ファイル削除）
 * - s3:ListBucket（バケット一覧）
 */
@Configuration
class AwsS3Config {

    @Value("\${aws.access-key-id:}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secret-access-key:}")
    private lateinit var secretAccessKey: String

    @Value("\${aws.region:ap-northeast-1}")
    private lateinit var region: String

    @Value("\${aws.s3.bucket-name:voicebooklm-audio-files}")
    lateinit var bucketName: String

    /**
     * AWS 認証情報プロバイダー
     *
     * 優先順位:
     * 1. application.yml の設定
     * 2. 環境変数（AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY）
     * 3. IAM Role（EC2/ECS/Lambda）
     */
    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider {
        return if (accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()) {
            // application.yml に設定がある場合
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            )
        } else {
            // 環境変数または IAM Role を使用
            DefaultCredentialsProvider.create()
        }
    }

    /**
     * S3 クライアント
     *
     * 基本的な S3 操作（PutObject, GetObject, DeleteObject など）に使用
     */
    @Bean
    fun s3Client(credentialsProvider: AwsCredentialsProvider): S3Client {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()
    }

    /**
     * S3 Async クライアント
     *
     * Transfer Manager に必要な非同期クライアント
     */
    @Bean
    fun s3AsyncClient(credentialsProvider: AwsCredentialsProvider): S3AsyncClient {
        return S3AsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()
    }

    /**
     * S3 Transfer Manager
     *
     * 大容量ファイルの効率的なアップロード・ダウンロードに使用
     * - マルチパートアップロード対応
     * - 自動リトライ
     * - 進捗管理
     *
     * 音声ファイルは数MB〜数十MBになるため、Transfer Manager を推奨
     */
    @Bean
    fun s3TransferManager(s3AsyncClient: S3AsyncClient): S3TransferManager {
        return S3TransferManager.builder()
            .s3Client(s3AsyncClient)
            .build()
    }
}
