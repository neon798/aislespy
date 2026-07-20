package app.aislespy.ui.result

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.AisleSpyApp
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.ScoreBand
import app.aislespy.domain.model.SourceDb
import app.aislespy.ui.components.AisleCard
import app.aislespy.ui.components.AisleOutlinePill
import app.aislespy.ui.components.AislePrimaryButton
import app.aislespy.ui.components.AislePrimaryPill
import app.aislespy.ui.components.AisleSecondaryButton
import app.aislespy.ui.components.BackLink
import app.aislespy.ui.components.DashedBorderCard
import app.aislespy.ui.components.DashedDivider
import app.aislespy.ui.components.LoadingRecon
import app.aislespy.ui.components.OutlineInfoChip
import app.aislespy.ui.components.ResultBadgeChip
import app.aislespy.ui.components.ScoreBandChip
import app.aislespy.ui.components.ScoreRing
import app.aislespy.ui.components.SectionHeader
import app.aislespy.ui.components.SeverityBar
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.AisleSpyTextStyles
import app.aislespy.ui.theme.BricolageGrotesque
import app.aislespy.ui.theme.CardBorder
import app.aislespy.ui.theme.CardBorderStrong
import app.aislespy.ui.theme.CardWhite
import app.aislespy.ui.theme.CreamSurface
import app.aislespy.ui.theme.IbmPlexMono
import app.aislespy.ui.theme.Ink
import app.aislespy.ui.theme.MutedText45
import app.aislespy.ui.theme.MutedText55
import app.aislespy.ui.theme.MutedText60
import app.aislespy.ui.theme.MutedText70
import app.aislespy.ui.theme.Olive
import app.aislespy.ui.theme.OliveContainer
import app.aislespy.ui.theme.OliveOnContainer
import app.aislespy.ui.theme.PublicSans
import app.aislespy.ui.theme.scoreBandColors
import app.aislespy.ui.util.rememberReducedMotion

/** Handoff disclaimer (result footer). */
private const val RESULT_DISCLAIMER =
    "Scores are informational only — not medical advice or an allergen guarantee. " +
        "Open data can be incomplete; the physical label is authoritative."

private const val ZERO_CONCERNS_COPY =
    "Nothing in this product matched our concern pack. " +
        "That doesn't guarantee perfection — just no known flags."

