package com.example.scrollblock.detector

/**
 * "DM'den gelen Reels'a izin ver (tek video)" modunun karar motoru.
 * Saf Kotlin; Android tiplerine bağlı değil (unit test edilebilir).
 *
 * Akış:
 *  1. Reel izleyici GÖRÜNÜR olduğunda oturum başlar -> ilk video SERBEST.
 *     (Oturum yalnızca girişe izin verildiyse açılır: DM'den veya dış bağlantıdan.)
 *  2. Açılıştaki geçiş animasyonu da scroll olayı ürettiği için [graceMs] boyunca
 *     kaydırma olayları yok sayılır.
 *  3. Grace sonrası, tam ekran dikey sayfalayıcıdan gelen kaydırma olayı VEYA ekrandaki
 *     videonun parmak izinin (kullanıcı adı/açıklama) değişmesi = sonraki videoya geçiş -> ENGELLE.
 *  4. Engelleme sonrası izleyici kapanana kadar ENGELLE dönmeye devam eder; izleyici
 *     kaybolunca oturum sıfırlanır.
 */
class SingleReelGuard(
    private val graceMs: Long = 900L
) {
    enum class Verdict { ALLOW, BLOCK }

    private var active = false
    private var blocking = false
    private var startedAt = 0L
    private var fingerprint: String? = null

    val isSessionActive: Boolean get() = active

    /** İzleyici görünürken her içerik/pencere olayında çağrılır. */
    fun onViewerVisible(now: Long, currentFingerprint: String?): Verdict {
        if (blocking) return Verdict.BLOCK
        if (!active) {
            active = true
            startedAt = now
            fingerprint = currentFingerprint
            return Verdict.ALLOW
        }
        if (fingerprint == null) {
            fingerprint = currentFingerprint
            return Verdict.ALLOW
        }
        if (currentFingerprint != null &&
            currentFingerprint != fingerprint &&
            now - startedAt > graceMs
        ) {
            return block()
        }
        return Verdict.ALLOW
    }

    /** Tam ekran dikey sayfalayıcıdan TYPE_VIEW_SCROLLED geldiğinde çağrılır. */
    fun onPagerScrolled(now: Long): Verdict {
        if (blocking) return Verdict.BLOCK
        if (!active) {
            // Oturum henüz kurulmadan gelen scroll = açılış animasyonu
            active = true
            startedAt = now
            return Verdict.ALLOW
        }
        return if (now - startedAt > graceMs) block() else Verdict.ALLOW
    }

    /** İzleyici artık görünmüyor (DM'e dönüldü vb.). */
    fun onViewerGone() = reset()

    fun reset() {
        active = false
        blocking = false
        startedAt = 0L
        fingerprint = null
    }

    private fun block(): Verdict {
        blocking = true
        return Verdict.BLOCK
    }
}
