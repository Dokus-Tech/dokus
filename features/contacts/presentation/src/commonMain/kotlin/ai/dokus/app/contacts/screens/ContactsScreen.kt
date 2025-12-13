package ai.dokus.app.contacts.screens

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_empty
import ai.dokus.app.resources.generated.contacts_empty_description
import ai.dokus.app.resources.generated.contacts_title
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * The main contacts screen showing a list of contacts.
 * Currently shows an empty state placeholder.
 */
@Composable
internal fun ContactsScreen() {
    Scaffold(
        topBar = {
            PTopAppBarSearchAction(
                title = stringResource(Res.string.contacts_title),
                showSearch = false,
                searchQuery = "",
                onSearchQueryChange = {},
                onSearchActiveChange = {},
                actions = {}
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.contacts_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
