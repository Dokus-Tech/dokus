package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.expense_category_hardware
import tech.dokus.aura.resources.expense_category_insurance
import tech.dokus.aura.resources.expense_category_marketing
import tech.dokus.aura.resources.expense_category_meals
import tech.dokus.aura.resources.expense_category_office_supplies
import tech.dokus.aura.resources.expense_category_other
import tech.dokus.aura.resources.expense_category_professional_services
import tech.dokus.aura.resources.expense_category_rent
import tech.dokus.aura.resources.expense_category_software
import tech.dokus.aura.resources.expense_category_telecommunications
import tech.dokus.aura.resources.expense_category_travel
import tech.dokus.aura.resources.expense_category_utilities
import tech.dokus.aura.resources.expense_category_vehicle
import tech.dokus.domain.enums.ExpenseCategory

val ExpenseCategory.localized: String
    @Composable get() = when (this) {
        ExpenseCategory.OfficeSupplies -> stringResource(Res.string.expense_category_office_supplies)
        ExpenseCategory.Travel -> stringResource(Res.string.expense_category_travel)
        ExpenseCategory.Meals -> stringResource(Res.string.expense_category_meals)
        ExpenseCategory.Software -> stringResource(Res.string.expense_category_software)
        ExpenseCategory.Hardware -> stringResource(Res.string.expense_category_hardware)
        ExpenseCategory.Utilities -> stringResource(Res.string.expense_category_utilities)
        ExpenseCategory.Rent -> stringResource(Res.string.expense_category_rent)
        ExpenseCategory.Insurance -> stringResource(Res.string.expense_category_insurance)
        ExpenseCategory.Marketing -> stringResource(Res.string.expense_category_marketing)
        ExpenseCategory.ProfessionalServices -> stringResource(Res.string.expense_category_professional_services)
        ExpenseCategory.Telecommunications -> stringResource(Res.string.expense_category_telecommunications)
        ExpenseCategory.Vehicle -> stringResource(Res.string.expense_category_vehicle)
        ExpenseCategory.Other -> stringResource(Res.string.expense_category_other)
    }
