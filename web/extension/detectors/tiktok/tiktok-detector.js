/**
 * ShortBan PC — TikTok & Yorum Engelleyici (Tamamen İzole Modül: scrollblock/tiktok)
 *  - "Yorumları Engelle (Yorum Kalkanı)" AÇIK ise: Yorumlar açıldığında yorum alanı bembeyaz (#FFFFFF) boş kalır.
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
        commentStyleEl.id = "shortban-tiktok-comment-shield";
        commentStyleEl.textContent = `
          [data-e2e="comment-list"],
          [class*="DivCommentListContainer"],
          [class*="DivCommentContainer"] {
            background: #FFFFFF !important;
            background-color: #FFFFFF !important;
            min-height: 220px !important;
            pointer-events: none !important;
          }
          [data-e2e="comment-list"] *,
          [data-e2e="comment-level-1"],
          [data-e2e="comment-level-2"],
          [data-e2e="comment-input"],
          [class*="DivCommentListContainer"] *,
          [class*="DivCommentItemContainer"],
          [class*="DivCommentFooter"] {
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

        if (path.startsWith("/messages")) {
          inDmContext = true;
          allowedDmVideoPath = null;
          return;
        }

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

        if (path.startsWith("/setting") || path.startsWith("/search")) {
          return;
        }

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
