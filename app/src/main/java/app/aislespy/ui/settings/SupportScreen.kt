package app.aislespy.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aislespy.ui.components.AisleCard
import app.aislespy.ui.components.AislePrimaryButton
import app.aislespy.ui.components.AisleSecondaryButton
import app.aislespy.ui.components.BackLink
import app.aislespy.ui.theme.AisleColors
import app.aislespy.ui.theme.IbmPlexMono
import app.aislespy.ui.theme.PublicSans
import kotlinx.coroutines.delay

/**
 * Settings sub-screen: optional Bitcoin donation to support AisleSpy development.
 *
 * Presentation only — no new network hosts. QR encoding is omitted because
 * zxing-cpp 2.3.0 (already in the app) exposes [zxingcpp.BarcodeReader] only.
 */
@Composable
fun SupportScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val address = SupportConfig.BITCOIN_ADDRESS
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(2_000)
            statusMessage = null
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
                    text = "Support AisleSpy",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AisleColors.current.ink,
                    modifier = Modifier.semantics { heading() },
                )

                Text(
                    text = "AisleSpy is free and open-source, with no ads and no tracking. " +
                        "If it's useful to you, you can chip in to support development. " +
                        "Entirely optional.",
                    fontFamily = PublicSans,
                    fontSize = 13.sp,
                    lineHeight = 20.8.sp,
                    color = AisleColors.current.muted70,
                )

                AisleCard(contentPadding = 15.dp) {
                    Text(
                        text = "Bitcoin (BTC)",
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AisleColors.current.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    SelectionContainer {
                        Text(
                            text = address,
                            fontFamily = IbmPlexMono,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = AisleColors.current.ink,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    AislePrimaryButton(
                        text = if (statusMessage == "Copied") "Copied" else "Copy address",
                        onClick = {
                            copyAddress(context, address)
                            statusMessage = "Copied"
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    AisleSecondaryButton(
                        text = "Open in wallet",
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("bitcoin:$address"),
                                    ),
                                )
                            } catch (_: ActivityNotFoundException) {
                                copyAddress(context, address)
                                statusMessage = "No wallet app found — address copied"
                            }
                        },
                    )
                    if (statusMessage != null && statusMessage != "Copied") {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = statusMessage!!,
                            fontFamily = PublicSans,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = AisleColors.current.muted60,
                        )
                    }
                }

                Text(
                    text = "More ways to support may be added later.",
                    fontFamily = PublicSans,
                    fontSize = 11.sp,
                    lineHeight = 17.6.sp,
                    color = AisleColors.current.muted55,
                )
            }
        }
    }
}

private fun copyAddress(context: Context, address: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Bitcoin address", address))
}
