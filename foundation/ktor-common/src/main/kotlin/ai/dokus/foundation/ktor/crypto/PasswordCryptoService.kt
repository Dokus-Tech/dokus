package be.police.pulse.foundation.ktor.crypto

import be.police.pulse.domain.model.Password

interface PasswordCryptoService {
    fun hashPassword(password: Password): String
    fun verifyPassword(password: String, hash: String): Boolean
    fun needsRehash(hash: String): Boolean
}