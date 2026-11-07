/**
 * ScrollBlock PC — TikTok Engelleyici (Tamamen İzole Modül: detectors/tiktok)
 *  - Bu dosyadaki hiçbir değişiklik Instagram veya YouTube Shorts modüllerini etkilemez.
 *  - TikTok Ana Sayfa (Sizin İçin / For You) ve Keşfet akışını -> Mesajlar (/messages) ekranına yönlendirir.
 *  - Mesajlar (/messages), Kullanıcı Profilleri (/@kullanici), Hikayeler ve Ayarlar -> %100 Serbesttir.
 *  - DM'den (/messages) açılan tam ekran videolarda:
 *      * "DM'den gelen ilk videoya izin ver" AÇIK ise: İlk video izlenir; 2. videoya geçilince Mesajlar'a döner.
 *      * KAPALI ise: Açıldığı an Mesajlar'a döner.
 */
(function () {
  "use strict";

  const PLATFORM_NAME = "TikTok";
  let lastBlockTime = 0;
  let inDmContext = false;
  let allowedDmVideoPath = null;

  function isProtectedAndActive(state) {
    if (!state) return true;
    if (state.isBreakActive && Date.now() < (state.breakEndTimeMs || 0)) {
      return false;
    }
    return state.isTikTokEnabled !== false;
  }

  function recordAndRedirect(targetUrl, reason) {
    const now = Date.now();
    if (now - lastBlockTime > 900) {
      lastBlockTime = now;
      chrome.runtime.sendMessage({
        type: "RECORD_BLOCK",
        platform: PLATFORM_NAME,
        reason
      });
    }
    window.location.replace(targetUrl);
  }

  function evaluateTikTokPage() {
    chrome.storage.local.get(
      ["isTikTokEnabled", "isTikTokDmAllowed", "isBreakActive", "breakEndTimeMs"],
      (state) => {
        if (!isProtectedAndActive(state)) return;

        const path = window.location.pathname || "/";

        // 1. Mesajlar (DM Gelen Kutusu) -> %100 Serbest ve DM bağlamını aktif et
        if (path.startsWith("/messages")) {
          inDmContext = true;
          allowedDmVideoPath = null;
          return;
        }

        // 2. Bir kullanıcının profil sayfası (/@kullanici) veya video sayfası (/@kullanici/video/123)
        if (path.startsWith("/@")) {
          const isSingleVideoUrl = path.includes("/video/");
          if (!isSingleVideoUrl) {
            // Kullanıcı profili ızgarası -> Serbest
            inDmContext = false;
            allowedDmVideoPath = null;
            return;
          }

          // Eğer DM sohbetinden tıklandıysa tek-video kuralını uygula
          if (inDmContext) {
            const dmAllowed = state.isTikTokDmAllowed !== false;
            if (!dmAllowed) {
              recordAndRedirect("https://www.tiktok.com/messages", "TikTok DM Video (Kapalı) -> Mesajlar");
              return;
            }
            if (!allowedDmVideoPath) {
              allowedDmVideoPath = path;
              return;
            }
            if (allowedDmVideoPath !== path) {
              allowedDmVideoPath = null;
              recordAndRedirect("https://www.tiktok.com/messages", "TikTok DM 2. Video Engellendi -> Mesajlar");
              return;
            }
            return;
          }
          return;
        }

        // 3. Ayarlar veya Arama sayfası -> Serbest
        if (path.startsWith("/setting") || path.startsWith("/search")) {
          return;
        }

        // 4. Ana Sayfa (Sizin İçin / Following / Explore / Live) -> Mesajlar (Gelen Kutusu) sekmesine yönlendir
        if (
          path === "/" ||
          path.startsWith("/foryou") ||
          path.startsWith("/following") ||
          path.startsWith("/explore") ||
          path.startsWith("/live")
        ) {
          inDmContext = false;
          allowedDmVideoPath = null;
          recordAndRedirect("https://www.tiktok.com/messages", "TikTok Sizin İçin Akışı -> Mesajlar");
        }
      }
    );
  }

  evaluateTikTokPage();
  window.addEventListener("popstate", evaluateTikTokPage, true);
  setInterval(evaluateTikTokPage, 400);

  chrome.storage.onChanged.addListener(() => {
    evaluateTikTokPage();
  });
})();

