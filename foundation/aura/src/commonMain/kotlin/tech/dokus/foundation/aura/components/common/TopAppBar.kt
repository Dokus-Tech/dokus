package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.back
import tech.dokus.navigation.local.LocalNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTopAppBar(
    title: String,
    navController: NavController? = LocalNavController.current,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TopAppBar(
            navigationIcon = {
                val showNav = navController != null &&
                    navController.previousBackStackEntry != null &&
                    showBackButton
                if (!showNav) return@TopAppBar
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back)
                    )
                }
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Bottom divider for visual separation
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTopAppBar(
    title: StringResource,
    navController: NavController? = LocalNavController.current,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    PTopAppBar(
        title = stringResource(resource = title),
        navController = navController,
        showBackButton = showBackButton,
        modifier = modifier
    )
}
