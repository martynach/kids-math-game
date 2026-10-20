package com.example.kidsmath

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GeneratorFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val state get() = ViewModelProvider(compose.activity)[AppState::class.java]

    @Before fun prepare() {
        compose.runOnIdle {
            state.activatePremium()
            state.configureLimits(true, 5, 1)
            state.resetProgress()
            state.save(Settings())
        }
    }

    private fun answer(value: Int) {
        value.toString().forEach { compose.onNodeWithContentDescription(it.toString()).performClick() }
        compose.onNodeWithContentDescription("Confirm answer").performClick()
    }

    private fun waitForQuestion() {
        compose.waitUntil(8_000) {
            compose.onAllNodes(hasText(" = ?", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun updatedRangeLabelIsVisible() {
        compose.onNodeWithText("⚙  Settings").performClick()
        compose.onNodeWithText("Result range (0–100)").performScrollTo().assertIsDisplayed()
    }

    @Test fun wrongZeroQuestionRepeatsAndSmallRangeStillCompletes() {
        compose.runOnIdle {
            state.save(Settings(operations = Operations.ADDITION, max = 0))
            state.start(1)
        }
        compose.onNodeWithText("0 + 0 = ?").assertIsDisplayed()
        answer(9)
        compose.onNodeWithContentDescription("Lives remaining: 2").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, state.game!!.correct) }
        waitForQuestion()
        compose.onNodeWithText("0 + 0 = ?").assertIsDisplayed()
        repeat(CampaignConfig.levels.first().requiredCorrectAnswers) { index ->
            answer(0)
            compose.onNodeWithContentDescription("Rocket energy: ${index + 1} of ${CampaignConfig.levels.first().requiredCorrectAnswers}").assertExists()
            if (index < CampaignConfig.levels.first().requiredCorrectAnswers - 1) waitForQuestion()
        }
        compose.waitUntil(8_000) {
            compose.onAllNodesWithContentDescription("Level 1 complete").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertEquals(Phase.LEVEL_COMPLETE, state.game!!.phase)
            assertEquals(2, state.campaign.highestUnlockedLevel)
        }
    }

    @Test fun actualAppStateResetsMemoryAndUsesSubtractionRange() {
        compose.runOnIdle {
            state.save(Settings(operations = Operations.SUBTRACTION, min = 8, max = 12))
            var observedBelowMin = false
            repeat(20) {
                state.start(1)
                val seen = mutableSetOf<Question>()
                var time = 0L
                repeat(CampaignConfig.levels.first().requiredCorrectAnswers) {
                    state.update { game ->
                        val q = game.question
                        assertFalse(q.addition)
                        assertTrue(q.result in 0..12)
                        assertTrue(q.left in 0..12 && q.right in 0..12)
                        assertTrue(seen.add(q))
                        if (q.result < 8) observedBelowMin = true
                        q.result.toString().forEach { game.digit(it.digitToInt()) }
                        game.confirm(time)
                        time += GameRules.CORRECT_FEEDBACK_MS
                        game.tick(time)
                    }
                }
                assertEquals(Phase.LEVEL_COMPLETE, state.game!!.phase)
                state.leaveGame()
            }
            assertTrue(observedBelowMin)
            state.save(Settings(operations = Operations.ADDITION, max = 1))
            state.start(1)
            val oldGame = state.game
            state.retry()
            assertNotSame(oldGame, state.game)
            assertEquals(0, state.game!!.correct)
            assertEquals(GameRules.LIVES, state.game!!.lives)
            state.leaveGame()
        }
    }
}
