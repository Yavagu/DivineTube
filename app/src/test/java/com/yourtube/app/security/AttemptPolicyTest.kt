package com.yourtube.app.security

import org.junit.Assert.*
import org.junit.Test

class AttemptPolicyTest {
    @Test fun backoffStartsAtFifthFailureAndCapsAtOneHour() {
        assertEquals(0L, AttemptPolicy.backoffMillis(4))
        assertEquals(30_000L, AttemptPolicy.backoffMillis(5))
        assertEquals(60_000L, AttemptPolicy.backoffMillis(6))
        assertEquals(3_600_000L, AttemptPolicy.backoffMillis(20))
    }

    @Test fun forwardWallClockChangeCannotShortenSameBootWait() {
        var state = AttemptState(lastWall = 10_000L, lastElapsed = 5_000L, lastBootCount = 7)
        repeat(5) { state = AttemptPolicy.reserveFailure(state, 10_000L + it, 5_000L + it, 7) }
        val check = AttemptPolicy.check(state, wallNow = 90_000_000L, elapsedNow = 5_008L, bootCount = 7)
        assertEquals(29_996L, check.retryAfterMillis)
    }

    @Test fun rebootUsesPersistentWallDeadlineAndKeepsMinimumLockEvenAfterClockMovesForward() {
        val state = AttemptState(failures = 5, blockedUntilElapsed = 40_000L, blockedUntilWall = 130_000L,
            lastWall = 100_000L, lastElapsed = 20_000L, lastBootCount = 7)
        val reboot = AttemptPolicy.check(state, wallNow = 110_000L, elapsedNow = 30_000L, bootCount = 8)
        assertEquals(30_000L, reboot.retryAfterMillis)
        val forwardedClock = AttemptPolicy.check(state, wallNow = 90_000_000L, elapsedNow = 30_000L, bootCount = 8)
        assertEquals(30_000L, forwardedClock.retryAfterMillis)
        val rollback = AttemptPolicy.check(state, wallNow = 1_000L, elapsedNow = 30_000L, bootCount = 8)
        assertEquals(AttemptPolicy.MAX_LOCK_MS, rollback.retryAfterMillis)
    }

    @Test fun wallClockRollbackTriggersMaxLockoutOnlyWhenExceedingTolerance() {
        val baseState = AttemptState(failures = 0, lastWall = 100_000L, lastElapsed = 20_000L)
        // Within 60s rollback tolerance: no max lockout
        val withinTolerance = AttemptPolicy.check(baseState, wallNow = 45_000L, elapsedNow = 25_000L)
        assertEquals(0L, withinTolerance.retryAfterMillis)
        // Exceeding 60s rollback tolerance (rollback by 65s): triggers MAX_LOCK_MS (1 hour)
        val beyondTolerance = AttemptPolicy.check(baseState, wallNow = 35_000L, elapsedNow = 25_000L)
        assertEquals(AttemptPolicy.MAX_LOCK_MS, beyondTolerance.retryAfterMillis)
    }
}
