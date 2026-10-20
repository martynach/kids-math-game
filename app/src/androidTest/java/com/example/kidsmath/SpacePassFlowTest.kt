package com.example.kidsmath

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SpacePassFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val state get() = ViewModelProvider(compose.activity)[AppState::class.java]
    private val application get() = compose.activity.application

    @Before fun freshFreePlan() {
        compose.runOnIdle {
            SpacePassStore(application).save(SpacePass())
            CampaignStore(application).save(CampaignProgress())
            compose.activity.viewModelStore.clear()
        }
        compose.activityRule.scenario.recreate()
    }

    @Test fun menuSettingsMapAndLockedLevelsAreFreeButLeavingStillCostsAnAttempt() {
        compose.onNodeWithText("⚙  Settings").performClick()
        compose.onNodeWithText("SPACE PASS", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("‹ Back").performClick()
        compose.onNodeWithText("Start Game").performClick()
        compose.onNodeWithContentDescription("Level 2, locked").assertIsNotEnabled()
        compose.runOnIdle { state.start(2); assertEquals(2, state.spacePass.remainingAttempts) }
        repeat(2) { index ->
            compose.onNodeWithContentDescription("Level 1, unlocked").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(1 - index, state.spacePass.remainingAttempts)
                assertEquals(1 - index, SpacePassStore(application).load().remainingAttempts)
            }
            compose.onNodeWithText("⌂ Map").performClick()
            compose.onNodeWithText("Space Map", substring = false).performClick()
        }
        compose.onNodeWithContentDescription("Level 1, unlocked").performClick()
        compose.onNodeWithText("Great space training today!").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sleeping astronaut and parked rocket").assertExists()
        compose.onNodeWithText("Back to map").performScrollTo().performClick()
        compose.onNodeWithText("Space Map").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, state.spacePass.remainingAttempts); assertNull(state.game) }
    }

    @Test fun retryCannotBypassExhaustionAndResetDoesNotRefundAttempts() {
        compose.runOnIdle {
            state.save(Settings("Karolina", Operations.SUBTRACTION, 6, 9))
            repeat(2) { state.start(1) }
            val consumed = state.spacePass
            var now = 0L
            repeat(GameRules.LIVES) {
                state.update { game ->
                    repeat(3) { game.digit(9) }; game.confirm(now)
                    now += GameRules.WRONG_FEEDBACK_MS; game.tick(now)
                }
            }
            assertEquals(Phase.GAME_OVER, state.game!!.phase)
            assertEquals(consumed, state.spacePass)
            state.resetProgress()
            assertEquals(consumed, state.spacePass)
            assertEquals(Settings("Karolina", Operations.SUBTRACTION, 6, 9), state.settings)
        }
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Great space training today!").assertIsDisplayed()
        compose.runOnIdle { assertNull(state.game); assertEquals(Screen.REST, state.screen) }
        compose.onNodeWithText("⚙ Settings").performScrollTo().performClick()
        compose.onNodeWithText("Switch to Premium").performScrollTo().performClick()
        compose.onNodeWithText("PLAY LIMITS").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save Settings").performClick()
        compose.onNodeWithContentDescription("Level 1, unlocked").performClick()
        compose.onNodeWithText("Level 1").assertIsDisplayed()
        compose.runOnIdle { assertTrue(AppState(application).spacePass.isPremium) }
    }

    @Test fun completingALevelDoesNotConsumeAgainAndPersistenceSurvivesReload() {
        compose.runOnIdle {
            var epoch = 1_800_000_000_000L
            val model = AppState(application) { epoch }
            model.start(1)
            var gameTime = 0L
            repeat(model.game!!.config.requiredCorrectAnswers) {
                model.update { game ->
                    game.question.result.toString().forEach { game.digit(it.digitToInt()) }
                    game.confirm(gameTime)
                    gameTime += GameRules.CORRECT_FEEDBACK_MS; game.tick(gameTime)
                }
            }
            assertEquals(Phase.LEVEL_COMPLETE, model.game!!.phase)
            assertEquals(1, model.spacePass.remainingAttempts)
            assertEquals(2, model.campaign.highestUnlockedLevel)
            model.openMap(); model.start(2); model.leaveGame()
            val exhausted = model.spacePass
            assertEquals(0, exhausted.remainingAttempts)
            assertEquals(epoch + SpacePassConfig.FREE_COOLDOWN_MS, exhausted.cooldownEnd)
            epoch += 60_000
            val reloaded = AppState(application) { epoch }
            assertEquals(exhausted, reloaded.spacePass)
            reloaded.start(1)
            assertEquals(Screen.REST, reloaded.screen)
            assertNull(reloaded.game)
            epoch = exhausted.cooldownEnd
            reloaded.refreshPass()
            assertEquals(SpacePass(), reloaded.spacePass)
            reloaded.start(2)
            assertEquals(1, reloaded.spacePass.remainingAttempts)
            assertEquals(Screen.GAME, reloaded.screen)
            assertEquals(reloaded.spacePass, AppState(application) { epoch }.spacePass)
        }
    }

    @Test fun premiumCustomPersistenceSwitchingAndResetPreserveOtherState() {
        compose.runOnIdle {
            var epoch = 1000L
            val model = AppState(application) { epoch }
            val settings = Settings("Pilot", Operations.SUBTRACTION, 2, 9)
            model.save(settings)
            model.activatePremium()
            assertTrue(model.spacePass.unlimitedAttempts)
            model.configureLimits(false, 5, 1)
            repeat(5) { model.start(1); model.leaveGame() }
            val exhausted = model.spacePass
            val reloaded = AppState(application) { epoch }
            assertEquals(exhausted, reloaded.spacePass)
            reloaded.start(1)
            assertEquals(Screen.REST, reloaded.screen)
            reloaded.configureLimits(true, 5, 1)
            reloaded.start(1)
            assertEquals(Screen.GAME, reloaded.screen)
            reloaded.configureLimits(false, 5, 1)
            assertEquals(exhausted, reloaded.spacePass)
            reloaded.activateFree()
            assertEquals(settings, reloaded.settings)
            assertEquals(CampaignProgress(), reloaded.campaign)
            reloaded.activatePremium()
            reloaded.resetProgress()
            assertEquals(exhausted, reloaded.spacePass)
            assertEquals(settings, AppState(application) { epoch }.settings)
            epoch = exhausted.premiumCooldownEnd
            reloaded.refreshPass()
            assertEquals(5, reloaded.spacePass.premiumRemaining)
            reloaded.start(1)
            assertEquals(4, reloaded.spacePass.premiumRemaining)
        }
    }
    @Test fun oneSavePersistsAllSettingsAndCanEditLimitsAgain() {
        compose.onNodeWithText("⚙  Settings").performClick()
        compose.onNodeWithText("Player name (optional)").performTextReplacement("Nova")
        // Finish text entry before scrolling. The native IME resize animation is
        // outside Compose's idle tracking and can move the plan button mid-tap.
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithText("Switch to Premium").performScrollTo().performClick()
        compose.onNodeWithText("Attempts before rest").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Unlimited attempts").performScrollTo().performClick()
        compose.onNodeWithText("Attempts before rest").performTextReplacement("3")
        compose.onNodeWithText("Rest duration (hours)").performTextReplacement("2")
        compose.runOnIdle { assertFalse(state.spacePass.isPremium) }
        compose.onNodeWithText("Save Settings").performClick()
        compose.onNodeWithText("Space Map").assertIsDisplayed()
        compose.runOnIdle {
            val loaded = AppState(application)
            assertEquals("Nova", loaded.settings.name)
            assertTrue(loaded.spacePass.isPremium)
            assertEquals(3, loaded.spacePass.premiumAttempts)
            assertEquals(2, loaded.spacePass.restHours)
            state.openSettings()
        }
        compose.onNodeWithText("Attempts before rest").performScrollTo().performTextReplacement("7")
        compose.onNodeWithText("Rest duration (hours)").performTextReplacement("1")
        compose.onNodeWithText("Save Settings").performClick()
        compose.onNodeWithText("Space Map").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(7, AppState(application).spacePass.premiumAttempts)
            assertEquals(1, AppState(application).spacePass.restHours)
            state.openSettings()
        }
        compose.onNodeWithText("Switch to Free").performScrollTo().performClick()
        compose.onNodeWithText("Player name (optional)").performScrollTo().performTextReplacement("Discard")
        compose.onNodeWithText("‹ Back").performClick()
        compose.runOnIdle {
            assertTrue(AppState(application).spacePass.isPremium)
            assertEquals("Nova", AppState(application).settings.name)
        }
    }
    @Test fun visibleCountdownExpiresAndRestoresMissionsWithoutRestart() {
        compose.runOnIdle {
            SpacePassStore(application).save(SpacePass(remainingAttempts = 0,
                cooldownEnd = System.currentTimeMillis() + 12_000))
            compose.activity.viewModelStore.clear()
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Start Game").performClick()
        compose.onNodeWithContentDescription("Level 1, unlocked").performClick()
        compose.onNodeWithText("New missions in").assertIsDisplayed()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Missions available again!").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertEquals(SpacePassConfig.FREE_ATTEMPTS, state.spacePass.remainingAttempts)
            assertEquals(0L, state.spacePass.cooldownEnd)
            assertEquals(SpacePass(), SpacePassStore(application).load())
        }
        compose.onNodeWithText("Back to map").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Level 1, unlocked").performClick()
        compose.runOnIdle { assertEquals(1, state.spacePass.remainingAttempts); assertEquals(Screen.GAME, state.screen) }
    }
}
