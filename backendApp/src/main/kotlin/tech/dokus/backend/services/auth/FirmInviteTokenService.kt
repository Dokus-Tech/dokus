@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package tech.dokus.backend.services.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import kotlinx.datetime.Instant
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.foundation.backend.config.JwtConfig
import java.util.Date
import java.time.Instant as JavaInstant

private const val TOKEN_ISSUER = "dokus-firm-invite"
private const val CLAIM_FIRM_ID = "firm_id"
private const val CLAIM_TOKEN_TYPE = "token_type"
private const val TOKEN_TYPE_INVITE = "firm_access_invite"

data class FirmInviteTokenPayload(
    val firmId: FirmId,
    val expiresAt: Instant,
)

class FirmInviteTokenService(
    jwtConfig: JwtConfig,
) {
    // Uses the same HMAC key as auth JWTs. Cross-use is prevented by the distinct
    // issuer ("dokus-firm-invite") and required token_type claim ("firm_access_invite")
    // enforced in parse(). If the main JWT verifier ever relaxes issuer/claim checks,
    // this should be revisited (consider a dedicated secret or derived key).
    private val algorithm = Algorithm.HMAC256(jwtConfig.secret)

    // Tokens are firm-scoped, not tenant-scoped. Any authenticated tenant Owner/Admin
    // who receives this link can accept it. This is intentional to support sharing
    // invite links via email, Slack, etc.
    fun generateToken(
        firmId: FirmId,
        expiresAt: Instant,
    ): String {
        return JWT.create()
            .withIssuer(TOKEN_ISSUER)
            .withSubject(firmId.toString())
            .withClaim(CLAIM_FIRM_ID, firmId.toString())
            .withClaim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_INVITE)
            .withExpiresAt(Date.from(JavaInstant.ofEpochSecond(expiresAt.epochSeconds)))
            .sign(algorithm)
    }

    fun parse(token: String): FirmInviteTokenPayload {
        val verifier = JWT.require(algorithm)
            .withIssuer(TOKEN_ISSUER)
            .withClaim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_INVITE)
            .build()

        val decoded = try {
            verifier.verify(token)
        } catch (error: JWTVerificationException) {
            throw DokusException.BadRequest("Invalid or expired invite link")
        }

        val firmRaw = decoded.getClaim(CLAIM_FIRM_ID).asString()
            ?: throw DokusException.BadRequest("Invalid invite link payload")
        val expiresAt = decoded.expiresAt?.toInstant()
            ?: throw DokusException.BadRequest("Invalid invite link expiration")

        return FirmInviteTokenPayload(
            firmId = FirmId.parse(firmRaw),
            expiresAt = Instant.fromEpochSeconds(expiresAt.epochSecond, expiresAt.nano)
        )
    }
}
