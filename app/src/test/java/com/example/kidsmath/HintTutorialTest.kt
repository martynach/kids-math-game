package com.example.kidsmath

import org.junit.Assert.*
import org.junit.Test

class HintTutorialTest {
    @Test fun campaignSplitUsesRoundedUpThirdIncludingSmallCampaigns() {
        for ((total, automatic) in listOf(1 to 1, 2 to 1, 3 to 1, 5 to 2, 6 to 2,
            12 to 4, 15 to 5, 30 to 10)) {
            (1..total).forEach { level ->
                assertEquals("$level/$total", level <= automatic, StarHintRules.automatic(level, total))
                assertEquals(level <= automatic, StarHintState(Question(1, 0, true), level, total).automatic)
            }
        }
        assertThrows(IllegalArgumentException::class.java) { StarHintRules.automatic(1, 0) }
    }
    @Test fun exactlyThreeFiniteCyclesSeparatedByEightSeconds() {
        val timer = HintTutorialTimer()
        var now = 0L
        repeat(3) { cycle ->
            assertNull(timer.frame(now))
            assertNull(timer.frame(now + 7_000))
            assertEquals(0f, timer.frame(now + HINT_IDLE_TIMEOUT_MS))
            assertEquals(cycle + 1, timer.cycles)
            repeat(HINT_TUTORIAL_MOVES) { move ->
                assertNotNull(timer.frame(now + HINT_IDLE_TIMEOUT_MS + move * HINT_TUTORIAL_MOVE_MS + 450))
            }
            now += HINT_IDLE_TIMEOUT_MS + HINT_TUTORIAL_MOVES * HINT_TUTORIAL_MOVE_MS
            assertNull(timer.frame(now))
        }
        assertNull(timer.frame(now + 100_000))
        assertEquals(3, timer.cycles)
    }
    @Test fun interruptionHidesAndRestartsIdleWithoutRefundingCycle() {
        val timer = HintTutorialTimer()
        timer.frame(0); assertNotNull(timer.frame(8_000))
        timer.interrupt()
        assertNull(timer.frame(8_100)); assertNull(timer.frame(15_100))
        assertNotNull(timer.frame(16_100)); assertEquals(2, timer.cycles)
        timer.interrupt(); timer.frame(16_200); timer.frame(24_200)
        timer.interrupt(); assertNull(timer.frame(100_000))
        assertEquals(3, timer.cycles)
    }
    @Test fun extractionAndDimmingAreSessionMilestonesNotCurrentCounts() {
        for (addition in listOf(true, false)) {
            val session = StarHintState(Question(3, 1, addition), 2, 3)
            assertEquals(HintTutorialStage.EXTRACT, session.tutorialStage)
            val id = session.add(HintColor.YELLOW)!!
            assertEquals(if (addition) null else HintTutorialStage.DIM, session.tutorialStage)
            if (!addition) {
                assertFalse(session.stars.single().crossed)
                session.toggle(id); assertNull(session.tutorialStage)
                session.toggle(id); assertNull(session.tutorialStage)
            }
            session.beginReturn(id, HintColor.YELLOW); session.finishReturn(id)
            assertNull(session.tutorialStage)
            session.add(HintColor.YELLOW); assertNull(session.tutorialStage)
            assertEquals(HintTutorialStage.EXTRACT, StarHintState(session.question, 2, 3).tutorialStage)
        }
        assertNull(StarHintState(Question(3, 1, false), 1, 3).tutorialStage)
    }
    @Test fun dimStageWaitsForLitStarAndKeepsIndependentBudget() {
        val session = StarHintState(Question(3, 1, false), 2, 3)
        val extract = HintTutorialTimer(); val dim = HintTutorialTimer()
        extract.frame(0); extract.frame(8_000)
        val id = session.add(HintColor.YELLOW)!!
        session.beginReturn(id, HintColor.YELLOW)
        assertNull(session.tutorialStage)
        session.finishReturn(id); assertNull(session.tutorialStage)
        session.add(HintColor.YELLOW)
        assertEquals(HintTutorialStage.DIM, session.tutorialStage)
        dim.frame(0); dim.frame(8_000)
        assertEquals(1, extract.cycles); assertEquals(1, dim.cycles)
    }
}
