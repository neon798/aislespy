package app.aislespy.ui.scan

/**
 * Manual-entry barcode validation (design handoff §3 / UI_UX §manual_entry).
 *
 * Digits only, length 8–13 (EAN-8 through EAN-13 family).
 */
object BarcodeValidation {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 13

    fun isValid(input: String): Boolean {
        if (input.isEmpty() || input.any { !it.isDigit() }) return false
        return input.length in MIN_LENGTH..MAX_LENGTH
    }

    /** Strip non-digits and cap at [MAX_LENGTH] for progressive typing. */
    fun filterDigits(raw: String): String =
        raw.filter { it.isDigit() }.take(MAX_LENGTH)

    /** True when the field has content but is not yet a valid barcode length. */
    fun showLengthError(input: String): Boolean =
        input.isNotEmpty() && input.length < MIN_LENGTH
}
