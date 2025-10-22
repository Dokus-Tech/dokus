package ai.dokus.foundation.sstorage

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

/**
 * Keychain-based storage delegate using iOS Keychain Services.
 * Provides hardware-backed secure storage on iOS devices.
 */
internal class KeychainStorageDelegate(private val serviceName: String) : IOSStorageDelegate {
    
    @OptIn(ExperimentalForeignApi::class)
    override fun store(key: String, value: String): Boolean {
        val data = value.encodeToByteArray().toNSData()
        val serviceString = serviceName.toCFString()
        val keyString = key.toCFString()

        val query = CFDictionaryCreateMutable(null, 0, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
        CFDictionaryAddValue(query, kSecAttrAccount, keyString)

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
        CFRelease(query)
        CFRelease(attributes)
        
        return success
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun retrieve(key: String): String? {
        val serviceString = serviceName.toCFString()
        val keyString = key.toCFString()
        val query = CFDictionaryCreateMutable(null, 0, null, null)!!
        
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
        CFDictionaryAddValue(query, kSecAttrAccount, keyString)
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
                } else null
            } else null
        }
        
        // Clean up
        CFRelease(serviceString)
        CFRelease(keyString)
        CFRelease(query)
        
        return result
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun remove(key: String): Boolean {
        val serviceString = serviceName.toCFString()
        val keyString = key.toCFString()
        val query = CFDictionaryCreateMutable(null, 0, null, null)!!
        
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
        CFDictionaryAddValue(query, kSecAttrAccount, keyString)

        val status = SecItemDelete(query)
        
        // Clean up
        CFRelease(serviceString)
        CFRelease(keyString)
        CFRelease(query)
        
        return status == errSecSuccess
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun clear(): Boolean {
        val serviceString = serviceName.toCFString()
        val query = CFDictionaryCreateMutable(null, 0, null, null)!!
        
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)

        val status = SecItemDelete(query)
        
        // Clean up
        CFRelease(serviceString)
        CFRelease(query)
        
        return status == errSecSuccess
    }

    override fun contains(key: String): Boolean {
        return retrieve(key) != null
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun getAllKeys(): Set<String> {
        val serviceString = serviceName.toCFString()
        val query = CFDictionaryCreateMutable(null, 0, null, null)!!
        
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceString)
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