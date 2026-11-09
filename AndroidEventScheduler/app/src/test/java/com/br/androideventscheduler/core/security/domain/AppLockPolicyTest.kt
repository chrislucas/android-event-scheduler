package com.br.androideventscheduler.core.security.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {
    private fun shouldLock(
        enabled: Boolean = true,
        backgroundedAt: Long? = 1_000L,
        now: Long,
        timeout: AppLockTimeout,
    ) = AppLockPolicy.shouldLock(enabled, backgroundedAt, now, timeout)

    @Test
    fun `given protection disabled when returning from background then it never locks`() {
        assertFalse(shouldLock(enabled = false, now = 10_000_000L, timeout = AppLockTimeout.IMMEDIATE))
    }

    @Test
    fun `given immediate timeout when returning after zero milliseconds then it locks`() {
        assertTrue(shouldLock(now = 1_000L, timeout = AppLockTimeout.IMMEDIATE))
    }

    @Test
    fun `given one minute timeout when returning after 59 seconds then it does not lock`() {
        assertFalse(shouldLock(now = 1_000L + 59_000L, timeout = AppLockTimeout.ONE_MINUTE))
    }

    @Test
    fun `given one minute timeout when returning exactly at the limit then it locks`() {
        assertTrue(shouldLock(now = 1_000L + 60_000L, timeout = AppLockTimeout.ONE_MINUTE))
    }

    @Test
    fun `given five minutes timeout when returning after four minutes then it does not lock and after five it does`() {
        assertFalse(shouldLock(now = 1_000L + 240_000L, timeout = AppLockTimeout.FIVE_MINUTES))
        assertTrue(shouldLock(now = 1_000L + 300_000L, timeout = AppLockTimeout.FIVE_MINUTES))
    }

    @Test
    fun `given the app never went to background when checking then it does not lock`() {
        assertFalse(shouldLock(backgroundedAt = null, now = 10_000_000L, timeout = AppLockTimeout.IMMEDIATE))
    }
}