@Composable
fun ResultScreen(
    barcode: String,
    source: String,
    onBack: () -> Unit,
    onScanAnother: () -> Unit = onBack,
    onConcernClick: (concernId: String) -> Unit = {},
    onNutrition: (barcode: String) -> Unit = {},
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

    LaunchedEffect(state) {
        val s = state
        if (s is ResultUiState.NavigateToCategoryChooser) {
            onNavigateToCategoryChooser(s.barcode)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = CreamSurface,
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
                    onBack = onBack,
                    onConcernClick = onConcernClick,
                    onNutrition = onNutrition,
                    onMethodology = onMethodology,
                )
                is ResultUiState.NotFound -> NotFoundContent(
                    state = s,
                    onBack = onBack,
                    onScanAnother = onScanAnother,
                )
                is ResultUiState.NetworkError -> NetworkErrorContent(
                    state = s,
                    onBack = onBack,
                    onRetry = resolvedViewModel::retry,
                    onScanAnother = onScanAnother,
                )
                is ResultUiState.NeedsCategoryChoice,
                is ResultUiState.NavigateToCategoryChooser,
                -> LoadingRecon(subtitle = barcode)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuccessContent(
    state: ResultUiState.Success,
    onBack: () -> Unit,
    onConcernClick: (String) -> Unit,
    onNutrition: (barcode: String) -> Unit,
    onMethodology: () -> Unit,
) {
    val product = state.product
    val isPartial = state.partialMessage != null ||
        (state.score == null && !state.beautyScoringPending)
    val isScored = state.score != null && !isPartial
    var saved by rememberSaveable(product.barcode) { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val fade by animateFloatAsState(
        targetValue = if (entered || reducedMotion) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else 350),
        label = "result-fade",
    )
    val slide by animateFloatAsState(
        targetValue = if (entered || reducedMotion) 0f else 8f,
        animationSpec = tween(if (reducedMotion) 0 else 350),
        label = "result-slide",
    )

    val app = LocalContext.current.applicationContext as? AisleSpyApp
    val nutriGrade = remember(product.barcode, app) {
        app?.container?.nutritionStore?.get(product.barcode)?.nutriScoreGrade
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 1. Back + Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackLink(onClick = onBack)
            if (isScored) {
                if (saved) {
                    AislePrimaryPill(
                        text = "Saved ✓",
                        onClick = { saved = false },
                    )
                } else {
                    AisleOutlinePill(
                        text = "Save",
                        onClick = { saved = true },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = fade
                    translationY = slide
                }
                .padding(horizontal = 24.dp)
                .padding(bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            // 2. Identity
            Text(
                text = sourceLabel(product.category, product.sourceDb),
                fontFamily = PublicSans,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.08.sp,
                color = Olive,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = product.name,
                style = AisleSpyTextStyles.productName,
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            val brandPart = product.brand?.takeIf { it.isNotBlank() }
            Text(
                text = buildString {
                    if (brandPart != null) append(brandPart)
                    if (brandPart != null) append(" · ")
                    append(product.barcode)
                },
                fontFamily = PublicSans,
                fontSize = 12.5.sp,
                color = MutedText55,
                textAlign = TextAlign.Center,
            )

            // 3–5. Score hero / partial
            when {
                isPartial -> {
                    PartialScoreBlock(
                        message = state.partialMessage
                            ?: ResultViewModel.PARTIAL_NO_INGREDIENTS,
                    )
                }
                state.beautyScoringPending || state.score == null -> {
                    PartialScoreBlock(message = "Beauty scoring coming soon")
                }
                else -> {
                    val score = checkNotNull(state.score)
                    val bandColors = scoreBandColors(score.band)
                    ScoreRing(
                        value = score.value,
                        band = score.band,
                        label = score.label,
                        confidenceLabel = score.confidenceLabel,
                        animated = !reducedMotion,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ScoreBandChip(label = score.label, band = score.band)
                        OutlineInfoChip(label = formatConfidenceChip(score.confidenceLabel))
                    }
                    Text(
                        text = score.summarySentence,
                        fontFamily = PublicSans,
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                        color = MutedText70,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (!score.driverSentence.isNullOrBlank()) {
                        Text(
                            text = score.driverSentence,
                            fontFamily = PublicSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = bandColors.accent,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // 6. Badge row
            if (state.badges.isNotEmpty() && isScored) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.badges.forEach { badge ->
                        ResultBadgeChip(
                            label = badge.label,
                            style = badge.style,
                            contentDescription = badge.contentDescription,
                        )
                    }
                }
            }
        }

        if (isScored) {
            DashedDivider(modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))

            // 8. What's behind the number
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(text = "What's behind the number")
                    Text(
                        text = "How we score",
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = Olive,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable(onClick = onMethodology)
                            .padding(4.dp),
                    )
                }

                if (state.breakdown.isNotEmpty() || state.omittedComponents.isNotEmpty()) {
                    val band = state.score?.band ?: ScoreBand.Ok
                    val accent = scoreBandColors(band).accent
                    AisleCard {
                        state.breakdown.forEachIndexed { index, component ->
                            ComponentRow(
                                name = component.label,
                                share = shareLabel(component.weight),
                                subscore = component.score.toString(),
                                detail = component.detail,
                                fillFraction = (component.score / 100f).coerceIn(0f, 1f),
                                barColor = accent,
                                showDivider = index < state.breakdown.lastIndex ||
                                    state.omittedComponents.isNotEmpty(),
                            )
                        }
                        state.omittedComponents.forEachIndexed { index, label ->
                            val name = label.removeSuffix(" (no data)").trim()
                            ComponentRow(
                                name = name.ifEmpty { label },
                                share = "omitted",
                                subscore = "—",
                                detail = omittedDetail(label),
                                fillFraction = 0f,
                                barColor = CardBorder,
                                muted = true,
                                showDivider = index < state.omittedComponents.lastIndex,
                            )
                        }
                    }
                }
            }

            // 9. Suspect ingredients
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 12.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeader(text = "Suspect ingredients")
                when {
                    state.beautyScoringPending -> {
                        Text(
                            text = "Beauty ingredient flags will appear here once scoring lands.",
                            fontFamily = PublicSans,
                            fontSize = 12.5.sp,
                            color = MutedText60,
                        )
                    }
                    state.concerns.isEmpty() -> {
                        AisleCard(contentPadding = 16.dp) {
                            Text(
                                text = ZERO_CONCERNS_COPY,
                                fontFamily = PublicSans,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = MutedText70,
                            )
                        }
                    }
                    else -> {
                        state.concerns.forEach { concern ->
                            ConcernCard(
                                concern = concern,
                                onClick = { onConcernClick(concern.id) },
                            )
                        }
                    }
                }
            }

            // 10. Nutrition entry (food only)
            if (product.category == ProductCategory.Food) {
                NutritionNavRow(
                    grade = nutriGrade,
                    onClick = { onNutrition(product.barcode) },
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                )
            }

            // 11. Ingredients text
            if (!state.ingredientsText.isNullOrBlank()) {
                Text(
                    text = "Ingredients: ${state.ingredientsText}",
                    fontFamily = PublicSans,
                    fontSize = 10.5.sp,
                    lineHeight = 16.8.sp,
                    color = MutedText45,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                )
            }
        } else if (isPartial && state.disclaimerVisible) {
            // Partial already shows message + secondary CTA inside PartialScoreBlock area
            Spacer(Modifier.height(8.dp))
        }

        // 12. Mandatory disclaimer (all result states except not-found)
        if (state.disclaimerVisible) {
            DashedBorderCard(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(
                    text = RESULT_DISCLAIMER + " ",
                    fontFamily = PublicSans,
                    fontSize = 11.sp,
                    lineHeight = 17.6.sp,
                    color = MutedText55,
                )
                Text(
                    text = "How we score",
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Olive,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable(onClick = onMethodology)
                        .padding(top = 2.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PartialScoreBlock(message: String) {
    Spacer(Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .size(110.dp)
            .background(CardWhite, CircleShape)
            .border(1.dp, CardBorderStrong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "—",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                color = Ink.copy(alpha = 0.35f),
                modifier = Modifier.semantics { contentDescription = "Score not available" },
            )
            Text(
                text = "unscored",
                fontFamily = PublicSans,
                fontSize = 10.sp,
                color = MutedText45,
            )
        }
    }
    OutlineInfoChip(label = "Confidence: Low")
    Text(
        text = message,
        fontFamily = PublicSans,
        fontSize = 13.sp,
        lineHeight = 20.8.sp,
        color = MutedText70,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp),
    )
    // Secondary CTA visual — opens contribute path is not wired for partial; keep label only.
    Text(
        text = "Add the ingredient list",
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        color = Olive,
        modifier = Modifier
            .padding(top = 8.dp)
            .border(1.5.dp, Olive.copy(alpha = 0.5f), AisleSpyShapes.pill)
            .padding(horizontal = 26.dp, vertical = 12.dp)
            .semantics { contentDescription = "Add the ingredient list" },
    )
}

@Composable
private fun ComponentRow(
    name: String,
    share: String,
    subscore: String,
    detail: String?,
    fillFraction: Float,
    barColor: androidx.compose.ui.graphics.Color,
    muted: Boolean = false,
    showDivider: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                fontFamily = PublicSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (muted) MutedText60 else Ink,
            )
            Text(
                text = share,
                fontFamily = IbmPlexMono,
                fontSize = 11.sp,
                color = MutedText55,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(CardBorder.copy(alpha = 0.5f), AisleSpyShapes.pill),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction.coerceIn(0f, 1f))
                        .height(5.dp)
                        .background(
                            if (muted) CardBorder else barColor,
                            AisleSpyShapes.pill,
                        ),
                )
            }
            Text(
                text = subscore,
                fontFamily = PublicSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (muted) MutedText55 else Ink,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End,
            )
        }
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                fontFamily = PublicSans,
                fontSize = 11.5.sp,
                lineHeight = 16.7.sp,
                color = MutedText60,
            )
        }
    }
    if (showDivider) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder.copy(alpha = 0.6f)),
        )
    }
}

