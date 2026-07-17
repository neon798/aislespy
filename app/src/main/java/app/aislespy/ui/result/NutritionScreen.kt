package app.aislespy.ui.result

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aislespy.AisleSpyApp
import app.aislespy.ui.components.SectionHeader
import java.util.Locale

/**
 * Nutrition sub-screen: Nutri-Score grade + per-100g nutriments.
 * Informational only — does not affect the AisleSpy score (ADR-018).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    barcode: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    nutrition: NutritionUi? = null,
) {
    val app = LocalContext.current.applicationContext as? AisleSpyApp
    val resolved = nutrition ?: remember(barcode, app) {
        app?.container?.nutritionStore?.get(barcode)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nutrition") },
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
        if (resolved == null || !resolved.hasData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No nutrition data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
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
                NutriScoreBadge(grade = resolved.nutriScoreGrade)

                val rows = nutrimentRows(resolved)
                if (rows.isNotEmpty()) {
                    SectionHeader(text = "Per 100 g")
                    rows.forEach { (label, value) ->
                        NutrimentRow(label = label, value = value)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = CAPTION_INFORMATIONAL,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun NutriScoreBadge(grade: Char?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (grade != null) {
                    "Nutri-Score ${grade.uppercaseChar()}"
                } else {
                    "No Nutri-Score available"
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Nutri-Score",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            if (grade != null) {
                Text(
                    text = grade.uppercaseChar().toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = "No Nutri-Score available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun NutrimentRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun nutrimentRows(ui: NutritionUi): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    ui.energyKcal100g?.let { out += "Energy" to formatNumber(it, "kcal") }
    ui.sugars100g?.let { out += "Sugars" to formatNumber(it, "g") }
    ui.salt100g?.let { out += "Salt" to formatNumber(it, "g") }
    ui.saturatedFat100g?.let { out += "Saturated fat" to formatNumber(it, "g") }
    ui.fiber100g?.let { out += "Fiber" to formatNumber(it, "g") }
    ui.proteins100g?.let { out += "Protein" to formatNumber(it, "g") }
    return out
}

private fun formatNumber(value: Double, unit: String): String {
    val formatted = if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
    return "$formatted $unit"
}

/** Caption required by ADR-018 / SCORING.md. */
const val CAPTION_INFORMATIONAL: String =
    "Nutrition is informational only — it does not affect the AisleSpy score."
