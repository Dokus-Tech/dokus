package tech.dokus.database.di

import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.database.repository.ai.ChatRepositoryImpl
import tech.dokus.database.repository.ai.DocumentChunksRepository
import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.InvitationRepository
import tech.dokus.database.repository.auth.PasswordResetTokenRepository
import tech.dokus.database.repository.auth.RefreshTokenRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.banking.BankingRepository
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CashflowRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceNumberRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.contacts.ContactAddressRepository
import tech.dokus.database.repository.contacts.ContactNoteRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.payment.PaymentRepository
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.database.repository.peppol.PeppolTransmissionRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.database.services.InvoiceNumberGenerator
import tech.dokus.domain.repository.ChatRepository
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.foundation.backend.crypto.CredentialCryptoService

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
    single { CashflowEntriesRepository() }
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
 * Includes address management via ContactAddressRepository.
 */
val repositoryModuleContacts = module {
    single { ContactRepository() }
    single { ContactAddressRepository() }
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
