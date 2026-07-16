package app.aislespy.ui.result

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import app.aislespy.AisleSpyApp
import app.aislespy.ui.components.ProductImagePlaceholder
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Dedicated category chooser (UI_UX §category_chooser / T-420).
 *
 * Reads the food/beauty pair from [ChoicePairStore] (published by [ResultViewModel]
 * on an ambiguous dual hit). Option cards stay serious; title may use spy flair.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChooserScreen(
    barcode: String,
    onChooseFood: () -> Unit,
    onChooseBeauty: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    pair: ChoicePairStore.Pair? = null,
) {
    val app = LocalContext.current.applicationContext as? AisleSpyApp
    val resolved = pair ?: remember(barcode, app) {
        app?.container?.choicePairStore?.get(barcode)
    }
    val food = resolved?.food
    val beauty = resolved?.beauty

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Category") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Two dossiers found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Which kind of product?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = barcode,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
            )

            CategoryOptionCard(
                categoryLabel = "Food product",
                productName = food?.name?.takeIf { it.isNotBlank() } ?: "Food product",
                imageUrl = food?.imageUrl,
                onClick = onChooseFood,
            )
            Spacer(Modifier.height(16.dp))
            CategoryOptionCard(
                categoryLabel = "Beauty product",
                productName = beauty?.name?.takeIf { it.isNotBlank() } ?: "Beauty product",
                imageUrl = beauty?.imageUrl,
                onClick = onChooseBeauty,
            )

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun CategoryOptionCard(
    categoryLabel: String,
    productName: String,
    imageUrl: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$categoryLabel: $productName"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OptionThumbnail(imageUrl = imageUrl, name = productName)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = productName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun OptionThumbnail(imageUrl: String?, name: String) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(10.dp)
    val size = 72.dp
    if (imageUrl.isNullOrBlank()) {
        Box(modifier = Modifier.size(size)) {
            ProductImagePlaceholder(shape = shape, label = "—")
        }
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Thumbnail for $name",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape),
            loading = {
                ProductImagePlaceholder(shape = shape, showLabel = false)
            },
            error = {
                ProductImagePlaceholder(shape = shape, label = "—")
            },
        )
    }
}
