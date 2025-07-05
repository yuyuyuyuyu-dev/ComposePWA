# ComposePWA

This Gradle plugin provides a Gradle Task, that build your Compose Multiplatform Web App as a PWA.

## Installation

gradle/libs.versions.toml

```diff
[versions]
...

+ yuyuyuyuyu-composePwa = "0.2.0"

[libraries]
...

[plugins]
...

+ yuyuyuyuyu-composePwa = { id = "dev.yuyuyuyuyu.composepwa", version.ref = "yuyuyuyuyu-composePwa" }
```

composeApp/build.gradle.kts

```diff
...

plugins {
    ...

+   alias(libs.plugins.yuyuyuyuyu.composePwa)
}
```

## How to use

Run `./gradlew :composeApp:buildPWA` instead of `./gradlew :composeApp:wasmJsBrowserDistribution`.

Then your app will be output as a PWA in `composeApp/build/dist/wasmJs/productionExecutable`.

`buildPWA` builds your app as a PWA with the following steps:

1. `./gradlew clean`
1. Generates `composeApp/workbox-config.js`, `composeApp/src/wasmJsMain/resources/manifest.json`,
   `composeApp/src/wasmJsMain/resources/registerServiceWorker.js` and `composeApp/src/wasmJsMain/resources/icons/*`, if
   not exists.
1. Add `<script type="application/javascript" src="registerServiceWorker.js"></script>` and
   `<link rel="manifest" href="manifest.json">` to `composeApp/src/wasmJsMain/resources/index.html`, if not exists.
1. `./gradlew wasmJsBrowserDistribution`
1. `npx workbox-cli generateSW workbox-config.js`

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
