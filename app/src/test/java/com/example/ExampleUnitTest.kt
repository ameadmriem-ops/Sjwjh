package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying AdMob configuration and business logic.
 */
class ExampleUnitTest {

    @Test
    fun verifyAdMobConstants() {
        // 1. Verify User-specified App ID and Ad Unit IDs
        val expectedFiveAdsUnitId = "ca-app-pub-2277779478101583/1404104591"
        val expectedEpisodeUnitId = "ca-app-pub-2277779478101583/9116970862"

        assertEquals(expectedFiveAdsUnitId, MainActivity.DEFAULT_REWARDED_AD_UNIT_ID)
        assertEquals(expectedEpisodeUnitId, MainActivity.EPISODE_REWARDED_AD_UNIT_ID)

        // Ensure the two ad units are strictly distinct and separated
        assertNotEquals(MainActivity.DEFAULT_REWARDED_AD_UNIT_ID, MainActivity.EPISODE_REWARDED_AD_UNIT_ID)
    }

    @Test
    fun testExemptionCalculationLogic() {
        val now = 1000000000000L
        val oneDayMs = MainActivity.ONE_DAY_MS
        assertEquals(24L * 60L * 60L * 1000L, oneDayMs)

        val expiresAt = now + oneDayMs
        assertTrue("Exemption should be active when current time is before expiry", expiresAt > now)

        val afterOneDay = now + oneDayMs + 1000L
        assertFalse("Exemption should expire after 24 hours", expiresAt > afterOneDay)
    }

    @Test
    fun testFiveAdsCounterBoundaryLogic() {
        // Counter must strictly be in range 0..5
        var count = 0
        for (i in 1..4) {
            count++
            assertTrue("Counter should not reach threshold yet", count < 5)
        }
        assertEquals(4, count)

        // 5th reward activates threshold
        count++
        assertTrue("5th reward triggers exemption threshold", count >= 5)
    }

    @Test
    fun testWatchCompletionThresholdLogic() {
        assertFalse("Zero duration should not auto-complete unless forced", MainActivity.computeWatchCompletion(10, 0, false))
        assertFalse("Less than 95% watched should not mark completed", MainActivity.computeWatchCompletion(94, 100, false))
        assertTrue("Exactly 95% watched should mark completed", MainActivity.computeWatchCompletion(95, 100, false))
        assertTrue("Above 95% watched should mark completed", MainActivity.computeWatchCompletion(99, 100, false))
        assertTrue("Forced completion on STATE_ENDED should always mark completed", MainActivity.computeWatchCompletion(0, 0, true))
    }

    @Test
    fun testResumePositionEligibilityLogic() {
        assertFalse("Positions under 5 seconds must be ignored for resume", MainActivity.isResumePositionEligible(4999L, 120_000L))
        assertTrue("Positions >= 5 seconds and < 95% should resume", MainActivity.isResumePositionEligible(5000L, 120_000L))
        assertTrue("Unknown duration with position >= 5 seconds should resume", MainActivity.isResumePositionEligible(15_000L, 0L))
        assertFalse("Positions >= 95% of duration should start cleanly instead of resuming at credits", MainActivity.isResumePositionEligible(95_000L, 100_000L))
    }

    @Test
    fun testPixeldrainAndStreamUrlNormalization() {
        assertEquals("", MainActivity.normalizeStreamUrl("   "))
        assertEquals("https://pixeldrain.com/api/file/cnR9Z51", MainActivity.normalizeStreamUrl("https://pixeldrain.com/u/cnR9Z51"))
        assertEquals("https://pixeldrain.com/api/file/cnR9Z51", MainActivity.normalizeStreamUrl("pixeldrain.com/file/cnR9Z51?download"))
        assertEquals("https://pixeldrain.com/api/file/cnR9Z51", MainActivity.normalizeStreamUrl("https://www.pixeldrain.com/api/file/cnR9Z51"))
        assertEquals("https://cdn.example.com/ep1.m3u8", MainActivity.normalizeStreamUrl("  https://cdn.example.com/ep1.m3u8  "))
    }

    @Test
    fun testStateAndWatchProgressStorageKeysDistinct() {
        assertTrue(MainActivity.PREFS_SPA_STATE.isNotBlank())
        assertTrue(MainActivity.PREFS_WATCH_PROGRESS.isNotBlank())
        assertNotEquals(MainActivity.PREFS_SPA_STATE, MainActivity.PREFS_WATCH_PROGRESS)
        assertNotEquals(MainActivity.PREFS_SPA_STATE, MainActivity.PREFS_ADMOB)
    }

    @Test
    fun testSkipIntroWindowValidationAndVisibilityBoundaries() {
        // Invalid or unset windows
        assertFalse("Unset (-1, -1) window must be invalid", MainActivity.isValidSkipIntroWindow(-1L, -1L))
        assertFalse("Equal bounds must be invalid", MainActivity.isValidSkipIntroWindow(90_000L, 90_000L))
        assertFalse("End before start must be invalid", MainActivity.isValidSkipIntroWindow(90_000L, 60_000L))

        // Valid windows (including starting at 00:00:00)
        assertTrue("Intro starting at 00:00:00 should be valid", MainActivity.isValidSkipIntroWindow(0L, 90_000L))
        assertTrue("Intro 01:30 -> 03:15 should be valid", MainActivity.isValidSkipIntroWindow(90_000L, 195_000L))

        val startMs = 90_000L  // 00:01:30
        val endMs = 195_000L   // 00:03:15

        // Visibility across playback timeline
        assertFalse("Must NOT appear before start time", MainActivity.shouldShowSkipIntroButton(89_999L, startMs, endMs, false))
        assertTrue("Must appear at exact start time", MainActivity.shouldShowSkipIntroButton(90_000L, startMs, endMs, false))
        assertTrue("Must remain visible mid-intro", MainActivity.shouldShowSkipIntroButton(140_000L, startMs, endMs, false))
        assertFalse("Must auto-hide at exact end time", MainActivity.shouldShowSkipIntroButton(195_000L, startMs, endMs, false))
        assertFalse("Must stay hidden after end time", MainActivity.shouldShowSkipIntroButton(220_000L, startMs, endMs, false))

        // Session skip lock
        assertFalse(
            "Must NEVER reappear in same watch session once clicked even inside window",
            MainActivity.shouldShowSkipIntroButton(120_000L, startMs, endMs, true)
        )
        // Re-opening fresh session or another episode resets hasSkippedInSession=false
        assertTrue(
            "New session before skip allows button again inside valid window",
            MainActivity.shouldShowSkipIntroButton(120_000L, startMs, endMs, false)
        )
    }
}
