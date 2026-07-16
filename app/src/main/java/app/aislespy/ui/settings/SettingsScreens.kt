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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.domain.ScoringConfig
import app.aislespy.ui.components.SectionHeader
import app.aislespy.ui.result.ResultViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsLinkRow(
                title = "How scoring works",
                onClick = onMethodology,
            )
            SettingsLinkRow(
                title = "Privacy",
                onClick = onPrivacy,
            )
            SettingsLinkRow(
                title = "Open-source licenses",
                onClick = onLicenses,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsReadOnlyRow(
                label = "Knowledge packs",
                value = uiState.knowledgePackVersion.ifEmpty { "—" },
            )
            SettingsReadOnlyRow(
                label = "Methodology",
                value = uiState.methodologyVersion.ifEmpty { "—" },
            )
            SettingsReadOnlyRow(
                label = "App version",
                value = uiState.appVersion.ifEmpty { "—" },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.privacySummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Attribution",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = OFF_OBF_ATTRIBUTION,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MethodologyScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ScrollableInfoScaffold(
        title = "Methodology",
        onBack = onBack,
        modifier = modifier,
    ) {
        SectionHeading("Score scale")
        BodyText(
            "AisleSpy scores products from 1 to 100. " +
                "100 is best (healthier / fewer concerns); 1 is worst.",
        )

        SectionHeading("Color bands")
        BodyText("75–100 — Excellent (green)")
        BodyText("50–74 — Ok (yellow)")
        BodyText("25–49 — Poor (orange)")
        BodyText("1–24 — Bad (red)")

        SectionHeading("Food score")
        BodyText(
            "Food uses weighted components (weights renormalized if data is missing):",
        )
        BodyText(
            "• Nutri-Score — ${pct(ScoringConfig.FoodWeights.NUTRISCORE)} " +
                "(nutritional quality grades A–E)",
        )
        BodyText(
            "• NOVA — ${pct(ScoringConfig.FoodWeights.NOVA)} " +
                "(ultra-processing groups 1–4)",
        )
        BodyText(
            "• Additives — ${pct(ScoringConfig.FoodWeights.ADDITIVES)} " +
                "(flagged additives from the knowledge pack)",
        )
        BodyText(
            "• Positives — ${pct(ScoringConfig.FoodWeights.POSITIVES)} " +
                "(small bonuses, e.g. organic; capped influence)",
        )
        BodyText(
            "Total = round(sum of subscore × normalized weight), clamped to 1–100.",
        )

        SectionHeading("Beauty score")
        BodyText("Beauty uses ingredient-focused components:")
        BodyText(
            "• Hazards — ${pct(ScoringConfig.BeautyWeights.HAZARDS)} " +
                "(matched INCI hazards × position in the ingredient list)",
        )
        BodyText(
            "• Allergens / fragrance — ${pct(ScoringConfig.BeautyWeights.ALLERGENS_FRAGRANCE)} " +
                "(fragrance umbrella + listed allergens)",
        )
        BodyText(
            "• Regulatory — ${pct(ScoringConfig.BeautyWeights.REGULATORY)} " +
                "(pack entries flagged restricted or banned)",
        )
        BodyText(
            "Earlier ingredients in an ordered list count more " +
                "(first ≈ full weight, last ≈ 40%). Unknown order uses a medium weight.",
        )

        SectionHeading("Confidence")
        BodyText(
            "High — strong inputs (e.g. Nutri-Score + NOVA for food, " +
                "or a structured beauty ingredient list).",
        )
        BodyText(
            "Medium — partial data (one of Nutri-Score/NOVA, free-text ingredients only).",
        )
        BodyText(
            "Low — mostly missing signals. Scores stay transparent about weak data.",
        )

        SectionHeading("Disclaimer")
        BodyText(ResultViewModel.DISCLAIMER_TEXT)

        Spacer(modifier = Modifier.height(8.dp))
        BodyText(
            "Methodology version: ${ScoringConfig.METHODOLOGY_VERSION}. " +
                "Formula details live in the open-source docs (SCORING.md).",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ScrollableInfoScaffold(
        title = "Privacy",
        onBack = onBack,
        modifier = modifier,
    ) {
        SectionHeading("Short version")
        BodyText("• No accounts.")
        BodyText("• No analytics or advertising SDKs in the default build.")
        BodyText("• Scan history is stored only on your phone.")
        BodyText(
            "• Looking up a product sends the barcode (and a normal HTTP request) " +
                "to Open Food Facts and/or Open Beauty Facts so the app can fetch " +
                "public product data.",
        )
        BodyText("• We do not sell your data. We do not have a cloud profile of you.")

        SectionHeading("What we collect")
        BodyText(
            "AisleSpy does not collect personal information about you. " +
                "There is no account, no cloud profile, and no first-party tracking backend.",
        )
        BodyText(
            "On your device only: scan history (barcode, name, score, time, optional " +
                "thumbnail URL), optional cached product data for faster repeat lookups, " +
                "and app preferences. These do not leave the device as a history upload.",
        )

        SectionHeading("Where requests go")
        BodyText(
            "When you scan or enter a barcode, AisleSpy may contact:",
        )
        BodyText("• https://world.openfoodfacts.org — food product data")
        BodyText("• https://world.openbeautyfacts.org — beauty/cosmetics data")
        BodyText("• Image CDNs used by those projects — product photos")
        BodyText(
            "Requests include the barcode, a User-Agent, and standard HTTP metadata " +
                "(the server can see your IP). We do not attach your name, email, " +
                "advertising ID, or scan history beyond the barcode you asked about.",
        )
        BodyText(
            "Open Food Facts and Open Beauty Facts are third-party non-profit projects; " +
                "their own privacy practices apply to traffic they receive.",
        )

        SectionHeading("Camera")
        BodyText(
            "Camera permission is used only to scan barcodes on this device. " +
                "Frames are processed locally for barcode decoding—not uploaded as video " +
                "or photo galleries. Internet permission is used to fetch product data and images.",
        )
        BodyText(
            "No location, contacts, microphone, or SMS permissions for this app’s MVP.",
        )

        SectionHeading("History stays local")
        BodyText(
            "Scan history stays on this phone. You can clear it from History. " +
                "Uninstalling the app removes local storage. There is no cloud sync of history.",
        )

        SectionHeading("What we do not do")
        BodyText("• No user accounts or login")
        BodyText("• No Google Play Services analytics, Firebase, or Crashlytics")
        BodyText("• No ads or ad identifiers")
        BodyText("• No selling or brokering personal data")
        BodyText("• No bulk uploading of your scan history")

        SectionHeading("Medical / safety disclaimer")
        BodyText(ResultViewModel.DISCLAIMER_TEXT)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ScrollableInfoScaffold(
        title = "Licenses",
        onBack = onBack,
        modifier = modifier,
    ) {
        SectionHeading("AisleSpy")
        BodyText("AisleSpy is licensed under the Apache License 2.0.")

        SectionHeading("Product data")
        BodyText(OFF_OBF_ATTRIBUTION)
        BodyText(
            "Data is available under the Open Database License (ODbL). " +
                "See https://world.openfoodfacts.org and https://world.openbeautyfacts.org.",
        )

        SectionHeading("Key open-source libraries")
        BodyText("• Jetpack Compose & Material 3 (AndroidX) — Apache-2.0")
        BodyText("• CameraX (AndroidX) — Apache-2.0")
        BodyText("• zxing-cpp (Android binding) — Apache-2.0")
        BodyText("• Retrofit — Apache-2.0")
        BodyText("• OkHttp — Apache-2.0")
        BodyText("• Room (AndroidX) — Apache-2.0")
        BodyText("• Coil — Apache-2.0")
        BodyText("• Kotlin / kotlinx libraries — Apache-2.0")
        BodyText("• AndroidX DataStore Preferences — Apache-2.0")
        BodyText("• Navigation Compose — Apache-2.0")

        Spacer(modifier = Modifier.height(8.dp))
        BodyText(
            "Full dependency licenses ship with each library’s distribution. " +
                "Source for AisleSpy is available in the public repository.",
        )
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsReadOnlyRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrollableInfoScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Spacer(modifier = Modifier.height(8.dp))
    SectionHeader(text = text)
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

private fun pct(weight: Double): String =
    "${(weight * 100).toInt()}%"

/** Required OFF/OBF attribution (docs/DATA_SOURCES.md). */
private const val OFF_OBF_ATTRIBUTION =
    "Product data © Open Food Facts / Open Beauty Facts contributors, " +
        "available under the Open Database License. " +
        "https://world.openfoodfacts.org https://world.openbeautyfacts.org"
