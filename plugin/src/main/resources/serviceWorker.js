importScripts(
  "https://storage.googleapis.com/workbox-cdn/releases/7.4.1/workbox-sw.js",
);

self.skipWaiting();
workbox.core.clientsClaim();
workbox.precaching.cleanupOutdatedCaches();
workbox.precaching.precacheAndRoute(self.__WB_MANIFEST);
