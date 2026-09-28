# ComposePWA

[![Publish](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/publish.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/publish.yml)
[![Tests](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/tests.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/tests.yml)
[![Lint](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/lint.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/lint.yml)
<a href="https://jetc.dev/issues/273.html"><img src="https://img.shields.io/badge/As_Seen_In-jetc.dev_Newsletter_Issue_%23273-blue?logo=Jetpack+Compose&amp;logoColor=white" alt="As Seen In - jetc.dev Newsletter Issue #273"></a>

A Gradle plugin that builds your Compose Multiplatform web app as a progressive web app (PWA).

## Table of Contents

- [Why it was built](#why-it-was-built)
- [What it does](#what-it-does)
- [How to use](#how-to-use)
- [Dependencies & Acknowledgments](#dependencies--acknowledgments)
- [Contributing](#contributing)
- [License](#license)

## Why it was built

When I turned my first Compose Multiplatform web app into a PWA, I didn't want to run the `workbox` command after every build, so I wrote a Gradle task to automate it.
But when I made my second app, I didn't want to copy and paste that task into every new web app and maintain each copy separately.
So, I built this plugin.

## What it does

When you run the `wasmJsBrowserDistribution` or `jsBrowserDistribution` task, this plugin does everything needed to turn your web app into a PWA.
Before the build, it automatically creates the required but missing resource files and config file, and adds the necessary tags to your `index.html`.
After the build, it creates the service worker.
The resource files are `manifest.json`, `registerServiceWorker.js`, and `icons/*`, and they are created next to your `index.html`.
The config file is `workbox-config-for-wasm.js` or `workbox-config-for-js.js`, and it is created directly in the project directory.
The service worker has to be recreated on every build, so it is generated directly in the build output directory.

## How to use

### Prerequisites

- Node.js (The author uses [Volta](https://volta.sh/) to install Node.js)

### Installation

gradle/libs.versions.toml

```toml
[versions]
composePwa = "x.x.x" # Please replace with the latest version.

[plugins]
composePwa = { id = "dev.yuyuyuyuyu.composepwa", version.ref = "composePwa" }
```

webApp/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.composePwa)
}
```

### How to run

Just apply the plugin and run the `wasmJsBrowserDistribution` or `jsBrowserDistribution` task as usual.

```bash
./gradlew :webApp:wasmJsBrowserDistribution
```

or

```bash
./gradlew :webApp:jsBrowserDistribution
```

Your PWA will be generated in `webApp/build/dist/wasmJs/productionExecutable` or `webApp/build/dist/js/productionExecutable`.

### How to customize

You can edit the following files to customize your PWA:

- `workbox-config-for-wasm.js` / `workbox-config-for-js.js`
- `manifest.json` (next to your `index.html` by default)
- `icons/*` (next to your `index.html` by default)

### Tips

#### Deploy to GitHub Pages

You can find a sample GitHub Actions workflow for deploying your PWA to GitHub Pages here:

[.github/workflows/deploy-to-github-pages-as-pwa.yml](.github/workflows/deploy-to-github-pages-as-pwa.yml)

And you can check out a live example here:

<https://compose-pwa-example.yuyuyuyuyu.dev>

#### Custom icon

If you want to generate PWA icons from your own icon, you can use [ngx-pwa-icons](https://github.com/pverhaert/ngx-pwa-icons) like this.

```bash
npx ngx-pwa-icons
```

## Dependencies & Acknowledgments

This plugin depends on the following open-source projects.<br />
Thanks to these projects!

- [Jsoup](https://jsoup.org/) (MIT License) - Used to modify HTML files.
- [Node Gradle Plugin](https://github.com/node-gradle/gradle-node-plugin) (Apache License 2.0) - Used to call the `npx` command.
- [Workbox](https://developer.chrome.com/docs/workbox) / `workbox-cli` (MIT License) - Used via `npx` to generate the Service Worker for the PWA.

## Contributing

- **Bug reports and bug-fix PRs are very welcome.**
- **Thinking about a new feature? Please open an issue first.**
  I'd love to talk it through before you write any code — partly to check it fits the "make PWAs effortless" goal, and partly because I'm still figuring out what's in scope for this project.
- **New features should come with tests**, so that if something breaks later, the tests — not my memory — say how it's supposed to behave.

Before opening a pull request, run the auto-fixers and make sure the Lint check is green:

```bash
npm ci
npx prettier --write .
npx eslint --fix .
npx markdownlint-cli2 --fix
./gradlew ktlintFormat versionCatalogFormat
```

See [.github/workflows/lint.yml](.github/workflows/lint.yml) for everything the Lint check runs.

## License

Apache License 2.0

```text
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