@Composable
private fun ConcernCard(
    concern: ConcernUi,
    onClick: () -> Unit,
) {
    AisleCard(
        onClick = onClick,
        contentPadding = 0.dp,
        modifier = Modifier.semantics {
            contentDescription =
                "${concern.name}, severity ${concern.severity} of 5. ${concern.shortWhy}"
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = concern.name,
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MutedText45,
                    modifier = Modifier.size(18.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SeverityBar(severity = concern.severity)
                if (!concern.positionHint.isNullOrBlank()) {
                    Text(
                        text = "· ${concern.positionHint.replaceFirstChar { it.lowercase() }}",
                        fontFamily = PublicSans,
                        fontSize = 10.5.sp,
                        color = MutedText45,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = concern.shortWhy,
                fontFamily = PublicSans,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = MutedText70,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NutritionNavRow(
    grade: Char?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AisleCard(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = "Nutrition information"
        },
        contentPadding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(OliveContainer, AisleSpyShapes.smallTile),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = grade?.uppercaseChar()?.toString() ?: "—",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = OliveOnContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nutrition",
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Ink,
                )
                Text(
                    text = "Nutri-Score + per-100 g figures · not part of the score",
                    fontFamily = PublicSans,
                    fontSize = 11.sp,
                    color = MutedText55,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedText45,
            )
        }
    }
}

@Composable
private fun NotFoundContent(
    state: ResultUiState.NotFound,
    onBack: () -> Unit,
    onScanAnother: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BackLink(
            onClick = onBack,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(CardWhite, CircleShape)
                    .border(
                        width = 1.dp,
                        color = CardBorder.copy(alpha = 0.9f),
                        shape = CircleShape,
                    )
                    .border(1.dp, MutedText45.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Dashed feel via lighter border; numeral
                Text(
                    text = "?",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = Ink.copy(alpha = 0.35f),
                )
            }
            Text(
                text = "Cold case",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = state.barcode,
                fontFamily = IbmPlexMono,
                fontSize = 12.sp,
                color = MutedText55,
            )
            Text(
                text = "This barcode isn't in Open Food Facts or Open Beauty Facts yet. " +
                    "You could be the first to file it.",
                fontFamily = PublicSans,
                fontSize = 13.5.sp,
                lineHeight = 21.6.sp,
                color = MutedText70,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            AislePrimaryButton(
                text = "Scan another",
                onClick = onScanAnother,
            )
            AisleSecondaryButton(
                text = "Contribute this product",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(state.contributeFoodUrl)),
                    )
                },
            )
            Text(
                text = "Or add on Open Beauty Facts",
                fontFamily = PublicSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Olive,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(state.contributeBeautyUrl)),
                        )
                    }
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun NetworkErrorContent(
    state: ResultUiState.NetworkError,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onScanAnother: () -> Unit,
) {
    // Reuse not-found layout with retry copy (handoff Gaps note).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BackLink(
            onClick = onBack,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(CardWhite, CircleShape)
                    .border(1.dp, MutedText45.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = Ink.copy(alpha = 0.35f),
                )
            }
            Text(
                text = "Lost contact",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = state.barcode,
                fontFamily = IbmPlexMono,
                fontSize = 12.sp,
                color = MutedText55,
            )
            Text(
                text = state.message.ifBlank {
                    "Check your connection and try again. Only the barcode is sent."
                },
                fontFamily = PublicSans,
                fontSize = 13.5.sp,
                lineHeight = 21.6.sp,
                color = MutedText70,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            AislePrimaryButton(text = "Retry", onClick = onRetry)
            AisleSecondaryButton(text = "Scan another", onClick = onScanAnother)
        }
    }
}

