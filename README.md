# ComposePWA

[![Publish](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/publish.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/publish.yml)
[![Tests](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/tests.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/tests.yml)
<a href="https://jetc.dev/issues/273.html"><img src="https://img.shields.io/badge/As_Seen_In-jetc.dev_Newsletter_Issue_%23273-blue?logo=Jetpack+Compose&amp;logoColor=white" alt="As Seen In - jetc.dev Newsletter Issue #273"></a>

This Gradle plugin provides a Gradle Task, that build your Compose Multiplatform Web App as a PWA.

## Prerequisites

- Node.js (The author uses [Volta](https://volta.sh/) to install Node.js)

## Installation

gradle/libs.versions.toml

```diff
[versions]
// ...

+ composePwa = "x.x.x" // Please replace with the latest version.

[libraries]
// ...

[plugins]
// ...

+ composePwa = { id = "dev.yuyuyuyuyu.composepwa", version.ref = "composePwa" }
```

composeApp/build.gradle.kts

```diff
// ...

plugins {
    // ...

+   alias(libs.plugins.composePwa)
}
```

## How to use

Just apply the plugin and run the `wasmJsBrowserDistribution` or `jsBrowserDistribution` task as
usual.

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

or

```bash
./gradlew :composeApp:jsBrowserDistribution
```

Your PWA will be generated in `composeApp/build/dist/wasmJs/productionExecutable` or
`composeApp/build/dist/js/productionExecutable`.

## What this plugin does

When you run the `wasmJsBrowserDistribution` task, this plugin automatically does the following:

- Creates these files:
    - `workbox-config-for-wasm.js`
    - `src/webMain/resources/manifest.json`
    - `src/webMain/resources/registerServiceWorker.js`
    - `src/webMain/resources/icons/*`
- Adds the necessary tags to `src/webMain/resources/index.html`.

When you run the `jsBrowserDistribution` task, this plugin automatically does the following:

- Creates these files:
    - `workbox-config-for-js.js`
    - `src/webMain/resources/manifest.json`
    - `src/webMain/resources/registerServiceWorker.js`
    - `src/webMain/resources/icons/*`
- Adds the necessary tags to `src/webMain/resources/index.html`.

## Deploy to GitHub Pages

You can find a sample GitHub Actions workflow for deploying your PWA to GitHub Pages here:

[.github/workflows/deploy-to-github-pages-as-pwa.yml](.github/workflows/deploy-to-github-pages-as-pwa.yml)

And you can check out a live example here:

https://compose-pwa-example.yuyuyuyuyu.dev

## How to customize your PWA

You can edit the following files to customize your PWA:

- `workbox-config.js`
- `src/webMain/resources/manifest.json`
- `src/webMain/resources/icons/*`

## Custom icon

If you want to generate PWA icons from your own icon, you can
use [ngx-pwa-icons](https://github.com/pverhaert/ngx-pwa-icons) like this.

```bash
npx ngx-pwa-icons
```

## Dependencies & Acknowledgments

This plugin depends on the following open-source projects.<br />
Thanks to these projects!

- [Jsoup](https://jsoup.org/) (MIT License) - Used to modify HTML files.
- [Node Gradle Plugin](https://github.com/node-gradle/gradle-node-plugin) (Apache License 2.0) - Used to call the `npx` command.
- [Workbox](https://developer.chrome.com/docs/workbox) / `workbox-cli` (MIT License) - Used via `npx` to generate the Service Worker for the PWA.

## License

Apache License 2.0

```
Copyright 2025 yu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
