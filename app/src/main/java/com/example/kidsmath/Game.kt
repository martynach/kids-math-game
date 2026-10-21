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

object GameRules {
    const val LIVES = 3
    const val ANSWER_FEEDBACK_MS = 2600L
    const val CORRECT_FEEDBACK_MS = ANSWER_FEEDBACK_MS
    const val WRONG_FEEDBACK_MS = ANSWER_FEEDBACK_MS
    fun feedbackDurationMs(feedback: Feedback) =
        if (feedback == Feedback.CORRECT) CORRECT_FEEDBACK_MS else WRONG_FEEDBACK_MS

}

enum class Phase { PLAYING, FEEDBACK, GAME_OVER, LEVEL_COMPLETE }
enum class Feedback { CORRECT, WRONG }

/** The caller supplies monotonic time for short feedback transitions; questions are untimed. */
class Game(val settings: Settings, val config: LevelConfig = CampaignConfig.levels.first(), random: Random = Random.Default, totalLevels: Int = CampaignConfig.TOTAL_LEVELS) {
    private val generator = QuestionGenerator(settings, random, config.level, totalLevels)
    val energy: Float get() = correct.toFloat() / config.requiredCorrectAnswers
    var correct = 0; private set
    var lives = GameRules.LIVES; private set
    var question = generator.next(); private set
    var input = ""; private set
    var phase = Phase.PLAYING; private set
    var feedback: Feedback? = null; private set
    private var transitionAt = 0L
    var pausedAt: Long? = null; private set
    fun pause(now: Long) {
        if (pausedAt != null) return
        tick(now)
        pausedAt = now
    }
    fun resume(now: Long) {
        val started = pausedAt ?: return
        val elapsed = (now - started).coerceAtLeast(0)
        transitionAt += elapsed
        pausedAt = null
    }

    fun digit(digit: Int) {
        require(digit in 0..9)
        if (pausedAt == null && phase == Phase.PLAYING && input.length < 3) input += digit
    }
    fun backspace() { if (pausedAt == null && phase == Phase.PLAYING) input = input.dropLast(1) }
    fun confirm(now: Long) {
        tick(now)
        if (pausedAt != null || phase != Phase.PLAYING || input.isEmpty()) return
        answer(if (input.toInt() == question.result) Feedback.CORRECT else Feedback.WRONG, now)
    }
    private fun answer(result: Feedback, now: Long) {
        feedback = result
        if (result == Feedback.CORRECT) correct++ else lives--
        phase = Phase.FEEDBACK
        transitionAt = now + GameRules.feedbackDurationMs(result)
    }
    fun tick(now: Long) {
        if (pausedAt != null) return
        when (phase) {
            Phase.PLAYING -> Unit
            Phase.FEEDBACK -> if (now >= transitionAt) {
                when {
                    lives == 0 -> phase = Phase.GAME_OVER
                    correct >= config.requiredCorrectAnswers -> {
                        phase = Phase.LEVEL_COMPLETE
                    }
                    else -> newQuestion()
                }
            }
            else -> Unit
        }
    }
    private fun newQuestion() {
        if (feedback == Feedback.CORRECT) question = generator.next()
        input = ""; feedback = null; phase = Phase.PLAYING
    }
}


data class LevelConfig(val level: Int, val requiredCorrectAnswers: Int = CampaignConfig.DEFAULT_REQUIRED_CORRECT_ANSWERS) {
    init { require(level > 0 && requiredCorrectAnswers > 0) }
}
object CampaignConfig {
    const val TOTAL_LEVELS = 3
    const val DEFAULT_REQUIRED_CORRECT_ANSWERS = 4
    val levels = List(TOTAL_LEVELS) { LevelConfig(it + 1) }
}
data class CampaignProgress(val highestUnlockedLevel: Int = 1, val finalLevelCompleted: Boolean = false) {
    fun canStart(level: Int, total: Int = CampaignConfig.levels.size) = level in 1..minOf(highestUnlockedLevel, total)
    fun complete(level: Int, total: Int = CampaignConfig.levels.size): CampaignProgress {
        require(canStart(level, total))
        return if (level != highestUnlockedLevel) this
        else CampaignProgress(minOf(level + 1, total), finalLevelCompleted || level == total)
    }
    fun isCompleted(level: Int) = level < highestUnlockedLevel || level == highestUnlockedLevel && finalLevelCompleted
}
