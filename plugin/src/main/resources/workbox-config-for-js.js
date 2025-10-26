module.exports = {
    globDirectory: "build/dist/js/productionExecutable/",
    globPatterns: [],
    maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,
    runtimeCaching: [{
        urlPattern: /.+/,
        handler: "StaleWhileRevalidate",
    }],
    swDest: "build/dist/js/productionExecutable/serviceWorker.js",
};
