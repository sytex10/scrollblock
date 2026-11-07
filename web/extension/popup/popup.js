(function () {
  const blockedCountEl = document.getElementById("blockedCount");
  const lastBlockedInfoEl = document.getElementById("lastBlockedInfo");
  const statusPillEl = document.getElementById("statusPill");
  const statusTextEl = document.getElementById("statusText");

  const toggleInstagram = document.getElementById("toggleInstagram");
  const toggleInstagramDm = document.getElementById("toggleInstagramDm");
  const igDmSubRow = document.getElementById("igDmSubRow");

  const toggleTikTok = document.getElementById("toggleTikTok");
  const toggleTikTokDm = document.getElementById("toggleTikTokDm");
  const ttDmSubRow = document.getElementById("ttDmSubRow");

  const toggleYouTube = document.getElementById("toggleYouTube");

  const breakInactiveView = document.getElementById("breakInactiveView");
  const breakActiveView = document.getElementById("breakActiveView");
  const breakTimerText = document.getElementById("breakTimerText");
  const startBreakBtn = document.getElementById("startBreakBtn");
  const endBreakBtn = document.getElementById("endBreakBtn");
  const openWebDashboard = document.getElementById("openWebDashboard");

  let currentState = null;

  function render(state) {
    currentState = state;
    blockedCountEl.textContent = state.totalBlockedCount || 0;
    if (state.lastBlockedPlatform) {
      lastBlockedInfoEl.textContent = `Son engelleme: ${state.lastBlockedPlatform}`;
    }

    toggleInstagram.checked = state.isInstagramEnabled !== false;
    toggleInstagramDm.checked = state.isInstagramDmAllowed !== false;
    igDmSubRow.style.display = toggleInstagram.checked ? "flex" : "none";

    toggleTikTok.checked = state.isTikTokEnabled !== false;
    toggleTikTokDm.checked = state.isTikTokDmAllowed !== false;
    ttDmSubRow.style.display = toggleTikTok.checked ? "flex" : "none";

    toggleYouTube.checked = state.isYouTubeEnabled !== false;

    const isBreak = state.isBreakActive && Date.now() < (state.breakEndTimeMs || 0);
    if (isBreak) {
      statusPillEl.className = "pill break";
      statusTextEl.textContent = "Mola";
      breakInactiveView.style.display = "none";
      breakActiveView.style.display = "block";
      const remSec = Math.max(0, Math.floor((state.breakEndTimeMs - Date.now()) / 1000));
      const mm = String(Math.floor(remSec / 60)).padStart(2, "0");
      const ss = String(remSec % 60).padStart(2, "0");
      breakTimerText.textContent = `${mm}:${ss}`;
    } else {
      statusPillEl.className = "pill active";
      statusTextEl.textContent = "Aktif";
      breakInactiveView.style.display = "block";
      breakActiveView.style.display = "none";
      startBreakBtn.textContent = `${state.breakDurationMinutes || 5} Dakika Mola Başlat`;
    }
  }

  function updateState(updates) {
    chrome.runtime.sendMessage({ type: "UPDATE_STATE", updates }, (res) => {
      if (res && res.ok) render(res.state);
    });
  }

  toggleInstagram.addEventListener("change", () => {
    updateState({ isInstagramEnabled: toggleInstagram.checked });
  });
  toggleInstagramDm.addEventListener("change", () => {
    updateState({ isInstagramDmAllowed: toggleInstagramDm.checked });
  });
  toggleTikTok.addEventListener("change", () => {
    updateState({ isTikTokEnabled: toggleTikTok.checked });
  });
  toggleTikTokDm.addEventListener("change", () => {
    updateState({ isTikTokDmAllowed: toggleTikTokDm.checked });
  });
  toggleYouTube.addEventListener("change", () => {
    updateState({ isYouTubeEnabled: toggleYouTube.checked });
  });

  startBreakBtn.addEventListener("click", () => {
    const mins = (currentState && currentState.breakDurationMinutes) || 5;
    updateState({
      isBreakActive: true,
      breakEndTimeMs: Date.now() + mins * 60 * 1000,
      breakUsedToday: ((currentState && currentState.breakUsedToday) || 0) + 1
    });
  });

  endBreakBtn.addEventListener("click", () => {
    updateState({ isBreakActive: false, breakEndTimeMs: 0 });
  });

  openWebDashboard.addEventListener("click", () => {
    chrome.tabs.create({ url: "https://vsolken.com" });
  });

  function refresh() {
    chrome.runtime.sendMessage({ type: "GET_STATE" }, (res) => {
      if (res && res.ok) render(res.state);
    });
  }

  refresh();
  setInterval(refresh, 1000);
})();

