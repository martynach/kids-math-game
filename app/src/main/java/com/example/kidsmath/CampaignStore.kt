package com.example.kidsmath

import android.content.Context

class CampaignStore(context: Context) {
    private val prefs = context.getSharedPreferences("campaign", Context.MODE_PRIVATE)
    fun load(): CampaignProgress {
        val highest = prefs.getInt("highest", 1).coerceIn(1, CampaignConfig.levels.size)
        return CampaignProgress(highest, highest == CampaignConfig.levels.size && prefs.getBoolean("finished", false))
    }
    fun save(progress: CampaignProgress) {
        check(prefs.edit().putInt("highest", progress.highestUnlockedLevel)
            .putBoolean("finished", progress.finalLevelCompleted).commit())
    }
}
