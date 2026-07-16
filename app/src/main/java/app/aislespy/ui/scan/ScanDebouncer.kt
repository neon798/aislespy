package app.aislespy.ui.scan

/**
 * Pure debounce + frame-stability gate for continuous barcode decode.
 *
 * - Rejects the same code more than once within [debounceMs].
 * - Requires [requiredStableFrames] consecutive identical non-null decodes
 *   before accepting (swallows single-frame flickers / misreads).
 * - Null/empty frames reset the stability counter.
 *
 * Injectable [clock] keeps this unit-testable on the JVM.
 */
class ScanDebouncer(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val requiredStableFrames: Int = DEFAULT_STABLE_FRAMES,
) {
    private var lastAccepted: String? = null
    private var lastAcceptedAtMs: Long = Long.MIN_VALUE / 2
    private var candidate: String? = null
    private var consecutiveFrames: Int = 0

    /**
     * Feed one analysis frame's decoded text (or null if none).
     *
     * @return accepted barcode when both stability and debounce pass; otherwise null.
     */
    @Synchronized
    fun onDecoded(code: String?): String? {
        if (code.isNullOrBlank()) {
            candidate = null
            consecutiveFrames = 0
            return null
        }

        if (code == candidate) {
            consecutiveFrames++
        } else {
            candidate = code
            consecutiveFrames = 1
        }

        if (consecutiveFrames < requiredStableFrames) {
            return null
        }

        val now = clock()
        if (code == lastAccepted && now - lastAcceptedAtMs < debounceMs) {
            return null
        }

        lastAccepted = code
        lastAcceptedAtMs = now
        return code
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS: Long = 2_000L
        const val DEFAULT_STABLE_FRAMES: Int = 2
    }
}
