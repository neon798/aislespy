package app.aislespy.ui.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import zxingcpp.BarcodeReader

/**
 * CameraX [ImageAnalysis.Analyzer] using zxing-cpp (FOSS; no ML Kit).
 *
 * Restricted to retail product formats: EAN-13, EAN-8, UPC-A, UPC-E.
 * Always closes [ImageProxy] in a finally block.
 */
class BarcodeAnalyzer(
    private val debouncer: ScanDebouncer,
    private val onBarcodeAccepted: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val reader = BarcodeReader(
        BarcodeReader.Options(
            formats = PRODUCT_FORMATS,
            tryHarder = false,
            tryRotate = true,
            tryInvert = true,
        ),
    )

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val results = reader.read(imageProxy)
            val text = results.firstOrNull()?.text
            val accepted = debouncer.onDecoded(text)
            if (accepted != null) {
                onBarcodeAccepted(accepted)
            }
        } catch (_: Throwable) {
            // Defensive: bad frames / native errors must not crash the analyzer loop.
        } finally {
            imageProxy.close()
        }
    }

    companion object {
        val PRODUCT_FORMATS: Set<BarcodeReader.Format> = setOf(
            BarcodeReader.Format.EAN_13,
            BarcodeReader.Format.EAN_8,
            BarcodeReader.Format.UPC_A,
            BarcodeReader.Format.UPC_E,
        )
    }
}
