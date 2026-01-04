@file:Suppress(
    "TooGenericExceptionCaught", // Security code must catch all exceptions for graceful fallback
    "SwallowedException", // Intentionally swallowed - fallback behavior is same regardless of exception
    "TooManyFunctions" // Comprehensive storage impl with crypto, platform detection, and file management
)

package tech.dokus.foundation.sstorage

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import tech.dokus.domain.utils.json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.NetworkInterface
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * JVM implementation of SecureStorage using DataStore with AES-GCM encryption.
 * Enhanced security for federal police systems with platform-specific optimizations.
 */
internal class JVMSecureStorage(
    private val serviceName: String
) : SecureStorage() {

    companion object {
        private const val KEY_DERIVATION_ITERATIONS = 100_000
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val HASH_PREFIX_BYTES = 8
    }

    private val osType = detectOS()
    private val storageDir = initializeStorageDirectory()
    private val dataFile = File(storageDir, "${hashServiceName()}.db")
    private val keyStoreFile = File(storageDir, "${hashServiceName()}.ks")
    private val keyAlias = "dokus_${serviceName}_key"

    private val keyStorePassword = deriveKeyStorePassword()

    private val secretKey: SecretKey = initializeSecretKey()

    private val dataStore: DataStore<Map<String, String>> = DataStoreFactory.create(
        serializer = EncryptedSerializer(),
        produceFile = { dataFile },
        corruptionHandler = null,
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.IO)
    )

    init {
        // Apply security permissions after initialization
        secureFiles()
    }

    /**
     * Detects the operating system type
     */
    private fun detectOS(): OSType {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> OSType.WINDOWS
            osName.contains("mac") -> OSType.MACOS
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OSType.LINUX
            else -> OSType.UNKNOWN
        }
    }

    /**
     * Initializes the storage directory with appropriate paths for each OS
     */
    private fun initializeStorageDirectory(): File {
        val userHome = System.getProperty("user.home")

        val dir = when (osType) {
            OSType.WINDOWS -> {
                val appData = System.getenv("LOCALAPPDATA") ?: "$userHome/AppData/Local"
                File(appData, "Dokus/data")
            }

            OSType.MACOS -> {
                File(userHome, "Library/Application Support/tech.dokus/data")
            }

            OSType.LINUX -> {
                val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
                File(xdgDataHome, "tech.dokus/data")
            }

            OSType.UNKNOWN -> {
                // Fallback to hidden directory in user home
                File(userHome, ".dokus/data")
            }
        }

        dir.mkdirs()
        return dir
    }

    /**
     * Generates a hashed name for storage files to obscure their purpose
     */
    private fun hashServiceName(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("$serviceName-dokus".toByteArray())
        return hash.take(HASH_PREFIX_BYTES).joinToString("") { "%02x".format(it) }
    }

    /**
     * Derives a strong password from machine-specific characteristics
     */
    private fun deriveKeyStorePassword(): CharArray {
        val factors = mutableListOf<String>()

        // Collect machine-specific factors
        factors.add(System.getProperty("user.name", "default"))
        factors.add(System.getProperty("os.name", "unknown"))
        factors.add(System.getProperty("os.arch", "unknown"))
        factors.add(System.getProperty("user.home", "/"))
        factors.add(serviceName)

        // Add hardware identifiers
        getMachineId()?.let { factors.add(it) }

        // Add Java installation path as additional entropy
        System.getProperty("java.home")?.let { factors.add(it) }

        // Combine all factors
        val combined = factors.joinToString("|")
        val salt = "Dokus-$serviceName-${osType.name}".toByteArray()

        // Use PBKDF2 to derive a strong password
        val spec = PBEKeySpec(
            combined.toCharArray(),
            salt,
            KEY_DERIVATION_ITERATIONS,
            AES_KEY_SIZE
        )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded

        // Convert to hex string for use as password
        return keyBytes.joinToString("") { "%02x".format(it) }.toCharArray()
    }

    /**
     * Gets a machine-specific identifier
     */
    private fun getMachineId(): String? {
        return try {
            when (osType) {
                OSType.WINDOWS -> {
                    // Try to get Windows machine GUID
                    getWindowsMachineId()
                }

                OSType.MACOS -> {
                    // Try to get macOS hardware UUID
                    getMacOSMachineId()
                }

                OSType.LINUX -> {
                    // Try to get Linux machine ID
                    getLinuxMachineId()
                }

                OSType.UNKNOWN -> {
                    // Fallback to MAC address
                    getMacAddress()
                }
            }
        } catch (e: Exception) {
            // If all else fails, try to get MAC address
            getMacAddress()
        }
    }

    private fun getWindowsMachineId(): String? {
        return try {
            val process = ProcessBuilder("wmic", "csproduct", "get", "UUID")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().useLines { lines ->
                lines.firstOrNull {
                    it.isNotBlank() && !it.contains("UUID")
                }?.trim()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getMacOSMachineId(): String? {
        return try {
            val process = ProcessBuilder("ioreg", "-rd1", "-c", "IOPlatformExpertDevice")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().useLines { lines ->
                lines.firstOrNull { it.contains("IOPlatformUUID") }
                    ?.split("=")
                    ?.getOrNull(1)
                    ?.trim()
                    ?.removeSurrounding("\"")
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getLinuxMachineId(): String? {
        return try {
            val machineIdFile = File("/etc/machine-id")
            if (machineIdFile.exists()) {
                machineIdFile.readText().trim()
            } else {
                // Fallback to /var/lib/dbus/machine-id
                val dbusIdFile = File("/var/lib/dbus/machine-id")
                if (dbusIdFile.exists()) {
                    dbusIdFile.readText().trim()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getMacAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.asSequence()
                ?.filter { !it.isLoopback && !it.isVirtual && it.hardwareAddress != null }
                ?.mapNotNull { iface ->
                    iface.hardwareAddress?.joinToString(":") { "%02x".format(it) }
                }
                ?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Initializes or retrieves the AES secret key from the keystore
     */
    private fun initializeSecretKey(): SecretKey {
        // Use PKCS12 which is more modern and cross-platform
        val keyStore = KeyStore.getInstance("PKCS12")

        if (keyStoreFile.exists()) {
            try {
                keyStoreFile.inputStream().use { input ->
                    keyStore.load(input, keyStorePassword)
                }
            } catch (e: Exception) {
                // If keystore is corrupted, recreate it
                keyStoreFile.delete()
                keyStore.load(null, keyStorePassword)
            }
        } else {
            keyStore.load(null, keyStorePassword)
        }

        // Check if key exists
        return if (keyStore.containsAlias(keyAlias)) {
            keyStore.getKey(keyAlias, keyStorePassword) as SecretKey
        } else {
            // Generate new AES key
            val keyGen = KeyGenerator.getInstance(KEY_ALGORITHM)
            keyGen.init(AES_KEY_SIZE, SecureRandom())
            val newKey = keyGen.generateKey()

            // Store in keystore
            val keyEntry = KeyStore.SecretKeyEntry(newKey)
            val protParam = KeyStore.PasswordProtection(keyStorePassword)
            keyStore.setEntry(keyAlias, keyEntry, protParam)

            // Save keystore
            keyStoreFile.outputStream().use { output ->
                keyStore.store(output, keyStorePassword)
            }

            newKey
        }
    }

    /**
     * Applies platform-specific file permissions for security
     */
    private fun secureFiles() {
        listOf(storageDir, dataFile, keyStoreFile)
            .filter { it.exists() }
            .forEach { secureFile(it) }
    }

    private fun secureFile(file: File) {
        when (osType) {
            OSType.LINUX, OSType.MACOS -> applyPosixPermissions(file)
            OSType.WINDOWS -> applyWindowsPermissions(file)
            OSType.UNKNOWN -> applyBasicPermissions(file)
        }
    }

    private fun applyPosixPermissions(file: File) {
        try {
            val permissions = if (file.isDirectory) {
                PosixFilePermissions.fromString("rwx------")
            } else {
                PosixFilePermissions.fromString("rw-------")
            }
            Files.setPosixFilePermissions(file.toPath(), permissions)
        } catch (e: Exception) {
            applyBasicPermissions(file)
        }
    }

    private fun applyWindowsPermissions(file: File) {
        applyBasicPermissions(file)
        try {
            Files.setAttribute(file.toPath(), "dos:hidden", true)
        } catch (e: Exception) {
            // Ignore if not supported
        }
    }

    private fun applyBasicPermissions(file: File) {
        file.setReadable(false, false)
        file.setReadable(true, true)
        file.setWritable(false, false)
        file.setWritable(true, true)
        if (file.isDirectory) {
            file.setExecutable(false, false)
            file.setExecutable(true, true)
        }
    }

    /**
     * Encrypts data using AES-GCM
     */
    private fun encrypt(plainData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainData)

        // Prepend IV to ciphertext
        return iv + cipherText
    }

    /**
     * Decrypts data using AES-GCM
     */
    private fun decrypt(encryptedData: ByteArray): ByteArray {
        require(encryptedData.size > GCM_IV_LENGTH) { "Invalid encrypted data" }

        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(cipherText)
    }

    /**
     * Custom serializer for encrypted data storage
     */
    private inner class EncryptedSerializer : Serializer<Map<String, String>> {
        override val defaultValue: Map<String, String> = emptyMap()

        override suspend fun readFrom(input: InputStream): Map<String, String> {
            val encryptedBytes = input.readBytes()
            if (encryptedBytes.isEmpty()) return emptyMap()

            return try {
                val decryptedBytes = decrypt(encryptedBytes)
                val jsonString = decryptedBytes.decodeToString()
                json.decodeFromString<Map<String, String>>(jsonString)
            } catch (e: Exception) {
                // Log error in production
                emptyMap()
            }
        }

        override suspend fun writeTo(t: Map<String, String>, output: OutputStream) {
            val jsonString = json.encodeToString(serializer = kotlinx.serialization.serializer(), t)
            val plainBytes = jsonString.encodeToByteArray()
            val encryptedBytes = encrypt(plainBytes)
            output.write(encryptedBytes)
        }
    }

    // SecureStorage implementation methods

    override suspend fun putString(key: String, value: String) {
        dataStore.updateData { current ->
            current.toMutableMap().apply {
                put(key, value)
            }
        }
    }

    override suspend fun getString(key: String): String? {
        return dataStore.data.map { it[key] }.firstOrNull()
    }

    override fun observeString(key: String): Flow<String?> {
        return dataStore.data.map { it[key] }.distinctUntilChanged()
    }

    override suspend fun remove(key: String) {
        dataStore.updateData { current ->
            current.toMutableMap().apply {
                remove(key)
            }
        }
    }

    override suspend fun clear() {
        dataStore.updateData { emptyMap() }
    }

    override suspend fun contains(key: String): Boolean {
        return dataStore.data.map { it.containsKey(key) }.firstOrNull() ?: false
    }

    override suspend fun getAllKeys(): Set<String> {
        return dataStore.data.map { it.keys }.firstOrNull() ?: emptySet()
    }

    /**
     * Cleanup method to securely wipe sensitive data
     */
    fun destroy() {
        try {
            // Clear the data store
            runBlocking { clear() }

            // Securely wipe keystore password from memory
            keyStorePassword.fill(' ')
        } catch (e: Exception) {
            // Log error in production
        }
    }
}

private enum class OSType {
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN
}
