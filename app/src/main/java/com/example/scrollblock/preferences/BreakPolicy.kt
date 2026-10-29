package com.example.scrollblock.preferences

/**
 * Mola (cooldown) kuralları — saf Kotlin, Android'e bağımlı değil (unit test edilebilir).
 *
 * Neden CountDownTimer/WorkManager değil?
 *  - Mola, bitiş zamanı (epoch ms) olarak kalıcı saklanır. Servis her olayda "şu an < bitiş mi?"
 *    diye bakar. Böylece uygulama öldürülse, telefon yeniden başlasa bile mola tam zamanında,
 *    hiçbir zamanlayıcıya ihtiyaç duymadan kendiliğinden biter.
 *  - CountDownTimer süreç ölünce kaybolur; WorkManager ise dakika hassasiyeti garanti etmez.
 */
object BreakPolicy {

    val DURATION_OPTIONS_MIN = listOf(5, 10, 15, 30)
    const val DEFAULT_DURATION_MIN = 10

    /** 0 = sınırsız */
    val DAILY_LIMIT_OPTIONS = listOf(0, 1, 2, 3)
    const val DEFAULT_DAILY_LIMIT = 0

    data class State(
        val endsAt: Long,
        val startedAt: Long,
        val dayKey: Int,
        val usedToday: Int
    )

    /**
     * Mola şu an aktif mi?
     * Saat geri alınırsa (now < startedAt) mola hile sayılır ve biter.
     */
    fun isActive(state: State, now: Long): Boolean =
        state.endsAt > now && now >= state.startedAt

    fun usedToday(state: State, todayKey: Int): Int =
        if (state.dayKey == todayKey) state.usedToday else 0

    fun remainingToday(state: State, todayKey: Int, dailyLimit: Int): Int? =
        if (dailyLimit <= 0) null else (dailyLimit - usedToday(state, todayKey)).coerceAtLeast(0)

    fun canStart(state: State, now: Long, todayKey: Int, dailyLimit: Int): Boolean {
        if (isActive(state, now)) return false
        val left = remainingToday(state, todayKey, dailyLimit)
        return left == null || left > 0
    }

    /** Yeni mola durumu; başlatılamıyorsa null. */
    fun start(state: State, now: Long, todayKey: Int, dailyLimit: Int, durationMin: Int): State? {
        if (!canStart(state, now, todayKey, dailyLimit)) return null
        val minutes = durationMin.coerceIn(1, 120)
        return State(
            endsAt = now + minutes * 60_000L,
            startedAt = now,
            dayKey = todayKey,
            usedToday = usedToday(state, todayKey) + 1
        )
    }

    /** Molayı bitirir; kullanılan hak geri verilmez (kötüye kullanımı önler). */
    fun end(state: State): State = state.copy(endsAt = 0L, startedAt = 0L)

    /** 2026-10-29 -> 20261029 */
    fun dayKey(year: Int, month1to12: Int, day: Int): Int = year * 10_000 + month1to12 * 100 + day
}
