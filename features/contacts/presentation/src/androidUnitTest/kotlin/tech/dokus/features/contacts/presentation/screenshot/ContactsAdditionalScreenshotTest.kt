package tech.dokus.features.contacts.presentation.screenshot

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.domain.Email
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.Country
import tech.dokus.domain.ids.VatNumber
import tech.dokus.features.contacts.mvi.CreateContactState
import tech.dokus.features.contacts.mvi.ManualContactFormData
import tech.dokus.features.contacts.presentation.contacts.screen.CreateContactScreen

@RunWith(Parameterized::class)
class ContactsAdditionalScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = viewport.deviceConfig,
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    @Test
    fun createContactScreen_manual() {
        val state = CreateContactState.ManualStep(
            contactType = ClientType.Business,
            formData = ManualContactFormData(
                companyName = LegalName("Acme Corporation"),
                country = Country.Belgium,
                vatNumber = VatNumber("BE0123456789"),
                email = Email("billing@acme.com")
            )
        )
        paparazzi.snapshotAllViewports("CreateContactScreen_manual", viewport) {
            CreateContactPreview(state)
        }
    }
}

@Composable
private fun CreateContactPreview(state: CreateContactState) {
    val snackbarHostState = remember { SnackbarHostState() }
    CreateContactScreen(
        prefillCompanyName = null,
        prefillVat = null,
        prefillAddress = null,
        origin = null,
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = {},
        onExistingContactSelected = {}
    )
}

private fun Paparazzi.snapshotAllViewports(
    baseName: String,
    viewport: ScreenshotViewport,
    content: @Composable () -> Unit
) {
    snapshot("${baseName}_${viewport.displayName}_light") {
        ScreenshotTestWrapper(isDarkMode = false, screenSize = viewport.screenSize) {
            content()
        }
    }
    snapshot("${baseName}_${viewport.displayName}_dark") {
        ScreenshotTestWrapper(isDarkMode = true, screenSize = viewport.screenSize) {
            content()
        }
    }
}
