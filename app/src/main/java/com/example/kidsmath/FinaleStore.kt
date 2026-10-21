package com.example.kidsmath

import android.content.Context

/** A resumable reward, stored separately from both campaign completion and play allowances. */
internal class FinaleStore(context: Context) {
    private val prefs = context.getSharedPreferences("finale", Context.MODE_PRIVATE)
    fun load(): Long? = if (prefs.getBoolean("pending", false))
        prefs.getLong("elapsed", 0).coerceIn(0, FinaleTimeline.durationMs) else null
    fun save(elapsedMs: Long, durable: Boolean = true) {
        val editor = prefs.edit().putBoolean("pending", true)
            .putLong("elapsed", elapsedMs.coerceIn(0, FinaleTimeline.durationMs))
        if (durable) check(editor.commit()) else editor.apply()
    }
    fun clear() { check(prefs.edit().clear().commit()) }
}
