# ComposePWA: Build Progressive Web Apps with Compose Multiplatform

ComposePWA is a Gradle plugin that makes it easy to create Progressive Web Apps (PWAs) from your
Compose Multiplatform projects. This article will show you how to use it.

## What is a Progressive Web App (PWA)?

A Progressive Web App is a web application that works like a mobile app. Users can:

- Install it on their phone or computer
- Use it offline
- Get notifications
- See it in their app drawer

## What is ComposePWA?

ComposePWA is a tool that turns your Compose Multiplatform web app into a PWA automatically. It does
all the hard work for you.

## Installation

### Step 1: Add the plugin to your project

First, open your `gradle/libs.versions.toml` file and add these lines:

```toml
[versions]
# ... your existing versions

composePwa = "x.x.x"  # Replace with the latest version

[plugins]
# ... your existing plugins

composePwa = { id = "dev.yuyuyuyuyu.composepwa", version.ref = "composePwa" }
```

### Step 2: Apply the plugin

Open your `composeApp/build.gradle.kts` file and add the plugin:

```kotlin
plugins {
    // ... your existing plugins

    alias(libs.plugins.composePwa)
}
```

That's it! Installation is complete.

## How to Use ComposePWA

### Building Your PWA

After installing the plugin, simply run `wasmJsBrowserDistribution` as usual.

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

Your PWA will be ready in the `composeApp/build/dist/wasmJs/productionExecutable` folder.

### What Files Are Created?

When you run the build command, ComposePWA automatically creates these files:

1. **`workbox-config.js`** - Configuration for offline support
2. **`src/wasmJsMain/resources/manifest.json`** - App information for browsers
3. **`src/wasmJsMain/resources/registerServiceWorker.js`** - Service worker registration
4. **`src/wasmJsMain/resources/icons/*`** - App icons for different sizes
5. **Updated `src/wasmJsMain/resources/index.html`** - HTML with PWA tags

## Customizing Your PWA

### App Information

You can change your PWA's name, colors, and other settings by editing the `manifest.json` file:

```json
{
  "name": "My Amazing App",
  "short_name": "Amazing App",
  "theme_color": "#FFFBFE",
  "background_color": "#FFFBFE",
  "display": "standalone",
  "scope": "./",
  "start_url": "./"
}
```

### App Icons

ComposePWA includes default icons, but you can replace them with your own:

1. Create icons in these sizes: 72x72, 96x96, 128x128, 144x144, 152x152, 192x192, 384x384, 512x512
2. Put them in the `src/wasmJsMain/resources/icons/` folder
3. Name them like: `icon-72x72.png`, `icon-96x96.png`, etc.

**Tip**: You can use the [ngx-pwa-icons](https://github.com/pverhaert/ngx-pwa-icons) tool to
generate all icon sizes from one image:

```bash
npx ngx-pwa-icons
```

### Offline Support

You can customize offline behavior by editing the `workbox-config.js` file:

```javascript
module.exports = {
    globDirectory: "build/dist/wasmJs/productionExecutable/",
    globPatterns: [],
    maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,
    runtimeCaching: [{
        urlPattern: /.+/,
        handler: "StaleWhileRevalidate",
    }],
    swDest: "build/dist/wasmJs/productionExecutable/serviceWorker.js",
};
```

## Example Project Structure

After using ComposePWA, your project will look like this:

```
composeApp/
├── src/
│   └── wasmJsMain/
│       ├── kotlin/
│       │   └── your app code
│       └── resources/
│           ├── index.html (updated with PWA tags)
│           ├── manifest.json (PWA configuration)
│           ├── registerServiceWorker.js (service worker)
│           └── icons/ (app icons)
├── build.gradle.kts
└── ...
workbox-config.js (offline support config)
```

## Benefits of Using ComposePWA

1. **Easy to use** - Just add the plugin and run one command
2. **Automatic** - Creates all PWA files for you
3. **Customizable** - You can change everything to fit your needs

## Conclusion

ComposePWA makes it very easy to turn your Compose Multiplatform web app into a Progressive Web App.
With just a few simple steps, your users can install your app on their devices and use it like a
native mobile app.

The plugin handles all the complex PWA setup automatically, so you can focus on building your app
instead of configuring PWA features.

Try ComposePWA today and give your users a better app experience!

## Resources

- [ComposePWA GitHub Repository](https://github.com/yuyuyuyuyu-dev/ComposePWA)
- [Workbox Documentation](https://developers.google.com/web/tools/workbox)

## License

ComposePWA is available under the Apache License 2.0.
