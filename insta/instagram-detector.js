/**
 * ScrollBlock PC — Instagram Reels, Keşfet & Yorum Engelleyici (Tamamen İzole Modül: scrollblock/insta)
 *  - Bu dosyadaki hiçbir değişiklik TikTok (scrollblock/tiktok) veya YouTube (scrollblock/yt) modüllerini etkilemez.
 *  - "Yorumları Engelle (Yorum Kalkanı)" AÇIK ise: Gönderi yorumlarını ve yorum yazma kutusunu gizler.
 *  - Alt/sol menüdeki Reels sekmesini (/reels/*) -> Ana Sayfa'ya (instagram.com) yönlendirir.
 *  - Keşfet (/explore/*) içinden açılan videoları -> İlk video dahil anında Keşfet ızgarasına döndürür.
 *  - DM sohbetinden (/direct/t/*) tıklanan Reels videoları:
 *      * "DM'den gelen ilk Reels'a izin ver" AÇIK ise: İlk video izlenir; 2. videoya kaydırılınca sohbete döndürür.
 *      * KAPALI ise: Açıldığı an sohbete döndürür.
 *  - Ana Sayfa, Hikayeler (/stories/*) ve Kullanıcı Profilleri (%100) serbesttir.
 */
(function () {
  "use strict";

  const PLATFORM_NAME = "Instagram Reels";
  let lastBlockTime = 0;
  let inDmContext = false;
  let allowedDmReelUrl = null;
  let lastKnownSafeUrl = "https://www.instagram.com/";
  let commentStyleEl = null;

  function isBreakRunning(state) {
    return Boolean(state && state.isBreakActive && Date.now() < (state.breakEndTimeMs || 0));
  }

  function applyInstagramCommentShield(blocked) {
    if (blocked) {
      if (!commentStyleEl) {
        commentStyleEl = document.createElement("style");
        commentStyleEl.id = "scrollblock-instagram-comment-shield";
        commentStyleEl.textContent = `
          ul._a9z6,
          div._ae5q,
          section._aamu form,
          form textarea[aria-label*="yorum" i],
          form textarea[aria-label*="comment" i],
          div[role="dialog"] ul {
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
    if (now - lastBlockTime > 800) {
      lastBlockTime = now;
      chrome.runtime.sendMessage({
        type: "RECORD_BLOCK",
        platform: PLATFORM_NAME,
        reason
      });
    }
    window.location.replace(targetUrl);
  }

  function evaluateInstagramPage() {
    chrome.storage.local.get(
      [
        "isInstagramEnabled",
        "isInstagramDmAllowed",
        "isInstagramCommentsBlocked",
        "isBreakActive",
        "breakEndTimeMs"
      ],
      (state) => {
        if (isBreakRunning(state)) {
          applyInstagramCommentShield(false);
          return;
        }

        applyInstagramCommentShield(Boolean(state && state.isInstagramCommentsBlocked));

        if (state && state.isInstagramEnabled === false) return;

        const href = window.location.href;
        const path = window.location.pathname || "/";

        // 1. DM sohbet ekranındayken (/direct/...) DM bağlamını işaretle ve güvenli URL olarak kaydet
        if (path.startsWith("/direct")) {
          inDmContext = true;
          allowedDmReelUrl = null;
          lastKnownSafeUrl = href;
          return;
        }

        // 2. Keşfet 3'lü ızgara ana ekranı (/explore/ veya /explore/search/) -> Güvenli, ancak DM bağlamını kapat
        if (path === "/explore" || path === "/explore/" || path.startsWith("/explore/search")) {
          inDmContext = false;
          allowedDmReelUrl = null;
          lastKnownSafeUrl = "https://www.instagram.com/explore/";
          return;
        }

        // 3. Keşfet içinden bir gönderiye/Reels'a tıklandığında açılan modal (/p/... veya /reel/... Keşfet üstündeyken)
        const isReelPath =
          path === "/reels" ||
          path === "/reels/" ||
          path.startsWith("/reels/") ||
          path.startsWith("/reel/");

        const isExploreOpenedMedia =
          !inDmContext &&
          lastKnownSafeUrl.includes("/explore") &&
          (path.startsWith("/p/") || isReelPath);

        if (isExploreOpenedMedia) {
          recordAndRedirect("https://www.instagram.com/explore/", "Instagram Keşfet Video -> Izgaraya Dön");
          return;
        }

        // 4. Doğrudan Reels sekmesi (/reels/ veya /reel/...)
        if (isReelPath) {
          if (inDmContext) {
            const dmAllowed = !state || state.isInstagramDmAllowed !== false;
            if (!dmAllowed) {
              recordAndRedirect(lastKnownSafeUrl || "https://www.instagram.com/direct/inbox/", "Instagram DM Reel (Kapalı) -> Sohbete Dön");
              return;
            }
            if (!allowedDmReelUrl) {
              allowedDmReelUrl = path;
              return;
            }
            if (allowedDmReelUrl !== path) {
              allowedDmReelUrl = null;
              recordAndRedirect(lastKnownSafeUrl || "https://www.instagram.com/direct/inbox/", "Instagram DM 2. Reel Engellendi -> Sohbete Dön");
              return;
            }
            return;
          }

          recordAndRedirect("https://www.instagram.com/", "Instagram Reels Sekmesi -> Ana Sayfa");
          return;
        }

        // 5. Ana Sayfa, Hikayeler (/stories/...) veya Kullanıcı Profilleri -> %100 Serbest
        inDmContext = false;
        allowedDmReelUrl = null;
        if (path === "/") {
          lastKnownSafeUrl = "https://www.instagram.com/";
        }
      }
    );
  }

  evaluateInstagramPage();
  window.addEventListener("popstate", evaluateInstagramPage, true);
  setInterval(evaluateInstagramPage, 350);

  chrome.storage.onChanged.addListener(() => {
    evaluateInstagramPage();
  });
})();

