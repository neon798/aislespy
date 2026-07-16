package app.aislespy.ui.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure JVM tests for [ScanDebouncer] — fake clock, no Android/CameraX.
 */
class ScanDebouncerTest {

    private class FakeClock(var now: Long = 0L) {
        fun tick(ms: Long = 1L) {
            now += ms
        }

        fun asProvider(): () -> Long = { now }
    }

    @Test
    fun firstFrame_notAccepted_untilTwoStableFrames() {
        val clock = FakeClock(1_000L)
        val debouncer = ScanDebouncer(clock = clock.asProvider())

        assertNull(debouncer.onDecoded("3017624010701"))
        assertEquals("3017624010701", debouncer.onDecoded("3017624010701"))
    }

    @Test
    fun sameCode_within2s_rejected() {
        val clock = FakeClock(0L)
        val debouncer = ScanDebouncer(clock = clock.asProvider())

        assertNull(debouncer.onDecoded("12345678"))
        assertEquals("12345678", debouncer.onDecoded("12345678"))

        // Still within debounce window — even with more stable frames.
        clock.tick(500L)
        assertNull(debouncer.onDecoded("12345678"))
        assertNull(debouncer.onDecoded("12345678"))

        clock.tick(1_499L) // total 1999 ms since accept
        assertNull(debouncer.onDecoded("12345678"))
    }

    @Test
    fun sameCode_after2s_accepted() {
        val clock = FakeClock(0L)
        val debouncer = ScanDebouncer(clock = clock.asProvider())

        assertNull(debouncer.onDecoded("12345678"))
        assertEquals("12345678", debouncer.onDecoded("12345678"))

        clock.tick(2_000L)
        // Consecutive frames still count; same candidate remains stable.
        assertEquals("12345678", debouncer.onDecoded("12345678"))
    }

    @Test
    fun differentCode_needsStability_firstFrameNotAccepted() {
        val clock = FakeClock(0L)
        val debouncer = ScanDebouncer(clock = clock.asProvider())

        // Accept first code
        assertNull(debouncer.onDecoded("11111111"))
        assertEquals("11111111", debouncer.onDecoded("11111111"))

        // Flicker to a different code — single frame must not fire navigation
        assertNull(debouncer.onDecoded("22222222"))
        // Second stable frame of the new code is accepted (different from last accepted)
        assertEquals("22222222", debouncer.onDecoded("22222222"))
    }

    @Test
    fun nullFrames_resetStability() {
        val clock = FakeClock(0L)
        val debouncer = ScanDebouncer(clock = clock.asProvider())

        assertNull(debouncer.onDecoded("3017624010701"))
        assertNull(debouncer.onDecoded(null)) // resets
        assertNull(debouncer.onDecoded("3017624010701")) // frame 1 again
        assertEquals("3017624010701", debouncer.onDecoded("3017624010701"))
    }

    @Test
    fun blankCode_treatedAsNoRead() {
        val clock = FakeClock(0L)
        val debouncer = ScanDebouncer(clock = clock.asProvider())

        assertNull(debouncer.onDecoded(""))
        assertNull(debouncer.onDecoded("   "))
        assertNull(debouncer.onDecoded("99999999"))
        assertEquals("99999999", debouncer.onDecoded("99999999"))
    }

    @Test
    fun rapidFlicker_betweenCodes_neverAcceptsUnstable() {
        val clock = FakeClock(0L)
        val debouncer = ScanDebouncer(clock = clock.asProvider())

        assertNull(debouncer.onDecoded("AAAAAAAA"))
        assertNull(debouncer.onDecoded("BBBBBBBB")) // different → count resets to 1
        assertNull(debouncer.onDecoded("AAAAAAAA"))
        assertNull(debouncer.onDecoded("BBBBBBBB"))
        // Never two consecutive identical frames
        assertNull(debouncer.onDecoded("AAAAAAAA"))
    }
}
