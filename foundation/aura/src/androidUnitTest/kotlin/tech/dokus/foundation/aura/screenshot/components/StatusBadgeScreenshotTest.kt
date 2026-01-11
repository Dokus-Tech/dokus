package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.foundation.aura.components.CashflowStatusBadge
import tech.dokus.foundation.aura.components.CashflowType
import tech.dokus.foundation.aura.components.CashflowTypeBadge
import tech.dokus.foundation.aura.components.DocumentStatusBadge
import tech.dokus.foundation.aura.components.DraftStatusBadge
import tech.dokus.foundation.aura.model.DocumentUiStatus
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class StatusBadgeScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun cashflowStatusBadge_allVariants() {
        paparazzi.snapshotAllViewports("CashflowStatusBadge_allVariants", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CashflowStatusBadge(
                    status = CashflowEntryStatus.Open,
                    detail = "Due in 5 days"
                )
                CashflowStatusBadge(
                    status = CashflowEntryStatus.Paid
                )
                CashflowStatusBadge(
                    status = CashflowEntryStatus.Overdue,
                    detail = "3 days overdue"
                )
                CashflowStatusBadge(
                    status = CashflowEntryStatus.Cancelled
                )
            }
        }
    }

    @Test
    fun cashflowTypeBadge_allVariants() {
        paparazzi.snapshotAllViewports("CashflowTypeBadge_allVariants", viewport) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CashflowTypeBadge(type = CashflowType.CashIn)
                CashflowTypeBadge(type = CashflowType.CashOut)
            }
        }
    }

    @Test
    fun documentStatusBadge_allVariants() {
        paparazzi.snapshotAllViewports("DocumentStatusBadge_allVariants", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentStatusBadge(status = DocumentUiStatus.Queued)
                DocumentStatusBadge(status = DocumentUiStatus.Processing)
                DocumentStatusBadge(status = DocumentUiStatus.Review)
                DocumentStatusBadge(status = DocumentUiStatus.Ready)
                DocumentStatusBadge(status = DocumentUiStatus.Failed)
            }
        }
    }

    @Test
    fun draftStatusBadge_allVariants() {
        paparazzi.snapshotAllViewports("DraftStatusBadge_allVariants", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DraftStatusBadge(status = DraftStatus.NeedsReview)
                DraftStatusBadge(status = DraftStatus.Ready)
                DraftStatusBadge(status = DraftStatus.Confirmed)
                DraftStatusBadge(status = DraftStatus.Rejected)
            }
        }
    }
}
