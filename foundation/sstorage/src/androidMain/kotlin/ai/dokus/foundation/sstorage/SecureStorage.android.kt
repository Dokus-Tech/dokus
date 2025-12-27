package ai.dokus.foundation.sstorage

import tech.dokus.domain.model.common.Feature
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android implementation of SecureStorage using DataStore (Okio) with AES-GCM encryption.
 * Secret key is stored in Android Keystore as a hardware-backed AES key.
 */
internal class AndroidSecureStorage(
    context: Context,
    private val serviceName: String
) : SecureStorage() {
    private val keyAlias: String = serviceName.lowercase().replace(" ", "_")
    private val fileName: String = serviceName.lowercase().replace(" ", "_") + ".datastore"

    private val json = Json { ignoreUnknownKeys = true }
    private val secretKey: SecretKey by lazy { getOrCreateAesKey() }
    private val file: File = File(context.filesDir, fileName)

    private val dataStore: DataStore<Map<String, String>> by lazy {
        DataStoreFactory.create(
            serializer = object : Serializer<Map<String, String>> {
                override val defaultValue: Map<String, String> = emptyMap()
                override suspend fun readFrom(input: InputStream): Map<String, String> {
                    val encrypted = input.readBytes()
                    if (encrypted.isEmpty()) return emptyMap()
                    val decrypted = decrypt(encrypted)
                    return runCatching { json.decodeFromString<Map<String, String>>(decrypted.decodeToString()) }
                        .getOrElse { emptyMap() }
                }

                override suspend fun writeTo(t: Map<String, String>, output: OutputStream) {
                    val clear = json.encodeToString(t).encodeToByteArray()
                    val encrypted = encrypt(clear)
                    output.write(encrypted)
                }
            },
            produceFile = { file },
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO)
        )
    }

    // region Crypto
    private fun getOrCreateAesKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(serviceName, null) as? SecretKey
        if (existing != null) return existing
        val keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            serviceName,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain)
        // Store IV + ciphertext
        return iv + cipherText
    }

    private fun decrypt(bytes: ByteArray): ByteArray {
        require(bytes.size > 12) { "Encrypted payload too short" }
        val iv = bytes.copyOfRange(0, 12)
        val cipherText = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }
    // endregion

    // region API
    override suspend fun putString(key: String, value: String) {
        dataStore.updateData { current -> current.toMutableMap().apply { put(key, value) } }
    }

    override suspend fun getString(key: String): String? {
        return dataStore.data.map { it[key] }.firstOrNull()
    }

    override fun observeString(key: String): Flow<String?> {
        return dataStore.data.map { it[key] }.distinctUntilChanged()
    }

    override suspend fun remove(key: String) {
        dataStore.updateData { current -> current.toMutableMap().apply { remove(key) } }
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
    // endregion
}

/**
 * Android-specific factory function
 */
actual fun createSecureStorage(context: Any?, feature: Feature): SecureStorage {
    require(context is Context) { "Android implementation requires a Context" }
    return AndroidSecureStorage(context, feature.serviceName)
}