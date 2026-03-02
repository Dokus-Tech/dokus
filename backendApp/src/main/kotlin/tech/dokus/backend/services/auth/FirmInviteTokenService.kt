@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package tech.dokus.backend.services.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.foundation.backend.config.JwtConfig
import java.time.Instant as JavaInstant
import java.util.Date

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
    private val algorithm = Algorithm.HMAC256(jwtConfig.secret)

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

        if (expiresAt.isBefore(JavaInstant.ofEpochSecond(Clock.System.now().epochSeconds))) {
            throw DokusException.BadRequest("Invite link expired")
        }

        return FirmInviteTokenPayload(
            firmId = FirmId.parse(firmRaw),
            expiresAt = Instant.fromEpochSeconds(expiresAt.epochSecond, expiresAt.nano)
        )
    }
}
