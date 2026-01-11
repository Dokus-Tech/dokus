package tech.dokus.features.contacts.presentation.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test
import tech.dokus.domain.enums.Language
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Screenshot tests for contacts screens.
 * Tests simplified versions of screens to capture UI layouts.
 */
class ContactsScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 600,
            screenHeight = 960,
            density = Density.XXHIGH,
            softButtons = false
        ),
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    private val lightTheme = PreviewParameters(
        isDarkMode = false,
        language = Language.En
    )

    private val darkTheme = PreviewParameters(
        isDarkMode = true,
        language = Language.En
    )

    @Test
    fun contactsScreen_empty() {
        paparazzi.snapshot("ContactsScreen_empty_light") {
            TestWrapper(parameters = lightTheme) {
                ContactsListContent(contacts = emptyList())
            }
        }
        paparazzi.snapshot("ContactsScreen_empty_dark") {
            TestWrapper(parameters = darkTheme) {
                ContactsListContent(contacts = emptyList())
            }
        }
    }

    @Test
    fun contactsScreen_withContacts() {
        val sampleContacts = listOf(
            Contact("Acme Corporation", "contact@acme.com", "Client"),
            Contact("TechStart Ltd", "info@techstart.com", "Supplier"),
            Contact("Global Consulting", "hello@global.com", "Client"),
            Contact("Local Services", "admin@local.com", "Partner")
        )

        paparazzi.snapshot("ContactsScreen_withContacts_light") {
            TestWrapper(parameters = lightTheme) {
                ContactsListContent(contacts = sampleContacts)
            }
        }
        paparazzi.snapshot("ContactsScreen_withContacts_dark") {
            TestWrapper(parameters = darkTheme) {
                ContactsListContent(contacts = sampleContacts)
            }
        }
    }

    @Test
    fun contactDetailsScreen() {
        val contact = Contact(
            name = "Acme Corporation",
            email = "contact@acme.com",
            type = "Client"
        )

        paparazzi.snapshot("ContactDetailsScreen_light") {
            TestWrapper(parameters = lightTheme) {
                ContactDetailsContent(contact = contact)
            }
        }
        paparazzi.snapshot("ContactDetailsScreen_dark") {
            TestWrapper(parameters = darkTheme) {
                ContactDetailsContent(contact = contact)
            }
        }
    }

    @Test
    fun contactFormScreen() {
        paparazzi.snapshot("ContactFormScreen_light") {
            TestWrapper(parameters = lightTheme) {
                ContactFormContent()
            }
        }
        paparazzi.snapshot("ContactFormScreen_dark") {
            TestWrapper(parameters = darkTheme) {
                ContactFormContent()
            }
        }
    }
}

private data class Contact(
    val name: String,
    val email: String,
    val type: String
)

@Composable
private fun ContactsListContent(contacts: List<Contact>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PTopAppBar(
            title = "Contacts",
            navController = null,
            showBackButton = false
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${contacts.size} contacts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            POutlinedButton(
                text = "Add Contact",
                onClick = {}
            )
        }

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No contacts yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your first contact to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contacts.forEach { contact ->
                    DokusCardSurface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {}
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompanyAvatarImage(
                                avatarUrl = null,
                                initial = contact.name.firstOrNull()?.toString() ?: "?",
                                size = AvatarSize.Medium
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = contact.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = contact.type,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactDetailsContent(contact: Contact) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PTopAppBar(
            title = "Contact Details",
            navController = null,
            showBackButton = true
        )

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompanyAvatarImage(
                avatarUrl = null,
                initial = contact.name.firstOrNull()?.toString() ?: "?",
                size = AvatarSize.ExtraLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = contact.type,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        DokusCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column {
                ContactInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = contact.email
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                ContactInfoRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = "+32 123 456 789"
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                ContactInfoRow(
                    icon = Icons.Default.Business,
                    label = "VAT Number",
                    value = "BE0123456789"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            POutlinedButton(
                text = "Edit",
                modifier = Modifier.weight(1f),
                onClick = {}
            )
            PPrimaryButton(
                text = "Create Invoice",
                modifier = Modifier.weight(1f),
                onClick = {}
            )
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ContactFormContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PTopAppBar(
            title = "New Contact",
            navController = null,
            showBackButton = true
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DokusCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Company Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    FormField(label = "Company Name", placeholder = "Enter company name")
                    Spacer(modifier = Modifier.height(12.dp))
                    FormField(label = "VAT Number", placeholder = "BE0123456789")
                    Spacer(modifier = Modifier.height(12.dp))
                    FormField(label = "Email", placeholder = "contact@company.com")
                    Spacer(modifier = Modifier.height(12.dp))
                    FormField(label = "Phone", placeholder = "+32 123 456 789")
                }
            }

            DokusCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Address",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    FormField(label = "Street", placeholder = "Enter street address")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            FormField(label = "City", placeholder = "City")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            FormField(label = "Postal Code", placeholder = "1000")
                        }
                    }
                }
            }

            PPrimaryButton(
                text = "Save Contact",
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )
        }
    }
}

@Composable
private fun FormField(label: String, placeholder: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
