package com.example.kidsmath

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FinaleFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val state get() = ViewModelProvider(compose.activity)[AppState::class.java]
    private val app get() = compose.activity.application
    @Before fun prepare() {
        compose.runOnIdle {
            state.openMap(); FinaleStore(app).clear()
            CampaignStore(app).save(CampaignProgress(CampaignConfig.levels.size))
            state.activatePremium(); state.configureLimits(true, 5, 1)
            compose.activity.viewModelStore.clear()
        }
        compose.activityRule.scenario.recreate()
        compose.mainClock.autoAdvance = false
    }
    @After fun clean() { compose.runOnIdle { state.openMap(); FinaleStore(app).clear() } }
    private fun complete(level: Int) {
        compose.runOnIdle {
            state.save(Settings(max = 20)); state.start(level)
            val game = state.game!!
            var now = 1_000_000L
            repeat(game.config.requiredCorrectAnswers) {
                game.question.result.toString().forEach { game.digit(it.digitToInt()) }
                state.update { it.confirm(now) }
                now += GameRules.CORRECT_FEEDBACK_MS
                state.update { it.tick(now) }
            }
            assertEquals(Phase.LEVEL_COMPLETE, game.phase)
        }
        compose.mainClock.advanceTimeBy(32)
    }
    @Test fun finalLaunchPrecedesFinaleAndFullStoryEndsAtPersistentVictory() {
        val last = CampaignConfig.levels.last().level
        complete(last)
        val pass = state.spacePass
        compose.onNodeWithContentDescription("Level $last complete").assertIsDisplayed()
        compose.onNodeWithTag("campaign-finale").assertDoesNotExist()
        compose.runOnIdle { assertTrue(CampaignStore(app).load().finalLevelCompleted) }
        compose.mainClock.advanceTimeBy(6000)
        compose.onNodeWithTag("campaign-finale").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(5000)
        compose.onNodeWithTag("campaign-finale").assertIsDisplayed()
        // Visit every scene with the production frame clock and renderer.
        FinaleScene.entries.forEach { scene ->
            compose.onNodeWithContentDescription(scene.description).assertExists()
            compose.mainClock.advanceTimeBy(scene.durationMs)
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithTag("finale-victory").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(30_000)
        compose.onNodeWithTag("finale-victory").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(pass, state.spacePass)
            val resumed = AppState(app)
            assertEquals(Screen.FINALE, resumed.screen)
            assertEquals(FinaleTimeline.durationMs, resumed.finaleElapsedMs)
            assertTrue(resumed.campaign.finalLevelCompleted)
        }
        compose.onNodeWithTag("finale-map").performClick(); compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Space Map").assertIsDisplayed()
        compose.runOnIdle { assertNull(FinaleStore(app).load()) }
    }
    @Test fun skipBackRecreationAndReplayKeepProgressAndAllowance() {
        complete(CampaignConfig.levels.last().level)
        compose.mainClock.advanceTimeBy(11_000)
        compose.mainClock.advanceTimeBy(7000)
        val before = state.finaleElapsedMs!!
        compose.activityRule.scenario.recreate()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("campaign-finale").assertIsDisplayed()
        compose.runOnIdle { assertTrue(state.finaleElapsedMs!! >= before) }
        val pass = state.spacePass
        compose.onNodeWithTag("skip-finale").performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("finale-victory").assertIsDisplayed()
        Espresso.pressBack(); compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Space Map").assertIsDisplayed()
        complete(CampaignConfig.levels.last().level)
        compose.runOnIdle {
            val game = state.game!!
            state.finishLaunch(game); state.finishLaunch(game)
            assertEquals(Screen.FINALE, state.screen)
            state.advanceFinale(777)
            state.persistFinale()
            val restored = AppState(app)
            assertEquals(777L, restored.finaleElapsedMs)
            assertTrue(restored.campaign.finalLevelCompleted)
            assertEquals(pass, state.spacePass)
        }
        Espresso.pressBack(); compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("finale-victory").assertIsDisplayed()
    }
    @Test fun earlierLevelUsesNormalLaunchAndNeverQueuesFinale() {
        if (CampaignConfig.levels.size == 1) {
            // This configuration has no earlier level; do not mistake its finale for a regression.
            compose.runOnIdle { assertTrue(FinaleTimeline.isFinalLevel(1)) }
            return
        }
        complete(1)
        compose.mainClock.advanceTimeBy(6000)
        compose.onNodeWithContentDescription("Level 1 complete").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(5000)
        compose.onNodeWithText("Space Map").assertIsDisplayed()
        compose.onNodeWithTag("campaign-finale").assertDoesNotExist()
        compose.runOnIdle { assertNull(FinaleStore(app).load()) }
    }
    @Test fun backgroundTimeDoesNotAdvanceTheStory() {
        complete(CampaignConfig.levels.last().level)
        compose.mainClock.advanceTimeBy(11_000)
        val vm = state
        val elapsed = vm.finaleElapsedMs!!
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        compose.mainClock.advanceTimeBy(20_000)
        // A frame already queued when ON_PAUSE arrives may finish; background time must not.
        assertTrue(vm.finaleElapsedMs!! in elapsed..elapsed + 32)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(200)
        assertTrue(vm.finaleElapsedMs!! in elapsed..elapsed + 250)
    }
    @Test fun limitedPassConsumesOnlyTheLevelStart() {
        compose.runOnIdle { state.configureLimits(false, 7, 1) }
        complete(CampaignConfig.levels.last().level)
        compose.runOnIdle {
            assertEquals(6, state.spacePass.premiumRemaining)
            val pass = state.spacePass
            state.finishLaunch(state.game!!)
            state.advanceFinale(15_000)
            state.skipFinale(); state.skipFinale()
            assertEquals(pass, state.spacePass)
            state.openMap()
            assertEquals(pass, state.spacePass)
        }
    }
    @Test fun processInterruptionRestoresRewardWithoutCreatingAnAttempt() {
        complete(CampaignConfig.levels.last().level)
        val pass = state.spacePass
        compose.runOnIdle {
            // A process killed during the old launch can safely resume the reward from its beginning.
            val restored = AppState(app)
            assertEquals(Screen.FINALE, restored.screen)
            assertEquals(0L, restored.finaleElapsedMs)
            assertNull(restored.game)
            assertEquals(pass, restored.spacePass)
            assertTrue(restored.campaign.finalLevelCompleted)
        }
    }
}
