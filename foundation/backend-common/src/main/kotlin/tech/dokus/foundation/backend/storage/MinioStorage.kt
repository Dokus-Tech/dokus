package tech.dokus.foundation.backend.storage

import tech.dokus.foundation.backend.config.MinioConfig
import tech.dokus.foundation.backend.utils.loggerFor
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
import java.io.ByteArrayInputStream
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * MinIO implementation of ObjectStorage.
 * Provides S3-compatible object storage operations.
 *
 * @param client The MinIO client configured with the internal endpoint (for PUT/GET/DELETE operations)
 * @param signingClient Optional MinIO client configured with the public base URL (for presigned URLs).
 *                      If null, the main client is used for signing (URLs will contain internal endpoint).
 * @param signingBaseUrl The base URL used for the signing client (e.g., http://192.168.0.44:8000)
 * @param publicPathPrefix Optional path prefix to insert in presigned URLs (e.g., /storage)
 * @param bucketName The bucket to store objects in
 */
class MinioStorage(
    private val client: MinioClient,
    private val signingClient: MinioClient?,
    private val signingBaseUrl: String?,
    private val publicPathPrefix: String?,
    private val bucketName: String,
) : ObjectStorage {

    private val logger = loggerFor()

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

            // Use signingClient if available (configured with public base URL)
            // This ensures the signature is calculated for the public host, not the internal one
            val clientForSigning = signingClient ?: client

            val url = clientForSigning.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .`object`(key)
                    .expiry(expiry.toInt(DurationUnit.SECONDS))
                    .build()
            )

            // If we have a path prefix (e.g., /storage), insert it after the base URL
            // URL format: http://host:port/bucket/key?signature -> http://host:port/storage/bucket/key?signature
            if (signingClient != null && publicPathPrefix != null && signingBaseUrl != null) {
                url.replace(signingBaseUrl, "$signingBaseUrl$publicPathPrefix")
            } else {
                url
            }
        }

    companion object {
        private val logger = loggerFor<MinioStorage>()
        /**
         * Create a MinioStorage instance from configuration.
         *
         * @param config MinIO configuration with endpoint and credentials
         * @param publicUrl Optional public URL for presigned URLs (e.g., http://192.168.0.44:8000/storage).
         *                  When provided, a separate signing client is created configured with this endpoint,
         *                  ensuring presigned URLs are signed for the public host (not the internal Docker host).
         */
        fun create(config: MinioConfig, publicUrl: String? = null): MinioStorage {
            // Main client for PUT/GET/DELETE operations - uses internal endpoint
            val client = MinioClient.builder()
                .endpoint(config.endpoint)
                .credentials(config.accessKey, config.secretKey)
                .build()

            // Parse public URL to extract base URL and path prefix
            // MinIO client doesn't allow paths in the endpoint, so we need to:
            // 1. Create signing client with just the base URL (scheme://host:port)
            // 2. Insert the path prefix into the generated URL afterward
            val (signingClient, signingBaseUrl, pathPrefix) = if (publicUrl != null) {
                try {
                    val url = java.net.URL(publicUrl)
                    val baseUrl =
                        "${url.protocol}://${url.host}${if (url.port != -1) ":${url.port}" else ""}"
                    val path = url.path.takeIf { it.isNotEmpty() && it != "/" }

                    logger.info("Creating signing client: baseUrl=$baseUrl, pathPrefix=$path")

                    // Set region explicitly to "us-east-1" to prevent MinIO from making
                    // a network call to get bucket location (which would fail since the
                    // signing client endpoint goes through Traefik, not directly to MinIO)
                    val signingClient = MinioClient.builder()
                        .endpoint(baseUrl)
                        .credentials(config.accessKey, config.secretKey)
                        .region("us-east-1")
                        .build()

                    Triple(signingClient, baseUrl, path)
                } catch (e: Exception) {
                    logger.warn(
                        "Failed to parse public URL '$publicUrl', falling back to internal endpoint",
                        e
                    )
                    Triple(null, null, null)
                }
            } else {
                Triple(null, null, null)
            }

            return MinioStorage(client, signingClient, signingBaseUrl, pathPrefix, config.bucket)
        }
    }
}
