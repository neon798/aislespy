package app.aislespy.ui.result

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.AisleSpyApp
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import app.aislespy.ui.components.InfoChip
import app.aislespy.ui.components.LoadingRecon
import app.aislespy.ui.components.ProductImagePlaceholder
import app.aislespy.ui.components.ScoreRing
import app.aislespy.ui.components.SectionHeader
import app.aislespy.ui.components.SeverityChip
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    barcode: String,
    source: String,
    onBack: () -> Unit,
    onScanAnother: () -> Unit = onBack,
    onConcernClick: (concernId: String) -> Unit = {},
    onMethodology: () -> Unit = {},
    onNavigateToCategoryChooser: (barcode: String) -> Unit = {},
    modifier: Modifier = Modifier,
    resultViewModel: ResultViewModel? = null,
) {
    val app = LocalContext.current.applicationContext as AisleSpyApp
    val resolvedViewModel = resultViewModel ?: viewModel(
        factory = ResultViewModel.Factory(
            application = app,
            barcode = barcode,
            source = source,
        ),
    )
    val state by resolvedViewModel.uiState.collectAsState()

    // State-driven handoff to choose/{barcode}. Popping this destination in NavGraph
    // prevents a second navigation on config change (screen is disposed).
    LaunchedEffect(state) {
        val s = state
        if (s is ResultUiState.NavigateToCategoryChooser) {
            onNavigateToCategoryChooser(s.barcode)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Result") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = state) {
                is ResultUiState.Loading -> LoadingRecon(subtitle = s.barcode)
                is ResultUiState.Success -> SuccessContent(
                    state = s,
                    onConcernClick = onConcernClick,
                    onMethodology = onMethodology,
                )
                is ResultUiState.NotFound -> NotFoundContent(
                    state = s,
                    onScanAnother = onScanAnother,
                )
                is ResultUiState.NetworkError -> NetworkErrorContent(
                    state = s,
                    onRetry = resolvedViewModel::retry,
                    onScanAnother = onScanAnother,
                )
                // Brief pass-through while navigation to the dedicated chooser runs.
                is ResultUiState.NeedsCategoryChoice,
                is ResultUiState.NavigateToCategoryChooser,
                -> LoadingRecon(subtitle = barcode)
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: ResultUiState.Success,
    onConcernClick: (String) -> Unit,
    onMethodology: () -> Unit,
) {
    val product = state.product
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoChip(label = categoryLabel(product.category))
            InfoChip(label = sourceDbLabel(product.sourceDb))
        }

        ProductImage(imageUrl = product.imageUrl, name = product.name)

        Text(
            text = product.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (!product.brand.isNullOrBlank()) {
            Text(
                text = product.brand,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = product.barcode,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Score hero
        when {
            state.partialMessage != null || (state.score == null && !state.beautyScoringPending) -> {
                PartialScorePlaceholder(
                    message = state.partialMessage
                        ?: ResultViewModel.PARTIAL_NO_INGREDIENTS,
                )
            }
            state.beautyScoringPending || state.score == null -> {
                BeautyScoringPlaceholder()
            }
            else -> {
                val score = checkNotNull(state.score)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ScoreRing(
                        value = score.value,
                        band = score.band,
                        label = score.label,
                        confidenceLabel = score.confidenceLabel,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = score.summarySentence,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    if (!score.driverSentence.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = score.driverSentence,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    // Adjacent readable confidence (also in ScoreRing TalkBack).
                    InfoChip(
                        label = score.confidenceLabel,
                        contentDescription = score.confidenceLabel,
                    )
                }
            }
        }

        // Badges
        if (state.badges.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.badges.forEach { badge ->
                    InfoChip(label = badge.label)
                }
            }
        }

        // Breakdown
        if (state.breakdown.isNotEmpty() || state.omittedComponents.isNotEmpty()) {
            SectionHeader(
                text = "Score breakdown",
                modifier = Modifier.padding(top = 4.dp),
            )
            state.breakdown.forEach { component ->
                BreakdownRow(component)
            }
            state.omittedComponents.forEach { label ->
                OmittedComponentRow(label)
            }
        }

        // Suspect ingredients (microcopy bank)
        SectionHeader(
            text = "Suspect ingredients",
            modifier = Modifier.padding(top = 8.dp),
        )
        if (state.beautyScoringPending) {
            Text(
                text = "Beauty ingredient flags will appear here once scoring lands.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (state.partialMessage != null) {
            Text(
                text = "No ingredients listed to flag.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (state.concerns.isEmpty()) {
            Text(
                text = "Clean dossier—nothing flagged in our pack.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.concerns.forEach { concern ->
                ConcernCard(
                    concern = concern,
                    onClick = { onConcernClick(concern.id) },
                )
            }
        }

        // Ingredients text (transparency)
        SectionHeader(
            text = "Ingredients",
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = state.ingredientsText ?: "No ingredients listed",
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.ingredientsText == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )

        if (state.disclaimerVisible) {
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            Text(
                text = ResultViewModel.DISCLAIMER_TEXT,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = onMethodology) {
                Text("How we score")
            }
        }
    }
}

@Composable
private fun BeautyScoringPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Text(
            text = "Beauty scoring coming soon",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun PartialScorePlaceholder(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "—",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Score not available"
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BreakdownRow(component: ScoreComponentUi) {
    val weightPercent = kotlin.math.round(component.weight * 100f).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = component.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (!component.detail.isNullOrBlank()) {
                Text(
                    text = component.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (component.weight > 0f) {
                Text(
                    text = "$weightPercent% of score",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = component.score.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Muted row for a component dropped for missing data.
 * Domain label form: "NOVA (no data)" → "NOVA — no data (score reweighted)".
 */
@Composable
private fun OmittedComponentRow(label: String) {
    val display = formatOmittedComponentLabel(label)
    Text(
        text = display,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

internal fun formatOmittedComponentLabel(label: String): String {
    val name = label.removeSuffix(" (no data)").trim()
    return if (name.isNotEmpty() && name != label) {
        "$name — no data (score reweighted)"
    } else {
        "$label — no data (score reweighted)"
    }
}

@Composable
private fun ConcernCard(
    concern: ConcernUi,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription =
                    "${concern.name}, severity ${concern.severity} of 5. ${concern.shortWhy}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeverityChip(severity = concern.severity)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = concern.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = concern.shortWhy,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!concern.positionHint.isNullOrBlank()) {
                    Text(
                        text = concern.positionHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ProductImage(imageUrl: String?, name: String) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Product image for $name",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape),
        loading = {
            ProductImagePlaceholder(shape = shape)
        },
        error = {
            ProductImagePlaceholder(shape = shape)
        },
    )
}

@Composable
private fun NotFoundContent(
    state: ResultUiState.NotFound,
    onScanAnother: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "This barcode isn’t in the open databases yet.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.barcode,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(state.contributeFoodUrl)),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add on Open Food Facts")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(state.contributeBeautyUrl)),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add on Open Beauty Facts")
        }
        Text(
            text = "Beauty products are still sparse in open databases—adding one takes a minute and helps everyone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onScanAnother) {
            Text("Scan another")
        }
    }
}

@Composable
private fun NetworkErrorContent(
    state: ResultUiState.NetworkError,
    onRetry: () -> Unit,
    onScanAnother: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Lost contact—check your connection.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Retry")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onScanAnother) {
            Text("Scan another")
        }
    }
}

private fun categoryLabel(category: ProductCategory): String = when (category) {
    ProductCategory.Food -> "Food"
    ProductCategory.Beauty -> "Beauty"
}

private fun sourceDbLabel(sourceDb: SourceDb): String = when (sourceDb) {
    SourceDb.OpenFoodFacts -> "Open Food Facts"
    SourceDb.OpenBeautyFacts -> "Open Beauty Facts"
}
