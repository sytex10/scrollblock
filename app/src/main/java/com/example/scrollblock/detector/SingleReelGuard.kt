package com.example.scrollblock.detector

/**
 * "DM'den gelen Reels / TikTok videosuna izin ver (tek video)" modunun karar motoru.
 * Saf Kotlin; Android tiplerine bağlı değil (unit test edilebilir).
 *
 * Akış:
 *  1. Video izleyici GÖRÜNÜR olduğunda oturum başlar -> ilk video SERBEST (ALLOW).
 *  2. İlk videonun açılışındaki geçiş animasyonu scroll/ölçekleme üretebileceğinden
 *     [graceMs] (500 ms) boyunca kaydırmalar yok sayılır.
 *  3. Grace süresi geçtikten sonra:
 *     a) Kullanıcı parmağıyla dikey kaydırdığında (TYPE_VIEW_SCROLLED) -> derhal BLOCK.
 *     b) Ekranda oynatılan videonun parmak izi (yazar, açıklama, başlık) değiştiğinde -> derhal BLOCK.
 *     c) 2'den fazla kaydırma algılandığında -> derhal BLOCK.
 *  4. İzleyici tamamen ekrandan kaybolana kadar BLOCK kararı korunur (kullanıcıyı sohbete atar).
 *     İzleyici kapandığında oturum sıfırlanır, böylece bir sonraki DM videosu yine tek video olarak açılabilir.
 */
class SingleReelGuard(
    private val graceMs: Long = 500L
) {
    enum class Verdict { ALLOW, BLOCK }

    private var active = false
    private var blocking = false
    private var startedAt = 0L
    private var initialFingerprint: String? = null
    private var scrollEventCount = 0

    val isSessionActive: Boolean get() = active
    val isBlocking: Boolean get() = blocking

    /** İzleyici görünürken her içerik/pencere olayında çağrılır. */
    fun onViewerVisible(now: Long, currentFingerprint: String?): Verdict {
        if (blocking) return Verdict.BLOCK

        if (!active) {
            active = true
            startedAt = now
            initialFingerprint = currentFingerprint
            scrollEventCount = 0
            return Verdict.ALLOW
        }

        // Başlangıçta parmak izi henüz hazır değilse ilk gelenle sabitle
        if (initialFingerprint.isNullOrEmpty() && !currentFingerprint.isNullOrEmpty()) {
            initialFingerprint = currentFingerprint
            return Verdict.ALLOW
        }

        // Grace süresi dolduktan sonra ekrandaki videonun parmak izi değiştiyse -> 2. videoya geçilmiş
        if (!initialFingerprint.isNullOrEmpty() &&
            !currentFingerprint.isNullOrEmpty() &&
            currentFingerprint != initialFingerprint &&
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
            // Oturum henüz kurulmadan gelen scroll = açılış geçişi
            active = true
            startedAt = now
            scrollEventCount = 1
            return Verdict.ALLOW
        }

        scrollEventCount++

        // Grace süresi bittiğinde veya birden fazla scroll olayı geldiyse engelle
        return if (now - startedAt > graceMs || scrollEventCount >= 2) {
            block()
        } else {
            Verdict.ALLOW
        }
    }

    /** İzleyici artık görünmüyor (DM'e/sohbete geri dönüldü). */
    fun onViewerGone() = reset()

    fun reset() {
        active = false
        blocking = false
        startedAt = 0L
        initialFingerprint = null
        scrollEventCount = 0
    }

    private fun block(): Verdict {
        blocking = true
        return Verdict.BLOCK
    }
}
