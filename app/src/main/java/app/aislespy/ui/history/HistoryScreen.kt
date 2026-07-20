package app.aislespy.ui.history

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.domain.model.ProductCategory
import app.aislespy.ui.components.AisleListRowCard
import app.aislespy.ui.components.ScoreBadge
import app.aislespy.ui.theme.CreamSurface
import app.aislespy.ui.theme.ErrorRed
import app.aislespy.ui.theme.Ink
import app.aislespy.ui.theme.MutedText45
import app.aislespy.ui.theme.MutedText55
import app.aislespy.ui.theme.PublicSans

private const val EMPTY_COPY =
    "No missions yet. Scan something in the aisle to start the log."

@Composable
fun HistoryScreen(
    onOpenResult: (barcode: String, source: String) -> Unit,
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel? = null,
) {
    val app = LocalContext.current.applicationContext as Application
    val resolvedVm = historyViewModel ?: viewModel(factory = HistoryViewModel.Factory(app))
    val uiState by resolvedVm.uiState.collectAsStateWithLifecycle()
    var pendingDeleteBarcode by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = CreamSurface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Mission log",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    modifier = Modifier.semantics { heading() },
                )
                if (!uiState.empty) {
                    Text(
                        text = "Clear all",
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = ErrorRed,
                        modifier = Modifier
                            .clickable(onClick = resolvedVm::requestClearAll)
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .semantics { contentDescription = "Clear all history" },
                    )
                }
            }

            if (uiState.empty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(38.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = EMPTY_COPY,
                        fontFamily = PublicSans,
                        fontSize = 13.sp,
                        lineHeight = 20.8.sp,
                        color = MutedText55,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = uiState.items,
                        key = { it.barcode },
                    ) { item ->
                        HistoryRow(
                            item = item,
                            deleteArmed = pendingDeleteBarcode == item.barcode,
                            onClick = {
                                pendingDeleteBarcode = null
                                val source = when (item.category) {
                                    ProductCategory.Food -> "food"
                                    ProductCategory.Beauty -> "beauty"
                                }
                                onOpenResult(item.barcode, source)
                            },
                            onDelete = {
                                if (pendingDeleteBarcode == item.barcode) {
                                    pendingDeleteBarcode = null
                                    resolvedVm.delete(item.barcode)
                                } else {
                                    pendingDeleteBarcode = item.barcode
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showClearConfirm) {
        AlertDialog(
            onDismissRequest = resolvedVm::dismissClearConfirm,
            title = { Text("Really clear all?") },
            text = {
                Text("This removes every local scan from this device. Product cache is kept.")
            },
            confirmButton = {
                TextButton(onClick = resolvedVm::confirmClearAll) {
                    Text("Clear all", color = ErrorRed)
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
    deleteArmed: Boolean = false,
) {
    val rowDescription = buildString {
        append(item.name)
        append(", score ${item.score}")
        append(", ${item.band.label}")
        append(", ${item.scannedAtLabel}")
    }
    AisleListRowCard(
        modifier = modifier,
        contentPadding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .semantics(mergeDescendants = true) {
                        contentDescription = rowDescription
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScoreBadge(
                    score = item.score,
                    band = item.band,
                    contentDescription = null,
                    minSize = 44.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 17.5.sp,
                        color = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${item.band.label} · ${item.scannedAtLabel}",
                        fontFamily = PublicSans,
                        fontSize = 11.5.sp,
                        color = MutedText55,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = if (deleteArmed) "Delete?" else "×",
                fontFamily = PublicSans,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (deleteArmed) ErrorRed else MutedText45,
                modifier = Modifier
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .semantics {
                        contentDescription = if (deleteArmed) {
                            "Confirm delete ${item.name}"
                        } else {
                            "Delete ${item.name} from history"
                        }
                    },
            )
        }
    }
}
