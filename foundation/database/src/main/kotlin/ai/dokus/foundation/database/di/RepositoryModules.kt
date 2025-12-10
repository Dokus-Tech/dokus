package ai.dokus.foundation.database.di

import ai.dokus.foundation.database.repository.auth.PasswordResetTokenRepository
import ai.dokus.foundation.database.repository.auth.RefreshTokenRepository
import ai.dokus.foundation.database.repository.auth.TenantRepository
import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.foundation.database.repository.cashflow.AttachmentRepository
import ai.dokus.foundation.database.repository.cashflow.BillRepository
import ai.dokus.foundation.database.repository.cashflow.CashflowRepository
import ai.dokus.foundation.database.repository.cashflow.ClientRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentProcessingRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentRepository
import ai.dokus.foundation.database.repository.cashflow.ExpenseRepository
import ai.dokus.foundation.database.repository.cashflow.InvoiceRepository
import ai.dokus.foundation.database.repository.peppol.PeppolSettingsRepository
import ai.dokus.foundation.database.repository.peppol.PeppolTransmissionRepository
import ai.dokus.foundation.ktor.crypto.CredentialCryptoService
import org.koin.dsl.module

/**
 * Auth repositories module.
 * Provides repositories for user authentication and tenant management.
 */
val repositoryModuleAuth = module {
    single { TenantRepository() }
    single { UserRepository(get()) }
    single { RefreshTokenRepository() }
    single { PasswordResetTokenRepository() }
}

/**
 * Cashflow repositories module.
 * Provides repositories for invoices, expenses, bills, clients, and documents.
 */
val repositoryModuleCashflow = module {
    single { AttachmentRepository() }
    single { DocumentRepository() }
    single { DocumentProcessingRepository() }
    single { InvoiceRepository() }
    single { ExpenseRepository() }
    single { BillRepository() }
    single { ClientRepository() }
    single { CashflowRepository(get(), get()) }
}

/**
 * Peppol repositories module.
 * Provides repositories for Peppol settings and transmissions.
 *
 * NOTE: Requires CredentialCryptoService to be registered in the application.
 * The calling service must register CredentialCryptoService before including this module.
 */
val repositoryModulePeppol = module {
    single { PeppolSettingsRepository(get<CredentialCryptoService>()) }
    single { PeppolTransmissionRepository() }
}

/**
 * Combined repository module including all domain repositories.
 *
 * Usage:
 * ```kotlin
 * install(Koin) {
 *     modules(
 *         repositoryModules,
 *         // ... other modules
 *     )
 * }
 * ```
 *
 * NOTE: When using repositoryModules, ensure CredentialCryptoService is registered
 * before this module (required by PeppolSettingsRepository).
 */
val repositoryModules = module {
    includes(
        repositoryModuleAuth,
        repositoryModuleCashflow,
        repositoryModulePeppol
    )
}
