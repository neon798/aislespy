package app.aislespy.ui.result

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.AisleSpyApp
import app.aislespy.ui.components.AisleCard
import app.aislespy.ui.components.BackLink
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.IbmPlexMono
import app.aislespy.ui.theme.PublicSans
import app.aislespy.ui.theme.AisleColors

/**
 * Category chooser when one barcode hits both OFF and OBF (handoff §7).
 */
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
        containerColor = AisleColors.current.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            BackLink(
                onClick = onCancel,
                contentDescription = "Cancel",
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "One barcode, two matches",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AisleColors.current.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "This code turned up in both open databases. Which product are you holding?",
                    fontFamily = PublicSans,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = AisleColors.current.muted60,
                )
                Text(
                    text = barcode,
                    fontFamily = IbmPlexMono,
                    fontSize = 12.sp,
                    color = AisleColors.current.muted55,
                )

                CategoryOptionCard(
                    sourceLabel = "FOOD · OPEN FOOD FACTS",
                    productName = food?.name?.takeIf { it.isNotBlank() } ?: "Food product",
                    meta = food?.brands?.takeIf { it.isNotBlank() }
                        ?: "Open Food Facts match",
                    onClick = onChooseFood,
                )
                CategoryOptionCard(
                    sourceLabel = "BEAUTY · OPEN BEAUTY FACTS",
                    productName = beauty?.name?.takeIf { it.isNotBlank() } ?: "Beauty product",
                    meta = beauty?.brands?.takeIf { it.isNotBlank() }
                        ?: "Open Beauty Facts match",
                    onClick = onChooseBeauty,
                )
            }
        }
    }
}

@Composable
private fun CategoryOptionCard(
    sourceLabel: String,
    productName: String,
    meta: String,
    onClick: () -> Unit,
) {
    AisleCard(
        onClick = onClick,
        shape = AisleSpyShapes.chooserOption,
        contentPadding = 0.dp,
        modifier = Modifier
            .border(1.5.dp, AisleColors.current.cardBorderStrong, AisleSpyShapes.chooserOption)
            .semantics {
                contentDescription = "$sourceLabel: $productName"
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = sourceLabel,
                fontFamily = PublicSans,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.08.sp,
                color = AisleColors.current.olive,
            )
            Text(
                text = productName,
                style = MaterialTheme.typography.titleLarge,
                color = AisleColors.current.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                fontFamily = PublicSans,
                fontSize = 12.sp,
                color = AisleColors.current.muted55,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
