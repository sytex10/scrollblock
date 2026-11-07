/**
 * ScrollBlock PC — YouTube Shorts & Yorum Engelleyici (Tamamen İzole Modül: scrollblock/yt)
 *  - Bu dosyadaki hiçbir değişiklik Instagram (scrollblock/insta) veya TikTok (scrollblock/tiktok) modüllerini etkilemez.
 *  - "Yorumları Engelle (Yorum Kalkanı)" AÇIK ise: YouTube videolarının altındaki yorum bölümünü (#comments) gizler.
 *  - youtube.com/shorts/* adreslerini anında yakalar ve YouTube Ana Sayfasına yönlendirir.
 *  - Ana sayfa ve yan menüdeki Shorts raflarını/sekmesini gizler; normal uzun videolara (%100) dokunmaz.
 */
(function () {
  "use strict";

  const PLATFORM_NAME = "YouTube Shorts";
  let lastBlockTime = 0;
  let styleElement = null;
  let commentStyleElement = null;

  function isBreakRunning(state) {
    return Boolean(state && state.isBreakActive && Date.now() < (state.breakEndTimeMs || 0));
  }

  function applyShortsShelfHiding(enabled) {
    if (enabled) {
      if (!styleElement) {
        styleElement = document.createElement("style");
        styleElement.id = "scrollblock-youtube-shield";
        styleElement.textContent = `
          ytd-rich-shelf-renderer[is-shorts],
          ytd-reel-shelf-renderer,
          ytd-mini-guide-entry-renderer[aria-label="Shorts"],
          ytd-guide-entry-renderer a[title="Shorts"] {
            display: none !important;
          }
        `;
        (document.head || document.documentElement).appendChild(styleElement);
      }
    } else if (styleElement) {
      styleElement.remove();
      styleElement = null;
    }
  }

  function applyYouTubeCommentShield(blocked) {
    if (blocked) {
      if (!commentStyleElement) {
        commentStyleElement = document.createElement("style");
        commentStyleElement.id = "scrollblock-youtube-comment-shield";
        commentStyleElement.textContent = `
          #comments,
          ytd-comments,
          ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-comments-section"] {
            display: none !important;
          }
        `;
        (document.head || document.documentElement).appendChild(commentStyleElement);
      }
    } else if (commentStyleElement) {
      commentStyleElement.remove();
      commentStyleElement = null;
    }
  }

  function checkAndBlockYoutubeShorts() {
    chrome.storage.local.get(
      ["isYouTubeEnabled", "isYouTubeCommentsBlocked", "isBreakActive", "breakEndTimeMs"],
      (state) => {
        if (isBreakRunning(state)) {
          applyShortsShelfHiding(false);
          applyYouTubeCommentShield(false);
          return;
        }

        applyYouTubeCommentShield(Boolean(state && state.isYouTubeCommentsBlocked));

        const active = !state || state.isYouTubeEnabled !== false;
        applyShortsShelfHiding(active);
        if (!active) return;

        const path = window.location.pathname || "";
        if (path.startsWith("/shorts")) {
          const now = Date.now();
          if (now - lastBlockTime > 900) {
            lastBlockTime = now;
            chrome.runtime.sendMessage({
              type: "RECORD_BLOCK",
              platform: PLATFORM_NAME,
              reason: "YouTube Shorts URL -> Ana Sayfa"
            });
          }
          window.location.replace("https://www.youtube.com/");
        }
      }
    );
  }

  checkAndBlockYoutubeShorts();
  window.addEventListener("yt-navigate-start", checkAndBlockYoutubeShorts, true);
  window.addEventListener("yt-navigate-finish", checkAndBlockYoutubeShorts, true);
  window.addEventListener("popstate", checkAndBlockYoutubeShorts, true);
  setInterval(checkAndBlockYoutubeShorts, 450);

  chrome.storage.onChanged.addListener(() => {
    checkAndBlockYoutubeShorts();
  });
})();
