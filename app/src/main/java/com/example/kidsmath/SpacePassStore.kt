package com.example.kidsmath

import android.content.Context

class SpacePassStore(context: Context) {
    private val prefs = context.getSharedPreferences("space_pass", Context.MODE_PRIVATE)
    fun load() = SpacePass(
        isPremium = prefs.getBoolean("premium", false),
        remainingAttempts = prefs.getInt("remaining", SpacePassConfig.FREE_ATTEMPTS)
            .coerceIn(0, SpacePassConfig.FREE_ATTEMPTS),
        unlimitedAttempts = prefs.getBoolean("unlimited", true),
        premiumAttempts = prefs.getInt("premium_attempts", SpacePassConfig.DEFAULT_PREMIUM_ATTEMPTS).coerceAtLeast(1),
        restHours = prefs.getInt("rest_hours", SpacePassConfig.DEFAULT_REST_HOURS).coerceIn(1, SpacePassConfig.MAX_REST_HOURS),
        premiumRemaining = prefs.getInt("premium_remaining", SpacePassConfig.DEFAULT_PREMIUM_ATTEMPTS).coerceIn(0, prefs.getInt("premium_attempts", SpacePassConfig.DEFAULT_PREMIUM_ATTEMPTS).coerceAtLeast(1)),
        premiumCooldownEnd = prefs.getLong("premium_cooldown", 0).coerceAtLeast(0),
        cooldownEnd = prefs.getLong("cooldown_end", 0).coerceAtLeast(0),
    )
    fun save(pass: SpacePass) {
        // Commit before gameplay starts: killing the app must never refund a mission.
        check(prefs.edit().putBoolean("premium", pass.isPremium)
            .putBoolean("unlimited", pass.unlimitedAttempts)
            .putInt("premium_attempts", pass.premiumAttempts).putInt("rest_hours", pass.restHours)
            .putInt("premium_remaining", pass.premiumRemaining).putLong("premium_cooldown", pass.premiumCooldownEnd)
            .putInt("remaining", pass.remainingAttempts).putLong("cooldown_end", pass.cooldownEnd).commit())
    }
}
