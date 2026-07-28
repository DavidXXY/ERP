(function () {
  "use strict";

  var reloadKey = "ops-erp:stale-asset-reload";
  var reloadCooldownMs = 10000;
  var moduleErrorMessages = [
    "Failed to fetch dynamically imported module",
    "Importing a module script failed",
    "error loading dynamically imported module",
  ];

  function reloadOnce() {
    var now = Date.now();
    var lastReload = Number(sessionStorage.getItem(reloadKey) || 0);

    if (now - lastReload < reloadCooldownMs) {
      return;
    }

    sessionStorage.setItem(reloadKey, String(now));
    window.location.reload();
  }

  function isModuleLoadError(message) {
    return moduleErrorMessages.some(function (candidate) {
      return message.indexOf(candidate) !== -1;
    });
  }

  window.addEventListener("vite:preloadError", function (event) {
    event.preventDefault();
    reloadOnce();
  });

  window.addEventListener("unhandledrejection", function (event) {
    var reason = event.reason;
    var message = String(reason && reason.message ? reason.message : reason || "");

    if (isModuleLoadError(message)) {
      event.preventDefault();
      reloadOnce();
    }
  });
})();
