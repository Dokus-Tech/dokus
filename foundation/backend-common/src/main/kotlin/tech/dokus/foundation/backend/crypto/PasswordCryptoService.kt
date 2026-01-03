package tech.dokus.foundation.backend.crypto

import tech.dokus.domain.Password

interface PasswordCryptoService {
    fun hashPassword(password: Password): String
    fun verifyPassword(password: String, hash: String): Boolean
    fun needsRehash(hash: String): Boolean
}
