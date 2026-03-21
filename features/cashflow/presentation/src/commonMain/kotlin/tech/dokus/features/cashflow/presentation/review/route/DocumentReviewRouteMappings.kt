package tech.dokus.features.cashflow.presentation.review.route

import tech.dokus.features.cashflow.presentation.review.DocumentReviewQueueContext
import tech.dokus.navigation.destinations.CashFlowDestination

internal fun CashFlowDestination.DocumentReview.toQueueContext(): DocumentReviewQueueContext =
    DocumentReviewQueueContext(source = queueSource)
