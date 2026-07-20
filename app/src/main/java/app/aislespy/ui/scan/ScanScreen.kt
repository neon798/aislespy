package app.aislespy.ui.scan

import android.Manifest
import android.app.Application
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aislespy.ui.components.AislePrimaryButton
import app.aislespy.ui.components.AisleSecondaryButton
import app.aislespy.ui.theme.AisleColors
import app.aislespy.ui.theme.AisleSpyShapes
import app.aislespy.ui.theme.AisleSpyTextStyles
import app.aislespy.ui.theme.PaleLime
import app.aislespy.ui.theme.PublicSans
import app.aislespy.ui.theme.ScanBackground
import app.aislespy.ui.util.rememberReducedMotion
import java.util.concurrent.Executors

private const val SCAN_HINT = "Line up a barcode inside the frame"
private const val SCAN_RUNNING = "Running recon…"

private const val RATIONALE_COPY =
    "Camera access is only used to read product barcodes. " +
        "Lookups send only the barcode to Open Food Facts / Open Beauty Facts — nothing else leaves the device."

private const val DENIED_COPY =
    "Camera permission needed to scan. You can still type the barcode instead."

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
    // onSettings / onHistory / onRecentClick kept for NavGraph call-site stability.
    @Suppress("UNUSED_PARAMETER")
    val unusedSettings = onSettings
    @Suppress("UNUSED_PARAMETER")
    val unusedHistory = onHistory
    @Suppress("UNUSED_PARAMETER")
    val unusedRecent = onRecentClick

    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel = scanViewModel ?: viewModel(factory = ScanViewModel.Factory(app))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var runningRecon by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionStatus(granted = granted, fromUserRequest = true)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionStatus(granted = granted, fromUserRequest = false)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ScanEvent.NavigateToResult -> {
                    runningRecon = true
                    onBarcodeDecoded(event.barcode)
                }
            }
        }
    }

    val aisleColors = AisleColors.current
    Scaffold(
        modifier = modifier,
        containerColor = aisleColors.surface,
    ) { innerPadding ->
        when (uiState.permission) {
            CameraPermission.Granted -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(aisleColors.surface),
                ) {
                    ScanGrantedContent(
                        torchEnabled = uiState.torchEnabled,
                        runningRecon = runningRecon,
                        lastError = uiState.lastError,
                        debouncer = viewModel.debouncer(),
                        onBarcodeAccepted = viewModel::onBarcodeAccepted,
                        onCameraError = viewModel::reportCameraError,
                        onToggleTorch = viewModel::toggleTorch,
                        onManualEntry = onManualEntry,
                        modifier = Modifier.fillMaxSize(),
                    )
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
private fun ScanGrantedContent(
    torchEnabled: Boolean,
    runningRecon: Boolean,
    lastError: String?,
    debouncer: ScanDebouncer,
    onBarcodeAccepted: (String) -> Unit,
    onCameraError: (String) -> Unit,
    onToggleTorch: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Header: wordmark + privacy chip
        RowHeader(
            torchEnabled = torchEnabled,
            onToggleTorch = onToggleTorch,
        )

        // Viewfinder
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp)
                .clip(AisleSpyShapes.viewfinder)
                .background(Color(0xFF2B2921)),
        ) {
            CameraPreview(
                torchEnabled = torchEnabled,
                debouncer = debouncer,
                onBarcodeAccepted = onBarcodeAccepted,
                onCameraError = onCameraError,
                modifier = Modifier.fillMaxSize(),
            )

            ViewfinderCorners(modifier = Modifier.fillMaxSize().padding(36.dp, 26.dp))

            if (runningRecon) {
                ScanLine(modifier = Modifier.fillMaxSize())
            }

            // a11y ready state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Camera ready, $SCAN_HINT"
                    },
            )
        }

        // Bottom controls
        val colors = AisleColors.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (runningRecon) SCAN_RUNNING else SCAN_HINT,
                fontFamily = PublicSans,
                fontSize = 12.5.sp,
                color = colors.muted55,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    contentDescription = if (runningRecon) SCAN_RUNNING else SCAN_HINT
                },
            )

            lastError?.let { err ->
                Text(
                    text = err,
                    fontFamily = PublicSans,
                    fontSize = 11.sp,
                    color = PaleLime,
                    textAlign = TextAlign.Center,
                )
            }

            ShutterButton(
                decoding = runningRecon,
                modifier = Modifier.semantics {
                    contentDescription = if (runningRecon) {
                        "Scanning in progress"
                    } else {
                        "Scan shutter"
                    }
                },
            )

            Text(
                text = "Type the barcode instead",
                fontFamily = PublicSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = colors.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable(onClick = onManualEntry)
                    .padding(4.dp)
                    .semantics { contentDescription = "Type the barcode instead" },
            )
        }
    }
}

