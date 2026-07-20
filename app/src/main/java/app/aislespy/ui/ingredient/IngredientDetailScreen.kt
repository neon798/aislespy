package app.aislespy.ui.ingredient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.AisleSpyApp
import app.aislespy.ui.components.AisleCard
import app.aislespy.ui.components.BackLink
import app.aislespy.ui.components.SectionHeader
import app.aislespy.ui.components.SeverityBar
import app.aislespy.ui.result.IngredientDetailUi
import app.aislespy.ui.theme.BricolageGrotesque
import app.aislespy.ui.theme.IbmPlexMono
import app.aislespy.ui.theme.PublicSans
import app.aislespy.ui.theme.AisleColors

private val SeverityMeaning = mapOf(
    1 to "Mild note — mainly for sensitive individuals.",
    2 to "Minor concern — limited-evidence debate.",
    3 to "Moderate concern — notable caveats.",
    4 to "Strong concern — restricted in some regions.",
    5 to "Highest concern in our pack — ban/restrict or strong evidence flags.",
)

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
        containerColor = AisleColors.current.surface,
    ) { innerPadding ->
        if (resolved == null) {
            Column(modifier = Modifier.padding(innerPadding)) {
                BackLink(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No detail available for this ingredient.",
                        fontFamily = PublicSans,
                        fontSize = 13.5.sp,
                        color = AisleColors.current.muted55,
                    )
                }
            }
        } else {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = resolved.id,
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = AisleColors.current.muted55,
                    )
                    Text(
                        text = resolved.name,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        color = AisleColors.current.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    SeverityBar(
                        severity = resolved.severity,
                        trackWidth = 80.dp,
                        trackHeight = 7.dp,
                        ofFiveLabel = true,
                    )
                    Text(
                        text = SeverityMeaning[resolved.severity.coerceIn(1, 5)].orEmpty(),
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AisleColors.current.muted55,
                    )
                    if (!resolved.positionHint.isNullOrBlank()) {
                        AisleCard(contentPadding = 12.dp) {
                            Text(
                                text = "Position on label: ${resolved.positionHint} — earlier usually means more of it.",
                                fontFamily = PublicSans,
                                fontSize = 12.sp,
                                color = AisleColors.current.muted60,
                            )
                        }
                    }
                    Text(
                        text = resolved.fullWhy,
                        fontFamily = PublicSans,
                        fontSize = 13.5.sp,
                        lineHeight = 22.sp,
                        color = AisleColors.current.muted70,
                    )
                    if (resolved.sources.isNotEmpty()) {
                        SectionHeader(
                            text = "Sources",
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        AisleCard(contentPadding = 0.dp) {
                            resolved.sources.forEachIndexed { index, source ->
                                Text(
                                    text = source,
                                    fontFamily = PublicSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = AisleColors.current.olive,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                )
                                if (index < resolved.sources.lastIndex) {
                                    HorizontalDivider(color = AisleColors.current.cardBorder.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                    Text(
                        text = "Informational only — not medical advice.",
                        fontFamily = PublicSans,
                        fontSize = 11.sp,
                        lineHeight = 17.6.sp,
                        color = AisleColors.current.muted45,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
