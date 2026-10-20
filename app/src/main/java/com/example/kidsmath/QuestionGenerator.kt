package com.example.kidsmath

import kotlin.math.exp
import kotlin.random.Random

/** Counts and unranking use Long; no operand-pair table is built. */
class UniformQuestionSampler(val min: Int, val max: Int, val operations: Operations,
                             private val random: Random = Random.Default) {
    init { require(min >= 0 && min <= max) }
    private fun triangle(n: Long) = n * (n + 1) / 2
    val additionCount = triangle(max.toLong() + 1) - triangle(min.toLong())
    val subtractionCount = triangle(max.toLong() + 1)
    val count: Long get() = when (operations) {
        Operations.ADDITION -> additionCount
        Operations.SUBTRACTION -> subtractionCount
        Operations.BOTH -> additionCount + subtractionCount
    }
    fun questionAt(index: Long, addition: Boolean): Question {
        val count = if (addition) additionCount else subtractionCount
        require(index in 0 until count)
        val absolute = index + if (addition) triangle(min.toLong()) else 0L
        var low = if (addition) min.toLong() else 0L
        var high = max.toLong()
        while (low < high) {
            val middle = low + (high - low) / 2
            if (triangle(middle + 1) <= absolute) low = middle + 1 else high = middle
        }
        val offset = (absolute - triangle(low)).toInt()
        return if (addition) Question(offset, low.toInt() - offset, true)
        else Question(low.toInt(), offset, false)
    }
    /** Combined indexing is for fallback traversal, not BOTH-mode random sampling. */
    fun questionAt(index: Long): Question {
        require(index in 0 until count)
        return when (operations) {
            Operations.ADDITION -> questionAt(index, true)
            Operations.SUBTRACTION -> questionAt(index, false)
            Operations.BOTH -> if (index < additionCount) questionAt(index, true)
                else questionAt(index - additionCount, false)
        }
    }
    fun next(): Question {
        val addition = when (operations) {
            Operations.ADDITION -> true
            Operations.SUBTRACTION -> false
            Operations.BOTH -> random.nextBoolean()
        }
        return questionAt(random.nextLong(if (addition) additionCount else subtractionCount), addition)
    }
}

data class DifficultyWeights(val magnitude: Double = 75.0, val zero: Double = 25.0,
                             val one: Double = 30.0, val carryBorrow: Double = 12.0)
class DifficultyEvaluator(private val weights: DifficultyWeights = DifficultyWeights()) {
    fun carryBorrowCount(q: Question): Int {
        var a = q.left; var b = q.right; var transfer = 0; var count = 0
        while (a > 0 || b > 0) {
            transfer = if (q.addition) {
                if (a % 10 + b % 10 + transfer >= 10) 1 else 0
            } else {
                if (a % 10 - transfer < b % 10) 1 else 0
            }
            count += transfer
            a /= 10; b /= 10
        }
        return count
    }
    fun score(q: Question, max: Int): Double {
        require(max >= 0)
        val magnitude = if (max == 0) 0.0 else
            (if (q.addition) q.result else q.left).toDouble() / max
        val penalty = when {
            q.addition && (q.left == 0 || q.right == 0) -> weights.zero
            !q.addition && (q.right == 0 || q.left == q.right) -> weights.zero
            q.addition && (q.left == 1 || q.right == 1) -> weights.one
            !q.addition && q.right == 1 -> weights.one
            else -> 0.0
        }
        return (weights.magnitude * magnitude - penalty + weights.carryBorrow * carryBorrowCount(q))
            .coerceIn(0.0, 100.0)
    }
}

data class ProgressionPolicy(val firstTarget: Double = 8.0, val targetSpan: Double = 84.0,
                             val earlyFraction: Double = 0.2, val firstCap: Double = 30.0) {
    init { require(earlyFraction > 0 && earlyFraction <= 1 && firstCap in 0.0..100.0) }
    fun progress(level: Int, totalLevels: Int): Double {
        require(totalLevels > 0 && level in 1..totalLevels)
        return if (totalLevels == 1) 0.0 else (level - 1).toDouble() / (totalLevels - 1)
    }
    fun target(progress: Double) = firstTarget + targetSpan * progress
    fun cap(progress: Double) = if (progress < earlyFraction)
        firstCap + (100.0 - firstCap) * progress / earlyFraction else 100.0
}

data class AcceptancePolicy(val floor: Double = 0.025, val spread: Double = 19.0) {
    init { require(floor in 0.0..1.0 && spread > 0) }
    fun probability(difficulty: Double, target: Double): Double {
        val distance = (difficulty - target) / spread
        return floor + (1 - floor) * exp(-0.5 * distance * distance)
    }
}