@Composable
private fun RowHeader(
    torchEnabled: Boolean,
    onToggleTorch: () -> Unit,
) {
    val colors = AisleColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AisleSpy",
            style = AisleSpyTextStyles.wordmark.copy(fontSize = 18.sp),
            color = colors.ink,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "only the barcode is sent",
                fontFamily = PublicSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                color = colors.primary,
                modifier = Modifier
                    .border(1.dp, colors.primary.copy(alpha = 0.35f), AisleSpyShapes.pill)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .semantics { contentDescription = "only the barcode is sent" },
            )
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(36.dp)
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
                    tint = colors.ink.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ViewfinderCorners(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val arm = 34.dp.toPx()
        val stroke = 4.dp.toPx()
        val radius = 8.dp.toPx()
        val color = PaleLime

        fun corner(ox: Float, oy: Float, flipX: Boolean, flipY: Boolean) {
            val sx = if (flipX) -1f else 1f
            val sy = if (flipY) -1f else 1f
            // Horizontal arm
            drawLine(
                color = color,
                start = Offset(ox, oy),
                end = Offset(ox + sx * arm, oy),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            // Vertical arm
            drawLine(
                color = color,
                start = Offset(ox, oy),
                end = Offset(ox, oy + sy * arm),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            // Soft radius hint via short arc at corner
            drawRoundRect(
                color = color,
                topLeft = Offset(
                    if (flipX) ox - radius else ox,
                    if (flipY) oy - radius else oy,
                ),
                size = ComposeSize(radius, radius),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = stroke),
            )
        }

        corner(0f, 0f, flipX = false, flipY = false)
        corner(size.width, 0f, flipX = true, flipY = false)
        corner(0f, size.height, flipX = false, flipY = true)
        corner(size.width, size.height, flipX = true, flipY = true)
    }
}

@Composable
private fun ScanLine(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    if (reduced) {
        // Static mid-frame line when reduce-motion is on.
        Box(modifier = modifier) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.84f)
                    .height(2.dp)
                    .background(PaleLime),
            )
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "scanline")
    val t by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.84f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanline-y",
    )

    Canvas(modifier = modifier) {
        val y = size.height * t
        val inset = size.width * 0.08f
        drawLine(
            color = PaleLime,
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        // Soft glow
        drawLine(
            color = PaleLime.copy(alpha = 0.35f),
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = 10.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ShutterButton(
    decoding: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AisleColors.current
    // Lime accent while decoding; otherwise theme primary fill + onPrimary glyph.
    val fill = if (decoding) PaleLime else colors.primary
    val glyph = if (decoding) ScanBackground else colors.onPrimary
    Box(
        modifier = modifier
            .size(74.dp)
            .border(5.dp, colors.primary.copy(alpha = 0.25f), CircleShape)
            .padding(5.dp)
            .background(color = fill, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Viewfinder glyph
        Canvas(modifier = Modifier.size(26.dp)) {
            val stroke = 3.dp.toPx()
            val r = 7.dp.toPx()
            drawRoundRect(
                color = glyph,
                topLeft = Offset(0f, 0f),
                size = ComposeSize(size.width, size.height),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = stroke),
            )
            drawLine(
                color = glyph,
                start = Offset(-3.dp.toPx(), size.height / 2f),
                end = Offset(size.width + 3.dp.toPx(), size.height / 2f),
                strokeWidth = stroke,
            )
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
    val colors = AisleColors.current
    Column(
        modifier = modifier.padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "AisleSpy",
            style = AisleSpyTextStyles.wordmark,
            color = colors.ink,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (denied) "Camera access denied" else "Camera for barcodes only",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (denied) DENIED_COPY else RATIONALE_COPY,
            fontFamily = PublicSans,
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            color = colors.muted55,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        AislePrimaryButton(
            text = if (denied) "Grant camera access" else "Allow camera",
            onClick = onGrant,
        )
        Spacer(Modifier.height(12.dp))
        AisleSecondaryButton(
            text = "Type the barcode instead",
            onClick = onManualEntry,
        )
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
