package tech.dokus.features.cashflow.presentation.detail

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_feedback_category_missing_info
import tech.dokus.aura.resources.cashflow_feedback_category_other
import tech.dokus.aura.resources.cashflow_feedback_category_wrong_amount
import tech.dokus.aura.resources.cashflow_feedback_category_wrong_contact
import tech.dokus.aura.resources.cashflow_feedback_category_wrong_date
import tech.dokus.aura.resources.cashflow_feedback_category_wrong_type

val FeedbackCategory.localized: String
    @Composable get() = when (this) {
        FeedbackCategory.WrongContact -> stringResource(Res.string.cashflow_feedback_category_wrong_contact)
        FeedbackCategory.WrongAmount -> stringResource(Res.string.cashflow_feedback_category_wrong_amount)
        FeedbackCategory.WrongDate -> stringResource(Res.string.cashflow_feedback_category_wrong_date)
        FeedbackCategory.WrongType -> stringResource(Res.string.cashflow_feedback_category_wrong_type)
        FeedbackCategory.MissingInfo -> stringResource(Res.string.cashflow_feedback_category_missing_info)
        FeedbackCategory.Other -> stringResource(Res.string.cashflow_feedback_category_other)
    }
