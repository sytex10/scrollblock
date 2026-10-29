package com.example.scrollblock.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SingleReelGuardTest {

    private lateinit var guard: SingleReelGuard
    private val graceMs = 900L

    @Before
    fun setUp() {
        guard = SingleReelGuard(graceMs = graceMs)
    }

    @Test
    fun testFirstVideoIsAllowed() {
        val now = 10_000L
        val verdict1 = guard.onViewerVisible(now, "author:john|caption:fun")
        assertEquals(SingleReelGuard.Verdict.ALLOW, verdict1)
        assertTrue(guard.isSessionActive)

        // Aynı video devam ederken ALLOW dönmeye devam eder
        val verdict2 = guard.onViewerVisible(now + 2_000L, "author:john|caption:fun")
        assertEquals(SingleReelGuard.Verdict.ALLOW, verdict2)
    }

    @Test
    fun testInitialAnimationScrollIgnoredWithinGracePeriod() {
        val now = 10_000L
        // İzleyici açılıyor
        guard.onViewerVisible(now, "author:alice")

        // Açılış anındaki geçiş animasyonu scroll tetikleyebilir (300ms sonra)
        val scrollVerdict = guard.onPagerScrolled(now + 300L)
        assertEquals(SingleReelGuard.Verdict.ALLOW, scrollVerdict)

        // Hâlâ aynı video
        val visibleVerdict = guard.onViewerVisible(now + 400L, "author:alice")
        assertEquals(SingleReelGuard.Verdict.ALLOW, visibleVerdict)
    }

    @Test
    fun testScrollAfterGracePeriodTriggersBlock() {
        val now = 10_000L
        guard.onViewerVisible(now, "author:alice")

        // Kullanıcı videoyu izledi, 2 saniye sonra bir sonraki videoya kaydırdı
        val scrollVerdict = guard.onPagerScrolled(now + 2_000L)
        assertEquals(SingleReelGuard.Verdict.BLOCK, scrollVerdict)

        // Artık izleyici kapanana kadar BLOCK döner
        val nextVideoVerdict = guard.onViewerVisible(now + 2_500L, "author:bob")
        assertEquals(SingleReelGuard.Verdict.BLOCK, nextVideoVerdict)
    }

    @Test
    fun testFingerprintChangeAfterGracePeriodTriggersBlock() {
        val now = 10_000L
        guard.onViewerVisible(now, "author:creator1|cap:hello")

        // 1.5 saniye sonra kullanıcı kaydırdı ve ekrandaki içerik değişti
        val changedVerdict = guard.onViewerVisible(now + 1_500L, "author:creator2|cap:different")
        assertEquals(SingleReelGuard.Verdict.BLOCK, changedVerdict)
    }

    @Test
    fun testResetWhenViewerGone() {
        val now = 10_000L
        guard.onViewerVisible(now, "author:first")
        // Kaydırma ile engelleme tetiklendi
        guard.onPagerScrolled(now + 1_500L)
        assertEquals(SingleReelGuard.Verdict.BLOCK, guard.onPagerScrolled(now + 1_600L))

        // Kullanıcı DM'e geri döndü (izleyici kapandı)
        guard.onViewerGone()
        assertFalse(guard.isSessionActive)

        // DM'den başka bir reels açtı -> yeni oturum başlar ve ilk video serbesttir
        val newSessionVerdict = guard.onViewerVisible(now + 5_000L, "author:second")
        assertEquals(SingleReelGuard.Verdict.ALLOW, newSessionVerdict)
        assertTrue(guard.isSessionActive)
    }
}
