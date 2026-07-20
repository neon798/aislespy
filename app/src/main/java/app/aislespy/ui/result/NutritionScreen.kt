package app.aislespy.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import app.aislespy.AisleSpyApp
import app.aislespy.ui.components.AisleCard
import app.aislespy.ui.components.BackLink
import app.aislespy.ui.components.SectionHeader
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.BricolageGrotesque
import app.aislespy.ui.theme.CardBorder
import app.aislespy.ui.theme.CreamSurface
import app.aislespy.ui.theme.IbmPlexMono
import app.aislespy.ui.theme.Ink
import app.aislespy.ui.theme.MutedText55
import app.aislespy.ui.theme.Olive
import app.aislespy.ui.theme.OliveContainer
import app.aislespy.ui.theme.OliveOnContainer
import app.aislespy.ui.theme.PublicSans
import java.util.Locale

/**
 * Nutrition sub-screen: Nutri-Score grade + per-100g nutriments.
 * Informational only — does not affect the AisleSpy score (ADR-018 / handoff §9).
 */
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
        containerColor = CreamSurface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            BackLink(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )

            if (resolved == null || !resolved.hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No nutrition data available",
                        fontFamily = PublicSans,
                        fontSize = 13.5.sp,
                        color = MutedText55,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Nutrition",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Ink,
                        modifier = Modifier.semantics { heading() },
                    )

                    // Context-only banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OliveContainer, AisleSpyShapes.smallTile)
                            .border(1.dp, Olive.copy(alpha = 0.25f), AisleSpyShapes.smallTile)
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "Shown for context only — nutrition does not affect the AisleSpy score.",
                            fontFamily = PublicSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = OliveOnContainer,
                        )
                    }

                    NutriScoreCard(grade = resolved.nutriScoreGrade)

                    val rows = nutrimentRows(resolved)
                    if (rows.isNotEmpty()) {
                        SectionHeader(text = "Per 100 g")
                        AisleCard(contentPadding = 0.dp) {
                            rows.forEachIndexed { index, (label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 15.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = PublicSans,
                                        fontSize = 12.5.sp,
                                        color = Ink,
                                    )
                                    Text(
                                        text = value,
                                        fontFamily = IbmPlexMono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Ink,
                                    )
                                }
                                if (index < rows.lastIndex) {
                                    HorizontalDivider(color = CardBorder.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutriScoreCard(grade: Char?) {
    AisleCard(
        contentPadding = 0.dp,
        modifier = Modifier.semantics {
            contentDescription = if (grade != null) {
                "Nutri-Score ${grade.uppercaseChar()}"
            } else {
                "No Nutri-Score available"
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(OliveContainer, AisleSpyShapes.smallTile),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = grade?.uppercaseChar()?.toString() ?: "—",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = OliveOnContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (grade != null) {
                        "Nutri-Score ${grade.uppercaseChar()}"
                    } else {
                        "No Nutri-Score available"
                    },
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Ink,
                )
                Text(
                    text = "From Open Food Facts, when the producer supplies data",
                    fontFamily = PublicSans,
                    fontSize = 11.5.sp,
                    color = MutedText55,
                )
            }
        }
    }
}

private fun nutrimentRows(ui: NutritionUi): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    ui.energyKcal100g?.let { out += "Energy" to formatNumber(it, "kcal") }
    ui.sugars100g?.let { out += "Sugars" to formatNumber(it, "g") }
    ui.salt100g?.let { out += "Salt" to formatNumber(it, "g") }
    ui.saturatedFat100g?.let { out += "Saturated fat" to formatNumber(it, "g") }
    ui.fiber100g?.let { out += "Fibre" to formatNumber(it, "g") }
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

/** Caption required by ADR-018 / SCORING.md (kept for callers / tests). */
const val CAPTION_INFORMATIONAL: String =
    "Nutrition is informational only — it does not affect the AisleSpy score."
