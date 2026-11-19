module.exports = {
  globDirectory: "build/dist/composeWebCompatibility/productionExecutable/",
  globPatterns: [
    "**/*.{html,css,js,json,png,jpg,jpeg,svg,gif,webp,ico,ttf,woff,woff2,wasm,cvr}",
  ],
  maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,
  skipWaiting: true,
  clientsClaim: true,
  cleanupOutdatedCaches: true,
  swDest:
    "build/dist/composeWebCompatibility/productionExecutable/serviceWorker.js",
};
