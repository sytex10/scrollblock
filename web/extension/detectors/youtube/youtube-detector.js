/**
 * ShortBan PC — YouTube Shorts & Yorum Engelleyici (Tamamen İzole Modül: scrollblock/yt)
 *  - YouTube Music (music.youtube.com) ve YouTube Studio (studio.youtube.com) kesinlikle hariç tutulur.
 *  - "Yorumları Engelle (Yorum Kalkanı)" AÇIK ise: Yorumlar bölümü açıldığında bembeyaz (#FFFFFF) boş bir alan olarak kalır.
 *  - youtube.com/shorts/* adreslerini anında yakalar ve YouTube Ana Sayfasına yönlendirir.
 */
(function () {
  "use strict";

  const host = (window.location.hostname || "").toLowerCase();
  if (
    host === "music.youtube.com" ||
    host === "studio.youtube.com" ||
    host === "kids.youtube.com" ||
    host === "tv.youtube.com"
  ) {
    return;
  }

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
        styleElement.id = "shortban-youtube-shield";
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
        commentStyleElement.id = "shortban-youtube-comment-shield";
        commentStyleElement.textContent = `
          #comments,
          ytd-comments,
          ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-comments-section"] #content {
            background: #FFFFFF !important;
            background-color: #FFFFFF !important;
            border-radius: 12px !important;
            min-height: 240px !important;
            pointer-events: none !important;
          }
          #comments *,
          ytd-comments *,
          ytd-comment-thread-renderer,
          ytd-comments-header-renderer,
          ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-comments-section"] #content * {
            visibility: hidden !important;
            opacity: 0 !important;
            pointer-events: none !important;
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