data class QuestionLimitRule(val id: String, val maximum: Int = 1, val matches: (Question) -> Boolean) {
    init { require(maximum >= 0) }
}
object QuestionLimits {
    val defaults = listOf(
        QuestionLimitRule("addition-zero") { it.addition && (it.left == 0 || it.right == 0) },
        QuestionLimitRule("subtraction-zero") { !it.addition && it.right == 0 },
        QuestionLimitRule("subtraction-equal") { !it.addition && it.left == it.right },
        QuestionLimitRule("addition-one") { it.addition && (it.left == 1 || it.right == 1) },
        QuestionLimitRule("subtraction-one") { !it.addition && it.right == 1 }
    )
}
class QuestionLimitTracker(val rules: List<QuestionLimitRule> = QuestionLimits.defaults) {
    init { require(rules.map { it.id }.distinct().size == rules.size) }
    private val counts = mutableMapOf<String, Int>()
    fun count(id: String) = counts[id] ?: 0
    fun allows(q: Question) = rules.all { !it.matches(q) || count(it.id) < it.maximum }
    fun record(q: Question) { rules.filter { it.matches(q) }.forEach { counts[it.id] = count(it.id) + 1 } }
}
class QuestionHistory {
    private val issued = mutableSetOf<Question>()
    val size get() = issued.size
    operator fun contains(q: Question) = q in issued
    fun record(q: Question) { issued.add(q) }
}

enum class SelectionStage { NORMAL, PREFERENCE_RELAXED, CAP_RELAXED, LIMITS_RELAXED, REPEAT }
data class SelectionConfig(val attempts: Int = 100, val exactSearchLimit: Long = 20_000,
                           val fallbackSamples: Int = 512) {
    init { require(attempts >= 0 && exactSearchLimit >= 0 && fallbackSamples >= 0) }
}
/** Generator-level range; parent-facing Settings retain their 0..100 bounds. */
data class ArithmeticRange(val min: Int, val max: Int, val operations: Operations) {
    init { require(min >= 0 && min <= max) }
}
/** One instance per level attempt. Only newly issued questions are recorded. */
class QuestionGenerator(range: ArithmeticRange, private val random: Random = Random.Default,
                        level: Int = 1, totalLevels: Int = CampaignConfig.TOTAL_LEVELS,
                        val limits: QuestionLimitTracker = QuestionLimitTracker(),
                        private val selection: SelectionConfig = SelectionConfig(),
                        private val evaluator: DifficultyEvaluator = DifficultyEvaluator(),
                        progression: ProgressionPolicy = ProgressionPolicy(),
                        private val acceptance: AcceptancePolicy = AcceptancePolicy()) {
    constructor(settings: Settings, random: Random = Random.Default,
                level: Int = 1, totalLevels: Int = CampaignConfig.TOTAL_LEVELS,
                limits: QuestionLimitTracker = QuestionLimitTracker(),
                selection: SelectionConfig = SelectionConfig(),
                evaluator: DifficultyEvaluator = DifficultyEvaluator(),
                progression: ProgressionPolicy = ProgressionPolicy(),
                acceptance: AcceptancePolicy = AcceptancePolicy()) : this(
        ArithmeticRange(settings.min, settings.max, settings.operations), random, level,
        totalLevels, limits, selection, evaluator, progression, acceptance)
    private val sampler = UniformQuestionSampler(range.min, range.max, range.operations, random)
    private val max = range.max
    private val progress = progression.progress(level, totalLevels)
    val target = progression.target(progress)
    val cap = progression.cap(progress)
    val history = QuestionHistory()
    var lastStage = SelectionStage.NORMAL; private set
    private fun eligible(q: Question, stage: SelectionStage) =
        (stage >= SelectionStage.REPEAT || q !in history) &&
        (stage >= SelectionStage.LIMITS_RELAXED || limits.allows(q)) &&
        (stage >= SelectionStage.CAP_RELAXED || evaluator.score(q, max) <= cap)
    private fun issue(q: Question, stage: SelectionStage): Question {
        lastStage = stage
        history.record(q); limits.record(q)
        return q
    }
    fun next(): Question {
        repeat(selection.attempts) {
            val q = sampler.next()
            if (eligible(q, SelectionStage.NORMAL) &&
                random.nextDouble() < acceptance.probability(evaluator.score(q, max), target))
                return issue(q, SelectionStage.NORMAL)
        }
        // Small spaces are searched exactly without storing the universe. Large spaces
        // use a bounded shared probe set; its prefix guarantees an unused expression
        // at LIMITS_RELAXED whenever one exists (pigeonhole principle).
        val probes = if (sampler.count > selection.exactSearchLimit)
            List(selection.fallbackSamples) { sampler.next() } else emptyList()
        val traversalCount = if (sampler.count <= selection.exactSearchLimit) sampler.count
            else minOf(sampler.count, history.size.toLong() + 1)
        for (stage in listOf(SelectionStage.PREFERENCE_RELAXED, SelectionStage.CAP_RELAXED,
                             SelectionStage.LIMITS_RELAXED)) {
            probes.firstOrNull { eligible(it, stage) }?.let { return issue(it, stage) }
            var index = 0L
            while (index < traversalCount) {
                val q = sampler.questionAt(index++)
                if (eligible(q, stage)) return issue(q, stage)
            }
        }
        return issue(sampler.questionAt(0), SelectionStage.REPEAT)
    }
}
