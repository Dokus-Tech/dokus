package ai.dokus.foundation.database.services

import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.*

/**
 * Storage service interface for file uploads (S3/MinIO compatible)
 * Supports receipt uploads, invoice PDFs, and other attachments
 */
interface StorageService {
    /**
     * Upload a file to storage
     * @param bucket The S3 bucket name
     * @param key The file key/path in the bucket
     * @param content The file content as byte array
     * @param contentType The MIME type (e.g., "image/jpeg", "application/pdf")
     * @param metadata Optional metadata to attach to the file
     * @return The URL where the file was stored
     */
    suspend fun uploadFile(
        bucket: String,
        key: String,
        content: ByteArray,
        contentType: String,
        metadata: Map<String, String> = emptyMap()
    ): String

    /**
     * Download a file from storage
     * @param bucket The S3 bucket name
     * @param key The file key/path in the bucket
     * @return The file content as byte array, or null if not found
     */
    suspend fun downloadFile(bucket: String, key: String): ByteArray?

    /**
     * Delete a file from storage
     * @param bucket The S3 bucket name
     * @param key The file key/path in the bucket
     */
    suspend fun deleteFile(bucket: String, key: String)

    /**
     * Check if a file exists in storage
     * @param bucket The S3 bucket name
     * @param key The file key/path in the bucket
     * @return True if the file exists, false otherwise
     */
    suspend fun fileExists(bucket: String, key: String): Boolean

    /**
     * Generate a pre-signed URL for temporary file access
     * @param bucket The S3 bucket name
     * @param key The file key/path in the bucket
     * @param expirationMinutes How long the URL should be valid (in minutes)
     * @return The pre-signed URL
     */
    suspend fun generatePresignedUrl(
        bucket: String,
        key: String,
        expirationMinutes: Int = 60
    ): String

    /**
     * List files in a bucket with a given prefix
     * @param bucket The S3 bucket name
     * @param prefix The key prefix to filter by
     * @param maxKeys Maximum number of keys to return
     * @return List of file keys
     */
    suspend fun listFiles(
        bucket: String,
        prefix: String = "",
        maxKeys: Int = 1000
    ): List<String>
}

/**
 * MinIO implementation of StorageService
 * Compatible with S3 API - works with both MinIO and AWS S3
 */
