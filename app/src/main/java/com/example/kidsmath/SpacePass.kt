package com.example.kidsmath

object SpacePassConfig {
    const val DEFAULT_PREMIUM_ATTEMPTS = 5
    const val DEFAULT_REST_HOURS = 1
    const val MAX_REST_HOURS = 8760
    const val FREE_ATTEMPTS = 2
    const val FREE_COOLDOWN_MS = 5 * 60 * 1000L
}

/** Epoch time is supplied by the caller so the allowance survives process death. */
data class SpacePass(
    val isPremium: Boolean = false,
    val remainingAttempts: Int = SpacePassConfig.FREE_ATTEMPTS,
    val cooldownEnd: Long = 0L,
    val unlimitedAttempts: Boolean = true,
    val premiumAttempts: Int = SpacePassConfig.DEFAULT_PREMIUM_ATTEMPTS,
    val restHours: Int = SpacePassConfig.DEFAULT_REST_HOURS,
    val premiumRemaining: Int = premiumAttempts,
    val premiumCooldownEnd: Long = 0L,
) {
    init { require(premiumAttempts > 0 && restHours in 1..SpacePassConfig.MAX_REST_HOURS && premiumRemaining in 0..premiumAttempts) }
    fun refresh(now: Long): SpacePass {
        var result = this
        if (cooldownEnd > 0 && now >= cooldownEnd)
            result = result.copy(remainingAttempts = SpacePassConfig.FREE_ATTEMPTS, cooldownEnd = 0)
        if (premiumCooldownEnd > 0 && now >= premiumCooldownEnd)
            result = result.copy(premiumRemaining = premiumAttempts, premiumCooldownEnd = 0)
        return result
    }
    fun canStart(now: Long): Boolean {
        val current = refresh(now)
        return if (isPremium) unlimitedAttempts || current.premiumRemaining > 0 else current.remainingAttempts > 0
    }
    /** Called once when an unlocked level actually starts, before navigation. */
    fun startAttempt(now: Long): SpacePass? {
        val current = refresh(now)
        if (!current.canStart(now)) return null
        if (current.isPremium) {
            if (current.unlimitedAttempts) return current
            val remaining = current.premiumRemaining - 1
            return current.copy(premiumRemaining = remaining,
                premiumCooldownEnd = if (remaining == 0) now + restHours * 3600_000L else 0)
        }
        val remaining = current.remainingAttempts - 1
        return current.copy(remainingAttempts = remaining,
            cooldownEnd = if (remaining == 0) now + SpacePassConfig.FREE_COOLDOWN_MS else 0)
    }
    fun remainingCooldown(now: Long) =
        ((if (isPremium) premiumCooldownEnd else cooldownEnd) - now).coerceAtLeast(0)
    fun activatePremium() = copy(isPremium = true)
    fun activateFree() = copy(isPremium = false)
    // Changing allowance starts a fresh cycle; toggling unlimited preserves usage.
    fun configureLimits(unlimited: Boolean, attempts: Int, hours: Int): SpacePass {
        require(attempts > 0 && hours in 1..SpacePassConfig.MAX_REST_HOURS)
        return copy(unlimitedAttempts = unlimited, premiumAttempts = attempts, restHours = hours,
            premiumRemaining = if (attempts != premiumAttempts) attempts else premiumRemaining,
            premiumCooldownEnd = if (attempts != premiumAttempts) 0 else premiumCooldownEnd)
    }
}

fun formatCooldown(milliseconds: Long): String {
    val seconds = (milliseconds.coerceAtLeast(0) + 999) / 1000
    return if (seconds >= 3600) "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    else "${seconds / 60}m ${seconds % 60}s"
}
