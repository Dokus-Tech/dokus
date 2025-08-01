package ai.thepredict.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import thepredict.application.ui.generated.resources.Res
import thepredict.application.ui.generated.resources.chart_bar_trend_up
import thepredict.application.ui.generated.resources.plus
import thepredict.application.ui.generated.resources.tasks_2
import thepredict.application.ui.generated.resources.users
import thepredict.application.ui.generated.resources.wallet_2

@Composable
fun NavigationBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chart icon - primary color (selected)
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(Res.drawable.chart_bar_trend_up),
                    contentDescription = "Charts",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Users icon
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(Res.drawable.users),
                    contentDescription = "Users",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Add button (FAB)
            FloatingActionButton(
                onClick = { },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(Res.drawable.plus),
                    contentDescription = "Add",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tasks icon
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(Res.drawable.tasks_2),
                    contentDescription = "Tasks",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Wallet icon
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(Res.drawable.wallet_2),
                    contentDescription = "Wallet",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}