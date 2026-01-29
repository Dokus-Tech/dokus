package tech.dokus.features.auth.usecases

import kotlinx.coroutines.flow.StateFlow
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.Name
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.User
import tech.dokus.features.auth.gateway.AuthGateway

internal class AuthSessionUseCaseImpl(
    private val authGateway: AuthGateway
) : AuthSessionUseCase {
    override val isAuthenticated: StateFlow<Boolean> = authGateway.isAuthenticated

    override suspend fun initialize() {
        authGateway.initialize()
    }
}

internal class GetCurrentUserUseCaseImpl(
    private val authGateway: AuthGateway
) : GetCurrentUserUseCase {
    override suspend fun invoke(): Result<User> {
        return authGateway.getCurrentUser()
    }
}

internal class UpdateProfileUseCaseImpl(
    private val authGateway: AuthGateway
) : UpdateProfileUseCase {
    override suspend fun invoke(firstName: Name?, lastName: Name?): Result<User> {
        return authGateway.updateProfile(firstName, lastName)
    }
}

internal class HasFreelancerTenantUseCaseImpl(
    private val authGateway: AuthGateway
) : HasFreelancerTenantUseCase {
    override suspend fun invoke(): Result<Boolean> {
        return authGateway.hasFreelancerTenant()
    }
}

internal class CreateTenantUseCaseImpl(
    private val authGateway: AuthGateway
) : CreateTenantUseCase {
    override suspend fun invoke(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: SubscriptionTier,
        language: Language,
        vatNumber: VatNumber,
        address: UpsertTenantAddressRequest,
    ): Result<Tenant> {
        return authGateway.createTenant(
            type = type,
            legalName = legalName,
            displayName = displayName,
            plan = plan,
            language = language,
            vatNumber = vatNumber,
            address = address
        )
    }
}
