package tech.dokus.features.banking.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.features.banking.datasource.BankingRemoteDataSource
import tech.dokus.features.banking.datasource.BankingRemoteDataSourceImpl
import tech.dokus.features.banking.usecase.ConfirmTransactionUseCaseImpl
import tech.dokus.features.banking.usecase.CreateExpenseFromTransactionUseCaseImpl
import tech.dokus.features.banking.usecase.GetAccountSummaryUseCaseImpl
import tech.dokus.features.banking.usecase.GetBalanceHistoryUseCaseImpl
import tech.dokus.features.banking.usecase.GetBankTransactionUseCaseImpl
import tech.dokus.features.banking.usecase.GetTransactionSummaryUseCaseImpl
import tech.dokus.features.banking.usecase.IgnoreTransactionUseCaseImpl
import tech.dokus.features.banking.usecase.LinkTransactionUseCaseImpl
import tech.dokus.features.banking.usecase.ListBankAccountsUseCaseImpl
import tech.dokus.features.banking.usecase.ListBankTransactionsUseCaseImpl
import tech.dokus.features.banking.usecase.MarkTransferTransactionUseCaseImpl
import tech.dokus.features.banking.usecase.UndoTransferTransactionUseCaseImpl
import tech.dokus.features.banking.usecases.ConfirmTransactionUseCase
import tech.dokus.features.banking.usecases.CreateExpenseFromTransactionUseCase
import tech.dokus.features.banking.usecases.GetAccountSummaryUseCase
import tech.dokus.features.banking.usecases.GetBalanceHistoryUseCase
import tech.dokus.features.banking.usecases.GetBankTransactionUseCase
import tech.dokus.features.banking.usecases.GetTransactionSummaryUseCase
import tech.dokus.features.banking.usecases.IgnoreTransactionUseCase
import tech.dokus.features.banking.usecases.LinkTransactionUseCase
import tech.dokus.features.banking.usecases.ListBankAccountsUseCase
import tech.dokus.features.banking.usecases.ListBankTransactionsUseCase
import tech.dokus.features.banking.usecases.MarkTransferTransactionUseCase
import tech.dokus.features.banking.usecases.UndoTransferTransactionUseCase

val bankingNetworkModule = module {
    single<BankingRemoteDataSource> {
        BankingRemoteDataSourceImpl(get(), get<DynamicDokusEndpointProvider>())
    }

    singleOf(::ListBankTransactionsUseCaseImpl) bind ListBankTransactionsUseCase::class
    singleOf(::GetBankTransactionUseCaseImpl) bind GetBankTransactionUseCase::class
    singleOf(::GetTransactionSummaryUseCaseImpl) bind GetTransactionSummaryUseCase::class
    singleOf(::GetAccountSummaryUseCaseImpl) bind GetAccountSummaryUseCase::class
    singleOf(::LinkTransactionUseCaseImpl) bind LinkTransactionUseCase::class
    singleOf(::IgnoreTransactionUseCaseImpl) bind IgnoreTransactionUseCase::class
    singleOf(::ConfirmTransactionUseCaseImpl) bind ConfirmTransactionUseCase::class
    singleOf(::CreateExpenseFromTransactionUseCaseImpl) bind CreateExpenseFromTransactionUseCase::class
    singleOf(::ListBankAccountsUseCaseImpl) bind ListBankAccountsUseCase::class
    singleOf(::GetBalanceHistoryUseCaseImpl) bind GetBalanceHistoryUseCase::class
    singleOf(::MarkTransferTransactionUseCaseImpl) bind MarkTransferTransactionUseCase::class
    singleOf(::UndoTransferTransactionUseCaseImpl) bind UndoTransferTransactionUseCase::class
}
