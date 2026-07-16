package app.aislespy.ui.result

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.AisleSpyApp
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    barcode: String,
    source: String,
    onBack: () -> Unit,
    onScanAnother: () -> Unit = onBack,
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
                is ResultUiState.Loading -> LoadingContent(barcode = s.barcode)
                is ResultUiState.Success -> SuccessContent(state = s)
                is ResultUiState.NotFound -> NotFoundContent(
                    state = s,
                    onScanAnother = onScanAnother,
                )
                is ResultUiState.NetworkError -> NetworkErrorContent(
                    state = s,
                    onRetry = resolvedViewModel::retry,
                    onScanAnother = onScanAnother,
                )
                is ResultUiState.NeedsCategoryChoice -> NeedsCategoryChoiceContent(
                    state = s,
                    onChoose = resolvedViewModel::choose,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(barcode: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Running recon…",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = barcode,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SuccessContent(state: ResultUiState.Success) {
    val product = state.product
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuggestionChip(
                onClick = {},
                label = { Text(categoryLabel(product.category)) },
                enabled = false,
            )
            SuggestionChip(
                onClick = {},
                label = { Text(sourceDbLabel(product.sourceDb)) },
                enabled = false,
            )
        }

        ProductImage(imageUrl = product.imageUrl, name = product.name)

        Text(
            text = product.name,
            style = MaterialTheme.typography.titleLarge,
        )
        if (!product.brand.isNullOrBlank()) {
            Text(
                text = product.brand,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = product.barcode,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "Ingredients",
            style = MaterialTheme.typography.titleMedium,
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
            ImagePlaceholder(shape = shape)
        },
        error = {
            ImagePlaceholder(shape = shape)
        },
    )
}

@Composable
private fun ImagePlaceholder(shape: RoundedCornerShape) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No image",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
            .padding(24.dp),
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
            .padding(24.dp),
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

@Composable
private fun NeedsCategoryChoiceContent(
    state: ResultUiState.NeedsCategoryChoice,
    onChoose: (ProductCategory) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Which kind of product?",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.barcode,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        CategoryChoiceCard(
            title = "Food — ${state.foodName}",
            onClick = { onChoose(ProductCategory.Food) },
        )
        Spacer(Modifier.height(12.dp))
        CategoryChoiceCard(
            title = "Beauty — ${state.beautyName}",
            onClick = { onChoose(ProductCategory.Beauty) },
        )
    }
}

@Composable
private fun CategoryChoiceCard(
    title: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(20.dp),
        )
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
