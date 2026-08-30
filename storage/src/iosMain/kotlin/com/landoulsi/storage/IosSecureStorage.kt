package com.landoulsi.storage

import dev.zacsweers.metro.Inject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFAutorelease
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus
import platform.darwin.noErr

/**
 * Keychain-backed [SecureStorage]. Query-dictionary/CFTypeRef plumbing follows the pattern
 * verified in Liftric/KVault (add-or-update via existence check, memScoped CFTypeRefVar for
 * SecItemCopyMatching's result, CFBridgingRetain/Release for NS<->CF handoff).
 */
@OptIn(ExperimentalForeignApi::class)
@Inject
class IosSecureStorage : SecureStorage {

    override fun getString(key: String): String? {
        val query = query(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to CFBridgingRetain(SERVICE_NAME),
            kSecAttrAccount to CFBridgingRetain(key),
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        )

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (!status.isSuccess()) return@memScoped null
            (CFBridgingRelease(result.value) as? NSData)?.toKotlinString()
        }
    }

    override fun putString(key: String, value: String) {
        if (getString(key) != null) {
            val matchQuery = query(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to CFBridgingRetain(SERVICE_NAME),
                kSecAttrAccount to CFBridgingRetain(key)
            )
            val updateFields = query(kSecValueData to CFBridgingRetain(value.toNSData()))
            SecItemUpdate(matchQuery, updateFields)
        } else {
            val addQuery = query(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to CFBridgingRetain(SERVICE_NAME),
                kSecAttrAccount to CFBridgingRetain(key),
                kSecValueData to CFBridgingRetain(value.toNSData()),
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock
            )
            SecItemAdd(addQuery, null)
        }
    }

    override fun remove(key: String) {
        val query = query(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to CFBridgingRetain(SERVICE_NAME),
            kSecAttrAccount to CFBridgingRetain(key)
        )
        SecItemDelete(query)
    }

    private fun query(vararg pairs: Pair<CFTypeRef?, CFTypeRef?>): CFDictionaryRef? {
        val dictionary = CFDictionaryCreateMutable(null, pairs.size.convert(), null, null)
        pairs.forEach { (key, value) -> CFDictionaryAddValue(dictionary, key, value) }
        CFAutorelease(dictionary)
        return dictionary
    }

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    private fun String.toNSData(): NSData? =
        NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    private fun NSData.toKotlinString(): String? =
        NSString.create(this, NSUTF8StringEncoding)?.toString()

    private fun OSStatus.isSuccess(): Boolean = toUInt() == noErr

    private companion object {
        const val SERVICE_NAME = "com.landoulsi.sdk.secure_settings"
    }
}
