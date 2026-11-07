/**
 * ScrollBlock PC — Arka Plan Servis Çalışanı (Service Worker)
 * Varsayılan ayarları yönetir, Mola (Cooldown) süresini takip eder ve engelleme sayacını kaydeder.
 */

const DEFAULT_STATE = {
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
  // Mola (Cooldown)
  isBreakActive: false,
  breakEndTimeMs: 0,
  breakDurationMinutes: 5,
  breakDailyLimit: 3,
  breakUsedToday: 0,
  breakLastDateKey: "",
  // PIN Kilidi
  isPinEnabled: false,
  pinHash: ""
};

function todayDateKey() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

chrome.runtime.onInstalled.addListener(() => {
  chrome.storage.local.get(null, (current) => {
    const merged = { ...DEFAULT_STATE, ...current };
    if (!merged.breakLastDateKey) {
      merged.breakLastDateKey = todayDateKey();
    }
    chrome.storage.local.set(merged);
  });
  chrome.alarms.create("scrollblock_break_check", { periodInMinutes: 0.25 });
});

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === "scrollblock_break_check") {
    checkBreakExpiration();
  }
});

function checkBreakExpiration() {
  chrome.storage.local.get(["isBreakActive", "breakEndTimeMs", "breakLastDateKey"], (data) => {
    const updates = {};
    const today = todayDateKey();
    if (data.breakLastDateKey !== today) {
      updates.breakLastDateKey = today;
      updates.breakUsedToday = 0;
    }
    if (data.isBreakActive && Date.now() >= (data.breakEndTimeMs || 0)) {
      updates.isBreakActive = false;
      updates.breakEndTimeMs = 0;
    }
    if (Object.keys(updates).length > 0) {
      chrome.storage.local.set(updates);
    }
  });
}

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (!msg || !msg.type) return false;

  if (msg.type === "RECORD_BLOCK") {
    chrome.storage.local.get(["totalBlockedCount"], (data) => {
      const nextCount = (data.totalBlockedCount || 0) + 1;
      const payload = {
        totalBlockedCount: nextCount,
        lastBlockedPlatform: msg.platform || "Kısa Video",
        lastBlockedTimestamp: Date.now()
      };
      chrome.storage.local.set(payload, () => {
        sendResponse({ ok: true, ...payload });
      });
    });
    return true;
  }

  if (msg.type === "GET_STATE") {
    checkBreakExpiration();
    chrome.storage.local.get(null, (data) => {
      sendResponse({ ok: true, state: { ...DEFAULT_STATE, ...data } });
    });
    return true;
  }

  if (msg.type === "UPDATE_STATE") {
    chrome.storage.local.set(msg.updates || {}, () => {
      chrome.storage.local.get(null, (data) => {
        sendResponse({ ok: true, state: { ...DEFAULT_STATE, ...data } });
      });
    });
    return true;
  }

  return false;
});

