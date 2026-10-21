package com.example.kidsmath

import org.junit.Assert.*
import org.junit.Test

class FinaleTest {
    @Test fun emotionsFollowOfferAndUnlockEvents() {
        for (scene in listOf(FinaleScene.CASTLE, FinaleScene.MEETING)) {
            assertEquals(0f, FinaleEmotions.dragonDelight(FinaleFrame(scene, 1f)))
        }
        assertEquals(0f, FinaleEmotions.dragonDelight(FinaleFrame(FinaleScene.GIFT, .18f)))
        assertTrue(FinaleEmotions.dragonDelight(FinaleFrame(FinaleScene.GIFT, .21f)) in .49f.. .51f)
        assertEquals(1f, FinaleEmotions.dragonDelight(FinaleFrame(FinaleScene.GIFT, .44f)))
        for (scene in FinaleScene.entries) {
            if (scene.ordinal < FinaleScene.RESCUE.ordinal) {
                assertFalse(FinaleEmotions.princessReleased(FinaleFrame(scene, 1f)))
            }
            if (scene.ordinal > FinaleScene.GIFT.ordinal) {
                assertEquals(1f, FinaleEmotions.dragonDelight(FinaleFrame(scene, 0f)))
            }
        }
        assertFalse(FinaleEmotions.princessReleased(FinaleFrame(FinaleScene.RESCUE, .319f)))
        assertTrue(FinaleEmotions.princessReleased(FinaleFrame(FinaleScene.RESCUE, .32f)))
        assertTrue(FinaleEmotions.princessReleased(FinaleFrame(FinaleScene.BOARDING, 0f)))
        assertTrue(FinaleEmotions.princessReleased(FinaleFrame(FinaleScene.HOME, 1f)))
    }
    @Test fun timelineHasEverySceneAndVictoryIsClamped() {
        assertTrue(FinaleTimeline.durationMs in 35_000..45_000)
        var start = 0L
        for (scene in FinaleScene.entries) {
            assertEquals(FinaleFrame(scene, 0f), FinaleTimeline.frame(start))
            assertEquals(scene, FinaleTimeline.frame(start + scene.durationMs - 1).scene)
            start += scene.durationMs
        }
        assertEquals(FinaleFrame(FinaleScene.HOME, 1f), FinaleTimeline.frame(Long.MAX_VALUE))
        assertEquals(0L, FinaleTimeline.advance(0, -500))
        assertEquals(FinaleTimeline.durationMs, FinaleTimeline.advance(100, Long.MAX_VALUE))
        assertEquals(FinaleTimeline.durationMs, FinaleTimeline.advance(FinaleTimeline.durationMs, 200))
    }
    @Test fun finalLevelUsesCampaignConfigurationAndReplayPreservesCompletion() {
        val last = CampaignConfig.levels.last().level
        assertFalse(FinaleTimeline.isFinalLevel(last - 1))
        assertTrue(FinaleTimeline.isFinalLevel(last))
        val completed = CampaignProgress(last).complete(last)
        assertTrue(completed.finalLevelCompleted)
        assertEquals(completed, completed.complete(last))
        assertEquals(completed, completed.complete(1))
        assertEquals(last, completed.highestUnlockedLevel)
    }
}
