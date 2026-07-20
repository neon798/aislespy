package app.aislespy.ui.settings

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.domain.ScoringConfig
import app.aislespy.domain.model.ScoreBand
import app.aislespy.ui.components.AisleBrandCard
import app.aislespy.ui.components.AisleCard
import app.aislespy.ui.components.BackLink
import app.aislespy.ui.components.ScoreBandChip
import app.aislespy.ui.components.SectionHeader
import app.aislespy.ui.theme.IbmPlexMono
import app.aislespy.ui.theme.PublicSans
import app.aislespy.ui.theme.AisleColors

private const val BRAND_BODY =
    "No accounts, no ads, no analytics. History stays on this phone. " +
        "Lookups send only the barcode to Open Food Facts / Open Beauty Facts."

@Composable
fun SettingsScreen(
    onMethodology: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onLicenses: () -> Unit = {},
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel? = null,
) {
    val app = LocalContext.current.applicationContext as Application
    val resolvedVm = settingsViewModel ?: viewModel(factory = SettingsViewModel.Factory(app))
    val uiState by resolvedVm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = AisleColors.current.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = AisleColors.current.ink,
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .padding(top = 18.dp, bottom = 12.dp)
                    .semantics { heading() },
            )

            AisleBrandCard(
                title = "Recon, not surveillance",
                body = BRAND_BODY,
                modifier = Modifier.padding(horizontal = 22.dp),
            )

            Spacer(Modifier.height(14.dp))
            SectionHeader(
                text = "Trust",
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            )
            AisleCard(
                modifier = Modifier.padding(horizontal = 22.dp),
                contentPadding = 0.dp,
            ) {
                SettingsLinkRow(title = "How we score", onClick = onMethodology, showDivider = true)
                SettingsLinkRow(title = "Privacy", onClick = onPrivacy, showDivider = true)
                SettingsLinkRow(title = "Licenses & attribution", onClick = onLicenses, showDivider = false)
            }

            Spacer(Modifier.height(14.dp))
            SectionHeader(
                text = "Versions",
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            )
            AisleCard(
                modifier = Modifier.padding(horizontal = 22.dp),
                contentPadding = 0.dp,
            ) {
                VersionRow("App", uiState.appVersion.ifEmpty { "—" }, showDivider = true)
                VersionRow("Methodology", uiState.methodologyVersion.ifEmpty { "—" }, showDivider = true)
                VersionRow(
                    "Knowledge pack",
                    uiState.knowledgePackVersion.ifEmpty { "—" },
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
fun MethodologyScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ScrollableInfoScaffold(
        title = "How we score",
        onBack = onBack,
        modifier = modifier,
    ) {
        BodyText(
            "One 1–100 number for ingredient quality only. Higher is better. " +
                "Nutrition, dietary badges and brand ownership are shown for context but never enter the number.",
        )

        SectionHeader(text = "The four bands", modifier = Modifier.padding(top = 8.dp))
        AisleCard(contentPadding = 0.dp) {
            BandRow(ScoreBand.Excellent, "75–100", showDivider = true)
            BandRow(ScoreBand.Ok, "50–74", showDivider = true)
            BandRow(ScoreBand.Poor, "25–49", showDivider = true)
            BandRow(ScoreBand.Bad, "1–24", showDivider = false)
        }

        SectionHeader(
            text = "Food weights (methodology ${ScoringConfig.METHODOLOGY_VERSION})",
            modifier = Modifier.padding(top = 8.dp),
        )
        AisleCard(contentPadding = 0.dp) {
            WeightRow("Flagged ingredients", "65%", showDivider = true)
            WeightRow("Ultra-processing (NOVA)", "30%", showDivider = true)
            WeightRow("Label positives", "5%", showDivider = false)
        }

        BodyText(
            "Missing a component? Its weight is dropped and the rest renormalize — we never invent data. " +
                "Beauty products use a parallel formula: position-weighted hazards, fragrance & allergens, " +
                "and regulatory flags. Confidence (High / Medium / Low) tells you how complete the inputs were.",
        )
        Text(
            text = "Scores are informational only — not medical advice, an allergen guarantee, or a safety certification.",
            fontFamily = PublicSans,
            fontSize = 11.sp,
            lineHeight = 17.6.sp,
            color = AisleColors.current.muted55,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun PrivacyScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        "No accounts, ever" to
            "There is nothing to sign up for and nothing to log in to.",
        "No telemetry or analytics" to
            "The app ships with zero tracking SDKs. Nothing phones home.",
        "History stays on this phone" to
            "Scans are stored in a local database only. Delete them any time.",
        "Lookups send only the barcode" to
            "Network requests go to Open Food Facts / Open Beauty Facts and their image servers — not to any AisleSpy server.",
        "Scoring runs on the device" to
            "The score is computed locally from product data plus a shipped knowledge pack.",
    )
    ScrollableInfoScaffold(
        title = "Privacy",
        onBack = onBack,
        modifier = modifier,
    ) {
        rows.forEach { (head, body) ->
            AisleCard(contentPadding = 0.dp) {
                Column(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = head,
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AisleColors.current.ink,
                    )
                    Text(
                        text = body,
                        fontFamily = PublicSans,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = AisleColors.current.muted60,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
fun LicensesScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        "AisleSpy — Apache-2.0" to
            "app.aislespy · source available on the project repository.",
        "Open Food Facts / Open Beauty Facts" to
            "Product data under the Open Database License (ODbL). Thank you, contributors.",
        "FOSS barcode decoder" to
            "Camera decoding uses a fully open-source library — no Play Services, no ML Kit.",
        "Fonts (OFL)" to
            "Bricolage Grotesque, Public Sans, and IBM Plex Mono are licensed under the SIL Open Font License. Texts ship in assets/licenses/fonts/.",
        "Knowledge packs" to
            "Curated from open regulatory and scientific sources (EFSA, SCCS, FDA, EU law).",
    )
    ScrollableInfoScaffold(
        title = "Licenses & attribution",
        onBack = onBack,
        modifier = modifier,
    ) {
        rows.forEach { (head, body) ->
            AisleCard(contentPadding = 0.dp) {
                Column(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = head,
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AisleColors.current.ink,
                    )
                    Text(
                        text = body,
                        fontFamily = PublicSans,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = AisleColors.current.muted60,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            fontFamily = PublicSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = AisleColors.current.ink,
        )
        Text(
            text = "›",
            fontSize = 16.sp,
            color = AisleColors.current.muted45,
        )
    }
    if (showDivider) {
        HorizontalDivider(color = AisleColors.current.cardBorder.copy(alpha = 0.5f))
    }
}

@Composable
private fun VersionRow(
    label: String,
    value: String,
    showDivider: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontFamily = PublicSans,
            fontSize = 12.5.sp,
            color = AisleColors.current.ink,
        )
        Text(
            text = value,
            fontFamily = IbmPlexMono,
            fontSize = 12.5.sp,
            color = AisleColors.current.muted60,
        )
    }
    if (showDivider) {
        HorizontalDivider(color = AisleColors.current.cardBorder.copy(alpha = 0.5f))
    }
}

@Composable
private fun BandRow(band: ScoreBand, range: String, showDivider: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScoreBandChip(label = band.label, band = band)
        Text(
            text = range,
            fontFamily = IbmPlexMono,
            fontSize = 12.sp,
            color = AisleColors.current.muted60,
        )
    }
    if (showDivider) {
        HorizontalDivider(color = AisleColors.current.cardBorder.copy(alpha = 0.5f))
    }
}

@Composable
private fun WeightRow(label: String, weight: String, showDivider: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontFamily = PublicSans,
            fontSize = 12.5.sp,
            color = AisleColors.current.ink,
        )
        Text(
            text = weight,
            fontFamily = PublicSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = AisleColors.current.ink,
        )
    }
    if (showDivider) {
        HorizontalDivider(color = AisleColors.current.cardBorder.copy(alpha = 0.5f))
    }
}

@Composable
private fun ScrollableInfoScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
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
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AisleColors.current.ink,
                    modifier = Modifier.semantics { heading() },
                )
                content()
            }
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        fontFamily = PublicSans,
        fontSize = 13.sp,
        lineHeight = 20.8.sp,
        color = AisleColors.current.muted70,
    )
}
