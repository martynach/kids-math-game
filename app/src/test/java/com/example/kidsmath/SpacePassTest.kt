package com.example.kidsmath

import org.junit.Assert.*
import org.junit.Test

class SpacePassTest {
    @Test fun twoFreeStartsThenCooldown() {
        var pass = SpacePass()
        assertEquals(2, pass.remainingAttempts)
        pass = pass.startAttempt(1000)!!
        assertEquals(1, pass.remainingAttempts)
        assertEquals(0L, pass.cooldownEnd)
        pass = pass.startAttempt(2000)!!
        assertEquals(0, pass.remainingAttempts)
        assertEquals(2000 + SpacePassConfig.FREE_COOLDOWN_MS, pass.cooldownEnd)
        assertNull(pass.startAttempt(2001))
    }
    @Test fun premiumCustomBlocksSixthAndRestoresAfterOneHour() {
        var pass = SpacePass().activatePremium().configureLimits(false, 5, 1)
        repeat(5) { pass = pass.startAttempt(1000)!! }
        assertEquals(0, pass.premiumRemaining)
        assertEquals(3601000L, pass.premiumCooldownEnd)
        assertNull(pass.startAttempt(3600999))
        assertEquals(4, pass.startAttempt(3601000)!!.premiumRemaining)
    }
    @Test fun switchingAndUnlimitedPreserveIndependentUsage() {
        val free = SpacePass().startAttempt(0)!!.startAttempt(1)!!
        var premium = free.activatePremium().configureLimits(false, 5, 1).startAttempt(2)!!
        premium = premium.configureLimits(true, 5, 1)
        repeat(10) { premium = premium.startAttempt(3)!! }
        assertEquals(4, premium.premiumRemaining)
        assertEquals(5, premium.premiumAttempts)
        assertFalse(premium.activateFree().canStart(3))
        assertEquals(free.cooldownEnd, premium.activateFree().cooldownEnd)
        assertEquals(3, premium.configureLimits(false, 5, 1).startAttempt(3)!!.premiumRemaining)
    }
    @Test fun invalidCustomLimitsAreRejected() {
        for ((attempts, hours) in listOf(0 to 1, -1 to 1, 5 to 0, 5 to -1, 5 to 8761)) {
            assertThrows(IllegalArgumentException::class.java) { SpacePass().configureLimits(false, attempts, hours) }
        }
    }
    @Test fun expiryRestoresExactlyOneFullAllowanceAndCanStartImmediately() {
        val end = 500_000L
        val pass = SpacePass(remainingAttempts = 0, cooldownEnd = end)
        assertFalse(pass.canStart(end - 1))
        assertEquals(1, pass.remainingCooldown(end - 1))
        val restored = pass.refresh(end)
        assertEquals(SpacePass(), restored)
        assertTrue(pass.canStart(end))
        assertEquals(1, pass.startAttempt(end)!!.remainingAttempts)
        assertEquals(restored, restored.refresh(end + SpacePassConfig.FREE_COOLDOWN_MS))
    }

    @Test fun midnightDoesNotResetOrExtendAnActiveCooldown() {
        val midnight = 86_400_000L
        val pass = SpacePass(remainingAttempts = 1).startAttempt(midnight - 60_000)!!
        assertFalse(pass.canStart(midnight))
        assertEquals(midnight + 240_000, pass.cooldownEnd)
        assertTrue(pass.canStart(midnight + 240_000))
    }

    @Test fun mockPremiumIgnoresAnExistingCooldownWithoutConsumingAttempts() {
        val free = SpacePass(remainingAttempts = 0, cooldownEnd = 500_000)
        val premium = free.activatePremium()
        assertTrue(premium.isPremium)
        assertTrue(premium.canStart(1000))
        repeat(20) { assertEquals(premium, premium.startAttempt(1000)) }
        assertEquals(0, premium.remainingAttempts)
    }

    @Test fun countdownRoundsUpAndSupportsLongerProductionCooldowns() {
        assertEquals("0m 0s", formatCooldown(-1))
        assertEquals("0m 1s", formatCooldown(1))
        assertEquals("4m 32s", formatCooldown(272_000))
        assertEquals("5m 0s", formatCooldown(SpacePassConfig.FREE_COOLDOWN_MS))
        assertEquals("12h 0m", formatCooldown(12 * 3600_000L))
    }
}