private fun sourceLabel(category: ProductCategory, sourceDb: SourceDb): String {
    val kind = when (category) {
        ProductCategory.Food -> "FOOD"
        ProductCategory.Beauty -> "BEAUTY"
    }
    val db = when (sourceDb) {
        SourceDb.OpenFoodFacts -> "OPEN FOOD FACTS"
        SourceDb.OpenBeautyFacts -> "OPEN BEAUTY FACTS"
    }
    return "$kind · $db"
}

/** Map ViewModel confidence labels to handoff chip copy without changing VM. */
internal fun formatConfidenceChip(label: String): String = when {
    label.contains("High", ignoreCase = true) -> "Confidence: High"
    label.contains("Partial", ignoreCase = true) ||
        label.contains("Medium", ignoreCase = true) -> "Confidence: Medium"
    label.contains("Low", ignoreCase = true) -> "Confidence: Low"
    label.startsWith("Confidence", ignoreCase = true) -> label
    else -> "Confidence: $label"
}

private fun shareLabel(weight: Float): String {
    if (weight <= 0f) return "—"
    val pct = kotlin.math.round(weight * 100f).toInt()
    return "$pct% of score"
}

/** Handoff detail for omitted components. */
internal fun formatOmittedComponentLabel(label: String): String {
    val name = label.removeSuffix(" (no data)").trim()
    return if (name.isNotEmpty() && name != label) {
        "No $name data — score reweighted"
    } else {
        "No data — score reweighted"
    }
}

private fun omittedDetail(label: String): String = formatOmittedComponentLabel(label)
