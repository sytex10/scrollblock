/**
 * ScrollBlock PC — YouTube Shorts Engelleyici (Tamamen İzole Modül: detectors/youtube)
 *  - Bu dosyadaki hiçbir değişiklik Instagram veya TikTok modüllerini etkilemez.
 *  - youtube.com/shorts/* adreslerini anında yakalar ve YouTube Ana Sayfasına yönlendirir.
 *  - Ana sayfa ve yan menüdeki Shorts raflarını/sekmesini gizler; normal uzun videolara (%100) dokunmaz.
 */
(function () {
  "use strict";

  const PLATFORM_NAME = "YouTube Shorts";
  let lastBlockTime = 0;
  let styleElement = null;

  function isProtectedAndActive(state) {
    if (!state) return true;
    if (state.isBreakActive && Date.now() < (state.breakEndTimeMs || 0)) {
      return false;
    }
    return state.isYouTubeEnabled !== false;
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

  function checkAndBlockYoutubeShorts() {
    chrome.storage.local.get(
      ["isYouTubeEnabled", "isBreakActive", "breakEndTimeMs"],
      (state) => {
        const active = isProtectedAndActive(state);
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

