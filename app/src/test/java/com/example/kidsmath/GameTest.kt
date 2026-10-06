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

    @Test fun inputIsOnlyCheckedOnConfirmAndBackspaceIsHarmless() {
        val game = Game(Settings(), 0)
        game.digit(9); game.digit(8); game.digit(7); game.digit(6)
        assertEquals("987", game.input)
        assertEquals(3, game.lives)
        game.backspace(); assertEquals("98", game.input)
        game.backspace(); game.backspace(); game.backspace()
        game.confirm(1)
        assertEquals(Phase.PLAYING, game.phase)
        assertEquals(3, game.lives)
    }

    @Test fun wrongAnswerCostsOneLifeAndMovesToANewQuestion() {
        val game = Game(Settings(), 0, Random(9))
        val previous = game.question
        type(game, 999)
        game.confirm(10)
        game.confirm(11)
        assertEquals(2, game.lives)
        assertEquals(0, game.correct)
        assertEquals(Feedback.WRONG, game.feedback)
        game.digit(1); game.backspace()
        assertEquals("999", game.input)
        game.tick(10 + GameRules.WRONG_FEEDBACK_MS - 1)
        assertEquals(Phase.FEEDBACK, game.phase)
        game.tick(10 + GameRules.WRONG_FEEDBACK_MS)
        assertEquals(Phase.PLAYING, game.phase)
        assertNotEquals(previous, game.question)
        assertEquals("", game.input)
        assertEquals(300_010L + GameRules.WRONG_FEEDBACK_MS, game.deadline)
    }

    @Test fun threeMistakesEndTheGame() {
        val game = Game(Settings(), 0)
        repeat(3) { index ->
            val now = index * GameRules.WRONG_FEEDBACK_MS
            type(game, 999); game.confirm(now)
            game.tick(now + GameRules.WRONG_FEEDBACK_MS)
        }
        assertEquals(0, game.lives)
        assertEquals(Phase.GAME_OVER, game.phase)
        game.tick(999_999); game.confirm(999_999)
        assertEquals(0, game.lives)
    }

    @Test fun roundsResetLivesAndFiveAnswersPerRoundWin() {
        val game = Game(Settings(), 0)
        var now = 0L
        for (round in 1..3) {
            assertEquals(round, game.round)
            assertEquals(3, game.lives)
            assertEquals(0, game.correct)
            type(game, 999); game.confirm(now); now += GameRules.WRONG_FEEDBACK_MS; game.tick(now)
            repeat(5) {
                type(game, game.question.result); game.confirm(now)
                assertEquals(it + 1, game.correct)
                now += GameRules.CORRECT_FEEDBACK_MS; game.tick(now)
            }
            if (round < 3) {
                assertEquals(Phase.ROUND_COMPLETE, game.phase)
                now += 600_000; game.tick(now)
                assertEquals(Phase.ROUND_COMPLETE, game.phase)
                assertEquals(round, game.round)
                assertEquals(2, game.lives)
                game.nextRound(now)
                assertEquals(now + GameRules.timeLimitMs(round + 1), game.deadline)
            }
        }
        assertEquals(Phase.VICTORY, game.phase)
        game.tick(now + 100_000)
        assertEquals(Phase.VICTORY, game.phase)
    }

    @Test fun timeoutHasOnePenaltyAndResetsDeadlineForEachRound() {
        val game = Game(Settings(), 0)
        var now = 0L
        for (round in 1..3) {
            val limit = GameRules.timeLimitMs(round)
            assertEquals(now + limit, game.deadline)
            game.tick(now + limit - 1)
            assertEquals(3, game.lives)
            now += limit
            type(game, game.question.result)
            game.confirm(now)
            assertEquals(Feedback.TIMEOUT, game.feedback)
            assertEquals(2, game.lives)
            game.tick(now + 100)
            assertEquals(2, game.lives)
            now += GameRules.WRONG_FEEDBACK_MS; game.tick(now)
            assertEquals(now + limit, game.deadline)
            repeat(5) {
                type(game, game.question.result); game.confirm(now)
                now += GameRules.CORRECT_FEEDBACK_MS; game.tick(now)
            }
            if (round < 3) { game.nextRound(now) }
        }
        assertEquals(Phase.VICTORY, game.phase)
    }

    @Test fun manualRoundTransitionCannotSkipRounds() {
        val game = Game(Settings(), 0)
        game.nextRound(0)
        assertEquals(1, game.round)
        var now = 0L
        repeat(5) { type(game, game.question.result); game.confirm(now); now += GameRules.CORRECT_FEEDBACK_MS; game.tick(now) }
        game.nextRound(now); game.nextRound(now)
        assertEquals(2, game.round)
    }

    @Test fun namesHaveNoStrayPunctuation() {
        assertEquals("Great job!", Settings().greeting("Great job"))
        assertEquals("Great job, Karolina!", Settings(name = " Karolina ").greeting("Great job"))
        assertEquals("Great job!", Settings(name = "  ").greeting("Great job"))
    }

    @Test fun wrongAndTimeoutFeedbackLastAtLeastTwiceTheOriginalDuration() {
        assertTrue(GameRules.feedbackDurationMs(Feedback.WRONG) >= 1400)
        assertEquals(GameRules.feedbackDurationMs(Feedback.WRONG), GameRules.feedbackDurationMs(Feedback.TIMEOUT))
        val game = Game(Settings(), 0)
        game.tick(300_000)
        game.tick(300_000 + GameRules.WRONG_FEEDBACK_MS - 1)
        assertEquals(Phase.FEEDBACK, game.phase)
        assertEquals(Feedback.TIMEOUT, game.feedback)
        assertEquals(2, game.lives)
    }

    private fun type(game: Game, answer: Int) { answer.toString().forEach { game.digit(it.digitToInt()) } }
}
