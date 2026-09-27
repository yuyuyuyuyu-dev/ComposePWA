# ComposePWA

[![Publish](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/publish.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/publish.yml)
[![Tests](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/tests.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/tests.yml)
[![Lint](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/lint.yml/badge.svg)](https://github.com/yuyuyuyuyu-dev/ComposePWA/actions/workflows/lint.yml)
<a href="https://jetc.dev/issues/273.html"><img src="https://img.shields.io/badge/As_Seen_In-jetc.dev_Newsletter_Issue_%23273-blue?logo=Jetpack+Compose&amp;logoColor=white" alt="As Seen In - jetc.dev Newsletter Issue #273"></a>

This Gradle plugin builds your Compose Multiplatform web app as a progressive web app (PWA).

**Table of Contents**

- [Why it was built](#why-it-was-built)
- [What it does](#what-it-does)
- [How to use](#how-to-use)
- [Tips](#tips)
- [Dependencies & Acknowledgments](#dependencies--acknowledgments)
- [Contributing](#contributing)
- [License](#license)

## Why it was built

最初にCompose Multiplatformで作ったWebアプリをPWA化した時、ビルドするたびに `workbox` コマンドを実行しなければいけないと思うととても憂鬱になったので、それを自動化するためにGradleタスクを定義しました。
2個目のCompose Multiplatform製Webアプリを作った時、今後Webアプリを作るたびにGradleタスクをコピペしてそれぞれで管理しなければならないと思うととても憂鬱になりました。
なのでGradleプラグインを使ってGradleタスクを使いまわせるようにしようと思い立ちました。
そのような経緯でこのプラグインは出来上がりました。

## What it does

When you run the `wasmJsBrowserDistribution` or `jsBrowserDistribution` task, this
plugin automatically does the following:

- Creates `workbox-config-for-wasm.js` / `workbox-config-for-js.js` in the project
  directory.
- Creates `manifest.json`, `registerServiceWorker.js`, and `icons/*` next to your
  `index.html`, skipping every file you already have.
- Adds the necessary tags to your `index.html`.

Each build searches only the resources directories that feed its target for
`index.html` and the files above:

- `wasmJsBrowserDistribution`: `src/webMain/resources`, `src/wasmJsMain/resources`,
  `src/commonMain/resources`
- `jsBrowserDistribution`: `src/webMain/resources`, `src/jsMain/resources`,
  `src/commonMain/resources`

## How to use

### Prerequisites

- Node.js (The author uses [Volta](https://volta.sh/) to install Node.js)

### Installation

gradle/libs.versions.toml

```toml
[versions]
composePwa = "x.x.x" // Please replace with the latest version.

[plugins]
composePwa = { id = "dev.yuyuyuyuyu.composepwa", version.ref = "composePwa" }
```

composeApp/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.composePwa)
}
```

### 実行方法

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

### カスタマイズ方法

You can edit the following files to customize your PWA:

- `workbox-config-for-wasm.js` / `workbox-config-for-js.js`
- `manifest.json` (next to your `index.html` by default)
- `icons/*` (next to your `index.html` by default)

## Tips

### Deploy to GitHub Pages

You can find a sample GitHub Actions workflow for deploying your PWA to GitHub Pages here:

[.github/workflows/deploy-to-github-pages-as-pwa.yml](.github/workflows/deploy-to-github-pages-as-pwa.yml)

And you can check out a live example here:

<https://compose-pwa-example.yuyuyuyuyu.dev>

### Custom icon

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

## Contributing

- **Bug reports and bug-fix PRs are very welcome.**
- **Thinking about a new feature? Please open an issue first.** I'd love to talk
  it through before you write any code — partly to check it fits the "make PWAs
  effortless" goal, and partly because I'm still figuring out what's in scope
  for this project.
- **New features should come with tests**, so that if something breaks later, the
  tests — not my memory — say how it's supposed to behave.

Before opening a pull request, run the auto-fixers and make sure the Lint check
is green:

```bash
npm install && npm run fix        # web assets
./gradlew ktlintFormat            # Kotlin & Gradle scripts
./gradlew -p plugin ktlintFormat
```

What runs (and how) is defined in `package.json` and `.github/workflows/`.

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