class MinIOStorageService(
    private val endpoint: String,
    private val accessKey: String,
    private val secretKey: String,
    private val region: String = "us-east-1"
) : StorageService {

    private val logger = LoggerFactory.getLogger(MinIOStorageService::class.java)

    // TODO: Initialize MinIO client
    // private val minioClient = MinioClient.builder()
    //     .endpoint(endpoint)
    //     .credentials(accessKey, secretKey)
    //     .build()

    override suspend fun uploadFile(
        bucket: String,
        key: String,
        content: ByteArray,
        contentType: String,
        metadata: Map<String, String>
    ): String {
        logger.info("Uploading file to bucket=$bucket, key=$key, size=${content.size} bytes")

        // TODO: Implement MinIO upload
        // minioClient.putObject(
        //     PutObjectArgs.builder()
        //         .bucket(bucket)
        //         .object(key)
        //         .stream(ByteArrayInputStream(content), content.size.toLong(), -1)
        //         .contentType(contentType)
        //         .userMetadata(metadata)
        //         .build()
        // )

        throw NotImplementedError("MinIO storage not yet configured - add MinIO dependency to build.gradle.kts")
    }

    override suspend fun downloadFile(bucket: String, key: String): ByteArray? {
        logger.info("Downloading file from bucket=$bucket, key=$key")

        // TODO: Implement MinIO download
        // return try {
        //     minioClient.getObject(
        //         GetObjectArgs.builder()
        //             .bucket(bucket)
        //             .object(key)
        //             .build()
        //     ).readBytes()
        // } catch (e: Exception) {
        //     logger.warn("File not found: bucket=$bucket, key=$key", e)
        //     null
        // }

        throw NotImplementedError("MinIO storage not yet configured - add MinIO dependency to build.gradle.kts")
    }

    override suspend fun deleteFile(bucket: String, key: String) {
        logger.info("Deleting file from bucket=$bucket, key=$key")

        // TODO: Implement MinIO delete
        // minioClient.removeObject(
        //     RemoveObjectArgs.builder()
        //         .bucket(bucket)
        //         .object(key)
        //         .build()
        // )

        throw NotImplementedError("MinIO storage not yet configured - add MinIO dependency to build.gradle.kts")
    }

    override suspend fun fileExists(bucket: String, key: String): Boolean {
        logger.debug("Checking if file exists: bucket=$bucket, key=$key")

        // TODO: Implement MinIO file existence check
        // return try {
        //     minioClient.statObject(
        //         StatObjectArgs.builder()
        //             .bucket(bucket)
        //             .object(key)
        //             .build()
        //     )
        //     true
        // } catch (e: Exception) {
        //     false
        // }

        throw NotImplementedError("MinIO storage not yet configured - add MinIO dependency to build.gradle.kts")
    }

    override suspend fun generatePresignedUrl(
        bucket: String,
        key: String,
        expirationMinutes: Int
    ): String {
        logger.info("Generating presigned URL: bucket=$bucket, key=$key, expiration=${expirationMinutes}min")

        // TODO: Implement MinIO presigned URL generation
        // return minioClient.getPresignedObjectUrl(
        //     GetPresignedObjectUrlArgs.builder()
        //         .bucket(bucket)
        //         .object(key)
        //         .expiry(expirationMinutes, TimeUnit.MINUTES)
        //         .method(Method.GET)
        //         .build()
        // )

        throw NotImplementedError("MinIO storage not yet configured - add MinIO dependency to build.gradle.kts")
    }

    override suspend fun listFiles(bucket: String, prefix: String, maxKeys: Int): List<String> {
        logger.info("Listing files: bucket=$bucket, prefix=$prefix, maxKeys=$maxKeys")

        // TODO: Implement MinIO list objects
        // return minioClient.listObjects(
        //     ListObjectsArgs.builder()
        //         .bucket(bucket)
        //         .prefix(prefix)
        //         .maxKeys(maxKeys)
        //         .build()
        // ).map { it.get().objectName() }

        throw NotImplementedError("MinIO storage not yet configured - add MinIO dependency to build.gradle.kts")
    }
}

/**
 * In-memory storage implementation for testing
 * Stores files in memory - data is lost when application restarts
 */
class InMemoryStorageService : StorageService {
    private val logger = LoggerFactory.getLogger(InMemoryStorageService::class.java)
    private val storage = mutableMapOf<String, MutableMap<String, ByteArray>>()

    override suspend fun uploadFile(
        bucket: String,
        key: String,
        content: ByteArray,
        contentType: String,
        metadata: Map<String, String>
    ): String {
        logger.info("Uploading file to in-memory storage: bucket=$bucket, key=$key")

        val bucketStorage = storage.getOrPut(bucket) { mutableMapOf() }
        bucketStorage[key] = content

        return "memory://$bucket/$key"
    }

    override suspend fun downloadFile(bucket: String, key: String): ByteArray? {
        logger.info("Downloading file from in-memory storage: bucket=$bucket, key=$key")

        return storage[bucket]?.get(key)
    }

    override suspend fun deleteFile(bucket: String, key: String) {
        logger.info("Deleting file from in-memory storage: bucket=$bucket, key=$key")

        storage[bucket]?.remove(key)
    }

    override suspend fun fileExists(bucket: String, key: String): Boolean {
        return storage[bucket]?.containsKey(key) == true
    }

    override suspend fun generatePresignedUrl(bucket: String, key: String, expirationMinutes: Int): String {
        // In-memory storage doesn't support presigned URLs
        return "memory://$bucket/$key"
    }

    override suspend fun listFiles(bucket: String, prefix: String, maxKeys: Int): List<String> {
        return storage[bucket]
            ?.keys
            ?.filter { it.startsWith(prefix) }
            ?.take(maxKeys)
            ?: emptyList()
    }
}

