package com.example.scrollblock.detector.instagram

/**
 * Yalnızca Instagram DM tek-video izni için izole edilmiş karar motoru.
 * TikTok veya YouTube Shorts kodlarından tamamen bağımsızdır.
 */
class InstagramSingleReelGuard(
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

        if (now - startedAt <= graceMs) {
            if (!currentFingerprint.isNullOrEmpty()) {
                initialFingerprint = currentFingerprint
            }
            return Verdict.ALLOW
        }

        if (initialFingerprint.isNullOrEmpty() && !currentFingerprint.isNullOrEmpty()) {
            initialFingerprint = currentFingerprint
            return Verdict.ALLOW
        }

        if (!initialFingerprint.isNullOrEmpty() &&
            !currentFingerprint.isNullOrEmpty() &&
            currentFingerprint != initialFingerprint
        ) {
            return block()
        }

        return Verdict.ALLOW
    }

    fun onPagerScrolled(now: Long): Verdict {
        lastVisibleAt = now
        if (blocking) return Verdict.BLOCK

        if (!active) {
            active = true
            startedAt = now
            scrollEventCount = 0
            return Verdict.ALLOW
        }

        if (now - startedAt <= graceMs) {
            return Verdict.ALLOW
        }

        scrollEventCount++
        return block()
    }

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

