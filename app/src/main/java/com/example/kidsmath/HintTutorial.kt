package com.example.kidsmath

const val HINT_IDLE_TIMEOUT_MS = 8_000L
internal const val HINT_TUTORIAL_MOVES = 3
internal const val HINT_TUTORIAL_CYCLES = 3
internal const val HINT_TUTORIAL_MOVE_MS = 900L
enum class HintTutorialStage { OPEN, EXTRACT, DIM }

/** Monotonic frame time only. Interaction cancels a cycle without refunding it. */
internal class HintTutorialTimer {
    var cycles = 0; private set
    private var idleSince: Long? = null
    private var started: Long? = null
    fun interrupt() { idleSince = null; started = null }
    fun frame(now: Long): Float? {
        started?.let { start ->
            val elapsed = now - start
            if (elapsed < HINT_TUTORIAL_MOVES * HINT_TUTORIAL_MOVE_MS)
                return (elapsed % HINT_TUTORIAL_MOVE_MS).toFloat() / HINT_TUTORIAL_MOVE_MS
            started = null
            idleSince = now
            return null
        }
        if (cycles >= HINT_TUTORIAL_CYCLES) return null
        val since = idleSince ?: now.also { idleSince = it }
        if (now - since < HINT_IDLE_TIMEOUT_MS) return null
        cycles++
        started = now
        return 0f
    }
}
