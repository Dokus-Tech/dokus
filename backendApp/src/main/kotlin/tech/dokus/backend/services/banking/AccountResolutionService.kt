package tech.dokus.backend.services.banking

import tech.dokus.database.repository.banking.BankAccountRepository
import tech.dokus.domain.enums.BankAccountProvider
import tech.dokus.domain.enums.BankAccountStatus
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching

/**
 * Resolves (or auto-creates) a bank account from statement data.
 *
 * Logic:
 * 1. IBAN null/blank → Unresolved
 * 2. Existing account found by IBAN → Resolved with trust based on account status
 * 3. Not found + balances reconcile → auto-create as Confirmed → Resolved(HIGH)
 * 4. Not found + uncertain → auto-create as PendingReview → Resolved(LOW)
 */
class AccountResolutionService(
    private val bankAccountRepository: BankAccountRepository,
    private val trustCalculator: StatementTrustCalculator,
) {
    private val logger = loggerFor()

    sealed class AccountResolution {
        data class Resolved(
            val accountId: BankAccountId,
            val accountStatus: BankAccountStatus,
        ) : AccountResolution()

        data object Unresolved : AccountResolution()
    }

    /**
     * Resolve a bank account for the given statement.
     *
     * @param tenantId Tenant context
     * @param draftData Bank statement draft data containing account IBAN and balances
     * @param validRowAmounts Sum of validated row signed amounts (minor units)
     */
    suspend fun resolve(
        tenantId: TenantId,
        draftData: BankStatementDraftData,
        validRowAmounts: Long,
        providerAccountId: String? = null,
    ): Result<AccountResolution> = runSuspendCatching {
        // 1. Provider account ID match (first-class for non-IBAN accounts)
        if (!providerAccountId.isNullOrBlank()) {
            val byProvider = bankAccountRepository.findByProviderAccountId(tenantId, providerAccountId)
            if (byProvider != null) {
                logger.info("Resolved providerAccountId {} to account {} for tenant {}", providerAccountId, byProvider.id, tenantId)
                return@runSuspendCatching AccountResolution.Resolved(
                    accountId = byProvider.id,
                    accountStatus = byProvider.status,
                )
            }
        }

        // 2. IBAN match
        val iban = draftData.accountIban
        if (iban == null || iban.value.isBlank()) {
            logger.info("No IBAN in statement for tenant {}, account unresolved", tenantId)
            return@runSuspendCatching AccountResolution.Unresolved
        }

        val existing = bankAccountRepository.findByIban(tenantId, iban)
        if (existing != null) {
            logger.info("Resolved IBAN {} to existing account {} for tenant {}", iban, existing.id, tenantId)
            return@runSuspendCatching AccountResolution.Resolved(
                accountId = existing.id,
                accountStatus = existing.status,
            )
        }

        // Auto-create: determine confidence
        val trustResult = trustCalculator.calculate(
            draftData = draftData,
            validRowAmounts = validRowAmounts,
            accountStatus = null,
        )

        val accountStatus = if (trustResult.trust == StatementTrust.High) {
            BankAccountStatus.Confirmed
        } else {
            BankAccountStatus.PendingReview
        }

        val institutionName = guessInstitutionName(iban)
        val accountName = institutionName ?: "Account ${iban.value.take(8)}"

        val created = bankAccountRepository.createAccount(
            tenantId = tenantId,
            iban = iban,
            name = accountName,
            institutionName = institutionName ?: "Unknown",
            accountType = BankAccountType.Current,
            currency = Currency.Eur,
            provider = BankAccountProvider.Unknown,
            status = accountStatus,
        )

        logger.info(
            "Auto-created account {} (status={}) for IBAN {} tenant {}",
            created.id, accountStatus, iban, tenantId
        )

        AccountResolution.Resolved(
            accountId = created.id,
            accountStatus = accountStatus,
        )
    }

    /**
     * Best-effort institution name from IBAN country + bank code.
     * Belgian IBANs: BE + 2 check digits + 3 bank code + 7 account + 2 check.
     */
    private fun guessInstitutionName(iban: Iban): String? {
        val clean = iban.value.replace(" ", "")
        if (!clean.startsWith("BE") || clean.length < 7) return null

        val bankCode = clean.substring(4, 7)
        return BelgianBankCodes[bankCode]
    }

    companion object {
        private val BelgianBankCodes = mapOf(
            "000" to "Nationale Bank van België",
            "001" to "BNP Paribas Fortis",
            "035" to "ING België",
            "063" to "Belfius",
            "068" to "KBC",
            "096" to "KBC",
            "097" to "Crelan",
            "103" to "Argenta",
            "539" to "KBC Brussels",
            "737" to "Triodos",
            "905" to "Wise",
            "967" to "bunq",
        )
    }
}
