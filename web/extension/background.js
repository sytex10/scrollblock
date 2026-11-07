/**
 * ShortBan PC — Arka Plan Servis Çalışanı (Service Worker)
 * Varsayılan ayarları yönetir, Mola (Cooldown) süresini takip eder, engelleme sayacını kaydeder
 * ve diskteki dosyalar güncellendiğinde eklentiyi otomatik olarak yeniden yükler (chrome.runtime.reload).
 */

const CURRENT_EXTENSION_VERSION = chrome.runtime.getManifest().version || "4.4.0";
const VERSION_JSON_URL = "https://raw.githubusercontent.com/sytex10/scrollblock/main/version.json";
let initialDiskManifestVersion = CURRENT_EXTENSION_VERSION;

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
  pinHash: "",
  // Otomatik Güncelleme Durumu
  extensionVersion: CURRENT_EXTENSION_VERSION,
  latestRemoteVersion: CURRENT_EXTENSION_VERSION,
  lastUpdateCheckMs: 0
};

function todayDateKey() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function checkRemoteAndDiskUpdate() {
  // 1. Disk üzerindeki manifest.json güncellendiyse eklentiyi otomatik yeniden yükle
  fetch(chrome.runtime.getURL("manifest.json") + "?t=" + Date.now(), { cache: "no-store" })
    .then((r) => (r.ok ? r.json() : null))
    .then((diskManifest) => {
      if (diskManifest && diskManifest.version && diskManifest.version !== initialDiskManifestVersion) {
        chrome.runtime.reload();
      }
    })
    .catch(() => {});

  // 2. GitHub üzerindeki güncel sürüm bilgisini kontrol et
  fetch(`${VERSION_JSON_URL}?t=${Date.now()}`, { cache: "no-store" })
    .then((res) => (res.ok ? res.json() : null))
    .then((info) => {
      if (info && info.versionName) {
        chrome.storage.local.set({
          extensionVersion: CURRENT_EXTENSION_VERSION,
          latestRemoteVersion: String(info.versionName),
          lastUpdateCheckMs: Date.now()
        });
      }
    })
    .catch(() => {});
}

chrome.runtime.onInstalled.addListener(() => {
  chrome.storage.local.get(null, (current) => {
    const merged = { ...DEFAULT_STATE, ...current, extensionVersion: CURRENT_EXTENSION_VERSION };
    if (!merged.breakLastDateKey) {
      merged.breakLastDateKey = todayDateKey();
    }
    chrome.storage.local.set(merged);
  });
  chrome.alarms.create("scrollblock_break_check", { periodInMinutes: 0.25 });
  chrome.alarms.create("shortban_update_check", { periodInMinutes: 1 });
  checkRemoteAndDiskUpdate();
});

chrome.runtime.onStartup.addListener(() => {
  chrome.alarms.create("scrollblock_break_check", { periodInMinutes: 0.25 });
  chrome.alarms.create("shortban_update_check", { periodInMinutes: 1 });
  checkRemoteAndDiskUpdate();
});

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === "scrollblock_break_check") {
    checkBreakExpiration();
  } else if (alarm.name === "shortban_update_check") {
    checkRemoteAndDiskUpdate();
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

  if (msg.type === "CHECK_UPDATE_NOW") {
    checkRemoteAndDiskUpdate();
    sendResponse({ ok: true });
    return true;
  }

  return false;
});
