package tech.dokus.features.banking.di

import org.koin.dsl.module

val bankingViewModelModule = module {
    // FlowMVI Containers will be registered here in Phase 5
}

val bankingPresentationModule = module {
    includes(bankingViewModelModule)
}
