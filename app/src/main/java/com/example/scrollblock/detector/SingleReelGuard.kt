package com.example.scrollblock.detector

/**
 * "DM'den gelen Reels / TikTok videosuna izin ver (tek video)" modunun karar motoru.
 * Saf Kotlin; Android tiplerine bağlı değil (unit test edilebilir).
 *
 * Akış:
 *  1. Video izleyici GÖRÜNÜR olduğunda oturum başlar -> ilk video SERBEST (ALLOW).
 *  2. İlk videonun açılışındaki geçiş animasyonu scroll/ölçekleme üretebileceğinden
 *     [graceMs] (450 ms) boyunca ilk açılış kaydırması yok sayılır.
 *  3. Grace süresi geçtikten sonra:
 *     a) Kullanıcı parmağıyla dikey kaydırdığında (TYPE_VIEW_SCROLLED) -> derhal BLOCK.
 *     b) Ekranda oynatılan videonun parmak izi (yazar, açıklama, metin) değiştiğinde -> derhal BLOCK.
 *     c) 2 veya daha fazla kaydırma olayı geldiğinde -> derhal BLOCK.
 *  4. Kaydırma animasyonu sırasında izleyici birkaç kareliğine (ör. 100-300ms) görünmez olsa bile
 *     oturum hemen sıfırlanmaz ([goneResetMs] = 1400 ms); böylece 2. videoya geçişte oturum sıfırlanıp
 *     2. videoya yanlışlıkla izin verilmesi engellenir.
 */
class SingleReelGuard(
    private val graceMs: Long = 1200L,
    private val goneResetMs: Long = 1400L
) {
    enum class Verdict { ALLOW, BLOCK }

    private var active = false
    private var blocking = false
    private var startedAt = 0L
    private var lastVisibleAt = 0L
    private var initialFingerprint: String? = null
    private var scrollEventCount = 0

    val isSessionActive: Boolean get() = active
    val isBlocking: Boolean get() = blocking

    /** İzleyici görünürken her içerik/pencere olayında çağrılır. */
    fun onViewerVisible(now: Long, currentFingerprint: String?): Verdict {
        lastVisibleAt = now
        if (blocking) return Verdict.BLOCK

        if (!active) {
            active = true
            startedAt = now
            initialFingerprint = currentFingerprint
            scrollEventCount = 0
            return Verdict.ALLOW
        }

        // İlk açılış animasyonu (graceMs) içinde arayüz henüz yerleşirken gelen yazar bilgisini ilk video olarak sabitle
        if (now - startedAt <= graceMs) {
            if (!currentFingerprint.isNullOrEmpty()) {
                initialFingerprint = currentFingerprint
            }
            return Verdict.ALLOW
        }

        // Başlangıçta parmak izi henüz hazır değilse ilk gelenle sabitle
        if (initialFingerprint.isNullOrEmpty() && !currentFingerprint.isNullOrEmpty()) {
            initialFingerprint = currentFingerprint
            return Verdict.ALLOW
        }

        // Grace süresi dolduktan sonra ekrandaki videonun parmak izi (yazar) değiştiyse -> 2. videoya geçilmiş
        if (!initialFingerprint.isNullOrEmpty() &&
            !currentFingerprint.isNullOrEmpty() &&
            currentFingerprint != initialFingerprint
        ) {
            return block()
        }

        return Verdict.ALLOW
    }

    /** Tam ekran dikey sayfalayıcıdan gerçek dikey TYPE_VIEW_SCROLLED geldiğinde çağrılır. */
    fun onPagerScrolled(now: Long): Verdict {
        lastVisibleAt = now
        if (blocking) return Verdict.BLOCK

        if (!active) {
            active = true
            startedAt = now
            scrollEventCount = 0
            return Verdict.ALLOW
        }

        // Açılış animasyonu (graceMs) içindeki yerleşim kaydırmalarını tamamen yok say
        if (now - startedAt <= graceMs) {
            return Verdict.ALLOW
        }

        scrollEventCount++
        return block()
    }

    /**
     * İzleyici o karede görünmediğinde çağrılır.
     * Kaydırma (swipe) animasyonu sırasında 100-300ms'lik anlık kaybolmalarda oturumu sıfırlamaz;
     * yalnızca izleyici gerçekten kapandıysa ([goneResetMs] geçtiyse) sıfırlar.
     */
    fun onViewerGone(now: Long = 0L) {
        if (!active) return
        if (now <= 0L || lastVisibleAt <= 0L || now - lastVisibleAt >= goneResetMs) {
            reset()
        }
    }

    fun reset() {
        active = false
        blocking = false
        startedAt = 0L
        lastVisibleAt = 0L
        initialFingerprint = null
        scrollEventCount = 0
    }

    private fun block(): Verdict {
        blocking = true
        return Verdict.BLOCK
    }
}
