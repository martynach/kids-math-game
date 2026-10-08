package com.example.kidsmath

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameTest {
    @Test fun generatorRespectsEveryValidRangeAndOperation() {
        for (min in 0..100) for (max in min..100) for (operations in Operations.entries) {
            val generator = QuestionGenerator(Settings(operations = operations, min = min, max = max), Random(min * 101 + max))
            val possibleQuestions = (min..max).sumOf { result ->
                when (operations) {
                    Operations.ADDITION -> result + 1
                    Operations.SUBTRACTION -> max - result + 1
                    Operations.BOTH -> max + 2
                }
            }
            var previous: Question? = null
            repeat(5) {
                val question = generator.next(previous)
                assertTrue(question.left in 0..max)
                assertTrue(question.right in 0..max)
                assertTrue(question.result in min..max)
                assertTrue(question.result >= 0)
                if (operations == Operations.ADDITION) assertTrue(question.addition)
                if (operations == Operations.SUBTRACTION) assertFalse(question.addition)
                if (possibleQuestions > 1) assertNotEquals(previous, question)
                previous = question
            }
        }
    }

    @Test fun bothOperationsAppear() {
        val generator = QuestionGenerator(Settings(), Random(42))
        val questions = List(100) { generator.next() }
        assertTrue(questions.any { it.addition })
        assertTrue(questions.any { !it.addition })
    }

    @Test fun singletonRangeStillGenerates() {
        Operations.entries.forEach {
            val generator = QuestionGenerator(Settings(operations = it, min = 0, max = 0))
            val first = generator.next()
            assertEquals(0, generator.next(first).result)
        }
        for (max in 0..100) {
            val generator = QuestionGenerator(Settings(operations = Operations.SUBTRACTION, min = max, max = max))
            val first = generator.next()
            assertEquals(Question(max, 0, false), generator.next(first))
        }
    }

    @Test fun invalidRangesAreRejected() {
        listOf(-1 to 10, 0 to 101, 10 to 9, 101 to 101, 0 to -1).forEach { (min, max) ->
            assertThrows(IllegalArgumentException::class.java) { Settings(min = min, max = max) }
        }
    }


    @Test fun campaignUnlockReplayAndFinalBoundary() {
        var progress = CampaignProgress()
        assertTrue(progress.canStart(1))
        assertFalse(progress.canStart(2))
        assertThrows(IllegalArgumentException::class.java) { progress.complete(2) }
        progress = progress.complete(1)
        assertEquals(2, progress.highestUnlockedLevel)
        assertEquals(progress, progress.complete(1))
        for (level in 2..CampaignConfig.levels.size) progress = progress.complete(level)
        assertEquals(CampaignConfig.levels.size, progress.highestUnlockedLevel)
        assertTrue(progress.finalLevelCompleted)
        assertFalse(progress.canStart(CampaignConfig.levels.size + 1))
        assertEquals(progress, progress.complete(CampaignConfig.levels.size))
    }
    @Test fun configurableEnergyAndLives() {
        for (required in listOf(1, 5, 7, 12)) {
            val game = Game(Settings(), LevelConfig(1, required))
            var now = 0L
            repeat(required) { index ->
                type(game, game.question.result); game.confirm(now)
                assertEquals(index + 1, game.correct)
                assertEquals((index + 1).toFloat() / required, game.energy, .0001f)
                now += GameRules.CORRECT_FEEDBACK_MS; game.tick(now)
                if (index == 0 && required > 1) {
                    type(game, 999); game.confirm(now)
                    assertEquals(1, game.correct); assertEquals(2, game.lives)
                    assertEquals(1f / required, game.energy, .0001f)
                    now += GameRules.WRONG_FEEDBACK_MS; game.tick(now)
                }
            }
            assertEquals(Phase.LEVEL_COMPLETE, game.phase)
            assertEquals(1f, game.energy, .0001f)
        }
    }
    @Test fun threeMistakesEndAttemptAndTimeDoesNotRemoveLives() {
        val game = Game(Settings())
        game.tick(900_000)
        assertEquals(3, game.lives)
        var now = 900_000L
        repeat(3) {
            type(game, 999); game.confirm(now); game.confirm(now)
            now += GameRules.WRONG_FEEDBACK_MS; game.tick(now)
        }
        assertEquals(0, game.lives)
        assertEquals(0, game.correct)
        assertEquals(Phase.GAME_OVER, game.phase)
    }
    @Test fun keypadAndPausePreserveAttempt() {
        val game = Game(Settings())
        game.confirm(0); assertEquals(Phase.PLAYING, game.phase)
        game.digit(4); game.digit(2); game.backspace()
        assertEquals("4", game.input)
        game.pause(100); game.digit(9); game.backspace(); game.confirm(200)
        assertEquals("4", game.input)
        assertEquals(3, game.lives)
        game.resume(300); game.backspace()
        type(game, game.question.result); game.confirm(300); game.pause(400)
        game.tick(10_000); assertEquals(Phase.FEEDBACK, game.phase)
        game.resume(10_000)
        val transition = 10_000 + GameRules.CORRECT_FEEDBACK_MS - 100
        game.tick(transition - 1); assertEquals(Phase.FEEDBACK, game.phase)
        game.tick(transition); assertEquals(Phase.PLAYING, game.phase)
    }
    @Test fun testingCampaignCompletesAtFourAndCustomLevelStillNeedsTen() {
        assertEquals(30, CampaignConfig.levels.size)
        assertTrue(CampaignConfig.levels.all { it.requiredCorrectAnswers == 4 })
        val shortGame = Game(Settings())
        var shortTime = 0L
        repeat(4) { index ->
            type(shortGame, shortGame.question.result); shortGame.confirm(shortTime)
            shortTime += GameRules.CORRECT_FEEDBACK_MS; shortGame.tick(shortTime)
            assertEquals(if (index == 3) Phase.LEVEL_COMPLETE else Phase.PLAYING, shortGame.phase)
        }
        val game = Game(Settings(), LevelConfig(1, 10))
        var now = 0L
        repeat(10) { index ->
            type(game, game.question.result); game.confirm(now)
            now += GameRules.CORRECT_FEEDBACK_MS; game.tick(now)
            assertEquals(if (index == 9) Phase.LEVEL_COMPLETE else Phase.PLAYING, game.phase)
        }
        game.confirm(now + 1); game.digit(1)
        assertEquals(10, game.correct)
    }
    @Test fun smallerCampaignDoesNotUnlockPastItsFinalLevel() {
        var progress = CampaignProgress()
        for (level in 1..3) progress = progress.complete(level, total = 3)
        assertEquals(CampaignProgress(3, true), progress)
        assertFalse(progress.canStart(4, total = 3))
        assertEquals(progress, progress.complete(1, total = 3))
    }
    @Test fun bothFeedbackTypesRemainVisibleForTheSameFullDuration() {
        assertEquals(GameRules.CORRECT_FEEDBACK_MS, GameRules.WRONG_FEEDBACK_MS)
        for (correct in listOf(true, false)) {
            val game = Game(Settings())
            type(game, if (correct) game.question.result else 999)
            game.confirm(100)
            game.tick(100 + GameRules.ANSWER_FEEDBACK_MS - 1)
            assertEquals(Phase.FEEDBACK, game.phase)
            assertEquals(if (correct) Feedback.CORRECT else Feedback.WRONG, game.feedback)
            game.tick(100 + GameRules.ANSWER_FEEDBACK_MS)
            assertEquals(Phase.PLAYING, game.phase)
            assertEquals(if (correct) 3 else 2, game.lives)
        }
    }
    private fun type(game: Game, answer: Int) { answer.toString().forEach { game.digit(it.digitToInt()) } }
}
