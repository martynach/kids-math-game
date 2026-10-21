package com.example.kidsmath

/** Durations are the only story clock. Victory has no timeout. */
enum class FinaleScene(val durationMs: Long, val description: String) {
    JOURNEY(5000, "Journey to the princess planet"),
    CASTLE(4000, "Princess behind a locked tower door"),
    MEETING(4000, "Astronaut approaches the friendly dragon"),
    GIFT(6000, "A magical star in exchange for the key"),
    RESCUE(6000, "Unlocking the tower and greeting the princess"),
    BOARDING(5000, "Friends board the rocket and wave goodbye"),
    EARTH(5000, "Flying home to Earth"),
    HOME(5000, "Safe landing and celebration")
}

data class FinaleFrame(val scene: FinaleScene, val progress: Float)

/** Story events shared by rendering and boundary tests; no persisted emotion state. */
internal object FinaleEmotions {
    fun princessReleased(frame: FinaleFrame): Boolean =
        frame.scene.ordinal > FinaleScene.RESCUE.ordinal ||
            (frame.scene == FinaleScene.RESCUE && frame.progress >= .32f)

    fun dragonDelight(frame: FinaleFrame): Float = when {
        frame.scene.ordinal < FinaleScene.GIFT.ordinal -> 0f
        frame.scene.ordinal > FinaleScene.GIFT.ordinal -> 1f
        else -> ((frame.progress - .18f) / .06f).coerceIn(0f, 1f)
    }
}

object FinaleTimeline {
    val durationMs = FinaleScene.entries.sumOf { it.durationMs }
    fun isFinalLevel(level: Int) = level == CampaignConfig.levels.last().level
    fun frame(elapsedMs: Long): FinaleFrame {
        var remaining = elapsedMs.coerceIn(0, durationMs)
        for (scene in FinaleScene.entries) {
            if (remaining < scene.durationMs) return FinaleFrame(scene, remaining.toFloat() / scene.durationMs)
            remaining -= scene.durationMs
        }
        return FinaleFrame(FinaleScene.HOME, 1f)
    }
    fun advance(elapsedMs: Long, deltaMs: Long): Long =
        elapsedMs.coerceIn(0, durationMs) + deltaMs.coerceIn(0, durationMs - elapsedMs.coerceIn(0, durationMs))
}
