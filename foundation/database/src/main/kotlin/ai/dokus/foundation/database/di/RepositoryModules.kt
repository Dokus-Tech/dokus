package ai.dokus.foundation.database.di

import ai.dokus.foundation.database.repository.ai.ChatRepositoryImpl
import ai.dokus.foundation.database.repository.ai.DocumentChunksRepository
import ai.dokus.foundation.database.repository.auth.AddressRepository
import ai.dokus.foundation.database.repository.auth.InvitationRepository
import ai.dokus.foundation.database.repository.auth.PasswordResetTokenRepository
import ai.dokus.foundation.database.repository.auth.RefreshTokenRepository
import ai.dokus.foundation.database.repository.auth.TenantRepository
import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.foundation.database.repository.banking.BankingRepository
import ai.dokus.foundation.database.repository.cashflow.BillRepository
import ai.dokus.foundation.database.repository.cashflow.CashflowRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentDraftRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentIngestionRunRepository
import ai.dokus.foundation.database.repository.contacts.ContactNoteRepository
import ai.dokus.foundation.database.repository.contacts.ContactRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentRepository
import ai.dokus.foundation.database.repository.cashflow.ExpenseRepository
import ai.dokus.foundation.database.repository.cashflow.InvoiceNumberRepository
import ai.dokus.foundation.database.repository.cashflow.InvoiceRepository
import ai.dokus.foundation.database.repository.payment.PaymentRepository
import ai.dokus.foundation.database.services.InvoiceNumberGenerator
import ai.dokus.foundation.database.repository.peppol.PeppolSettingsRepository
import ai.dokus.foundation.database.repository.peppol.PeppolTransmissionRepository
import ai.dokus.foundation.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.repository.ChatRepository
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.foundation.backend.crypto.CredentialCryptoService
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Auth repositories module.
 * Provides repositories for user authentication and tenant management.
 */
val repositoryModuleAuth = module {
    single { TenantRepository() }
    single { AddressRepository() }
    single { UserRepository(get()) }
    single { RefreshTokenRepository() }
    single { PasswordResetTokenRepository() }
    single { InvitationRepository() }
}

/**
 * Cashflow repositories module.
 * Provides repositories for invoices, expenses, bills, clients, and documents.
 */
val repositoryModuleCashflow = module {
    single { DocumentRepository() }
    single { DocumentIngestionRunRepository() }
    single { DocumentDraftRepository() }
    single { InvoiceNumberRepository() }
    single { InvoiceNumberGenerator(get()) }
    single { InvoiceRepository(get()) }
    single { ExpenseRepository() }
    single { BillRepository() }
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
 * Processor repositories module.
 * Provides repositories for document ingestion operations.
 */
val repositoryModuleProcessor = module {
    single { ProcessorIngestionRepository() }
}

/**
 * Audit repositories module.
 * Provides repositories for audit logging.
 */
/**
 * Banking repositories module.
 * Provides repositories for bank connections and transactions.
 */
val repositoryModuleBanking = module {
    single { BankingRepository() }
}

/**
 * Payment repositories module.
 * Provides repositories for payment records.
 */
val repositoryModulePayment = module {
    single { PaymentRepository() }
}

/**
 * Reporting repositories module.
 * Provides repositories for VAT returns and reporting.
 */
/**
 * Contacts repositories module.
 * Provides repositories for unified contact management (customers AND vendors).
 */
val repositoryModuleContacts = module {
    single { ContactRepository() }
    single { ContactNoteRepository() }
}

/**
 * AI repositories module.
 * Provides repositories for document chunks (RAG) and chat messages.
 */
val repositoryModuleAI = module {
    single { DocumentChunksRepository() } bind ChunkRepository::class
    single { ChatRepositoryImpl() } bind ChatRepository::class
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
        repositoryModulePeppol,
        repositoryModuleProcessor,
        repositoryModuleBanking,
        repositoryModulePayment,
        repositoryModuleContacts,
        repositoryModuleAI
    )
}
