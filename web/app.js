/**
 * ScrollBlock Web Kontrol Paneli (vsolken.com)
 *  - Tarayıcıda ScrollBlock PC Eklentisi yüklüyse (vsolken-bridge.js üzerinden) eklentiyle
 *    gerçek zamanlı senkronize çalışır.
 *  - Eklenti henüz kurulmadıysa ayarları localStorage üzerinde tutar ve eklenti kurulduğu an aktarır.
 *  - GitHub üzerinden canlı Android sürüm bilgisini (version.json) otomatik çeker.
 */
(function () {
  "use strict";

  const STORAGE_KEY = "scrollblock_web_state_v36";

  const defaultState = {
    isInstagramEnabled: true,
    isInstagramDmAllowed: true,
    isInstagramCommentsBlocked: false,
    isTikTokEnabled: true,
    isTikTokDmAllowed: true,
    isTikTokCommentsBlocked: false,
    isYouTubeEnabled: true,
    isYouTubeCommentsBlocked: false,
    totalBlockedCount: 0,
    lastBlockedPlatform: "",
    lastBlockedTimestamp: 0,
    isBreakActive: false,
    breakEndTimeMs: 0,
    breakDurationMinutes: 5,
    breakDailyLimit: 3,
    breakUsedToday: 0,
    isPinEnabled: false,
    pinCode: ""
  };

  let state = loadLocalState();
  let isExtensionConnected = false;
  let pendingProtectedAction = null;
  let isSettingNewPin = false;

  // DOM Referansları
  const extensionStatusPill = document.getElementById("extensionStatusPill");
  const extensionStatusText = document.getElementById("extensionStatusText");
  const toggleGuideBtn = document.getElementById("toggleGuideBtn");
  const pcInstallGuide = document.getElementById("pcInstallGuide");

  const totalBlockedCountEl = document.getElementById("totalBlockedCount");
  const lastBlockedTextEl = document.getElementById("lastBlockedText");
  const resetStatsBtn = document.getElementById("resetStatsBtn");

  const breakNormalPanel = document.getElementById("breakNormalPanel");
  const breakActivePanel = document.getElementById("breakActivePanel");
  const dailyLimitBadge = document.getElementById("dailyLimitBadge");
  const durationChipRow = document.getElementById("durationChipRow");
  const limitChipRow = document.getElementById("limitChipRow");
  const startBreakWebBtn = document.getElementById("startBreakWebBtn");
  const endBreakWebBtn = document.getElementById("endBreakWebBtn");
  const webBreakCountdown = document.getElementById("webBreakCountdown");

  const webToggleInstagram = document.getElementById("webToggleInstagram");
  const webToggleInstagramDm = document.getElementById("webToggleInstagramDm");
  const webToggleInstagramComments = document.getElementById("webToggleInstagramComments");
  const webIgDmRow = document.getElementById("webIgDmRow");

  const webToggleTikTok = document.getElementById("webToggleTikTok");
  const webToggleTikTokDm = document.getElementById("webToggleTikTokDm");
  const webToggleTikTokComments = document.getElementById("webToggleTikTokComments");
  const webTtDmRow = document.getElementById("webTtDmRow");

  const webToggleYouTube = document.getElementById("webToggleYouTube");
  const webToggleYouTubeComments = document.getElementById("webToggleYouTubeComments");
  const webTogglePin = document.getElementById("webTogglePin");
  const pinStatusDesc = document.getElementById("pinStatusDesc");

  const pinModalBackdrop = document.getElementById("pinModalBackdrop");
  const pinModalTitle = document.getElementById("pinModalTitle");
  const pinModalSub = document.getElementById("pinModalSub");
  const pinInput = document.getElementById("pinInput");
  const pinErrorText = document.getElementById("pinErrorText");
  const pinCancelBtn = document.getElementById("pinCancelBtn");
  const pinConfirmBtn = document.getElementById("pinConfirmBtn");

  function loadLocalState() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? { ...defaultState, ...JSON.parse(raw) } : { ...defaultState };
    } catch (_) {
      return { ...defaultState };
    }
  }

  function saveState(updates) {
    state = { ...state, ...updates };
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch (_) {}

    if (isExtensionConnected) {
      window.postMessage(
        {
          source: "SCROLLBLOCK_WEB_DASHBOARD",
          type: "PUSH_UPDATES",
          updates
        },
        "*"
      );
    }
    render();
  }

  function executeProtected(title, subtitle, action) {
    if (!state.isPinEnabled || !state.pinCode) {
      action();
      return;
    }
    isSettingNewPin = false;
    pendingProtectedAction = action;
    pinModalTitle.textContent = title;
    pinModalSub.textContent = subtitle;
    pinInput.value = "";
    pinErrorText.style.display = "none";
    pinModalBackdrop.style.display = "flex";
    pinInput.focus();
  }

  function openNewPinModal() {
    isSettingNewPin = true;
    pendingProtectedAction = null;
    pinModalTitle.textContent = "Yeni PIN Belirleyin";
    pinModalSub.textContent = "Ayarları ve molayı korumak için 4 haneli bir şifre girin.";
    pinInput.value = "";
    pinErrorText.style.display = "none";
    pinModalBackdrop.style.display = "flex";
    pinInput.focus();
  }

  function render() {
    const isBreak = state.isBreakActive && Date.now() < (state.breakEndTimeMs || 0);

    if (isExtensionConnected) {
      extensionStatusPill.className = isBreak ? "status-pill warning" : "status-pill active";
      extensionStatusText.textContent = isBreak
        ? "PC Eklentisi Bağlı · Mola Modu"
        : "PC Eklentisi Bağlı · Koruma Aktif";
    } else {
      extensionStatusPill.className = "status-pill warning";
      extensionStatusText.textContent = "Web Paneli Hazır · PC Eklentisi Bekleniyor";
    }

    totalBlockedCountEl.textContent = state.totalBlockedCount || 0;
    if (state.totalBlockedCount > 0 && state.lastBlockedPlatform) {
      lastBlockedTextEl.textContent = `Son engelleme: ${state.lastBlockedPlatform}`;
    } else {
      lastBlockedTextEl.textContent = "Henüz hiçbir kısa video akışı engellenmedi";
    }

    const limit = state.breakDailyLimit ?? 3;
    const used = state.breakUsedToday || 0;
    dailyLimitBadge.textContent = limit === 0 ? "Sınırsız" : `${used}/${limit} kullanıldı`;

    durationChipRow.querySelectorAll(".select-chip").forEach((btn) => {
      const m = Number(btn.getAttribute("data-min"));
      btn.classList.toggle("selected", m === (state.breakDurationMinutes || 5));
    });

    limitChipRow.querySelectorAll(".select-chip").forEach((btn) => {
      const l = Number(btn.getAttribute("data-limit"));
      btn.classList.toggle("selected", l === limit);
    });

    const canStartBreak = limit === 0 || used < limit;
    startBreakWebBtn.disabled = !canStartBreak;
    startBreakWebBtn.textContent = canStartBreak
      ? `${state.breakDurationMinutes || 5} Dakika Mola Başlat`
      : "Bugünkü Mola Hakkınız Doldu";

    if (isBreak) {
      breakNormalPanel.style.display = "none";
      breakActivePanel.style.display = "flex";
      breakActivePanel.style.flexDirection = "column";
      breakActivePanel.style.gap = "10px";
      const remSec = Math.max(0, Math.floor((state.breakEndTimeMs - Date.now()) / 1000));
      const mm = String(Math.floor(remSec / 60)).padStart(2, "0");
      const ss = String(remSec % 60).padStart(2, "0");
      webBreakCountdown.textContent = `${mm}:${ss}`;
    } else {
      breakNormalPanel.style.display = "flex";
      breakNormalPanel.style.flexDirection = "column";
      breakNormalPanel.style.gap = "12px";
      breakActivePanel.style.display = "none";
    }

    webToggleInstagram.checked = state.isInstagramEnabled !== false;
    webToggleInstagramDm.checked = state.isInstagramDmAllowed !== false;
    webToggleInstagramComments.checked = Boolean(state.isInstagramCommentsBlocked);
    webIgDmRow.style.display = webToggleInstagram.checked ? "flex" : "none";

    webToggleTikTok.checked = state.isTikTokEnabled !== false;
    webToggleTikTokDm.checked = state.isTikTokDmAllowed !== false;
    webToggleTikTokComments.checked = Boolean(state.isTikTokCommentsBlocked);
    webTtDmRow.style.display = webToggleTikTok.checked ? "flex" : "none";

    webToggleYouTube.checked = state.isYouTubeEnabled !== false;
    webToggleYouTubeComments.checked = Boolean(state.isYouTubeCommentsBlocked);

    webTogglePin.checked = Boolean(state.isPinEnabled && state.pinCode);
    pinStatusDesc.textContent = webTogglePin.checked
      ? "Koruma anahtarları ve mola 4 haneli PIN ile korunuyor."
      : "Ayarları ve molayı 4 haneli şifreyle koruma altına alın.";
  }

  // Olay Dinleyicileri
  toggleGuideBtn.addEventListener("click", () => {
    const visible = pcInstallGuide.style.display !== "none";
    pcInstallGuide.style.display = visible ? "none" : "block";
  });

  resetStatsBtn.addEventListener("click", () => {
    executeProtected("Sayacı Sıfırla", "İstatistikleri sıfırlamak için PIN girin.", () => {
      saveState({ totalBlockedCount: 0, lastBlockedPlatform: "", lastBlockedTimestamp: 0 });
    });
  });

  durationChipRow.querySelectorAll(".select-chip").forEach((btn) => {
    btn.addEventListener("click", () => {
      const mins = Number(btn.getAttribute("data-min"));
      executeProtected("Mola Süresi", "Mola süresini değiştirmek için PIN girin.", () => {
        saveState({ breakDurationMinutes: mins });
      });
    });
  });

  limitChipRow.querySelectorAll(".select-chip").forEach((btn) => {
    btn.addEventListener("click", () => {
      const lim = Number(btn.getAttribute("data-limit"));
      executeProtected("Günlük Limit", "Günlük mola limitini değiştirmek için PIN girin.", () => {
        saveState({ breakDailyLimit: lim });
      });
    });
  });

  startBreakWebBtn.addEventListener("click", () => {
    const limit = state.breakDailyLimit ?? 3;
    const used = state.breakUsedToday || 0;
    if (limit !== 0 && used >= limit) return;

    executeProtected("Mola Başlat", "Geçici molayı başlatmak için PIN girin.", () => {
      const mins = state.breakDurationMinutes || 5;
      saveState({
        isBreakActive: true,
        breakEndTimeMs: Date.now() + mins * 60 * 1000,
        breakUsedToday: used + 1
      });
    });
  });

  endBreakWebBtn.addEventListener("click", () => {
    saveState({ isBreakActive: false, breakEndTimeMs: 0 });
  });

  webToggleInstagram.addEventListener("change", () => {
    const target = webToggleInstagram.checked;
    if (!target) {
      webToggleInstagram.checked = true;
      executeProtected("Instagram Koruması", "Instagram engelini kapatmak için PIN girin.", () => {
        saveState({ isInstagramEnabled: false });
      });
    } else {
      saveState({ isInstagramEnabled: true });
    }
  });

  webToggleInstagramDm.addEventListener("change", () => {
    const target = webToggleInstagramDm.checked;
    if (target) {
      webToggleInstagramDm.checked = false;
      executeProtected("DM Video İzni", "Instagram DM tek-video iznini açmak için PIN girin.", () => {
        saveState({ isInstagramDmAllowed: true });
      });
    } else {
      saveState({ isInstagramDmAllowed: false });
    }
  });

  webToggleTikTok.addEventListener("change", () => {
    const target = webToggleTikTok.checked;
    if (!target) {
      webToggleTikTok.checked = true;
      executeProtected("TikTok Koruması", "TikTok engelini kapatmak için PIN girin.", () => {
        saveState({ isTikTokEnabled: false });
      });
    } else {
      saveState({ isTikTokEnabled: true });
    }
  });

  webToggleTikTokDm.addEventListener("change", () => {
    const target = webToggleTikTokDm.checked;
    if (target) {
      webToggleTikTokDm.checked = false;
      executeProtected("TikTok DM İzni", "TikTok DM tek-video iznini açmak için PIN girin.", () => {
        saveState({ isTikTokDmAllowed: true });
      });
    } else {
      saveState({ isTikTokDmAllowed: false });
    }
  });

  webToggleYouTube.addEventListener("change", () => {
    const target = webToggleYouTube.checked;
    if (!target) {
      webToggleYouTube.checked = true;
      executeProtected("YouTube Shorts Koruması", "YouTube Shorts engelini kapatmak için PIN girin.", () => {
        saveState({ isYouTubeEnabled: false });
      });
    } else {
      saveState({ isYouTubeEnabled: true });
    }
  });

  webToggleInstagramComments.addEventListener("change", () => {
    const target = webToggleInstagramComments.checked;
    if (!target) {
      webToggleInstagramComments.checked = true;
      executeProtected("Instagram Yorum Kalkanı", "Yorum kalkanını kapatmak için PIN girin.", () => {
        saveState({ isInstagramCommentsBlocked: false });
      });
    } else {
      saveState({ isInstagramCommentsBlocked: true });
    }
  });

  webToggleTikTokComments.addEventListener("change", () => {
    const target = webToggleTikTokComments.checked;
    if (!target) {
      webToggleTikTokComments.checked = true;
      executeProtected("TikTok Yorum Kalkanı", "Yorum kalkanını kapatmak için PIN girin.", () => {
        saveState({ isTikTokCommentsBlocked: false });
      });
    } else {
      saveState({ isTikTokCommentsBlocked: true });
    }
  });

  webToggleYouTubeComments.addEventListener("change", () => {
    const target = webToggleYouTubeComments.checked;
    if (!target) {
      webToggleYouTubeComments.checked = true;
      executeProtected("YouTube Yorum Kalkanı", "Yorum kalkanını kapatmak için PIN girin.", () => {
        saveState({ isYouTubeCommentsBlocked: false });
      });
    } else {
      saveState({ isYouTubeCommentsBlocked: true });
    }
  });

  webTogglePin.addEventListener("change", () => {
    if (webTogglePin.checked) {
      webTogglePin.checked = false;
      openNewPinModal();
    } else {
      webTogglePin.checked = true;
      executeProtected("PIN Kilidini Kaldır", "Kaldırmak için mevcut PIN kodunuzu girin.", () => {
        saveState({ isPinEnabled: false, pinCode: "" });
      });
    }
  });

  pinCancelBtn.addEventListener("click", () => {
    pinModalBackdrop.style.display = "none";
  });

  pinConfirmBtn.addEventListener("click", () => {
    const val = (pinInput.value || "").trim();
    if (!/^\d{4}$/.test(val)) {
      pinErrorText.textContent = "PIN 4 rakamdan oluşmalıdır.";
      pinErrorText.style.display = "block";
      return;
    }

    if (isSettingNewPin) {
      pinModalBackdrop.style.display = "none";
      saveState({ isPinEnabled: true, pinCode: val });
      return;
    }

    if (val === state.pinCode) {
      pinModalBackdrop.style.display = "none";
      if (pendingProtectedAction) {
        const fn = pendingProtectedAction;
        pendingProtectedAction = null;
        fn();
      }
    } else {
      pinErrorText.textContent = "Hatalı PIN kodu.";
      pinErrorText.style.display = "block";
      pinInput.value = "";
    }
  });

  // Tarayıcı eklentisi (vsolken-bridge.js) ile canlı mesajlaşma
  window.addEventListener("message", (event) => {
    if (event.source !== window || !event.data || event.data.source !== "SCROLLBLOCK_EXTENSION") {
      return;
    }
    if (event.data.type === "STATE_SYNC" && event.data.state) {
      isExtensionConnected = true;
      state = { ...state, ...event.data.state };
      const autoModal = document.getElementById("autoInstallModalBackdrop");
      if (autoModal) autoModal.style.display = "none";
      render();
    }
  });

  // Açık Rıza ve Gizlilik Politikası Modalı
  const webPrivacyModalBackdrop = document.getElementById("webPrivacyModalBackdrop");
  const openWebPrivacyModalRow = document.getElementById("openWebPrivacyModalRow");
  const openWebPrivacyModalBtn = document.getElementById("openWebPrivacyModalBtn");
  const closeWebPrivacyModalBtn = document.getElementById("closeWebPrivacyModalBtn");

  if (openWebPrivacyModalRow && webPrivacyModalBackdrop) {
    openWebPrivacyModalRow.addEventListener("click", () => {
      webPrivacyModalBackdrop.style.display = "flex";
    });
  }
  if (openWebPrivacyModalBtn && webPrivacyModalBackdrop) {
    openWebPrivacyModalBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      webPrivacyModalBackdrop.style.display = "flex";
    });
  }
  if (closeWebPrivacyModalBtn && webPrivacyModalBackdrop) {
    closeWebPrivacyModalBtn.addEventListener("click", () => {
      webPrivacyModalBackdrop.style.display = "none";
    });
  }

  // GitHub üzerinden güncel sürüm bilgisini çek
  fetch("https://raw.githubusercontent.com/sytex10/scrollblock/main/version.json?t=" + Date.now())
    .then((r) => r.json())
    .then((info) => {
      if (info && info.versionName) {
        document.getElementById("versionBadge").textContent = "v" + info.versionName;
        document.getElementById("androidVersionText").textContent = "v" + info.versionName + " · Güncel";
        const pcVer = document.getElementById("pcVersionText");
        if (pcVer) pcVer.textContent = "v" + info.versionName + " · Güncel";
      }
    })
    .catch(() => {});

  window.postMessage({ source: "SCROLLBLOCK_WEB_DASHBOARD", type: "PING_EXTENSION" }, "*");
  render();
  setInterval(render, 1000);
})();

