package app.aislespy.ui.onboarding

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.ui.result.ResultViewModel

/**
 * First-launch privacy / welcome screen (T-510). Shown before scan when
 * [app.aislespy.data.prefs.UserPrefs.firstLaunchDone] is false.
 *
 * Copy is short and sourced from PRIVACY.md; spy flavor only in the title.
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

    LaunchedEffect(resolvedVm) {
        resolvedVm.events.collect { event ->
            when (event) {
                OnboardingEvent.NavigateToScan -> onFinished()
            }
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Mission brief: AisleSpy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Scan food and beauty barcodes to see a clear 1–100 score " +
                    "and plain-language notes on problem ingredients—what’s really in the aisle.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            PrivacyBullet("No accounts — nothing to sign up for.")
            PrivacyBullet("No tracking or analytics in the default build.")
            PrivacyBullet(
                "Lookups send only the barcode to Open Food Facts and Open Beauty Facts.",
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = ResultViewModel.DISCLAIMER_TEXT,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = resolvedVm::onStartScanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start scanning")
            }
        }
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyLarge,
    )
}
