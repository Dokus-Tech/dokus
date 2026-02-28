package tech.dokus.features.cashflow.presentation.cashflow.route

import tech.dokus.features.cashflow.mvi.CreateInvoiceAction
import tech.dokus.navigation.destinations.ContactsDestination

internal fun CreateInvoiceAction.NavigateToCreateContact.toCreateContactDestination(): ContactsDestination.CreateContact {
    return ContactsDestination.CreateContact(
        prefillCompanyName = prefillCompanyName,
        prefillVat = prefillVat,
        prefillAddress = prefillAddress,
        origin = origin
    )
}
