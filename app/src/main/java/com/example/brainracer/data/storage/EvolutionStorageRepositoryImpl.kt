package com.example.brainracer.data.storage

import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.example.brainracer.BuildConfig
import com.example.brainracer.data.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date

/**
 * Реализация [EvolutionStorageRepository] через AWS S3 SDK для Evolution (s3.cloud.ru).
 * Все сетевые операции на [Dispatchers.IO].
 */
class EvolutionStorageRepositoryImpl : EvolutionStorageRepository {

    private val s3: AmazonS3Client by lazy { buildClient() }

    private fun buildClient(): AmazonS3Client {
        val access = BuildConfig.S3_ACCESS_KEY.trim()
        val secret = BuildConfig.S3_SECRET_KEY.trim()
        val credentials = BasicAWSCredentials(access, secret)
        return AmazonS3Client(credentials).apply {
            // ru-central-1 отсутствует в enum Regions SDK → Regions.fromName падает, setRegion(null/us-east-1)
            // давали SigV4 с областью не ru-central-1. Evolution требует scope .../ru-central-1/s3/aws4_request
            // (см. cloud.ru docs по SigV4). Трёхаргументный setEndpoint задаёт endpoint + регион подписи.
            setEndpoint(StorageConfig.ENDPOINT, "s3", StorageConfig.REGION)
            setS3ClientOptions(
                S3ClientOptions.builder()
                    .setPathStyleAccess(true)
                    .build()
            )
        }
    }

    override suspend fun upload(
        bucket: String,
        key: String,
        bytes: ByteArray,
        mimeType: String,
        isPublic: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val metadata = ObjectMetadata().apply {
                contentType = mimeType
                contentLength = bytes.size.toLong()
            }
            val request = PutObjectRequest(bucket, key, ByteArrayInputStream(bytes), metadata).apply {
                if (isPublic) {
                    cannedAcl = CannedAccessControlList.PublicRead
                }
            }
            s3.putObject(request)
            // Публичный URL формируется по бакету через единую точку (avatars / quizzes / ...).
            val baseUrl = StorageConfig.publicUrlForBucket(bucket, key)
            // Объект мог быть перезаписан по тому же ключу: добавляем версию в query,
            // чтобы клиенты (Coil, браузер) точно подтянули новую версию.
            val url = StorageConfig.appendVersion(baseUrl)
            Log.d(TAG, "Uploaded ${bytes.size / 1024}KB → $url")
            Result.success(url)
        } catch (e: Exception) {
            Log.e(TAG, "upload($bucket/$key) failed", e)
            Result.error(e)
        }
    }

    override suspend fun download(bucket: String, key: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                val s3Object = s3.getObject(bucket, key)
                val out = ByteArrayOutputStream()
                s3Object.objectContent.use { it.copyTo(out) }
                Result.success(out.toByteArray())
            } catch (e: Exception) {
                Log.e(TAG, "download($bucket/$key) failed", e)
                Result.error(e)
            }
        }

    override suspend fun listKeys(bucket: String, prefix: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val keys = mutableListOf<String>()
                var continuationToken: String? = null
                do {
                    val request = ListObjectsV2Request().apply {
                        bucketName = bucket
                        this.prefix = prefix
                        this.continuationToken = continuationToken
                    }
                    val result = s3.listObjectsV2(request)
                    keys.addAll(result.objectSummaries.map { it.key })
                    continuationToken = if (result.isTruncated) result.nextContinuationToken else null
                } while (continuationToken != null)
                Result.success(keys)
            } catch (e: Exception) {
                Log.e(TAG, "listKeys($bucket, prefix=$prefix) failed", e)
                Result.error(e)
            }
        }

    override suspend fun delete(bucket: String, key: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                s3.deleteObject(bucket, key)
                Log.d(TAG, "Deleted $bucket/$key")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "delete($bucket/$key) failed", e)
                Result.error(e)
            }
        }

    override suspend fun deleteWithPrefix(bucket: String, prefix: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val keysResult = listKeys(bucket, prefix)
                if (keysResult is Result.Error) return@withContext keysResult

                val keys = (keysResult as Result.Success).data
                if (keys.isEmpty()) return@withContext Result.success(Unit)

                keys.chunked(1000).forEach { chunk ->
                    val keyVersions = chunk.map { DeleteObjectsRequest.KeyVersion(it) }
                    s3.deleteObjects(
                        DeleteObjectsRequest(bucket).withKeys(keyVersions)
                    )
                }
                Log.d(TAG, "Deleted ${keys.size} objects with prefix '$prefix' from $bucket")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "deleteWithPrefix($bucket, prefix=$prefix) failed", e)
                Result.error(e)
            }
        }

    override suspend fun presignedUrl(
        bucket: String,
        key: String,
        expirationSecs: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val expiration = Date(System.currentTimeMillis() + expirationSecs * 1000L)
            val request = GeneratePresignedUrlRequest(bucket, key).apply {
                this.expiration = expiration
            }
            val url = s3.generatePresignedUrl(request).toString()
            Result.success(url)
        } catch (e: Exception) {
            Log.e(TAG, "presignedUrl($bucket/$key) failed", e)
            Result.error(e)
        }
    }

    companion object {
        private const val TAG = "EvolutionStorage"
    }
}
