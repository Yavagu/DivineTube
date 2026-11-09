package com.yourtube.app.security

internal data class AttemptState(
    val failures: Long = 0,
    val blockedUntilElapsed: Long = 0,
    val blockedUntilWall: Long = 0,
    val lastWall: Long = 0,
    val lastElapsed: Long = 0,
    val lastBootCount: Long = -1
)

internal data class AttemptCheck(val state: AttemptState, val retryAfterMillis: Long)

/** Uses monotonic time within a boot and a fail-closed wall deadline across reboot. */
internal object AttemptPolicy {
    const val FAILURE_LIMIT = 5L
    const val INITIAL_LOCK_MS = 30_000L
    const val MAX_LOCK_MS = 3_600_000L
    private const val CLOCK_ROLLBACK_TOLERANCE = 60_000L

    fun check(state: AttemptState, wallNow: Long, elapsedNow: Long, bootCount: Long = -1): AttemptCheck {
        val rebooted = (bootCount >= 0 && state.lastBootCount >= 0 && bootCount != state.lastBootCount) || state.lastElapsed > elapsedNow
        val wallRollback = state.lastWall > 0 && wallNow + CLOCK_ROLLBACK_TOLERANCE < state.lastWall
        val wait = when {
            wallRollback -> MAX_LOCK_MS
            rebooted -> maxOf((state.blockedUntilWall - wallNow).coerceAtLeast(0L), backoffMillis(state.failures))
            else -> (state.blockedUntilElapsed - elapsedNow).coerceAtLeast(0L)
        }
        val normalized = state.copy(
            blockedUntilElapsed = elapsedNow + wait,
            blockedUntilWall = wallNow + wait,
            lastWall = wallNow,
            lastElapsed = elapsedNow,
            lastBootCount = bootCount
        )
        return AttemptCheck(normalized, wait)
    }

    fun reserveFailure(state: AttemptState, wallNow: Long, elapsedNow: Long, bootCount: Long = -1): AttemptState {
        val failures = state.failures + 1
        val wait = backoffMillis(failures)
        return state.copy(
            failures = failures,
            blockedUntilElapsed = elapsedNow + wait,
            blockedUntilWall = wallNow + wait,
            lastWall = wallNow,
            lastElapsed = elapsedNow,
            lastBootCount = bootCount
        )
    }

    fun backoffMillis(failures: Long): Long {
        if (failures < FAILURE_LIMIT) return 0
        val exponent = (failures - FAILURE_LIMIT).coerceIn(0L, 10L).toInt()
        return (INITIAL_LOCK_MS * (1L shl exponent)).coerceAtMost(MAX_LOCK_MS)
    }
}
