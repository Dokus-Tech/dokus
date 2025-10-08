package be.police.pulse.foundation.ktor.crypto

import com.password4j.Password
import be.police.pulse.domain.model.Password as PulsePassword

class PasswordCryptoService4j : PasswordCryptoService {
    override fun hashPassword(password: PulsePassword): String {
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