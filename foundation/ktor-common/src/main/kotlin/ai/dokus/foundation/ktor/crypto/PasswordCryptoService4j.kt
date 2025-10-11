package ai.dokus.foundation.ktor.crypto

import com.password4j.Password
import ai.dokus.foundation.domain.Password as DokusPassword

class PasswordCryptoService4j : PasswordCryptoService {
    override fun hashPassword(password: DokusPassword): String {
        return Password.hash(password.value)
            .addRandomSalt(32)
            .withArgon2()
            .result
    }

    override fun verifyPassword(password: String, hash: String): Boolean {
        return Password.check(password, hash).withArgon2()
    }

    override fun needsRehash(hash: String): Boolean {
        return false
    }
}