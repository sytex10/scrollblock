/**
 * ScrollBlock PC — TikTok & Yorum Engelleyici (Tamamen İzole Modül: scrollblock/tiktok)
 *  - Bu dosyadaki hiçbir değişiklik Instagram (scrollblock/insta) veya YouTube (scrollblock/yt) modüllerini etkilemez.
 *  - "Yorumları Engelle (Yorum Kalkanı)" AÇIK ise: TikTok video yorumlarını ve yorum çekmecesini gizler.
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
  let commentStyleEl = null;

  function isBreakRunning(state) {
    return Boolean(state && state.isBreakActive && Date.now() < (state.breakEndTimeMs || 0));
  }

  function applyTikTokCommentShield(blocked) {
    if (blocked) {
      if (!commentStyleEl) {
        commentStyleEl = document.createElement("style");
        commentStyleEl.id = "scrollblock-tiktok-comment-shield";
        commentStyleEl.textContent = `
          [data-e2e="comment-list"],
          [data-e2e="comment-level-1"],
          [data-e2e="comment-input"],
          [class*="DivCommentListContainer"],
          [class*="DivCommentContainer"],
          [class*="DivCommentFooter"] {
            display: none !important;
          }
        `;
        (document.head || document.documentElement).appendChild(commentStyleEl);
      }
    } else if (commentStyleEl) {
      commentStyleEl.remove();
      commentStyleEl = null;
    }
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
      [
        "isTikTokEnabled",
        "isTikTokDmAllowed",
        "isTikTokCommentsBlocked",
        "isBreakActive",
        "breakEndTimeMs"
      ],
      (state) => {
        if (isBreakRunning(state)) {
          applyTikTokCommentShield(false);
          return;
        }

        applyTikTokCommentShield(Boolean(state && state.isTikTokCommentsBlocked));

        if (state && state.isTikTokEnabled === false) return;

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
            inDmContext = false;
            allowedDmVideoPath = null;
            return;
          }

          if (inDmContext) {
            const dmAllowed = !state || state.isTikTokDmAllowed !== false;
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

        // 4. Ana Sayfa (Sizin İçin / Following / Explore / Live) -> Mesajlar sekmesine yönlendir
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

