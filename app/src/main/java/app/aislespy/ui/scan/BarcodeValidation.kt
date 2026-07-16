package app.aislespy.ui.scan

/**
 * Manual-entry barcode validation (UI_UX §manual_entry + T-230).
 *
 * Accepts EAN-8 (8 digits) or UPC/EAN-13 family (12–14 digits). Digits only.
 */
object BarcodeValidation {
    fun isValid(input: String): Boolean {
        if (input.isEmpty() || input.any { !it.isDigit() }) return false
        val len = input.length
        return len == 8 || len in 12..14
    }

    /** Strip non-digits for progressive typing (field filters to digits). */
    fun filterDigits(raw: String): String = raw.filter { it.isDigit() }
}
