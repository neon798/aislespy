package app.aislespy.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.domain.model.ProductCategory
import app.aislespy.ui.components.ScoreBadge
import app.aislespy.ui.history.HistoryItemUi
import app.aislespy.ui.theme.brandAmber
import java.util.concurrent.Executors

/** Amber reticle accent from UI_UX visual language. */
private val ReticleAmber = brandAmber

/** Overlay scan hint (readable + TalkBack). */
private const val SCAN_HINT = "Point at a barcode"

private const val RATIONALE_COPY =
    "Camera access is only used to read product barcodes. " +
        "Nothing is uploaded except the barcode lookup to Open Food Facts / Open Beauty Facts."

private const val DENIED_COPY =
    "Camera permission needed to scan. You can still enter a barcode manually."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onManualEntry: () -> Unit,
    onBarcodeDecoded: (barcode: String) -> Unit,
    onSettings: () -> Unit = {},
    onHistory: () -> Unit = {},
    onRecentClick: (barcode: String, source: String) -> Unit = { barcode, _ ->
        onBarcodeDecoded(barcode)
    },
    modifier: Modifier = Modifier,
    scanViewModel: ScanViewModel? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel = scanViewModel ?: viewModel(factory = ScanViewModel.Factory(app))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionStatus(granted = granted, fromUserRequest = true)
    }

    // Sync initial / resumed permission without auto-prompting.
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionStatus(granted = granted, fromUserRequest = false)
    }

    // Single navigation on accepted decode (debounced in analyzer).
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ScanEvent.NavigateToResult -> onBarcodeDecoded(event.barcode)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("AisleSpy") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = if (uiState.permission == CameraPermission.Granted) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
            )
        },
    ) { innerPadding ->
        when (uiState.permission) {
            CameraPermission.Granted -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    CameraPreview(
                        torchEnabled = uiState.torchEnabled,
                        debouncer = viewModel.debouncer(),
                        onBarcodeAccepted = viewModel::onBarcodeAccepted,
                        onCameraError = viewModel::reportCameraError,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ScanOverlay(
                        torchEnabled = uiState.torchEnabled,
                        onToggleTorch = viewModel::toggleTorch,
                        onManualEntry = onManualEntry,
                        recent = uiState.recent,
                        onRecentClick = onRecentClick,
                        onHistory = onHistory,
                        modifier = Modifier.fillMaxSize(),
                    )
                    uiState.lastError?.let { err ->
                        Text(
                            text = err,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                        )
                    }
                }
            }

            CameraPermission.Rationale,
            CameraPermission.Denied,
            -> {
                PermissionPane(
                    denied = uiState.permission == CameraPermission.Denied,
                    onGrant = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onManualEntry = onManualEntry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun PermissionPane(
    denied: Boolean,
    onGrant: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (denied) DENIED_COPY else RATIONALE_COPY,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Grant camera access")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onManualEntry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Enter barcode manually")
        }
    }
}

@Composable
private fun ScanOverlay(
    torchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onManualEntry: () -> Unit,
    recent: List<HistoryItemUi>,
    onRecentClick: (barcode: String, source: String) -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Decorative viewfinder; a11y announces ready state + scan hint.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Camera ready, $SCAN_HINT"
                },
        ) {
            val frameWidth = size.width * 0.72f
            val frameHeight = frameWidth * 0.55f
            val left = (size.width - frameWidth) / 2f
            val top = (size.height - frameHeight) / 2f - size.height * 0.05f
            val corner = 16.dp.toPx()
            val stroke = 3.dp.toPx()
            val cornerLen = 28.dp.toPx()

            // Amber corner brackets (rounded-rect accent per UI_UX)
            // Top-left
            drawLine(ReticleAmber, Offset(left, top + cornerLen), Offset(left, top + corner), stroke)
            drawLine(ReticleAmber, Offset(left, top), Offset(left + cornerLen, top), stroke)
            // Top-right
            drawLine(ReticleAmber, Offset(left + frameWidth - cornerLen, top), Offset(left + frameWidth, top), stroke)
            drawLine(ReticleAmber, Offset(left + frameWidth, top), Offset(left + frameWidth, top + cornerLen), stroke)
            // Bottom-left
            drawLine(ReticleAmber, Offset(left, top + frameHeight - cornerLen), Offset(left, top + frameHeight), stroke)
            drawLine(ReticleAmber, Offset(left, top + frameHeight), Offset(left + cornerLen, top + frameHeight), stroke)
            // Bottom-right
            drawLine(
                ReticleAmber,
                Offset(left + frameWidth - cornerLen, top + frameHeight),
                Offset(left + frameWidth, top + frameHeight),
                stroke,
            )
            drawLine(
                ReticleAmber,
                Offset(left + frameWidth, top + frameHeight - cornerLen),
                Offset(left + frameWidth, top + frameHeight),
                stroke,
            )
            // Soft dashed outline
            drawRoundRect(
                color = ReticleAmber.copy(alpha = 0.45f),
                topLeft = Offset(left, top),
                size = ComposeSize(frameWidth, frameHeight),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                ),
            )
        }

        Text(
            text = SCAN_HINT,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 140.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics {
                    contentDescription = SCAN_HINT
                },
        )

        IconButton(
            onClick = onToggleTorch,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                .semantics {
                    contentDescription = if (torchEnabled) {
                        "Flashlight on, double-tap to turn off"
                    } else {
                        "Flashlight off, double-tap to turn on"
                    }
                },
        ) {
            Icon(
                imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = null,
                tint = Color.White,
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.55f),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onManualEntry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Enter barcode")
                }
                if (recent.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recent.forEach { item ->
                            RecentChip(
                                item = item,
                                onClick = {
                                    val source = when (item.category) {
                                        ProductCategory.Food -> "food"
                                        ProductCategory.Beauty -> "beauty"
                                    }
                                    onRecentClick(item.barcode, source)
                                },
                            )
                        }
                    }
                }
                TextButton(onClick = onHistory) {
                    Text("History", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RecentChip(
    item: HistoryItemUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${item.name}, score ${item.score}"
            },
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ScoreBadge(
                score = item.score,
                band = item.band,
                contentDescription = null,
                compact = true,
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
    }
}

@Composable
private fun CameraPreview(
    torchEnabled: Boolean,
    debouncer: ScanDebouncer,
    onBarcodeAccepted: (String) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    // Snapshot torch for the bind callback without rebinding on every toggle.
    val torchEnabledState = remember { mutableStateOf(torchEnabled) }
    torchEnabledState.value = torchEnabled

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        val bindRunnable = Runnable {
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                provider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            analysisExecutor,
                            BarcodeAnalyzer(debouncer) { code ->
                                mainExecutor.execute { onBarcodeAccepted(code) }
                            },
                        )
                    }

                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
                boundCamera = camera
                if (camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(torchEnabledState.value)
                }
            } catch (t: Throwable) {
                onCameraError(t.message ?: "Camera failed to start")
            }
        }

        cameraProviderFuture.addListener(bindRunnable, mainExecutor)

        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (_: Throwable) {
                // ignore unbind races on teardown
            }
            boundCamera = null
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(torchEnabled, boundCamera) {
        val camera = boundCamera ?: return@LaunchedEffect
        if (camera.cameraInfo.hasFlashUnit()) {
            try {
                camera.cameraControl.enableTorch(torchEnabled)
            } catch (_: Throwable) {
                // Some devices reject torch while inactive; ignore.
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
