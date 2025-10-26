# ComposePWA

[![Publish](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/publish.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/publish.yml)
[![Tests](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/tests.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/tests.yml)

This Gradle plugin provides a Gradle Task, that build your Compose Multiplatform Web App as a PWA.

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
