package com.example.kidsmath

import kotlin.random.Random

enum class Operations { ADDITION, SUBTRACTION, BOTH }

data class Settings(val name: String = "", val operations: Operations = Operations.BOTH,
                    val min: Int = 0, val max: Int = 10) {
    init { require(min in 0..100 && max in 0..100 && min <= max) }
    fun greeting(message: String) = if (name.isBlank()) "$message!" else "$message, ${name.trim()}!"
}

data class Question(val left: Int, val right: Int, val addition: Boolean) {
    val result: Int get() = if (addition) left + right else left - right
    val expression: String get() = "$left ${if (addition) "+" else "−"} $right"
}

class QuestionGenerator(settings: Settings, private val random: Random = Random.Default) {
    private val questions = buildList {
        for (a in 0..settings.max) for (b in 0..settings.max) {
            if (settings.operations != Operations.SUBTRACTION && a + b in settings.min..settings.max)
                add(Question(a, b, true))
            if (settings.operations != Operations.ADDITION && a - b in settings.min..settings.max)
                add(Question(a, b, false))
        }
    }
    fun next(previous: Question? = null): Question {
        val excluded = questions.indexOf(previous)
        if (excluded < 0 || questions.size == 1) return questions.random(random)
        val index = random.nextInt(questions.size - 1)
        return questions[if (index >= excluded) index + 1 else index]
    }
}

object GameRules {
    const val ROUNDS = 3
    const val ANSWERS_PER_ROUND = 5
    const val LIVES = 3
    const val CORRECT_FEEDBACK_MS = 1000L
    const val WRONG_FEEDBACK_MS = 1600L
    fun feedbackDurationMs(feedback: Feedback) =
        if (feedback == Feedback.CORRECT) CORRECT_FEEDBACK_MS else WRONG_FEEDBACK_MS
    fun timeLimitMs(round: Int): Long = when (round) {
        1 -> 300_000L
        2 -> 60_000L
        3 -> 20_000L
        else -> error("Invalid round")
    }
}

enum class Phase { PLAYING, FEEDBACK, ROUND_COMPLETE, GAME_OVER, VICTORY }
enum class Feedback { CORRECT, WRONG, TIMEOUT }

/** All time comes from the caller's monotonic clock, making timeout rules testable. */
class Game(val settings: Settings, now: Long, random: Random = Random.Default) {
    private val generator = QuestionGenerator(settings, random)
    var round = 1; private set
    var correct = 0; private set
    var lives = GameRules.LIVES; private set
    var question = generator.next(); private set
    var input = ""; private set
    var phase = Phase.PLAYING; private set
    var feedback: Feedback? = null; private set
    var deadline = now + GameRules.timeLimitMs(round); private set
    private var transitionAt = 0L

    fun digit(digit: Int) {
        require(digit in 0..9)
        if (phase == Phase.PLAYING && input.length < 3) input += digit
    }
    fun backspace() { if (phase == Phase.PLAYING) input = input.dropLast(1) }
    fun confirm(now: Long) {
        tick(now)
        if (phase != Phase.PLAYING || input.isEmpty()) return
        answer(if (input.toInt() == question.result) Feedback.CORRECT else Feedback.WRONG, now)
    }
    private fun answer(result: Feedback, now: Long) {
        feedback = result
        if (result == Feedback.CORRECT) correct++ else lives--
        phase = Phase.FEEDBACK
        transitionAt = now + GameRules.feedbackDurationMs(result)
    }
    fun tick(now: Long) {
        when (phase) {
            Phase.PLAYING -> if (now >= deadline) answer(Feedback.TIMEOUT, now)
            Phase.FEEDBACK -> if (now >= transitionAt) {
                when {
                    lives == 0 -> phase = Phase.GAME_OVER
                    correct == GameRules.ANSWERS_PER_ROUND -> {
                        phase = if (round == GameRules.ROUNDS) Phase.VICTORY else Phase.ROUND_COMPLETE
                    }
                    else -> newQuestion(now)
                }
            }
            else -> Unit
        }
    }
    fun nextRound(now: Long) {
        if (phase != Phase.ROUND_COMPLETE) return
        round++; correct = 0; lives = GameRules.LIVES
        newQuestion(now)
    }
    private fun newQuestion(now: Long) {
        question = generator.next(question)
        input = ""; feedback = null; phase = Phase.PLAYING
        deadline = now + GameRules.timeLimitMs(round)
    }
}
