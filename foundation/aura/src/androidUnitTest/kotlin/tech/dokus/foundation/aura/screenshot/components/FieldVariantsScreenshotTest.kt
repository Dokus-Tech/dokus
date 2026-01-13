package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.PhoneNumber
import tech.dokus.foundation.aura.components.fields.PDateField
import tech.dokus.foundation.aura.components.fields.PDropdownField
import tech.dokus.foundation.aura.components.fields.PTextFieldEmail
import tech.dokus.foundation.aura.components.fields.PTextFieldFree
import tech.dokus.foundation.aura.components.fields.PTextFieldName
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldPhone
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.fields.PTextFieldTaxNumber
import tech.dokus.foundation.aura.components.fields.PTextFieldWorkspaceName
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class FieldVariantsScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun identityFields() {
        paparazzi.snapshotAllViewports("FieldVariants_identity", viewport) {
            FieldGroup {
                PTextFieldEmail(
                    fieldName = "Email",
                    value = Email("user@example.com"),
                    onValueChange = {}
                )
                PTextFieldPassword(
                    fieldName = "Password",
                    value = Password("password123"),
                    onValueChange = {}
                )
            }
        }
    }

    @Test
    fun contactFields() {
        paparazzi.snapshotAllViewports("FieldVariants_contact", viewport) {
            FieldGroup {
                PTextFieldName(
                    fieldName = "Full Name",
                    value = Name("Ada Lovelace"),
                    onValueChange = {}
                )
                PTextFieldPhone(
                    fieldName = "Phone",
                    value = PhoneNumber("+32 470 12 34 56"),
                    onValueChange = {}
                )
            }
        }
    }

    @Test
    fun workspaceFields() {
        paparazzi.snapshotAllViewports("FieldVariants_workspace", viewport) {
            FieldGroup {
                PTextFieldWorkspaceName(
                    fieldName = "Workspace",
                    value = "Dokus Studio",
                    onValueChange = {}
                )
                PTextFieldTaxNumber(
                    fieldName = "VAT Number",
                    value = "BE0123456789",
                    onValueChange = {}
                )
                PTextFieldStandard(
                    fieldName = "Address",
                    value = "Rue de la Loi 16",
                    onValueChange = {}
                )
                PTextFieldFree(
                    fieldName = "Notes",
                    value = "Include the reference number on all invoices.",
                    onValueChange = {}
                )
            }
        }
    }

    @Test
    fun dropdownAndDateFields() {
        val options = listOf("Monthly", "Quarterly", "Yearly")
        paparazzi.snapshotAllViewports("FieldVariants_dropdown_date", viewport) {
            FieldGroup {
                PDropdownField(
                    label = "Frequency",
                    value = options.first(),
                    onValueChange = {},
                    options = options,
                    optionLabel = { it },
                    placeholder = "Select"
                )
                PDateField(
                    label = "Due Date",
                    value = LocalDate(2024, 2, 1),
                    onValueChange = {}
                )
            }
        }
    }
}

@Composable
private fun FieldGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
    }
}
