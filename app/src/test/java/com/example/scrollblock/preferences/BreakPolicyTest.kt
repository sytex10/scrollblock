package com.example.scrollblock.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakPolicyTest {

    private val sampleDayKey = BreakPolicy.dayKey(2026, 10, 29) // 20261029

    @Test
    fun testDayKeyFormat() {
        assertEquals(20261029, sampleDayKey)
        assertEquals(20270101, BreakPolicy.dayKey(2027, 1, 1))
    }

    @Test
    fun testIsActive() {
        val now = 100_000L
        val activeState = BreakPolicy.State(
            endsAt = now + 600_000L,
            startedAt = now - 1_000L,
            dayKey = sampleDayKey,
            usedToday = 1
        )
        assertTrue(BreakPolicy.isActive(activeState, now))

        // Süresi dolmuş
        val expiredState = activeState.copy(endsAt = now - 1L)
        assertFalse(BreakPolicy.isActive(expiredState, now))

        // Saat geri alınmış (hile/clock manipulation)
        val tamperedClock = activeState.copy(startedAt = now + 500L)
        assertFalse(BreakPolicy.isActive(tamperedClock, now))
    }

    @Test
    fun testDailyLimitAndRemaining() {
        val state = BreakPolicy.State(
            endsAt = 0L,
            startedAt = 0L,
            dayKey = sampleDayKey,
            usedToday = 2
        )

        // Sınırsız mola (0)
        assertNull(BreakPolicy.remainingToday(state, sampleDayKey, 0))

        // Günlük limit 3, kullanılan 2 -> kalan 1
        assertEquals(1, BreakPolicy.remainingToday(state, sampleDayKey, 3))

        // Farklı gün (ertesi gün reset sayılır)
        val nextDayKey = sampleDayKey + 1
        assertEquals(3, BreakPolicy.remainingToday(state, nextDayKey, 3))
    }

    @Test
    fun testStartBreakSuccessAndLimitEnforcement() {
        val now = 1_000_000L
        var state = BreakPolicy.State(0L, 0L, sampleDayKey, 0)

        // Limit: 2 kez mola
        val limit = 2
        assertTrue(BreakPolicy.canStart(state, now, sampleDayKey, limit))

        // 1. Mola (10 dk)
        val state1 = BreakPolicy.start(state, now, sampleDayKey, limit, 10)
        assertNotNull(state1)
        assertEquals(now + 10 * 60_000L, state1!!.endsAt)
        assertEquals(1, state1.usedToday)
        assertTrue(BreakPolicy.isActive(state1, now))

        // Mola devam ederken yeni mola başlatılamaz
        assertFalse(BreakPolicy.canStart(state1, now + 1_000L, sampleDayKey, limit))

        // 1. Molayı erken bitirelim
        val stateEnded = BreakPolicy.end(state1)
        assertFalse(BreakPolicy.isActive(stateEnded, now + 5_000L))
        assertEquals(1, stateEnded.usedToday) // Hak iade edilmez

        // 2. Mola (15 dk)
        val state2 = BreakPolicy.start(stateEnded, now + 6_000L, sampleDayKey, limit, 15)
        assertNotNull(state2)
        assertEquals(2, state2!!.usedToday)

        // 2. Molayı bitirelim
        val state2Ended = BreakPolicy.end(state2)
        assertEquals(2, state2Ended.usedToday)

        // Günlük limit doldu (2/2) -> artık başlatılamaz
        assertFalse(BreakPolicy.canStart(state2Ended, now + 10_000L, sampleDayKey, limit))
        val state3 = BreakPolicy.start(state2Ended, now + 10_000L, sampleDayKey, limit, 10)
        assertNull(state3)

        // Yeni gün gelince tekrar başlatılabilir
        val nextDay = sampleDayKey + 1
        assertTrue(BreakPolicy.canStart(state2Ended, now + 86_400_000L, nextDay, limit))
    }
}
