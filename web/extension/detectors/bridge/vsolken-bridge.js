/**
 * ScrollBlock PC — vsolken.com Web Paneli Köprüsü (detectors/bridge)
 * vsolken.com (veya yerel test) web arayüzü açıldığında, web sayfası ile tarayıcı eklentisini
 * gerçek zamanlı senkronize eder.
 */
(function () {
  "use strict";

  function broadcastStateToPage() {
    chrome.runtime.sendMessage({ type: "GET_STATE" }, (res) => {
      if (res && res.ok) {
        window.postMessage(
          {
            source: "SCROLLBLOCK_EXTENSION",
            type: "STATE_SYNC",
            state: res.state
          },
          "*"
        );
      }
    });
  }

  window.addEventListener("message", (event) => {
    if (event.source !== window || !event.data || event.data.source !== "SCROLLBLOCK_WEB_DASHBOARD") {
      return;
    }

    const msg = event.data;
    if (msg.type === "PING_EXTENSION") {
      broadcastStateToPage();
    } else if (msg.type === "PUSH_UPDATES" && msg.updates) {
      chrome.runtime.sendMessage({ type: "UPDATE_STATE", updates: msg.updates }, (res) => {
        if (res && res.ok) {
          window.postMessage(
            {
              source: "SCROLLBLOCK_EXTENSION",
              type: "STATE_SYNC",
              state: res.state
            },
            "*"
          );
        }
      });
    }
  });

  chrome.storage.onChanged.addListener(() => {
    broadcastStateToPage();
  });

  window.addEventListener("DOMContentLoaded", () => {
    document.documentElement.setAttribute("data-scrollblock-extension", "installed");
    broadcastStateToPage();
  });

  setTimeout(broadcastStateToPage, 250);
})();

