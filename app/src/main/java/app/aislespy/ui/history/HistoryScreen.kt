package app.aislespy.ui.history

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreBand
import app.aislespy.ui.theme.scoreBad
import app.aislespy.ui.theme.scoreExcellent
import app.aislespy.ui.theme.scoreOk
import app.aislespy.ui.theme.scorePoor
import coil.compose.AsyncImage

private const val EMPTY_COPY = "No missions yet—scan something in the aisle."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenResult: (barcode: String, source: String) -> Unit,
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel? = null,
) {
    val app = LocalContext.current.applicationContext as Application
    val resolvedVm = historyViewModel ?: viewModel(factory = HistoryViewModel.Factory(app))
    val uiState by resolvedVm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    if (!uiState.empty) {
                        IconButton(
                            onClick = resolvedVm::requestClearAll,
                            modifier = Modifier.semantics {
                                contentDescription = "Clear all history"
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.empty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = EMPTY_COPY,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(
                    items = uiState.items,
                    key = { it.barcode },
                ) { item ->
                    HistoryRow(
                        item = item,
                        onClick = {
                            val source = when (item.category) {
                                ProductCategory.Food -> "food"
                                ProductCategory.Beauty -> "beauty"
                            }
                            onOpenResult(item.barcode, source)
                        },
                        onDelete = { resolvedVm.delete(item.barcode) },
                    )
                }
            }
        }
    }

    if (uiState.showClearConfirm) {
        AlertDialog(
            onDismissRequest = resolvedVm::dismissClearConfirm,
            title = { Text("Clear all history?") },
            text = { Text("This removes every local scan from this device. Product cache is kept.") },
            confirmButton = {
                TextButton(onClick = resolvedVm::confirmClearAll) {
                    Text("Clear all")
                }
            },
            dismissButton = {
                TextButton(onClick = resolvedVm::dismissClearConfirm) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun HistoryRow(
    item: HistoryItemUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bandColor = item.band.toColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "${item.name}, score ${item.score}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.thumbnailUrl != null) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.scannedAtLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bandColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.score.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.semantics {
                contentDescription = "Delete ${item.name} from history"
            },
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
            )
        }
    }
}

private fun ScoreBand.toColor(): Color = when (this) {
    ScoreBand.Excellent -> scoreExcellent
    ScoreBand.Ok -> scoreOk
    ScoreBand.Poor -> scorePoor
    ScoreBand.Bad -> scoreBad
}
