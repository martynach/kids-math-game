package com.example.kidsmath

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AnswerButtonTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val state get() = ViewModelProvider(compose.activity)[AppState::class.java]
    @Before fun resetState() { compose.runOnIdle { state.resetProgress(); state.save(Settings()) } }
    private fun start() {
        compose.onNodeWithText("Start Game").performClick()
        compose.onNodeWithText("Space Map").assertIsDisplayed()
        compose.onNodeWithContentDescription("Level 2, locked").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Level 1, unlocked").performClick()
    }
    private fun waitForQuestion() {
        compose.waitUntil(5000) { compose.onAllNodes(hasText(" = ?", substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun answer(correct: Boolean) {
        var result = 0
        compose.runOnIdle { result = if (correct) state.game!!.question.result else 999 }
        result.toString().forEach { compose.onNodeWithContentDescription(it.toString()).performClick() }
        compose.onNodeWithContentDescription("Confirm answer").performClick()
    }
    @Test fun keypadAndEnergy() {
        start()
        compose.onNodeWithContentDescription("Confirm answer").assertIsNotEnabled()
        compose.onNodeWithContentDescription("0").performClick()
        compose.onNodeWithContentDescription("Confirm answer").assertIsEnabled()
        compose.onNodeWithContentDescription("Backspace").performClick()
        compose.onNodeWithContentDescription("Confirm answer").assertIsNotEnabled()
        answer(true)
        compose.onNodeWithText("★  1 / ${CampaignConfig.levels.first().requiredCorrectAnswers}").assertIsDisplayed()
        waitForQuestion(); answer(false)
        compose.onNodeWithContentDescription("Lives remaining: 2").assertIsDisplayed()
        compose.onNodeWithText("★  1 / ${CampaignConfig.levels.first().requiredCorrectAnswers}").assertIsDisplayed()
    }
    @Test fun completionUnlocksAndReturnsToMap() {
        start()
        repeat(CampaignConfig.levels.first().requiredCorrectAnswers) { index ->
            answer(true)
            compose.onNodeWithContentDescription("Rocket energy: ${index + 1} of ${CampaignConfig.levels.first().requiredCorrectAnswers}").assertExists()
            if (index == CampaignConfig.levels.first().requiredCorrectAnswers - 2) compose.runOnIdle { assertNotEquals(Phase.LEVEL_COMPLETE, state.game!!.phase); assertFalse(compose.activity.isFinishing) }
            if (index < CampaignConfig.levels.first().requiredCorrectAnswers - 1) waitForQuestion()
        }
        compose.mainClock.autoAdvance = false
        compose.waitUntil(5000) {
            compose.mainClock.advanceTimeByFrame()
            compose.onAllNodesWithContentDescription("Level 1 complete").fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.advanceTimeBy(1400)
        compose.waitForIdle()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Transferring fuel")).assertExists()
        saveScreenshot("completion-fuel.png")
        compose.mainClock.advanceTimeBy(4000)
        compose.waitForIdle()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Celebration loop")).assertExists()
        saveScreenshot("completion-loop.png")
        compose.mainClock.autoAdvance = true
        compose.waitUntil(16000) { compose.onAllNodesWithContentDescription("Level 2, unlocked").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Level 1, completed").assertIsEnabled()
        compose.onNodeWithContentDescription("Current rocket at Level 2").assertExists()
        compose.runOnIdle { assertFalse(compose.activity.isFinishing) }
        compose.runOnIdle { assertEquals(2, CampaignStore(compose.activity).load().highestUnlockedLevel) }
    }
    @Test fun persistenceResetAndSettings() {
        compose.runOnIdle {
            unlockThrough(6)
            state.save(Settings("Karolina", Operations.SUBTRACTION, 6, 9))
            assertEquals(CampaignProgress(6), CampaignStore(compose.activity).load())
        }
        compose.onNodeWithText("⚙  Settings").performClick()
        compose.onNodeWithText("Reset Progress").performScrollTo().performClick()
        compose.onNodeWithText("Reset progress?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(6, CampaignStore(compose.activity).load().highestUnlockedLevel) }
        compose.onNodeWithText("Reset Progress").performClick()
        compose.onNodeWithText("Reset", substring = false).performClick()
        compose.runOnIdle {
            assertEquals(CampaignProgress(), CampaignStore(compose.activity).load())
            assertEquals(Settings("Karolina", Operations.SUBTRACTION, 6, 9), state.settings)
            val reloaded = AppState(compose.activity.application)
            assertEquals(CampaignProgress(), reloaded.campaign)
            assertEquals(state.settings, reloaded.settings)
        }
        compose.onNodeWithText("‹ Back").performClick()
        compose.onNodeWithText("Start Game").performClick()
        compose.onNodeWithContentDescription("Level 1, unlocked").assertIsEnabled()
        compose.onNodeWithContentDescription("Level 2, locked").assertIsNotEnabled()
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            assertEquals(CampaignProgress(), state.campaign)
            assertEquals(Settings("Karolina", Operations.SUBTRACTION, 6, 9), state.settings)
        }
    }
    @Test fun gameOverRetryAndLeavingAttempt() {
        start()
        repeat(3) { index -> answer(false); if (index < 2) waitForQuestion() }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithContentDescription("Lives remaining: 3").assertIsDisplayed()
        compose.onNodeWithText("⌂ Map").performClick()
        compose.onNodeWithText("Keep Playing").performClick()
        compose.onNodeWithText("⌂ Map").performClick()
        compose.onNodeWithText("Space Map", substring = false).performClick()
        compose.onNodeWithContentDescription("Level 1, unlocked").assertIsEnabled()
    }
    private fun saveScreenshot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = compose.activity.getExternalFilesDir(null)
        val temporary = File(directory, "$name.tmp")
        temporary.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(temporary.renameTo(File(directory, name)))
    }
    private fun unlockThrough(highest: Int) {
        for (level in 1 until highest) {
            state.start(level)
            var now = 0L
            repeat(state.game!!.config.requiredCorrectAnswers) {
                state.update { game ->
                    game.question.result.toString().forEach { game.digit(it.digitToInt()) }
                    game.confirm(now)
                    now += GameRules.CORRECT_FEEDBACK_MS
                    game.tick(now)
                }
            }
        }
        state.openMap()
    }
    @Test fun replayLevelThreeKeepsLevelSixAndLockedLevelsCannotStart() {
        compose.runOnIdle {
            unlockThrough(6)
            assertEquals(CampaignProgress(6), AppState(compose.activity.application).campaign)
            state.start(7)
            assertEquals(Screen.MAP, state.screen)
            assertNull(state.game)
        }
        compose.onNodeWithContentDescription("Level 6, unlocked").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithContentDescription("Level 7, locked").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Level 3, completed").performScrollTo().performClick()
        compose.onNodeWithText("Level 3").assertIsDisplayed()
        repeat(CampaignConfig.levels[2].requiredCorrectAnswers) { index ->
            answer(true)
            if (index < CampaignConfig.levels[2].requiredCorrectAnswers - 1) waitForQuestion()
        }
        compose.waitUntil(16000) { compose.onAllNodesWithContentDescription("Level 6, unlocked").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Current rocket at Level 6").assertExists()
        compose.runOnIdle { assertEquals(6, state.campaign.highestUnlockedLevel) }
        // Future destinations remain inspectable by swiping, but unavailable.
        compose.onNodeWithContentDescription("Campaign route").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("Level 7, locked").assertIsNotEnabled()
    }

}
