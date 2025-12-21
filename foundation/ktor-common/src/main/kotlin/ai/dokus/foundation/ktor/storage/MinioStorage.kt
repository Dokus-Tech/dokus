package ai.dokus.foundation.ktor.storage

import ai.dokus.foundation.ktor.config.MinioConfig
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.http.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * MinIO implementation of ObjectStorage.
 * Provides S3-compatible object storage operations.
 *
 * @param client The MinIO client configured with the internal endpoint
 * @param bucketName The bucket to store objects in
 * @param internalEndpoint The internal MinIO endpoint (e.g., http://minio:9000)
 * @param publicUrl The public URL base for presigned URLs (e.g., https://app.dokus.tech/storage)
 *                  If null, presigned URLs will use the internal endpoint (backward compatible)
 */
class MinioStorage(
    private val client: MinioClient,
    private val bucketName: String,
    private val internalEndpoint: String,
    private val publicUrl: String?
) : ObjectStorage {

    private val logger = LoggerFactory.getLogger(MinioStorage::class.java)

    init {
        // Ensure bucket exists on startup
        try {
            val bucketExists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            )
            if (!bucketExists) {
                logger.info("Creating bucket: $bucketName")
                client.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
                )
                logger.info("Bucket created: $bucketName")
            } else {
                logger.info("Bucket already exists: $bucketName")
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize bucket: $bucketName", e)
            throw e
        }
    }

    override suspend fun put(key: String, data: ByteArray, contentType: String): String =
        withContext(Dispatchers.IO) {
            logger.debug("Uploading object: key=$key, size=${data.size}, contentType=$contentType")

            ByteArrayInputStream(data).use { inputStream ->
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .`object`(key)
                        .stream(inputStream, data.size.toLong(), -1)
                        .contentType(contentType)
                        .build()
                )
            }

            logger.info("Successfully uploaded object: $key")
            key
        }

    override suspend fun get(key: String): ByteArray = withContext(Dispatchers.IO) {
        logger.debug("Downloading object: $key")

        try {
            client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(key)
                    .build()
            ).use { stream ->
                stream.readAllBytes()
            }
        } catch (e: io.minio.errors.ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                throw NoSuchElementException("Object not found: $key")
            }
            throw e
        }
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        logger.debug("Deleting object: $key")

        client.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .`object`(key)
                .build()
        )

        logger.info("Successfully deleted object: $key")
    }

    override suspend fun exists(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(key)
                    .build()
            )
            true
        } catch (e: io.minio.errors.ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                false
            } else {
                throw e
            }
        }
    }

    override suspend fun getSignedUrl(key: String, expiry: Duration): String =
        withContext(Dispatchers.IO) {
            logger.debug("Generating signed URL for: $key, expiry=$expiry")

            val internalUrl = client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .`object`(key)
                    .expiry(expiry.toInt(DurationUnit.SECONDS))
                    .build()
            )

            // Rewrite URL to use public endpoint if configured
            if (publicUrl != null) {
                val rewrittenUrl = internalUrl.replace(internalEndpoint, publicUrl)
                logger.debug("Rewrote URL from internal to public: $rewrittenUrl")
                rewrittenUrl
            } else {
                internalUrl
            }
        }

    companion object {
        /**
         * Create a MinioStorage instance from configuration.
         *
         * @param config MinIO configuration with endpoint and credentials
         * @param publicUrl Optional public URL for presigned URLs (from AppBaseConfig.storage.publicUrl)
         */
        fun create(config: MinioConfig, publicUrl: String? = null): MinioStorage {
            val client = MinioClient.builder()
                .endpoint(config.endpoint)
                .credentials(config.accessKey, config.secretKey)
                .build()

            return MinioStorage(client, config.bucket, config.endpoint, publicUrl)
        }
    }
}
