package app.aislespy.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_700_000_000_000L

    @Test
    fun justNow() {
        assertEquals("just now", formatRelativeTime(now - 30_000L, now))
    }

    @Test
    fun minutes() {
        assertEquals("5m ago", formatRelativeTime(now - 5 * 60_000L, now))
    }

    @Test
    fun hours() {
        assertEquals("2h ago", formatRelativeTime(now - 2 * 60 * 60_000L, now))
    }

    @Test
    fun days() {
        assertEquals("3d ago", formatRelativeTime(now - 3 * 24 * 60 * 60_000L, now))
    }
}
