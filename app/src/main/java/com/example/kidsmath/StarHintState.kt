package com.example.kidsmath

object StarHintRules {
    const val COLUMNS = 5
    const val ROWS = 4
    const val CAPACITY = COLUMNS * ROWS
    const val LAST_AUTOMATIC_LEVEL = 10
    const val SPAWN_INTERVAL_MS = 500L
    const val TRAVEL_MS = 420
    const val CROSS_MS = 220
    const val SUBTRACTION_PAUSE_MS = 350L
    fun available(max: Int) = max in 0..CAPACITY
    fun automatic(level: Int) = level in 1..LAST_AUTOMATIC_LEVEL
    fun supports(q: Question) = q.left in 0..CAPACITY && q.right in 0..CAPACITY &&
        q.result in 0..CAPACITY
    fun cell(index: Int): Pair<Int, Int> {
        require(index in 0 until CAPACITY)
        return index % COLUMNS to index / COLUMNS
    }
}

enum class HintColor { YELLOW, PINK }
data class HintStar(val id: Long, val color: HintColor, val crossed: Boolean = false,
                    val returning: Boolean = false, val slot: Int = 0)

/** Temporary counting objects only. No reference to Game, settings storage or campaign. */
class StarHintState(val question: Question, val level: Int) {
    init { require(level > 0 && StarHintRules.supports(question)) }
    private var nextId = 0L
    private val items = mutableListOf<HintStar>()
    val stars: List<HintStar> get() = items.toList()
    val automatic = StarHintRules.automatic(level)
    fun count(color: HintColor) = items.count { it.color == color }
    fun add(color: HintColor, preferredSlots: List<Int> = (0 until StarHintRules.CAPACITY).toList()): Long? {
        if (items.size >= StarHintRules.CAPACITY || (!question.addition && color == HintColor.PINK)) return null
        val slot = freeSlot(preferredSlots) ?: return null
        val id = nextId++
        items.add(HintStar(id, color, slot = slot))
        return id
    }
    private fun freeSlot(preferredSlots: List<Int>, movingId: Long? = null): Int? {
        val occupied = items.filter { it.id != movingId }.map { it.slot }.toSet()
        return preferredSlots.firstOrNull { it in 0 until StarHintRules.CAPACITY && it !in occupied }
    }
    fun move(id: Long, preferredSlots: List<Int>): Boolean {
        if (automatic) return false
        val index = items.indexOfFirst { it.id == id && !it.returning }
        if (index < 0) return false
        val slot = freeSlot(preferredSlots, id) ?: return false
        items[index] = items[index].copy(slot = slot)
        return true
    }
    fun toggle(id: Long): Boolean {
        if (question.addition) return false
        val index = items.indexOfFirst { it.id == id && !it.returning }
        if (index < 0) return false
        items[index] = items[index].copy(crossed = !items[index].crossed)
        return true
    }
    fun beginReturn(id: Long, bag: HintColor): Boolean {
        val index = items.indexOfFirst { it.id == id && it.color == bag && !it.returning }
        if (index < 0) return false
        items[index] = items[index].copy(returning = true)
        return true
    }
    fun finishReturn(id: Long): Boolean = items.removeAll { it.id == id && it.returning }
}

sealed interface HintAction {
    data class Add(val color: HintColor) : HintAction
    data class Cross(val index: Int) : HintAction
}
data class HintCue(val atMs: Long, val action: HintAction)

/** Pure timeline; UI runs it on the Compose frame clock, so tests can advance deterministically. */
fun hintTimeline(q: Question): List<HintCue> {
    require(StarHintRules.supports(q))
    return buildList {
        repeat(q.left) { add(HintCue(it * StarHintRules.SPAWN_INTERVAL_MS, HintAction.Add(HintColor.YELLOW))) }
        if (q.addition) repeat(q.right) {
            add(HintCue((q.left + it) * StarHintRules.SPAWN_INTERVAL_MS, HintAction.Add(HintColor.PINK)))
        } else repeat(q.right) {
            add(HintCue(q.left * StarHintRules.SPAWN_INTERVAL_MS + StarHintRules.SUBTRACTION_PAUSE_MS +
                it * StarHintRules.SPAWN_INTERVAL_MS, HintAction.Cross(it)))
        }
    }
}
