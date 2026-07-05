# Layout test web app

A minimal Compose Multiplatform web app that CI uses to test the ComposePWA plugin
against every `index.html` location the plugin supports.

On purpose, no source set contains an `index.html`: each CI job copies the
[index.html](index.html) template into the resources directory under test and then
builds. Building this project as committed — without copying one first — is the test
for the plugin's missing-index.html error message.

Dependency versions are pinned on purpose; this app is a test fixture, not a sample to
copy (see [example](../../example) for that).
