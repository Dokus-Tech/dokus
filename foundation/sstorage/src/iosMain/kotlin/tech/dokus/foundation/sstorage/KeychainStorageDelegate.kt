package tech.dokus.foundation.sstorage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessGroup
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Keychain-based storage delegate using iOS Keychain Services.
 * Provides hardware-backed secure storage on iOS devices.
 */
internal class KeychainStorageDelegate(
    private val serviceName: String,
    private val accessGroup: String? = null
) : IOSStorageDelegate {

    @OptIn(ExperimentalForeignApi::class)
    override fun store(key: String, value: String): Boolean {
        val data = value.encodeToByteArray().toNSData()
        val serviceString = serviceName.toCFString()
        val keyString = key.toCFString()
        val accessGroupString = accessGroup?.toCFString()

        val query = CFDictionaryCreateMutable(null, 0, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
        CFDictionaryAddValue(query, kSecAttrAccount, keyString)
        accessGroupString?.let { CFDictionaryAddValue(query, kSecAttrAccessGroup, it) }

        val attributes = CFDictionaryCreateMutable(null, 0, null, null)!!
        CFDictionaryAddValue(attributes, kSecValueData, CFBridgingRetain(data))

        var status = SecItemUpdate(query, attributes)

        // If item doesn't exist, add it
        if (status == errSecItemNotFound) {
            CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(data))
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
            status = SecItemAdd(query, null)
        }

        val success = status == errSecSuccess

        // Clean up CF strings
        CFRelease(serviceString)
        CFRelease(keyString)
        accessGroupString?.let { CFRelease(it) }
        CFRelease(query)
        CFRelease(attributes)

        return success
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun retrieve(key: String): String? {
        val serviceString = serviceName.toCFString()
        val keyString = key.toCFString()
        val accessGroupString = accessGroup?.toCFString()
        val query = CFDictionaryCreateMutable(null, 0, null, null)!!

        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
        CFDictionaryAddValue(query, kSecAttrAccount, keyString)
        accessGroupString?.let { CFDictionaryAddValue(query, kSecAttrAccessGroup, it) }
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val result = memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)

            if (status == errSecSuccess) {
                val cfData = result.value
                if (cfData != null) {
                    val nsData = CFBridgingRelease(cfData) as? NSData
                    nsData?.let { data ->
                        NSString.create(data, NSUTF8StringEncoding)?.toString()
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }

        // Clean up
        CFRelease(serviceString)
        CFRelease(keyString)
        accessGroupString?.let { CFRelease(it) }
        CFRelease(query)

        return result
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun remove(key: String): Boolean {
        val serviceString = serviceName.toCFString()
        val keyString = key.toCFString()
        val accessGroupString = accessGroup?.toCFString()
        val query = CFDictionaryCreateMutable(null, 0, null, null)!!

        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
        CFDictionaryAddValue(query, kSecAttrAccount, keyString)
        accessGroupString?.let { CFDictionaryAddValue(query, kSecAttrAccessGroup, it) }

        val status = SecItemDelete(query)

        // Clean up
        CFRelease(serviceString)
        CFRelease(keyString)
        accessGroupString?.let { CFRelease(it) }
        CFRelease(query)

        return status == errSecSuccess
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun clear(): Boolean {
        val serviceString = serviceName.toCFString()
        val accessGroupString = accessGroup?.toCFString()
        val query = CFDictionaryCreateMutable(null, 0, null, null)!!

        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
        accessGroupString?.let { CFDictionaryAddValue(query, kSecAttrAccessGroup, it) }

        val status = SecItemDelete(query)

        // Clean up
        CFRelease(serviceString)
        accessGroupString?.let { CFRelease(it) }
        CFRelease(query)

        return status == errSecSuccess
    }

    override fun contains(key: String): Boolean {
        return retrieve(key) != null
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun getAllKeys(): Set<String> {
        val serviceString = serviceName.toCFString()
        val accessGroupString = accessGroup?.toCFString()
        val query = CFDictionaryCreateMutable(null, 0, null, null)!!

        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
        accessGroupString?.let { CFDictionaryAddValue(query, kSecAttrAccessGroup, it) }
        CFDictionaryAddValue(query, kSecReturnAttributes, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitAll)

        val keys = memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)

            if (status == errSecSuccess && result.value != null) {
                val array = (CFBridgingRelease(result.value) as? NSArray)
                array?.let { items ->
                    (0 until items.count.toInt()).mapNotNull { index ->
                        val item = items.objectAtIndex(index.toULong()) as? NSDictionary
                        item?.objectForKey(kSecAttrAccount) as? String
                    }.toSet()
                } ?: emptySet()
            } else {
                emptySet()
            }
        }

        // Clean up
        CFRelease(serviceString)
        accessGroupString?.let { CFRelease(it) }
        CFRelease(query)

        return keys
    }
}

// Helper extensions for iOS keychain operations
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.toULong()
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toCFString(): CFStringRef {
    return CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)!!
}
