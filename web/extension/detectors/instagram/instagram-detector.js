/**
 * ShortBan PC — Instagram Reels, Keşfet & Yorum Engelleyici (Tamamen İzole Modül: scrollblock/insta)
 *  - "Yorumları Engelle (Yorum Kalkanı)" AÇIK ise: Yorumlar açıldığında yalnızca sağdaki/alttaki yorum alanı
 *    bembeyaz (#FFFFFF) boş kalır; çoklu/kaydırmalı (carousel) gönderi fotoğraflarına (ul._acay) kesinlikle dokunmaz!
 */
(function () {
  "use strict";

  const PLATFORM_NAME = "Instagram Reels";
  let lastBlockTime = 0;
  let inDmContext = false;
  let allowedDmReelUrl = null;
  let lastKnownSafeUrl = "https://www.instagram.com/";
  let commentStyleEl = null;

  // Eski sürümden kalan stil etiketi varsa temizle
  const legacyStyle = document.getElementById("scrollblock-instagram-comment-shield");
  if (legacyStyle) legacyStyle.remove();

  function isBreakRunning(state) {
    return Boolean(state && state.isBreakActive && Date.now() < (state.breakEndTimeMs || 0));
  }

  function applyInstagramCommentShield(blocked) {
    const oldEl = document.getElementById("scrollblock-instagram-comment-shield");
    if (oldEl) oldEl.remove();

    if (blocked) {
      if (!commentStyleEl) {
        commentStyleEl = document.createElement("style");
        commentStyleEl.id = "shortban-instagram-comment-shield";
        commentStyleEl.textContent = `
          /* Yalnızca yorum listesi kapsayıcılarını bembeyaz yap (kaydırmalı foto/video ul._acay hariç) */
          ul._a9z6:not(._acay),
          ul._a9ym:not(._acay),
          div._ae5q {
            background: #FFFFFF !important;
            background-color: #FFFFFF !important;
            pointer-events: none !important;
          }
          ul._a9z6:not(._acay) *,
          ul._a9ym:not(._acay) *,
          div._ae5q ul._a9z6 *,
          section._aamu form,
          form textarea[aria-label*="yorum" i],
          form textarea[aria-label*="comment" i] {
            visibility: hidden !important;
            opacity: 0 !important;
            pointer-events: none !important;
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

        if (path.startsWith("/direct")) {
          inDmContext = true;
          allowedDmReelUrl = null;
          lastKnownSafeUrl = href;
          return;
        }

        if (path === "/explore" || path === "/explore/" || path.startsWith("/explore/search")) {
          inDmContext = false;
          allowedDmReelUrl = null;
          lastKnownSafeUrl = "https://www.instagram.com/explore/";
          return;
        }

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
