package app.aislespy.ui.ingredient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aislespy.AisleSpyApp
import app.aislespy.ui.components.SectionHeader
import app.aislespy.ui.components.SeverityChip
import app.aislespy.ui.result.IngredientDetailUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientDetailScreen(
    concernId: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    detail: IngredientDetailUi? = null,
) {
    val app = LocalContext.current.applicationContext as? AisleSpyApp
    val resolved = detail ?: remember(concernId, app) {
        app?.container?.concernDetailStore?.get(concernId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(resolved?.name ?: "Ingredient") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (resolved == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No detail available for this ingredient.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = resolved.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SeverityChip(severity = resolved.severity)
                    Text(
                        text = "Severity ${resolved.severity} of 5",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (!resolved.positionHint.isNullOrBlank()) {
                    Text(
                        text = resolved.positionHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SectionHeader(
                    text = "Why it matters",
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = resolved.fullWhy,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (resolved.sources.isNotEmpty()) {
                    SectionHeader(
                        text = "Sources",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    resolved.sources.forEach { source ->
                        Text(
                            text = "• $source",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