/**
 * Attachment storage service
 * Provides high-level file storage operations for attachments (receipts, invoices, etc.)
 */
class AttachmentStorageService(
    private val storageService: StorageService,
    private val bucket: String = "dokus-attachments"
) {
    private val logger = LoggerFactory.getLogger(AttachmentStorageService::class.java)

    /**
     * Upload a receipt file
     * @param tenantId The tenant ID
     * @param expenseId The expense ID
     * @param filename The original filename
     * @param content The file content
     * @param contentType The MIME type
     * @return The storage URL
     */
    suspend fun uploadReceipt(
        tenantId: String,
        expenseId: String,
        filename: String,
        content: ByteArray,
        contentType: String
    ): String {
        val sanitizedFilename = sanitizeFilename(filename)
        val key = "receipts/$tenantId/$expenseId/$sanitizedFilename"

        logger.info("Uploading receipt: tenantId=$tenantId, expenseId=$expenseId, filename=$filename")

        return storageService.uploadFile(
            bucket = bucket,
            key = key,
            content = content,
            contentType = contentType,
            metadata = mapOf(
                "tenant-id" to tenantId,
                "expense-id" to expenseId,
                "original-filename" to filename
            )
        )
    }

    /**
     * Download a receipt file
     * @param tenantId The tenant ID
     * @param expenseId The expense ID
     * @param filename The filename
     * @return The file content, or null if not found
     */
    suspend fun downloadReceipt(
        tenantId: String,
        expenseId: String,
        filename: String
    ): ByteArray? {
        val sanitizedFilename = sanitizeFilename(filename)
        val key = "receipts/$tenantId/$expenseId/$sanitizedFilename"

        logger.info("Downloading receipt: tenantId=$tenantId, expenseId=$expenseId, filename=$filename")

        return storageService.downloadFile(bucket, key)
    }

    /**
     * Delete a receipt file
     * @param tenantId The tenant ID
     * @param expenseId The expense ID
     * @param filename The filename
     */
    suspend fun deleteReceipt(
        tenantId: String,
        expenseId: String,
        filename: String
    ) {
        val sanitizedFilename = sanitizeFilename(filename)
        val key = "receipts/$tenantId/$expenseId/$sanitizedFilename"

        logger.info("Deleting receipt: tenantId=$tenantId, expenseId=$expenseId, filename=$filename")

        storageService.deleteFile(bucket, key)
    }

    /**
     * Upload an invoice PDF
     * @param tenantId The tenant ID
     * @param invoiceId The invoice ID
     * @param invoiceNumber The invoice number
     * @param content The PDF content
     * @return The storage URL
     */
    suspend fun uploadInvoice(
        tenantId: String,
        invoiceId: String,
        invoiceNumber: String,
        content: ByteArray
    ): String {
        val sanitizedNumber = sanitizeFilename(invoiceNumber)
        val key = "invoices/$tenantId/$invoiceId/$sanitizedNumber.pdf"

        logger.info("Uploading invoice: tenantId=$tenantId, invoiceId=$invoiceId, number=$invoiceNumber")

        return storageService.uploadFile(
            bucket = bucket,
            key = key,
            content = content,
            contentType = "application/pdf",
            metadata = mapOf(
                "tenant-id" to tenantId,
                "invoice-id" to invoiceId,
                "invoice-number" to invoiceNumber
            )
        )
    }

    /**
     * Sanitize filename to prevent path traversal attacks
     */
    private fun sanitizeFilename(filename: String): String {
        return filename
            .replace("..", "")
            .replace("/", "_")
            .replace("\\", "_")
            .take(255) // Limit filename length
    }

    /**
     * Generate a temporary download URL for a receipt
     */
    suspend fun generateReceiptDownloadUrl(
        tenantId: String,
        expenseId: String,
        filename: String,
        expirationMinutes: Int = 60
    ): String {
        val sanitizedFilename = sanitizeFilename(filename)
        val key = "receipts/$tenantId/$expenseId/$sanitizedFilename"

        return storageService.generatePresignedUrl(bucket, key, expirationMinutes)
    }
}
