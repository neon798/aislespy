package app.aislespy.ui.onboarding

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.ui.components.AislePrimaryButton
import app.aislespy.ui.theme.AisleSpyTextStyles
import app.aislespy.ui.theme.BricolageGrotesque
import app.aislespy.ui.theme.PublicSans
import app.aislespy.ui.theme.scoreExcellentLight
import app.aislespy.ui.theme.scoreOkLight
import app.aislespy.ui.theme.AisleColors

private data class OnboardingStep(
    val title: String,
    val body: String,
    val cta: String,
    val showDisclaimer: Boolean = false,
)

private val Steps = listOf(
    OnboardingStep(
        title = "Your eyes in the aisle",
        body = "Scan any food or beauty barcode. AisleSpy checks it against open community databases and reports back in seconds.",
        cta = "Continue",
    ),
    OnboardingStep(
        title = "Every score shows its work",
        body = "One 1–100 score for ingredient quality — with the flagged ingredients, processing level and exact weights behind it, in plain language.",
        cta = "Continue",
    ),
    OnboardingStep(
        title = "Recon, not surveillance",
        body = "No account, no ads, no analytics. Lookups send only the barcode — your history never leaves this phone.",
        cta = "Start the recon",
        showDisclaimer = true,
    ),
)

/**
 * First-launch 3-step onboarding (design handoff §1). Completing or skipping
 * persists first-launch done via [OnboardingViewModel], then navigates to scan.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onboardingViewModel: OnboardingViewModel? = null,
) {
    val app = LocalContext.current.applicationContext as Application
    val resolvedVm = onboardingViewModel
        ?: viewModel(factory = OnboardingViewModel.Factory(app))
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = Steps[stepIndex.coerceIn(0, Steps.lastIndex)]

    LaunchedEffect(resolvedVm) {
        resolvedVm.events.collect { event ->
            when (event) {
                OnboardingEvent.NavigateToScan -> onFinished()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = AisleColors.current.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp)
                .padding(top = 20.dp, bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AisleSpy",
                    style = AisleSpyTextStyles.wordmark,
                    color = AisleColors.current.ink,
                )
                Text(
                    text = "Skip",
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = AisleColors.current.muted55,
                    modifier = Modifier
                        .clickable(onClick = resolvedVm::onStartScanning)
                        .padding(8.dp)
                        .semantics { contentDescription = "Skip onboarding" },
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(AisleColors.current.card, CircleShape)
                        .border(1.dp, AisleColors.current.cardBorderStrong, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    OnboardingStepArt(stepIndex = stepIndex)
                }

                Spacer(Modifier.height(26.dp))

                Text(
                    text = step.title,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp,
                    lineHeight = 31.sp,
                    letterSpacing = (-0.01).sp,
                    color = AisleColors.current.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .semantics { heading() },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = step.body,
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 22.4.sp,
                    color = AisleColors.current.muted70,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                if (step.showDisclaimer) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Scores are informational — not medical advice.",
                        fontFamily = PublicSans,
                        fontSize = 11.sp,
                        color = AisleColors.current.muted45,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Steps.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (i == stepIndex) AisleColors.current.olive else AisleColors.current.muted45.copy(alpha = 0.35f),
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                AislePrimaryButton(
                    text = step.cta,
                    onClick = {
                        if (stepIndex < Steps.lastIndex) {
                            stepIndex += 1
                        } else {
                            resolvedVm.onStartScanning()
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = step.cta
                    },
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepArt(stepIndex: Int) {
    when (stepIndex) {
        0 -> BarcodeBarsArt()
        1 -> ScoreRingArt()
        else -> PadlockArt()
    }
}

/** Simple barcode bars (step 0). */
@Composable
private fun BarcodeBarsArt() {
    val bars = listOf(
        4.dp to AisleColors.current.ink,
        8.dp to AisleColors.current.ink,
        4.dp to scoreOkLight,
        6.dp to AisleColors.current.ink,
        3.dp to AisleColors.current.ink,
        9.dp to scoreExcellentLight,
        4.dp to AisleColors.current.ink,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(52.dp),
    ) {
        bars.forEach { (width, color) ->
            Box(
                modifier = Modifier
                    .size(width = width, height = 52.dp)
                    .background(color),
            )
        }
    }
}

/** Mini score ring “68” (step 1). */
@Composable
private fun ScoreRingArt() {
    val trackColor = AisleColors.current.cardBorderStrong
    val ink = AisleColors.current.ink
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(96.dp),
    ) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val stroke = 8.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = scoreOkLight,
                startAngle = -90f,
                sweepAngle = 245f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "68",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = ink,
        )
    }
}

/** Padlock (step 2). */
@Composable
private fun PadlockArt() {
    val olive = AisleColors.current.olive
    val onPrimary = AisleColors.current.onPrimary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(width = 34.dp, height = 26.dp)) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = olive,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height * 1.6f),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
        }
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 44.dp)
                .background(olive, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(onPrimary, CircleShape),
            )
        }
    }
}
